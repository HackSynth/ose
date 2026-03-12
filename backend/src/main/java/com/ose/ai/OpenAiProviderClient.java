package com.ose.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiProviderClient implements AiProviderClient {

    private final AiProviderConfigurationResolver resolver;
    private final AiProviderCatalogService catalogService;
    private final ObjectMapper objectMapper;

    @Override
    public AiProviderType provider() {
        return AiProviderType.OPENAI;
    }

    @Override
    public boolean isConfigured() {
        return resolver.resolve(provider()).isAvailable();
    }

    @Override
    public List<AiQuestionDtos.AiModelConfig> models() {
        ResolvedAiProviderConfig config = resolver.resolve(provider());
        return catalogService.models(provider(), config.defaultModel());
    }

    @Override
    public AiQuestionDtos.ProviderGenerationPayload generate(AiQuestionDtos.AiQuestionGenerationRequest request,
                                                             String systemPrompt,
                                                             String userPrompt,
                                                             String jsonSchema) {
        ResolvedAiProviderConfig config = resolver.resolve(provider());
        if (!config.isAvailable()) {
            throw new AiProviderException(config.message());
        }
        String model = config.resolveModel(request.model());
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("input", List.of(
                Map.of("role", "system", "content", List.of(Map.of("type", "input_text", "text", systemPrompt))),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", userPrompt)))
        ));
        payload.put("temperature", config.temperature());
        payload.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "question_batch",
                "schema", objectToMap(jsonSchema),
                "strict", true
        )));

        RestClient client = buildClient(config);
        int maxRetries = Math.max(0, config.maxRetries());
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

    @Override
    public AiProviderHealthResult testConnection(ResolvedAiProviderConfig config) {
        String model = config.defaultModel();
        long startedAt = System.currentTimeMillis();
        try {
            buildClient(config).get()
                    .uri("/v1/models/{model}", model)
                    .retrieve()
                    .body(JsonNode.class);
            return new AiProviderHealthResult(
                    true,
                    provider(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    "OpenAI 连通性测试通过",
                    config.configSource(),
                    AiProviderHealthStatus.SUCCESS
            );
        } catch (RestClientResponseException ex) {
            AiProviderException mapped = mapException(ex);
            return new AiProviderHealthResult(
                    false,
                    provider(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    mapped.getMessage(),
                    config.configSource(),
                    AiProviderHealthStatus.FAILED
            );
        } catch (Exception ex) {
            return new AiProviderHealthResult(
                    false,
                    provider(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    "OpenAI 连通性测试失败，请检查网络或配置",
                    config.configSource(),
                    AiProviderHealthStatus.FAILED
            );
        }
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
        log.warn("OpenAI 调用失败: status={}", code);
        return new AiProviderException("OpenAI 请求失败: " + ex.getStatusText());
    }

    private RestClient buildClient(ResolvedAiProviderConfig config) {
        return RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .requestFactory(requestFactory(config.timeoutMs()))
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(1000, timeoutMs);
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
