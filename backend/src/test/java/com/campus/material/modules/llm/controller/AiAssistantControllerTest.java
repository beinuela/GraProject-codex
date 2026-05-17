package com.campus.material.modules.llm.controller;

import com.campus.material.modules.llm.dto.AiTaskResponse;
import com.campus.material.modules.llm.dto.WarningAiAnalysisResult;
import com.campus.material.modules.llm.service.AiAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistantControllerTest {

    @Mock
    private AiAnalysisService aiAnalysisService;

    private AiAssistantController aiAssistantController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aiAssistantController = new AiAssistantController(aiAnalysisService);
    }

    @Test
    void analyzeWarningShouldDelegateToService() {
        AiTaskResponse<WarningAiAnalysisResult> response = new AiTaskResponse<>();
        when(aiAnalysisService.analyzeWarning(11L)).thenReturn(response);

        var apiResponse = aiAssistantController.analyzeWarning(11L);

        verify(aiAnalysisService).analyzeWarning(11L);
        assertSame(response, apiResponse.getData());
    }
}
