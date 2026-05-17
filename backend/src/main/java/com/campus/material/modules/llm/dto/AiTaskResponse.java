package com.campus.material.modules.llm.dto;

import lombok.Data;

@Data
public class AiTaskResponse<T> {
    private Long taskId;
    private String status;
    private String source;
    private String bizType;
    private T result;
}
