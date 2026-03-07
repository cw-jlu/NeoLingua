package com.speakmaster.practice.dto;

import lombok.Data;

/**
 * 主题DTO
 * 
 * @author SpeakMaster
 */
@Data
public class ThemeDTO {
    
    /** 主题ID */
    private Long id;
    
    /** 主题名称 */
    private String name;
    
    /** 主题描述 */
    private String description;
    
    /** 封面图片 */
    private String cover;
    
    /** 分类 */
    private String category;
    
    /** 难度等级 (1-5) */
    private Integer difficulty;
    
    /** 标签 */
    private String tags;
    
    /** 使用次数 */
    private Integer useCount;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 状�?(0-草稿 1-已发�? */
    private Integer status;
}
