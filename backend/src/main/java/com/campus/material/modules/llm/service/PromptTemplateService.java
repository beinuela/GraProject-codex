package com.campus.material.modules.llm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptTemplateService {

    private final ObjectMapper objectMapper;

    public PromptTemplateService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiPrompt buildWarningAnalysisPrompt(Map<String, Object> snapshot) {
        try {
            String snapshotJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
            String systemPrompt = """
                    你是校园物资管理辅助决策助手。
                    你必须严格基于输入数据输出 json，不得编造不存在的库存、仓库、批次或审批信息。
                    你不能输出 markdown，不能输出解释文字，不能输出 SQL，不能直接给出审批结论。
                    你只允许输出一个合法 json 对象，结构如下：
                    {
                      "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                      "summary": "string",
                      "possibleCauses": ["string"],
                      "actions": ["string"],
                      "ownerRole": "ADMIN|WAREHOUSE_ADMIN|APPROVER",
                      "deadlineHours": 24
                    }
                    """;
            String userPrompt = """
                    请阅读下面的校园物资预警上下文 json，并输出一个用于处置建议的 json 结果。
                    要求：
                    1. 只能基于输入 json 分析。
                    2. possibleCauses 输出 1 到 3 条。
                    3. actions 输出 2 到 4 条，内容要具体且可执行。
                    4. ownerRole 只能从 ADMIN、WAREHOUSE_ADMIN、APPROVER 中选择。
                    5. deadlineHours 输出 4 到 168 之间的整数。
                    6. 如果信息不足，请在 summary 中明确说明信息不足，但仍要给出保守建议。
                    7. 只输出 json。

                    输入 json:
                    %s
                    """.formatted(snapshotJson);
            return new AiPrompt("WARNING_ANALYSIS_V1", systemPrompt, userPrompt);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("构建预警 AI 提示词失败", ex);
        }
    }

    public record AiPrompt(String templateCode, String systemPrompt, String userPrompt) {
    }
}
