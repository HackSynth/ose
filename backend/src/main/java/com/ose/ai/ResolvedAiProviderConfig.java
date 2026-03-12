package com.ose.ai;

import java.util.List;

public record ResolvedAiProviderConfig(
        AiProviderType provider,
        boolean enabled,
        boolean configured,
        String apiKey,
        String maskedKey,
        String baseUrl,
        String defaultModel,
        int timeoutMs,
        int maxRetries,
        double temperature,
        int maxTokens,
        AiProviderConfigSource configSource,
        String message,
        List<AiQuestionDtos.AiModelConfig> models
) {
    public boolean isAvailable() {
        return enabled && configured && apiKey != null && !apiKey.isBlank();
    }

    public String resolveModel(String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            return defaultModel;
        }
        return requestedModel;
    }
}
