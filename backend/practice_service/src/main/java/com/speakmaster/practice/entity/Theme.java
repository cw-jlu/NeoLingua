package com.speakmaster.practice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 主题实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("theme")
public class Theme extends BaseEntity {

    /** 主题名称 */
    @TableField("name")
    private String name;

    /** 主题描述 */
    @TableField("description")
    private String description;

    /** 主题封面图 */
    @TableField("cover")
    private String cover;

    /** 主题分类 (1-日常, 2-商务, 3-旅行, 4-考试) */
    @TableField("category")
    private Integer category;

    /** 难度等级 (1-初级, 2-中级, 3-高级) */
    @TableField("difficulty")
    private Integer difficulty;

    /** 标签 (逗号分隔) */
    @TableField("tags")
    private String tags;

    /** 使用次数 */
    @TableField("use_count")
    private Long useCount = 0L;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder = 0;

    /** 状态(0-草稿, 1-发布) */
    @TableField("status")
    private Integer status = 0;
}
