package com.campus.material.modules.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTemplateServiceTest {

    @Test
    void buildWarningAnalysisPromptShouldContainJsonHintAndSnapshot() {
        PromptTemplateService service = new PromptTemplateService(new ObjectMapper());

        var prompt = service.buildWarningAnalysisPrompt(Map.of(
                "warning", Map.of("id", 1, "content", "库存不足"),
                "material", Map.of("materialName", "医用口罩")
        ));

        assertTrue(prompt.systemPrompt().toLowerCase().contains("json"));
        assertTrue(prompt.userPrompt().toLowerCase().contains("json"));
        assertTrue(prompt.userPrompt().contains("医用口罩"));
    }
}
