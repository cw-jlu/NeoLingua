package com.speakmaster.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.speakmaster.admin.dto.SystemLogDTO;
import com.speakmaster.admin.entity.SystemLog;
import com.speakmaster.admin.mapper.SystemLogMapper;
import com.speakmaster.admin.service.ISystemLogService;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl extends ServiceImpl<SystemLogMapper, SystemLog> implements ISystemLogService {

    @Override
    public IPage<SystemLogDTO> getLogs(String module, Long userId, int page, int size) {
        Page<SystemLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        
        if (module != null && !module.isEmpty()) {
            wrapper.eq(SystemLog::getModule, module);
        }
        if (userId != null) {
            wrapper.eq(SystemLog::getUserId, userId);
        }
        wrapper.eq(SystemLog::getDeleted, 0)
                .orderByDesc(SystemLog::getCreateTime);

        IPage<SystemLog> logPage = this.page(pageParam, wrapper);
        return logPage.convert(this::convertToDTO);
    }

    @Override
    public SystemLogDTO getLogById(Long id) {
        SystemLog systemLog = this.getById(id);
        if (systemLog == null || systemLog.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.LOG_NOT_FOUND);
        }
        return convertToDTO(systemLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLog(Long id) {
        SystemLog systemLog = this.getById(id);
        if (systemLog == null || systemLog.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.LOG_NOT_FOUND);
        }
        systemLog.markDeleted();
        this.updateById(systemLog);
        log.info("删除系统日志: id={}", id);
    }

    @Override
    public Map<String, Long> getLogStatistics() {
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemLog::getDeleted, 0);
        List<SystemLog> allLogs = this.list(wrapper);
        
        return allLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getModule() != null ? l.getModule() : "unknown",
                        Collectors.counting()
                ));
    }

    @Override
    public List<SystemLogDTO> exportLogs(String module, Long userId) {
        IPage<SystemLogDTO> logs = getLogs(module, userId, 0, Integer.MAX_VALUE);
        return logs.getRecords();
    }

    /**
     * 实体转DTO
     */
    private SystemLogDTO convertToDTO(SystemLog systemLog) {
        SystemLogDTO dto = new SystemLogDTO();
        dto.setId(systemLog.getId());
        dto.setUserId(systemLog.getUserId());
        dto.setModule(systemLog.getModule());
        dto.setOperation(systemLog.getOperation());
        dto.setDescription(systemLog.getDescription());
        dto.setMethod(systemLog.getMethod());
        dto.setUrl(systemLog.getUrl());
        dto.setParams(systemLog.getParams());
        dto.setIp(systemLog.getIp());
        dto.setExecutionTime(systemLog.getExecutionTime());
        dto.setStatus(systemLog.getStatus());
        dto.setErrorMsg(systemLog.getErrorMsg());
        dto.setCreateTime(systemLog.getCreateTime());
        return dto;
    }
}
