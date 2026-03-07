package com.speakmaster.user.controller;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.dto.Result;
import com.speakmaster.user.dto.UserDTO;
import com.speakmaster.user.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public Result<UserDTO> getProfile(@RequestHeader("X-User-Id") Long userId) {
        log.info("获取用户信息: userId={}", userId);
        UserDTO user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/profile")
    public Result<UserDTO> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                         @Valid @RequestBody UserDTO userDTO) {
        log.info("更新用户信息: userId={}", userId);
        UserDTO user = userService.updateUser(userId, userDTO);
        return Result.success("更新成功", user);
    }

    /**
     * 删除当前用户
     */
    @DeleteMapping("/profile")
    public Result<Void> deleteProfile(@RequestHeader("X-User-Id") Long userId) {
        log.info("删除用户: userId={}", userId);
        userService.deleteUser(userId);
        return Result.success("删除成功", null);
    }

    /**
     * 获取积分信息
     */
    @GetMapping("/points")
    public Result<Long> getPoints(@RequestHeader("X-User-Id") Long userId) {
        log.info("获取用户积分: userId={}", userId);
        UserDTO user = userService.getUserById(userId);
        return Result.success(user.getPoints());
    }
}
