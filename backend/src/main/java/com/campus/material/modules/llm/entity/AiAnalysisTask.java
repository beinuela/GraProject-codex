package com.campus.material.modules.llm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_analysis_task")
public class AiAnalysisTask extends BaseEntity {
    private String bizType;
    private Long bizId;
    private String requestSnapshot;
    private String status;
    private String resultSource;
    private String resultJson;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
