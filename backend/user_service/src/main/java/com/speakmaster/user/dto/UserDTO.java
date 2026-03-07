package com.speakmaster.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户DTO
 * 
 * @author SpeakMaster
 */
@Data
public class UserDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户�?
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机�?
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别 (0-未知, 1-�? 2-�?
     */
    private Integer gender;

    /**
     * 生日
     */
    private String birthday;

    /**
     * 个人简�?
     */
    private String bio;

    /**
     * 积分
     */
    private Long points;

    /**
     * 状�?(0-正常, 1-禁用, 2-锁定)
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
