package com.speakmaster.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息DTO
 * 
 * @author SpeakMaster
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    /** 角色: system / user / assistant */
    private String role;
    /** 消息内容 */
    private String content;
}
