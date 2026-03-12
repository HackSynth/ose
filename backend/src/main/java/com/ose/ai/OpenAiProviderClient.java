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
public class OpenAiProviderClient implements AiProviderClient {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AiProviderType provider() {
        return AiProviderType.OPENAI;
    }

    @Override
    public boolean isConfigured() {
        return properties.getAi().getOpenai().getApiKey() != null && !properties.getAi().getOpenai().getApiKey().isBlank();
    }

    @Override
    public List<AiQuestionDtos.AiModelConfig> models() {
        String modelStr = properties.getAi().getOpenai().getModels();
        String defaultModel = properties.getAi().getOpenai().getDefaultModel();
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
            throw new AiProviderException("OpenAI 未配置 API Key");
        }
        String model = request.model() == null || request.model().isBlank()
                ? properties.getAi().getOpenai().getDefaultModel() : request.model();
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("input", List.of(
                Map.of("role", "system", "content", List.of(Map.of("type", "input_text", "text", systemPrompt))),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", userPrompt)))
        ));
        payload.put("temperature", properties.getAi().getOpenai().getTemperature());
        payload.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "question_batch",
                "schema", objectToMap(jsonSchema),
                "strict", true
        )));

        RestClient client = RestClient.builder()
                .baseUrl(properties.getAi().getOpenai().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getAi().getOpenai().getApiKey())
                .requestFactory(requestFactory())
                .build();
        int maxRetries = Math.max(0, properties.getAi().getMaxRetries());
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                JsonNode response = client.post()
                        .uri("/v1/responses")
                        .body(payload)
                        .retrieve()
                        .body(JsonNode.class);
                String text = extractOpenAiJson(response);
                return objectMapper.readValue(text, AiQuestionDtos.ProviderGenerationPayload.class);
            } catch (RestClientResponseException ex) {
                if (attempt == maxRetries || ex.getStatusCode().value() < 500) {
                    throw mapException(ex);
                }
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    throw new AiProviderException("OpenAI 返回结果解析失败", ex);
                }
            }
        }
        throw new AiProviderException("OpenAI 请求失败，请稍后重试");
    }

    private Map<String, Object> objectToMap(String schema) {
        try {
            return objectMapper.readValue(schema, Map.class);
        } catch (Exception ex) {
            throw new AiProviderException("OpenAI Schema 配置错误", ex);
        }
    }

    private String extractOpenAiJson(JsonNode response) {
        if (response == null) {
            throw new AiProviderException("OpenAI 未返回内容");
        }
        JsonNode outputTextNode = response.get("output_text");
        if (outputTextNode != null && !outputTextNode.isNull() && !outputTextNode.asText().isBlank()) {
            return outputTextNode.asText();
        }
        JsonNode output = response.path("output");
        for (JsonNode item : output) {
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }
        throw new AiProviderException("OpenAI 响应中缺少结构化内容");
    }

    private AiProviderException mapException(RestClientResponseException ex) {
        HttpStatusCode code = ex.getStatusCode();
        if (code.value() == 401 || code.value() == 403) {
            return new AiProviderException("OpenAI 认证失败，请检查 API Key");
        }
        if (code.value() == 429) {
            return new AiProviderException("OpenAI 配额不足或请求过于频繁");
        }
        if (code.value() >= 500) {
            return new AiProviderException("OpenAI 服务暂时不可用，请稍后再试");
        }
        log.warn("OpenAI 调用失败: status={}, body={}", code, ex.getResponseBodyAsString());
        return new AiProviderException("OpenAI 请求失败: " + ex.getStatusText());
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(1000, properties.getAi().getRequestTimeoutMs());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
