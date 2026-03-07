package com.speakmaster.user.controller;

import com.speakmaster.common.dto.Result;
import com.speakmaster.user.dto.UserDTO;
import com.speakmaster.user.entity.PointsRecord;
import com.speakmaster.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端 - 用户管理控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IUserService userService;

    /**
     * 获取用户列表（分页，支持搜索�?
     */
    @GetMapping
    public Result<Page<UserDTO>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserDTO> users = userService.adminGetUserList(keyword, status, page, size);
        return Result.success(users);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserDTO> createUser(@RequestBody Map<String, Object> request) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername((String) request.get("username"));
        userDTO.setNickname((String) request.get("nickname"));
        userDTO.setEmail((String) request.get("email"));
        userDTO.setPhone((String) request.get("phone"));
        if (request.get("gender") != null) userDTO.setGender((Integer) request.get("gender"));
        if (request.get("status") != null) userDTO.setStatus((Integer) request.get("status"));
        String password = (String) request.getOrDefault("password", "123456");
        UserDTO user = userService.adminCreateUser(userDTO, password);
        return Result.success(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        UserDTO user = userService.adminUpdateUser(id, userDTO);
        return Result.success(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 封禁用户
     */
    @PutMapping("/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        userService.adminBanUser(id);
        return Result.success();
    }

    /**
     * 解封用户
     */
    @PutMapping("/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        userService.adminUnbanUser(id);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newPassword = request.getOrDefault("password", "123456");
        userService.adminResetPassword(id, newPassword);
        return Result.success();
    }

    /**
     * 发放积分
     */
    @PostMapping("/{id}/points")
    public Result<Void> grantPoints(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long points = Long.valueOf(request.get("points").toString());
        String reason = (String) request.get("reason");
        userService.adminGrantPoints(id, points, reason);
        return Result.success();
    }

    /**
     * 查询积分记录
     */
    @GetMapping("/{id}/points/records")
    public Result<Page<PointsRecord>> getPointsRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PointsRecord> records = userService.adminGetPointsRecords(id, page, size);
        return Result.success(records);
    }

    /**
     * 用户统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = userService.adminGetUserStatistics();
        return Result.success(stats);
    }
}
