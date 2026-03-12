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
public class AnthropicProviderClient implements AiProviderClient {

    private final AiProviderConfigurationResolver resolver;
    private final AiProviderCatalogService catalogService;
    private final ObjectMapper objectMapper;

    @Override
    public AiProviderType provider() {
        return AiProviderType.ANTHROPIC;
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
        payload.put("system", systemPrompt + "\n请严格符合以下 JSON Schema: " + jsonSchema);
        payload.put("temperature", config.temperature());
        payload.put("max_tokens", config.maxTokens());
        payload.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        RestClient client = buildClient(config);
        int maxRetries = Math.max(0, config.maxRetries());
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

    @Override
    public AiProviderHealthResult testConnection(ResolvedAiProviderConfig config) {
        long startedAt = System.currentTimeMillis();
        String model = config.defaultModel();
        try {
            buildClient(config).post()
                    .uri("/v1/messages")
                    .body(Map.of(
                            "model", model,
                            "max_tokens", Math.max(8, config.maxTokens()),
                            "temperature", 0,
                            "messages", List.of(Map.of("role", "user", "content", "ping"))
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            return new AiProviderHealthResult(
                    true,
                    provider(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    "Claude 连通性测试通过",
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
                    "Claude 连通性测试失败，请检查网络或配置",
                    config.configSource(),
                    AiProviderHealthStatus.FAILED
            );
        }
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
        log.warn("Anthropic 调用失败: status={}", code);
        return new AiProviderException("Claude 请求失败: " + ex.getStatusText());
    }

    private RestClient buildClient(ResolvedAiProviderConfig config) {
        return RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("x-api-key", config.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
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
