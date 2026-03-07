package com.speakmaster.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知Mapper
 *
 * @author SpeakMaster
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
