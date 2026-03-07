package com.speakmaster.meeting.entity;

import com.speakmaster.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户画像实体(长期记忆)
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_profile")
public class UserProfile extends BaseEntity {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 英语水平: beginner/intermediate/advanced
     */
    private String englishLevel;
    
    /**
     * 学习目标
     */
    private String learningGoals;
    
    /**
     * 兴趣话题(JSON数组)
     */
    private String interests;
    
    /**
     * 常见错误类型(JSON数组)
     */
    private String commonMistakes;
    
    /**
     * 发音弱点(JSON数组)
     */
    private String pronunciationWeaknesses;
    
    /**
     * 偏好的学习风格
     */
    private String learningStyle;
    
    /**
     * 总练习时长(分钟)
     */
    private Integer totalPracticeMinutes;
    
    /**
     * 总对话轮数
     */
    private Integer totalConversations;
    
    /**
     * 个性化提示词
     */
    private String personalizedPrompt;
}
