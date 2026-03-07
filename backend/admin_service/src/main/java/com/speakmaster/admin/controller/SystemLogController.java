package com.speakmaster.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.speakmaster.admin.dto.SystemLogDTO;
import com.speakmaster.admin.service.ISystemLogService;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统日志控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final ISystemLogService logService;

    /**
     * 分页查询日志（支持按模块、用户ID筛选）
     */
    @GetMapping
    public Result<IPage<SystemLogDTO>> getLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(logService.getLogs(module, userId, page, size));
    }

    /**
     * 获取日志详情
     */
    @GetMapping("/{id}")
    public Result<SystemLogDTO> getLogById(@PathVariable Long id) {
        return Result.success(logService.getLogById(id));
    }

    /**
     * 删除日志
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteLog(@PathVariable Long id) {
        logService.deleteLog(id);
        return Result.success();
    }

    /**
     * 日志统计（按模块分组�?
     */
    @GetMapping("/statistics")
    public Result<Map<String, Long>> getStatistics() {
        return Result.success(logService.getLogStatistics());
    }

    /**
     * 导出日志
     */
    @PostMapping("/export")
    public Result<List<SystemLogDTO>> exportLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long userId) {
        return Result.success(logService.exportLogs(module, userId));
    }
}
