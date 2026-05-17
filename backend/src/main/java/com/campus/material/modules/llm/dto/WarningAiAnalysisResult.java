package com.campus.material.modules.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WarningAiAnalysisResult {
    private String riskLevel;
    private String summary;
    private List<String> possibleCauses;
    private List<String> actions;
    private String ownerRole;
    private Integer deadlineHours;
}
