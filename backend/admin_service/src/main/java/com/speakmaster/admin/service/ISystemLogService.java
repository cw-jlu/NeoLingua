package com.speakmaster.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.admin.dto.SystemLogDTO;
import com.speakmaster.admin.entity.SystemLog;

import java.util.List;
import java.util.Map;

/**
 * 系统日志服务接口
 */
public interface ISystemLogService extends IService<SystemLog> {

    /**
     * 分页查询日志（支持按模块、用户ID筛选）
     */
    IPage<SystemLogDTO> getLogs(String module, Long userId, int page, int size);

    /**
     * 获取日志详情
     */
    SystemLogDTO getLogById(Long id);

    /**
     * 逻辑删除日志
     */
    void deleteLog(Long id);

    /**
     * 日志统计（按模块分组）
     */
    Map<String, Long> getLogStatistics();

    /**
     * 导出日志（返回列表）
     */
    List<SystemLogDTO> exportLogs(String module, Long userId);
}
