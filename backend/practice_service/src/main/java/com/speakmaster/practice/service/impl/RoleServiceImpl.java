package com.speakmaster.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.practice.dto.RoleDTO;
import com.speakmaster.practice.entity.Role;
import com.speakmaster.practice.mapper.RoleMapper;
import com.speakmaster.practice.service.IRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色服务实现类
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    @Override
    @Cacheable(value = "role_list", key = "#page + '_' + #size + '_' + #userId")
    public Page<RoleDTO> getRoleList(int page, int size, Long userId) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            // 查询用户自定义角色
            wrapper.eq(Role::getCreatorId, userId)
                    .eq(Role::getType, 2)
                    .eq(Role::getDeleted, 0)
                    .orderByDesc(Role::getCreateTime);
        } else {
            // 查询所有启用的角色
            wrapper.eq(Role::getStatus, 1)
                    .eq(Role::getDeleted, 0)
                    .orderByDesc(Role::getUseCount);
        }
        
        Page<Role> pageRequest = new Page<>(page + 1, size);
        Page<Role> roles = this.page(pageRequest, wrapper);
        
        return (Page<RoleDTO>) roles.convert(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "role", key = "#roleId")
    public RoleDTO getRoleById(Long roleId) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getId, roleId)
                .eq(Role::getDeleted, 0);
        Role role = this.getOne(wrapper);
        
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return convertToDTO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role_list", allEntries = true)
    public RoleDTO createRole(RoleDTO roleDTO, Long userId) {
        Role role = new Role();
        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());
        role.setSetting(roleDTO.getPrompt());
        role.setAvatar(roleDTO.getAvatar());
        role.setType(2); // 自定义角色
        role.setCreatorId(userId);

        this.save(role);
        log.info("创建自定义角色成功: roleId={}, userId={}", role.getId(), userId);
        return convertToDTO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"role", "role_list"}, allEntries = true)
    public RoleDTO updateRole(Long roleId, RoleDTO roleDTO, Long userId) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getId, roleId)
                .eq(Role::getDeleted, 0);
        Role role = this.getOne(wrapper);
        
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 检查权限:只能修改自己的自定义角色
        if (role.getType() == 2 && !role.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (roleDTO.getName() != null) role.setName(roleDTO.getName());
        if (roleDTO.getDescription() != null) role.setDescription(roleDTO.getDescription());
        if (roleDTO.getPrompt() != null) role.setSetting(roleDTO.getPrompt());
        if (roleDTO.getAvatar() != null) role.setAvatar(roleDTO.getAvatar());

        this.updateById(role);
        log.info("更新角色成功: roleId={}", roleId);
        return convertToDTO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"role", "role_list"}, allEntries = true)
    public void deleteRole(Long roleId, Long userId) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getId, roleId)
                .eq(Role::getDeleted, 0);
        Role role = this.getOne(wrapper);
        
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 检查权限:只能删除自己的自定义角色
        if (role.getType() == 2 && !role.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        role.markDeleted();
        this.updateById(role);
        log.info("删除角色成功: roleId={}", roleId);
    }

    /**
     * 转换为DTO
     */
    private RoleDTO convertToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setPrompt(role.getSetting());
        dto.setAvatar(role.getAvatar());
        dto.setType(role.getType());
        dto.setUserId(role.getCreatorId());
        return dto;
    }
}
