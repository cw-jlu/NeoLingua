package com.speakmaster.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.practice.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
