package com.speakmaster.practice.dto;

import lombok.Data;

/**
 * 角色DTO
 * 
 * @author SpeakMaster
 */
@Data
public class RoleDTO {
    
    /** 角色ID */
    private Long id;
    
    /** 角色名称 */
    private String name;
    
    /** 角色描述 */
    private String description;
    
    /** 角色提示�?*/
    private String prompt;
    
    /** 角色头像 */
    private String avatar;
    
    /** 类型 (0-预制 1-自定�? */
    private Integer type;
    
    /** 创建者ID (自定义角�? */
    private Long userId;
}
