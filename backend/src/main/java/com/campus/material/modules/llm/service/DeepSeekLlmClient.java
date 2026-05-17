package com.campus.material.modules.llm.service;

import com.campus.material.modules.llm.config.LlmProperties;
import com.campus.material.modules.llm.dto.DeepSeekChatResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekLlmClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DeepSeekLlmClient(RestClient.Builder restClientBuilder,
                             LlmProperties llmProperties,
                             ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) llmProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) llmProperties.getReadTimeout().toMillis());

        this.restClient = restClientBuilder
                .baseUrl(trimTrailingSlash(llmProperties.getBaseUrl()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    public DeepSeekChatResult chatJson(String systemPrompt, String userPrompt) {
        if (!llmProperties.isEnabled()) {
            throw new IllegalStateException("DeepSeek 功能未启用");
        }
        if (llmProperties.getApiKey() == null || llmProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", llmProperties.getModel());
        requestBody.put("messages", List.of(
                message("system", systemPrompt),
                message("user", userPrompt)
        ));
        requestBody.put("response_format", Map.of("type", "json_object"));
        requestBody.put("max_tokens", llmProperties.getMaxTokens());
        requestBody.put("stream", false);
        requestBody.put("thinking", Map.of("type", llmProperties.isThinkingEnabled() ? "enabled" : "disabled"));

        long startedAt = System.nanoTime();
        String rawResponse = restClient.post()
                .uri("/chat/completions")
                .headers(headers -> headers.setBearerAuth(llmProperties.getApiKey()))
                .body(requestBody)
                .retrieve()
                .body(String.class);
        long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("DeepSeek 返回空响应");
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("DeepSeek 返回空 content");
            }

            DeepSeekChatResult result = new DeepSeekChatResult();
            result.setContent(content);
            result.setPromptTokens(root.path("usage").path("prompt_tokens").asInt(0));
            result.setCompletionTokens(root.path("usage").path("completion_tokens").asInt(0));
            result.setTotalTokens(root.path("usage").path("total_tokens").asInt(0));
            result.setLatencyMs(latencyMs);
            result.setModelName(root.path("model").asText(llmProperties.getModel()));
            return result;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析 DeepSeek 响应失败", ex);
        }
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.deepseek.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
