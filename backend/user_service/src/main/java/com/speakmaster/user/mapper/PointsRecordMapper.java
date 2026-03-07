package com.speakmaster.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.user.entity.PointsRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分记录Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    /**
     * 分页查询用户积分记录
     */
    default Page<PointsRecord> selectByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted, Page<PointsRecord> page) {
        return selectPage(page, new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getDeleted, deleted)
                .orderByDesc(PointsRecord::getCreateTime));
    }
}
