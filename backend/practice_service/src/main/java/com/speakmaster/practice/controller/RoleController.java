package com.speakmaster.practice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.practice.dto.RoleDTO;
import com.speakmaster.practice.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 角色控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleService;

    /**
     * 获取角色列表 (用户端)
     */
    @GetMapping("/user/practice/roles")
    public Result<Page<RoleDTO>> getRoleList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        Page<RoleDTO> roles = roleService.getRoleList(page, size, userId);
        return Result.success(roles);
    }

    /**
     * 获取角色详情 (用户端)
     */
    @GetMapping("/user/practice/roles/{id}")
    public Result<RoleDTO> getRoleById(@PathVariable Long id) {
        RoleDTO role = roleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 创建自定义角色(用户端)
     */
    @PostMapping("/user/practice/roles")
    public Result<RoleDTO> createRole(
            @RequestBody RoleDTO roleDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        RoleDTO role = roleService.createRole(roleDTO, userId);
        return Result.success(role);
    }

    /**
     * 更新角色 (用户端)
     */
    @PutMapping("/user/practice/roles/{id}")
    public Result<RoleDTO> updateRole(
            @PathVariable Long id,
            @RequestBody RoleDTO roleDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        RoleDTO role = roleService.updateRole(id, roleDTO, userId);
        return Result.success(role);
    }

    /**
     * 删除角色 (用户端)
     */
    @DeleteMapping("/user/practice/roles/{id}")
    public Result<Void> deleteRole(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        roleService.deleteRole(id, userId);
        return Result.success();
    }

    /**
     * 创建预制角色 (管理端)
     */
    @PostMapping("/admin/practice/roles")
    public Result<RoleDTO> createPresetRole(@RequestBody RoleDTO roleDTO) {
        RoleDTO role = roleService.createRole(roleDTO, null);
        return Result.success(role);
    }

    /**
     * 更新角色 (管理端)
     */
    @PutMapping("/admin/practice/roles/{id}")
    public Result<RoleDTO> updateRoleAdmin(
            @PathVariable Long id,
            @RequestBody RoleDTO roleDTO) {
        RoleDTO role = roleService.updateRole(id, roleDTO, null);
        return Result.success(role);
    }

    /**
     * 删除角色 (管理端)
     */
    @DeleteMapping("/admin/practice/roles/{id}")
    public Result<Void> deleteRoleAdmin(@PathVariable Long id) {
        roleService.deleteRole(id, null);
        return Result.success();
    }
}
