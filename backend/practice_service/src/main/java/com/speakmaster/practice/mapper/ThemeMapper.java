package com.speakmaster.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.practice.entity.Theme;
import org.apache.ibatis.annotations.Mapper;

/**
 * 主题Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface ThemeMapper extends BaseMapper<Theme> {
}
