package com.speakmaster.admin.service.impl;

import com.speakmaster.admin.dto.DashboardDTO;
import com.speakmaster.admin.service.IDashboardService;
import com.speakmaster.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * 仪表盘服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final RedisUtil redisUtil;

    @Override
    public DashboardDTO getDashboardData() {
        DashboardDTO dashboard = new DashboardDTO();

        // 从Redis获取统计数据
        dashboard.setTotalUsers(getCountFromRedis("stats:total_users"));
        dashboard.setTodayNewUsers(getCountFromRedis("stats:today_new_users"));
        dashboard.setTotalPosts(getCountFromRedis("stats:total_posts"));
        dashboard.setTodayNewPosts(getCountFromRedis("stats:today_new_posts"));
        dashboard.setTotalMeetings(getCountFromRedis("stats:total_meetings"));
        dashboard.setActiveMeetings(getCountFromRedis("stats:active_meetings"));
        dashboard.setTotalSessions(getCountFromRedis("stats:total_sessions"));
        dashboard.setTodaySessions(getCountFromRedis("stats:today_sessions"));

        // 获取系统信息
        dashboard.setUptime(getSystemUptime());
        dashboard.setCpuUsage(getCpuUsage());
        dashboard.setMemoryUsage(getMemoryUsage());

        return dashboard;
    }

    /**
     * 从Redis获取统计数据
     */
    private Long getCountFromRedis(String key) {
        String value = redisUtil.get(key, String.class);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 获取系统运行时间
     */
    private String getSystemUptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long days = uptime / (1000 * 60 * 60 * 24);
        long hours = (uptime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%d天%d小时%d分钟", days, hours, minutes);
    }

    /**
     * 获取CPU使用率
     */
    private Double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        return osBean.getSystemLoadAverage();
    }

    /**
     * 获取内存使用率
     */
    private Double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return (double) (totalMemory - freeMemory) / totalMemory * 100;
    }
}
