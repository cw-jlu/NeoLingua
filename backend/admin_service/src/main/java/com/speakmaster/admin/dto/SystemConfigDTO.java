package com.speakmaster.admin.dto;

import lombok.Data;

/**
 * 系统配置DTO
 * 
 * @author SpeakMaster
 */
@Data
public class SystemConfigDTO {
    
    /** 配置ID */
    private Long id;
    
    /** 配置�?*/
    private String configKey;
    
    /** 配置�?*/
    private String configValue;
    
    /** 配置描述 */
    private String description;
    
    /** 配置分类 */
    private String category;
    
    /** 是否启用 */
    private Integer isEnabled;
}
