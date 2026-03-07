package com.speakmaster.meeting.entity;

import com.speakmaster.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对话总结实体(中期记忆)
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversation_summary")
public class ConversationSummary extends BaseEntity {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 会话ID(meeting或practice)
     */
    private String sessionId;
    
    /**
     * 会话类型: meeting/practice
     */
    private String sessionType;
    
    /**
     * 对话总结内容
     */
    private String summary;
    
    /**
     * 关键词(逗号分隔)
     */
    private String keywords;
    
    /**
     * 对话开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 对话结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
}
