package com.speakmaster.admin.service;

import com.speakmaster.admin.dto.DashboardDTO;

/**
 * 仪表盘服务接口
 */
public interface IDashboardService {

    /**
     * 获取仪表盘数据
     */
    DashboardDTO getDashboardData();
}
