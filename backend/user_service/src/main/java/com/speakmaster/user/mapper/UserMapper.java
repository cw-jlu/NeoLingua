package com.speakmaster.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名和删除状态查询用户
     */
    default User selectByUsernameAndDeleted(String username, Integer deleted) {
        return selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, deleted));
    }

    /**
     * 检查用户名是否存在
     */
    default boolean existsByUsernameAndDeleted(String username, Integer deleted) {
        return selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, deleted)) > 0;
    }

    /**
     * 检查邮箱是否存在
     */
    default boolean existsByEmailAndDeleted(String email, Integer deleted) {
        return selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .eq(User::getDeleted, deleted)) > 0;
    }

    /**
     * 根据状态统计用户数
     */
    default long countByStatusAndDeleted(Integer status, Integer deleted) {
        return selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, status)
                .eq(User::getDeleted, deleted));
    }

    /**
     * 根据删除状态统计用户数
     */
    default long countByDeleted(Integer deleted) {
        return selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, deleted));
    }
}
