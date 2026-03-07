package com.speakmaster.meeting.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakmaster.meeting.entity.Meeting;
import com.speakmaster.meeting.entity.MeetingMessage;
import com.speakmaster.meeting.mapper.MeetingMapper;
import com.speakmaster.meeting.mapper.MeetingMessageMapper;
import com.speakmaster.meeting.service.IMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆总结定时任务
 * 自动为已结束的会话生成对话总结
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemorySummaryTask {

    private final MeetingMapper meetingMapper;
    private final MeetingMessageMapper messageMapper;
    private final IMemoryService memoryService;

    /**
     * 每天凌晨2点执行,为昨天结束的会话生成总结
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailySummaries() {
        log.info("开始生成每日对话总结...");
        
        try {
            // 查询昨天结束的会议
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime today = LocalDateTime.now();
            
            LambdaQueryWrapper<Meeting> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Meeting::getStatus, 1) // 已结束
                    .between(Meeting::getUpdateTime, yesterday, today)
                    .eq(Meeting::getDeleted, 0);
            
            List<Meeting> meetings = meetingMapper.selectList(wrapper);
            
            int successCount = 0;
            for (Meeting meeting : meetings) {
                try {
                    // 检查是否有消息
                    LambdaQueryWrapper<MeetingMessage> msgWrapper = new LambdaQueryWrapper<>();
                    msgWrapper.eq(MeetingMessage::getMeetingId, meeting.getId())
                              .eq(MeetingMessage::getDeleted, 0);
                    long messageCount = messageMapper.selectCount(msgWrapper);
                    
                    if (messageCount < 5) {
                        log.debug("会议消息太少,跳过: meetingId={}, count={}", meeting.getId(), messageCount);
                        continue;
                    }
                    
                    // 生成总结
                    String sessionId = "meeting-" + meeting.getId();
                    String summary = memoryService.generateConversationSummary(sessionId, "meeting");
                    
                    // 保存总结
                    memoryService.saveMidTermMemory(
                        meeting.getCreatorId(),
                        sessionId,
                        "meeting",
                        summary,
                        extractKeywords(summary),
                        (int) messageCount
                    );
                    
                    successCount++;
                    log.debug("会议总结生成成功: meetingId={}", meeting.getId());
                    
                    // 避免频繁调用AI
                    Thread.sleep(2000);
                } catch (Exception e) {
                    log.error("生成会议总结失败: meetingId={}", meeting.getId(), e);
                }
            }
            
            log.info("每日对话总结生成完成: 总数={}, 成功={}", meetings.size(), successCount);
        } catch (Exception e) {
            log.error("生成每日对话总结失败", e);
        }
    }

    /**
     * 从总结中提取关键词(简单实现)
     */
    private String extractKeywords(String summary) {
        // TODO: 可以使用NLP工具提取关键词
        // 这里简单实现:取前50个字符作为关键词
        if (summary == null || summary.isEmpty()) {
            return "";
        }
        return summary.length() > 50 ? summary.substring(0, 50) : summary;
    }
}
