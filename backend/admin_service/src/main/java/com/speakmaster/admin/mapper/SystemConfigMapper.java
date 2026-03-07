package com.speakmaster.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.admin.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SystemConfig Mapper接口
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 查询所有不重复的配置分类
     */
    @Select("SELECT DISTINCT category FROM system_config WHERE deleted = 0 AND category IS NOT NULL")
    List<String> selectDistinctCategories();
}
