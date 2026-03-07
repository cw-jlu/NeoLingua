package com.speakmaster.notification.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {

    /** 接收者ID */
    @TableField("receiver_id")
    private Long receiverId;

    /** 发送者ID (系统通知为null) */
    @TableField("sender_id")
    private Long senderId;

    /** 通知类型 (0-系统 1-点赞 2-评论 3-关注 4-私信) */
    @TableField("type")
    private Integer type;

    /** 标题 */
    @TableField("title")
    private String title;

    /** 内容 */
    @TableField("content")
    private String content;

    /** 关联ID (帖子ID、评论ID等) */
    @TableField("related_id")
    private Long relatedId;

    /** 是否已读 (0-未读 1-已读) */
    @TableField("is_read")
    private Integer isRead = 0;

    /** 读取时间 */
    @TableField("read_time")
    private String readTime;
}
