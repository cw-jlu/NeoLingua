package com.speakmaster.user.service;

import com.speakmaster.user.dto.LoginRequest;
import com.speakmaster.user.dto.RegisterRequest;
import com.speakmaster.user.dto.UserDTO;
import com.speakmaster.user.entity.PointsRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

/**
 * 用户服务接口
 * 
 * @author SpeakMaster
 */
public interface IUserService {

    /**
     * 用户注册
     */
    UserDTO register(RegisterRequest request);

    /**
     * 用户登录
     */
    String login(LoginRequest request, String ip);

    /**
     * 根据ID获取用户信息
     */
    UserDTO getUserById(Long userId);

    /**
     * 更新用户信息
     */
    UserDTO updateUser(Long userId, UserDTO userDTO);

    /**
     * 删除用户 (逻辑删除)
     */
    void deleteUser(Long userId);

    /**
     * 增加积分
     */
    void addPoints(Long userId, Long points, String reason);

    /**
     * 扣除积分
     */
    void deductPoints(Long userId, Long points, String reason);

    // ==================== 管理端方法 ====================

    /**
     * [管理端] 分页查询用户列表
     */
    Page<UserDTO> adminGetUserList(String keyword, Integer status, int page, int size);

    /**
     * [管理端] 创建用户
     */
    UserDTO adminCreateUser(UserDTO userDTO, String password);

    /**
     * [管理端] 更新用户信息
     */
    UserDTO adminUpdateUser(Long userId, UserDTO userDTO);

    /**
     * [管理端] 封禁用户
     */
    void adminBanUser(Long userId);

    /**
     * [管理端] 解封用户
     */
    void adminUnbanUser(Long userId);

    /**
     * [管理端] 重置密码
     */
    void adminResetPassword(Long userId, String newPassword);

    /**
     * [管理端] 发放积分
     */
    void adminGrantPoints(Long userId, Long points, String reason);

    /**
     * [管理端] 查询积分记录
     */
    Page<PointsRecord> adminGetPointsRecords(Long userId, int page, int size);

    /**
     * [管理端] 用户统计
     */
    Map<String, Object> adminGetUserStatistics();
}
