package com.speakmaster.admin.controller;

import com.speakmaster.admin.dto.DashboardDTO;
import com.speakmaster.admin.service.IDashboardService;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    /**
     * 获取仪表盘完整数�?
     */
    @GetMapping
    public Result<DashboardDTO> getDashboard() {
        return Result.success(dashboardService.getDashboardData());
    }

    /**
     * 获取概览数据（用户数、帖子数等业务指标）
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        DashboardDTO data = dashboardService.getDashboardData();
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalUsers", data.getTotalUsers());
        overview.put("todayNewUsers", data.getTodayNewUsers());
        overview.put("totalPosts", data.getTotalPosts());
        overview.put("todayNewPosts", data.getTodayNewPosts());
        overview.put("totalMeetings", data.getTotalMeetings());
        overview.put("activeMeetings", data.getActiveMeetings());
        overview.put("totalSessions", data.getTotalSessions());
        overview.put("todaySessions", data.getTodaySessions());
        return Result.success(overview);
    }

    /**
     * 获取实时系统数据（CPU、内存等�?
     */
    @GetMapping("/realtime")
    public Result<Map<String, Object>> getRealtime() {
        DashboardDTO data = dashboardService.getDashboardData();
        Map<String, Object> realtime = new HashMap<>();
        realtime.put("uptime", data.getUptime());
        realtime.put("cpuUsage", data.getCpuUsage());
        realtime.put("memoryUsage", data.getMemoryUsage());
        return Result.success(realtime);
    }
}
