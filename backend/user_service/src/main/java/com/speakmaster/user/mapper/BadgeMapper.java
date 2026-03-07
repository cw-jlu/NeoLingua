package com.speakmaster.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.user.entity.Badge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 徽章Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface BadgeMapper extends BaseMapper<Badge> {

    /**
     * 查询所有启用的徽章
     */
    default List<Badge> selectByStatusAndDeletedOrderBySortOrderAsc(Integer status, Integer deleted) {
        return selectList(new LambdaQueryWrapper<Badge>()
                .eq(Badge::getStatus, status)
                .eq(Badge::getDeleted, deleted)
                .orderByAsc(Badge::getSortOrder));
    }

    /**
     * 根据类型查询徽章
     */
    default List<Badge> selectByTypeAndStatusAndDeletedOrderBySortOrderAsc(Integer type, Integer status, Integer deleted) {
        return selectList(new LambdaQueryWrapper<Badge>()
                .eq(Badge::getType, type)
                .eq(Badge::getStatus, status)
                .eq(Badge::getDeleted, deleted)
                .orderByAsc(Badge::getSortOrder));
    }
}
