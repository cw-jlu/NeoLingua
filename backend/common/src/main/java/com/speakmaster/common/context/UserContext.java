package com.speakmaster.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户上下文信息
 * 
 * @author SpeakMaster
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
}
