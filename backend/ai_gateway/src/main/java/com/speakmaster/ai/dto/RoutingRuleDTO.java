package com.speakmaster.ai.dto;

import lombok.Data;
import java.util.List;

/**
 * 路由规则DTO
 * 
 * @author SpeakMaster
 */
@Data
public class RoutingRuleDTO {
    private Long id;
    private String name;
    private String strategy;
    private Boolean enabled;
    private Integer priority;
    private String description;
    private List<Long> modelIds;
}
