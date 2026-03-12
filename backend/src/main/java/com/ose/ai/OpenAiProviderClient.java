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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiProviderClient implements AiProviderClient {

    private static final String CONNECTION_PROBE_SCHEMA = """
            {
              "type":"object",
              "required":["ok"],
              "properties":{
                "ok":{"type":"boolean"}
              }
            }
            """;

    private static final String CONNECTION_PROBE_SYSTEM_PROMPT = "你是连通性测试助手，只输出 JSON。";
    private static final String CONNECTION_PROBE_USER_PROMPT = "请返回 {\"ok\":true}";

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
        RestClient client = buildClient(config);
        int maxRetries = Math.max(0, config.maxRetries());

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String text = requestGenerationText(client, model, config.temperature(), systemPrompt, userPrompt, jsonSchema);
                return objectMapper.readValue(text, AiQuestionDtos.ProviderGenerationPayload.class);
            } catch (AiProviderException ex) {
                if (attempt == maxRetries || !isRetryable(ex)) {
                    throw ex;
                }
                log.warn("OpenAI 请求失败，准备重试: {}", ex.getMessage());
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
            OpenAiApiMode mode = probeGenerationCapability(buildClient(config), model, config.temperature());
            return new AiProviderHealthResult(
                    true,
                    provider(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    "OpenAI 连通性测试通过（" + mode.displayName + "）",
                    config.configSource(),
                    AiProviderHealthStatus.SUCCESS
            );
        } catch (AiProviderException ex) {
            return new AiProviderHealthResult(
                    false,
                    provider(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    ex.getMessage(),
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

    private String requestGenerationText(RestClient client,
                                         String model,
                                         double temperature,
                                         String systemPrompt,
                                         String userPrompt,
                                         String jsonSchema) {
        AiProviderException lastFailure = null;
        List<String> endpointFailures = new ArrayList<>();

        for (OpenAiApiMode mode : OpenAiApiMode.values()) {
            try {
                return switch (mode) {
                    case RESPONSES -> extractResponsesJson(client.post()
                            .uri("/v1/responses")
                            .body(responsesPayload(model, temperature, systemPrompt, userPrompt, jsonSchema))
                            .retrieve()
                            .body(JsonNode.class));
                    case CHAT_COMPLETIONS_JSON_SCHEMA -> extractChatCompletionJson(client.post()
                            .uri("/v1/chat/completions")
                            .body(chatCompletionPayload(model, temperature, systemPrompt, userPrompt, jsonSchema, false))
                            .retrieve()
                            .body(JsonNode.class));
                    case CHAT_COMPLETIONS_PROMPT_ONLY -> extractChatCompletionJson(client.post()
                            .uri("/v1/chat/completions")
                            .body(chatCompletionPayload(model, temperature, systemPrompt, userPrompt, jsonSchema, true))
                            .retrieve()
                            .body(JsonNode.class));
                };
            } catch (RestClientResponseException ex) {
                AiProviderException mapped = mapException(ex);
                endpointFailures.add(mode.displayName + "=" + ex.getStatusCode().value());
                if (!shouldFallback(mode, ex)) {
                    if (mode == OpenAiApiMode.CHAT_COMPLETIONS_PROMPT_ONLY && isCompatibilityFailure(ex)) {
                        throw buildEndpointUnavailableException("生成", endpointFailures);
                    }
                    throw mapped;
                }
                lastFailure = mapped;
                log.warn("OpenAI {} 不可用，回退到下一种兼容模式: status={}", mode.displayName, ex.getStatusCode().value());
            } catch (AiProviderException ex) {
                if (!shouldFallback(mode, ex)) {
                    throw ex;
                }
                lastFailure = ex;
                log.warn("OpenAI {} 返回内容不可解析，回退到下一种兼容模式", mode.displayName);
            }
        }

        if (endpointFailures.size() == OpenAiApiMode.values().length) {
            throw buildEndpointUnavailableException("生成", endpointFailures);
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AiProviderException("OpenAI 请求失败，请稍后重试");
    }

    private OpenAiApiMode probeGenerationCapability(RestClient client, String model, double temperature) {
        AiProviderException lastFailure = null;
        List<String> endpointFailures = new ArrayList<>();

        for (OpenAiApiMode mode : OpenAiApiMode.values()) {
            try {
                String text = switch (mode) {
                    case RESPONSES -> extractResponsesJson(client.post()
                            .uri("/v1/responses")
                            .body(responsesPayload(
                                    model,
                                    temperature,
                                    CONNECTION_PROBE_SYSTEM_PROMPT,
                                    CONNECTION_PROBE_USER_PROMPT,
                                    CONNECTION_PROBE_SCHEMA
                            ))
                            .retrieve()
                            .body(JsonNode.class));
                    case CHAT_COMPLETIONS_JSON_SCHEMA -> extractChatCompletionJson(client.post()
                            .uri("/v1/chat/completions")
                            .body(chatCompletionPayload(
                                    model,
                                    temperature,
                                    CONNECTION_PROBE_SYSTEM_PROMPT,
                                    CONNECTION_PROBE_USER_PROMPT,
                                    CONNECTION_PROBE_SCHEMA,
                                    false
                            ))
                            .retrieve()
                            .body(JsonNode.class));
                    case CHAT_COMPLETIONS_PROMPT_ONLY -> extractChatCompletionJson(client.post()
                            .uri("/v1/chat/completions")
                            .body(chatCompletionPayload(
                                    model,
                                    temperature,
                                    CONNECTION_PROBE_SYSTEM_PROMPT,
                                    CONNECTION_PROBE_USER_PROMPT,
                                    CONNECTION_PROBE_SCHEMA,
                                    true
                            ))
                            .retrieve()
                            .body(JsonNode.class));
                };

                if (objectMapper.readTree(text).path("ok").asBoolean(false)) {
                    return mode;
                }
                throw new AiProviderException("OpenAI 连通性测试失败，生成接口未返回预期结果");
            } catch (RestClientResponseException ex) {
                AiProviderException mapped = mapException(ex);
                endpointFailures.add(mode.displayName + "=" + ex.getStatusCode().value());
                if (!shouldFallback(mode, ex)) {
                    if (mode == OpenAiApiMode.CHAT_COMPLETIONS_PROMPT_ONLY && isCompatibilityFailure(ex)) {
                        throw buildEndpointUnavailableException("连通性测试", endpointFailures);
                    }
                    throw mapped;
                }
                lastFailure = mapped;
                log.warn("OpenAI 连通性测试 {} 不可用，回退到下一种兼容模式: status={}", mode.displayName, ex.getStatusCode().value());
            } catch (AiProviderException ex) {
                if (!shouldFallback(mode, ex)) {
                    throw ex;
                }
                lastFailure = ex;
                log.warn("OpenAI 连通性测试 {} 返回内容不可解析，回退到下一种兼容模式", mode.displayName);
            } catch (Exception ex) {
                AiProviderException mapped = new AiProviderException("OpenAI 连通性测试失败，请检查模型或生成接口兼容性", ex);
                if (!shouldFallback(mode, mapped)) {
                    throw mapped;
                }
                lastFailure = mapped;
            }
        }

        if (endpointFailures.size() == OpenAiApiMode.values().length) {
            throw buildEndpointUnavailableException("连通性测试", endpointFailures);
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AiProviderException("OpenAI 连通性测试失败，请检查模型或生成接口兼容性");
    }

    private Map<String, Object> responsesPayload(String model,
                                                 double temperature,
                                                 String systemPrompt,
                                                 String userPrompt,
                                                 String jsonSchema) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("input", List.of(
                Map.of("role", "system", "content", List.of(Map.of("type", "input_text", "text", systemPrompt))),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", userPrompt)))
        ));
        payload.put("temperature", temperature);
        payload.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "question_batch",
                "schema", objectToMap(jsonSchema),
                "strict", true
        )));
        return payload;
    }

    private Map<String, Object> chatCompletionPayload(String model,
                                                      double temperature,
                                                      String systemPrompt,
                                                      String userPrompt,
                                                      String jsonSchema,
                                                      boolean promptOnly) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", temperature);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", promptOnly ? systemPromptWithSchema(systemPrompt, jsonSchema) : systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (!promptOnly) {
            payload.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "question_batch",
                            "schema", objectToMap(jsonSchema),
                            "strict", true
                    )
            ));
        }
        return payload;
    }

    private String systemPromptWithSchema(String systemPrompt, String jsonSchema) {
        return systemPrompt + "\n请严格符合以下 JSON Schema，并且仅输出 JSON：\n" + jsonSchema;
    }

    private Map<String, Object> objectToMap(String schema) {
        try {
            return objectMapper.readValue(schema, Map.class);
        } catch (Exception ex) {
            throw new AiProviderException("OpenAI Schema 配置错误", ex);
        }
    }

    private String extractResponsesJson(JsonNode response) {
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

    private String extractChatCompletionJson(JsonNode response) {
        if (response == null) {
            throw new AiProviderException("OpenAI 未返回内容");
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual() && !contentNode.asText().isBlank()) {
            return contentNode.asText();
        }

        for (JsonNode content : contentNode) {
            if ("text".equals(content.path("type").asText())) {
                String text = content.path("text").asText();
                if (!text.isBlank()) {
                    return text;
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

    private boolean shouldFallback(OpenAiApiMode mode, RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 401 || status == 403 || status == 429) {
            return false;
        }
        return mode != OpenAiApiMode.CHAT_COMPLETIONS_PROMPT_ONLY;
    }

    private boolean shouldFallback(OpenAiApiMode mode, AiProviderException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        return mode != OpenAiApiMode.CHAT_COMPLETIONS_PROMPT_ONLY
                && (message.contains("缺少结构化内容")
                || message.contains("解析失败")
                || message.contains("未返回内容")
                || message.contains("未返回预期结果"));
    }

    private boolean isRetryable(AiProviderException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("暂时不可用");
    }

    private boolean isCompatibilityFailure(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        return status != 401 && status != 403 && status != 429;
    }

    private AiProviderException buildEndpointUnavailableException(String action, List<String> endpointFailures) {
        return new AiProviderException(
                "OpenAI " + action + "失败：当前 Base URL 的生成接口不可用，请检查网关兼容性或改用官方地址。"
                        + "失败端点: " + String.join(", ", endpointFailures)
        );
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

    enum OpenAiApiMode {
        RESPONSES("responses"),
        CHAT_COMPLETIONS_JSON_SCHEMA("chat.completions/json_schema"),
        CHAT_COMPLETIONS_PROMPT_ONLY("chat.completions/prompt");

        private final String displayName;

        OpenAiApiMode(String displayName) {
            this.displayName = displayName;
        }
    }
}
