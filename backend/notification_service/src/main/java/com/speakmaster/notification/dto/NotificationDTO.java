package com.speakmaster.notification.dto;

import lombok.Data;

/**
 * 通知DTO
 * 
 * @author SpeakMaster
 */
@Data
public class NotificationDTO {
    
    /** 通知ID */
    private Long id;
    
    /** 接收者ID */
    private Long receiverId;
    
    /** 发送者ID */
    private Long senderId;
    
    /** 发送者昵�?*/
    private String senderNickname;
    
    /** 发送者头�?*/
    private String senderAvatar;
    
    /** 通知类型 */
    private Integer type;
    
    /** 标题 */
    private String title;
    
    /** 内容 */
    private String content;
    
    /** 关联ID */
    private Long relatedId;
    
    /** 是否已读 */
    private Integer isRead;
    
    /** 读取时间 */
    private String readTime;
    
    /** 创建时间 */
    private String createTime;
}
