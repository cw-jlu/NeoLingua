package com.speakmaster.practice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.practice.dto.RoleDTO;
import com.speakmaster.practice.entity.Role;

/**
 * 角色服务接口
 * 
 * @author SpeakMaster
 */
public interface IRoleService extends IService<Role> {

    /**
     * 分页查询角色列表
     */
    Page<RoleDTO> getRoleList(int page, int size, Long userId);

    /**
     * 获取角色详情
     */
    RoleDTO getRoleById(Long roleId);

    /**
     * 创建自定义角色
     */
    RoleDTO createRole(RoleDTO roleDTO, Long userId);

    /**
     * 更新角色
     */
    RoleDTO updateRole(Long roleId, RoleDTO roleDTO, Long userId);

    /**
     * 删除角色
     */
    void deleteRole(Long roleId, Long userId);
}
