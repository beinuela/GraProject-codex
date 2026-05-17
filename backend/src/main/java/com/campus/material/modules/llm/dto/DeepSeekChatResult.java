package com.campus.material.modules.llm.dto;

import lombok.Data;

@Data
public class DeepSeekChatResult {
    private String content;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private long latencyMs;
    private String modelName;
}
