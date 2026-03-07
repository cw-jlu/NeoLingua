package com.speakmaster.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式聊天事件DTO
 * 
 * @author SpeakMaster
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamEvent {
    /** 事件类型: content / done / error */
    private String type;
    /** 内容片段 */
    private String content;
    /** 使用的模型ID */
    private Long modelId;
}
