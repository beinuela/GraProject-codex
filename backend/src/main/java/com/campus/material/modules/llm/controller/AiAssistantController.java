package com.campus.material.modules.llm.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.llm.dto.AiTaskResponse;
import com.campus.material.modules.llm.dto.WarningAiAnalysisResult;
import com.campus.material.modules.llm.service.AiAnalysisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAnalysisService aiAnalysisService;

    public AiAssistantController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/warnings/{id}/analyze")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<AiTaskResponse<WarningAiAnalysisResult>> analyzeWarning(@PathVariable Long id) {
        return ApiResponse.ok(aiAnalysisService.analyzeWarning(id));
    }
}
