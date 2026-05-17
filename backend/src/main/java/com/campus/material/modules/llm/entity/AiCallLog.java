package com.campus.material.modules.llm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_call_log")
public class AiCallLog extends BaseEntity {
    private Long taskId;
    private String providerName;
    private String modelName;
    private String promptTemplateCode;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long latencyMs;
    private Integer successFlag;
    private String errorMessage;
}
