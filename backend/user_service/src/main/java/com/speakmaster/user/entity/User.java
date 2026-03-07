package com.speakmaster.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码 (加密后)
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 性别 (0-未知, 1-男, 2-女)
     */
    @TableField("gender")
    private Integer gender = 0;

    /**
     * 生日
     */
    @TableField("birthday")
    private String birthday;

    /**
     * 个人简介
     */
    @TableField("bio")
    private String bio;

    /**
     * 积分
     */
    @TableField("points")
    private Long points = 0L;

    /**
     * 状态(0-正常, 1-禁用, 2-锁定)
     */
    @TableField("status")
    private Integer status = 0;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private String lastLoginTime;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;
}
