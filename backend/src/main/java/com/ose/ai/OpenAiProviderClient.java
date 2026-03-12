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

    private final ObjectMapper objectMapper;
    private final AiProviderUrlBuilder urlBuilder;

    @Override
    public AiProviderType providerType() {
        return AiProviderType.OPENAI;
    }

    @Override
    public AiQuestionDtos.ProviderGenerationPayload generate(ResolvedAiProviderConfig config,
                                                             String model,
                                                             String systemPrompt,
                                                             String userPrompt,
                                                             String jsonSchema) {
        if (!config.isAvailable()) {
            throw new AiProviderException(config.message());
        }
        RestClient client = buildClient(config);
        int maxRetries = Math.max(0, config.maxRetries());

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String text = requestGenerationText(client, config, model, config.temperature(), systemPrompt, userPrompt, jsonSchema);
                return objectMapper.readValue(text, AiQuestionDtos.ProviderGenerationPayload.class);
            } catch (AiProviderException ex) {
                if (attempt == maxRetries || !isRetryable(ex)) {
                    throw ex;
                }
                log.warn("{} 请求失败，准备重试: {}", label(config.providerType()), ex.getMessage());
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    throw new AiProviderException(label(config.providerType()) + " 返回结果解析失败", ex);
                }
            }
        }

        throw new AiProviderException(label(config.providerType()) + " 请求失败，请稍后重试");
    }

    @Override
    public AiProviderHealthResult testConnection(ResolvedAiProviderConfig config) {
        String model = config.defaultModel();
        long startedAt = System.currentTimeMillis();
        try {
            OpenAiApiMode mode = probeGenerationCapability(buildClient(config), config, model, config.temperature());
            return new AiProviderHealthResult(
                    true,
                    config.providerType(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    label(config.providerType()) + " 连通性测试通过（" + mode.displayName + "）",
                    config.configSource(),
                    AiProviderHealthStatus.SUCCESS
            );
        } catch (AiProviderException ex) {
            return new AiProviderHealthResult(
                    false,
                    config.providerType(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    ex.getMessage(),
                    config.configSource(),
                    AiProviderHealthStatus.FAILED
            );
        } catch (Exception ex) {
            return new AiProviderHealthResult(
                    false,
                    config.providerType(),
                    model,
                    System.currentTimeMillis() - startedAt,
                    label(config.providerType()) + " 连通性测试失败，请检查网络或配置",
                    config.configSource(),
                    AiProviderHealthStatus.FAILED
            );
        }
    }

    @Override
    public List<AiProviderAdminDtos.CreateModelRequest> discoverModels(ResolvedAiProviderConfig config) {
        if (config.baseUrlMode() == AiBaseUrlMode.FULL_OVERRIDE && !config.baseUrl().contains("/models")) {
            throw new AiProviderException("FULL_OVERRIDE 模式下无法推断模型发现地址，请手动维护模型列表");
        }
        JsonNode response = buildClient(config).get()
                .uri(urlBuilder.build(config, AiProviderUrlBuilder.Endpoint.MODELS))
                .retrieve()
                .body(JsonNode.class);
        List<AiProviderAdminDtos.CreateModelRequest> models = new ArrayList<>();
        if (response != null && response.path("data").isArray()) {
            for (JsonNode item : response.path("data")) {
                String modelId = item.path("id").asText();
                if (!modelId.isBlank()) {
                    models.add(new AiProviderAdminDtos.CreateModelRequest(
                            modelId,
                            modelId,
                            AiModelType.CHAT,
                            List.of("discovered"),
                            true,
                            models.size()
                    ));
                }
            }
        }
        return models;
    }

    private String requestGenerationText(RestClient client,
                                         ResolvedAiProviderConfig config,
                                         String model,
                                         double temperature,
                                         String systemPrompt,
                                         String userPrompt,
                                         String jsonSchema) {
        if (config.baseUrlMode() == AiBaseUrlMode.FULL_OVERRIDE) {
            return requestFullOverride(client, config, model, temperature, systemPrompt, userPrompt, jsonSchema);
        }

        AiProviderException lastFailure = null;
        List<String> endpointFailures = new ArrayList<>();

        for (OpenAiApiMode mode : OpenAiApiMode.values()) {
            try {
                return invokeMode(client, config, mode, model, temperature, systemPrompt, userPrompt, jsonSchema);
            } catch (RestClientResponseException ex) {
                AiProviderException mapped = mapException(ex, config.providerType());
                endpointFailures.add(mode.displayName + "=" + ex.getStatusCode().value());
                if (!shouldFallback(mode, ex)) {
                    if (mode == OpenAiApiMode.CHAT_COMPLETIONS_PROMPT_ONLY && isCompatibilityFailure(ex)) {
                        throw buildEndpointUnavailableException(config.providerType(), "生成", endpointFailures);
                    }
                    throw mapped;
                }
                lastFailure = mapped;
                log.warn("{} {} 不可用，回退到下一种兼容模式: status={}",
                        label(config.providerType()), mode.displayName, ex.getStatusCode().value());
            } catch (AiProviderException ex) {
                if (!shouldFallback(mode, ex)) {
                    throw ex;
                }
                lastFailure = ex;
                log.warn("{} {} 返回内容不可解析，回退到下一种兼容模式", label(config.providerType()), mode.displayName);
            }
        }

        if (endpointFailures.size() == OpenAiApiMode.values().length) {
            throw buildEndpointUnavailableException(config.providerType(), "生成", endpointFailures);
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AiProviderException(label(config.providerType()) + " 请求失败，请稍后重试");
    }

    private String requestFullOverride(RestClient client,
                                       ResolvedAiProviderConfig config,
                                       String model,
                                       double temperature,
                                       String systemPrompt,
                                       String userPrompt,
                                       String jsonSchema) {
        String url = config.baseUrl();
        try {
            if (url.contains("/responses")) {
                return extractResponsesJson(client.post()
                        .uri(url)
                        .body(responsesPromptOnlyPayload(model, systemPrompt, userPrompt, jsonSchema))
                        .retrieve()
                        .body(JsonNode.class));
            }
            return extractChatCompletionJson(client.post()
                    .uri(url)
                    .body(chatCompletionPayload(model, temperature, systemPrompt, userPrompt, jsonSchema, true))
                    .retrieve()
                    .body(JsonNode.class));
        } catch (RestClientResponseException ex) {
            throw mapException(ex, config.providerType());
        }
    }

    private OpenAiApiMode probeGenerationCapability(RestClient client,
                                                    ResolvedAiProviderConfig config,
                                                    String model,
                                                    double temperature) {
        if (config.baseUrlMode() == AiBaseUrlMode.FULL_OVERRIDE) {
            requestFullOverride(client, config, model, temperature, CONNECTION_PROBE_SYSTEM_PROMPT, CONNECTION_PROBE_USER_PROMPT, CONNECTION_PROBE_SCHEMA);
            return OpenAiApiMode.CHAT_COMPLETIONS_PROMPT_ONLY;
        }

        AiProviderException lastFailure = null;
        List<String> endpointFailures = new ArrayList<>();
        for (OpenAiApiMode mode : OpenAiApiMode.values()) {
            try {
                String text = invokeMode(
                        client,
                        config,
                        mode,
                        model,
                        temperature,
                        CONNECTION_PROBE_SYSTEM_PROMPT,
                        CONNECTION_PROBE_USER_PROMPT,
                        CONNECTION_PROBE_SCHEMA
                );
                JsonNode node = objectMapper.readTree(text);
                if (node.path("ok").asBoolean(false)) {
                    return mode;
                }
            } catch (RestClientResponseException ex) {
                endpointFailures.add(mode.displayName + "=" + ex.getStatusCode().value());
                if (!shouldFallback(mode, ex)) {
                    throw mapException(ex, config.providerType());
                }
                lastFailure = mapException(ex, config.providerType());
            } catch (Exception ex) {
                lastFailure = new AiProviderException(label(config.providerType()) + " 返回结果解析失败", ex);
            }
        }
        if (endpointFailures.size() == OpenAiApiMode.values().length) {
            throw buildEndpointUnavailableException(config.providerType(), "连通性测试", endpointFailures);
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AiProviderException(label(config.providerType()) + " 连通性测试失败");
    }

    private String invokeMode(RestClient client,
                              ResolvedAiProviderConfig config,
                              OpenAiApiMode mode,
                              String model,
                              double temperature,
                              String systemPrompt,
                              String userPrompt,
                              String jsonSchema) {
        return switch (mode) {
            case RESPONSES -> extractResponsesJson(client.post()
                    .uri(urlBuilder.build(config, AiProviderUrlBuilder.Endpoint.RESPONSES))
                    .body(responsesPayload(model, temperature, systemPrompt, userPrompt, jsonSchema))
                    .retrieve()
                    .body(JsonNode.class));
            case RESPONSES_PROMPT_ONLY -> extractResponsesJson(client.post()
                    .uri(urlBuilder.build(config, AiProviderUrlBuilder.Endpoint.RESPONSES))
                    .body(responsesPromptOnlyPayload(model, systemPrompt, userPrompt, jsonSchema))
                    .retrieve()
                    .body(JsonNode.class));
            case CHAT_COMPLETIONS_JSON_SCHEMA -> extractChatCompletionJson(client.post()
                    .uri(urlBuilder.build(config, AiProviderUrlBuilder.Endpoint.CHAT_COMPLETIONS))
                    .body(chatCompletionPayload(model, temperature, systemPrompt, userPrompt, jsonSchema, false))
                    .retrieve()
                    .body(JsonNode.class));
            case CHAT_COMPLETIONS_PROMPT_ONLY -> extractChatCompletionJson(client.post()
                    .uri(urlBuilder.build(config, AiProviderUrlBuilder.Endpoint.CHAT_COMPLETIONS))
                    .body(chatCompletionPayload(model, temperature, systemPrompt, userPrompt, jsonSchema, true))
                    .retrieve()
                    .body(JsonNode.class));
        };
    }

    private RestClient buildClient(ResolvedAiProviderConfig config) {
        return RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + config.apiKeyValue())
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

    private Map<String, Object> responsesPayload(String model,
                                                double temperature,
                                                String systemPrompt,
                                                String userPrompt,
                                                String jsonSchema) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", temperature);
        payload.put("instructions", systemPrompt);
        payload.put("input", userPrompt);
        payload.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", "question_generation",
                        "strict", true,
                        "schema", readSchema(jsonSchema)
                )
        ));
        return payload;
    }

    private Map<String, Object> responsesPromptOnlyPayload(String model,
                                                           String systemPrompt,
                                                           String userPrompt,
                                                           String jsonSchema) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("instructions", systemPrompt + "\n请严格符合以下 JSON Schema: " + jsonSchema);
        payload.put("input", userPrompt);
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
                Map.of("role", "system", "content", promptOnly ? systemPrompt + "\n请严格符合以下 JSON Schema: " + jsonSchema : systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (!promptOnly) {
            payload.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "question_generation",
                            "strict", true,
                            "schema", readSchema(jsonSchema)
                    )
            ));
        }
        return payload;
    }

    private JsonNode readSchema(String jsonSchema) {
        try {
            return objectMapper.readTree(jsonSchema);
        } catch (Exception ex) {
            throw new AiProviderException("JSON Schema 解析失败", ex);
        }
    }

    private String extractResponsesJson(JsonNode response) {
        if (response == null) {
            throw new AiProviderException("OpenAI 未返回内容");
        }
        JsonNode output = response.path("output");
        for (JsonNode item : output) {
            if ("message".equals(item.path("type").asText())) {
                for (JsonNode content : item.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        String value = content.path("text").asText();
                        if (!value.isBlank()) {
                            return value;
                        }
                    }
                }
            }
        }
        throw new AiProviderException("OpenAI 响应中缺少结构化内容");
    }

    private String extractChatCompletionJson(JsonNode response) {
        if (response == null) {
            throw new AiProviderException("OpenAI 未返回内容");
        }
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new AiProviderException("OpenAI 响应中缺少 choices");
        }
        String content = choices.get(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new AiProviderException("OpenAI 响应内容为空");
        }
        return content;
    }

    private AiProviderException mapException(RestClientResponseException ex, AiProviderType providerType) {
        HttpStatusCode code = ex.getStatusCode();
        String label = label(providerType);
        if (code.value() == 401 || code.value() == 403) {
            return new AiProviderException(label + " 认证失败，请检查 API Key");
        }
        if (code.value() == 429) {
            return new AiProviderException(label + " 配额不足或请求过于频繁");
        }
        if (code.value() >= 500) {
            return new AiProviderException(label + " 服务暂时不可用，请稍后再试");
        }
        return new AiProviderException(label + " 请求失败: " + ex.getStatusText());
    }

    private boolean isRetryable(AiProviderException ex) {
        return ex.getMessage() != null && (ex.getMessage().contains("暂时不可用") || ex.getMessage().contains("频繁"));
    }

    private boolean shouldFallback(OpenAiApiMode mode, RestClientResponseException ex) {
        return switch (mode) {
            case RESPONSES, RESPONSES_PROMPT_ONLY -> isCompatibilityFailure(ex);
            case CHAT_COMPLETIONS_JSON_SCHEMA -> ex.getStatusCode().value() == 400;
            case CHAT_COMPLETIONS_PROMPT_ONLY -> false;
        };
    }

    private boolean shouldFallback(OpenAiApiMode mode, AiProviderException ex) {
        return mode == OpenAiApiMode.CHAT_COMPLETIONS_JSON_SCHEMA && ex.getMessage() != null && ex.getMessage().contains("结构化内容");
    }

    private boolean isCompatibilityFailure(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        return status == 404 || status == 405 || status == 400;
    }

    private AiProviderException buildEndpointUnavailableException(AiProviderType providerType,
                                                                  String action,
                                                                  List<String> endpointFailures) {
        return new AiProviderException(label(providerType) + action + "失败，当前地址未提供兼容的标准接口: " + String.join("，", endpointFailures));
    }

    private String label(AiProviderType providerType) {
        return providerType == AiProviderType.OPENAI_COMPATIBLE ? "OpenAI-Compatible" : "OpenAI";
    }

    private enum OpenAiApiMode {
        RESPONSES("responses"),
        RESPONSES_PROMPT_ONLY("responses-prompt"),
        CHAT_COMPLETIONS_JSON_SCHEMA("chat-completions"),
        CHAT_COMPLETIONS_PROMPT_ONLY("chat-completions-prompt");

        private final String displayName;

        OpenAiApiMode(String displayName) {
            this.displayName = displayName;
        }
    }
}
