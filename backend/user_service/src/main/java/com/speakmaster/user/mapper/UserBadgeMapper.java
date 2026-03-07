package com.speakmaster.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.user.entity.UserBadge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户徽章Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface UserBadgeMapper extends BaseMapper<UserBadge> {

    /**
     * 查询用户的所有徽章
     */
    default List<UserBadge> selectByUserIdAndDeleted(Long userId, Integer deleted) {
        return selectList(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getDeleted, deleted));
    }

    /**
     * 查询用户是否拥有某个徽章
     */
    default UserBadge selectByUserIdAndBadgeIdAndDeleted(Long userId, Long badgeId, Integer deleted) {
        return selectOne(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeId, badgeId)
                .eq(UserBadge::getDeleted, deleted));
    }

    /**
     * 判断用户是否拥有某个徽章
     */
    default boolean existsByUserIdAndBadgeIdAndDeleted(Long userId, Long badgeId, Integer deleted) {
        return selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeId, badgeId)
                .eq(UserBadge::getDeleted, deleted)) > 0;
    }
}
