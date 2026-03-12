package com.ose.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnthropicProviderClient implements AiProviderClient {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AiProviderType provider() {
        return AiProviderType.ANTHROPIC;
    }

    @Override
    public boolean isConfigured() {
        return properties.getAi().getAnthropic().getApiKey() != null && !properties.getAi().getAnthropic().getApiKey().isBlank();
    }

    @Override
    public List<AiQuestionDtos.AiModelConfig> models() {
        String modelStr = properties.getAi().getAnthropic().getModels();
        String defaultModel = properties.getAi().getAnthropic().getDefaultModel();
        List<AiQuestionDtos.AiModelConfig> models = new ArrayList<>();
        for (String item : modelStr.split(",")) {
            String model = item.trim();
            if (!model.isBlank()) {
                models.add(new AiQuestionDtos.AiModelConfig(model, model, model.equals(defaultModel)));
            }
        }
        return models;
    }

    @Override
    public AiQuestionDtos.ProviderGenerationPayload generate(AiQuestionDtos.AiQuestionGenerationRequest request,
                                                             String systemPrompt,
                                                             String userPrompt,
                                                             String jsonSchema) {
        if (!isConfigured()) {
            throw new AiProviderException("Anthropic 未配置 API Key");
        }
        String model = request.model() == null || request.model().isBlank()
                ? properties.getAi().getAnthropic().getDefaultModel() : request.model();
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("system", systemPrompt + "\n请严格符合以下 JSON Schema: " + jsonSchema);
        payload.put("temperature", properties.getAi().getAnthropic().getTemperature());
        payload.put("max_tokens", properties.getAi().getAnthropic().getMaxTokens());
        payload.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        RestClient client = RestClient.builder()
                .baseUrl(properties.getAi().getAnthropic().getBaseUrl())
                .defaultHeader("x-api-key", properties.getAi().getAnthropic().getApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .requestFactory(requestFactory())
                .build();
        int maxRetries = Math.max(0, properties.getAi().getMaxRetries());
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                JsonNode response = client.post()
                        .uri("/v1/messages")
                        .body(payload)
                        .retrieve()
                        .body(JsonNode.class);
                String text = extractAnthropicJson(response);
                return objectMapper.readValue(text, AiQuestionDtos.ProviderGenerationPayload.class);
            } catch (RestClientResponseException ex) {
                if (attempt == maxRetries || ex.getStatusCode().value() < 500) {
                    throw mapException(ex);
                }
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    throw new AiProviderException("Claude 返回结果解析失败", ex);
                }
            }
        }
        throw new AiProviderException("Claude 请求失败，请稍后重试");
    }

    private String extractAnthropicJson(JsonNode response) {
        if (response == null) {
            throw new AiProviderException("Claude 未返回内容");
        }
        JsonNode contents = response.path("content");
        for (JsonNode item : contents) {
            if ("text".equals(item.path("type").asText())) {
                String value = item.path("text").asText();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        throw new AiProviderException("Claude 响应中缺少结构化内容");
    }

    private AiProviderException mapException(RestClientResponseException ex) {
        HttpStatusCode code = ex.getStatusCode();
        if (code.value() == 401 || code.value() == 403) {
            return new AiProviderException("Claude 认证失败，请检查 API Key");
        }
        if (code.value() == 429) {
            return new AiProviderException("Claude 配额不足或请求过于频繁");
        }
        if (code.value() >= 500) {
            return new AiProviderException("Claude 服务暂时不可用，请稍后再试");
        }
        log.warn("Anthropic 调用失败: status={}, body={}", code, ex.getResponseBodyAsString());
        return new AiProviderException("Claude 请求失败: " + ex.getStatusText());
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(1000, properties.getAi().getRequestTimeoutMs());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
