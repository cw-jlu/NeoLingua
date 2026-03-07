package com.speakmaster.admin.dto;

import lombok.Data;

/**
 * 仪表盘DTO
 * 
 * @author SpeakMaster
 */
@Data
public class DashboardDTO {
    
    /** 总用户数 */
    private Long totalUsers;
    
    /** 今日新增用户 */
    private Long todayNewUsers;
    
    /** 总帖子数 */
    private Long totalPosts;
    
    /** 今日新增帖子 */
    private Long todayNewPosts;
    
    /** 总Meeting�?*/
    private Long totalMeetings;
    
    /** 进行中的Meeting */
    private Long activeMeetings;
    
    /** 总练习会话数 */
    private Long totalSessions;
    
    /** 今日练习会话 */
    private Long todaySessions;
    
    /** 系统运行时间 */
    private String uptime;
    
    /** CPU使用�?*/
    private Double cpuUsage;
    
    /** 内存使用�?*/
    private Double memoryUsage;
}
