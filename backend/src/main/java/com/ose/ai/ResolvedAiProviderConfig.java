package com.ose.ai;

public record ResolvedAiProviderConfig(
        String providerId,
        AiProviderType providerType,
        String providerDisplayName,
        boolean enabled,
        boolean configured,
        ResolvedAiApiKey apiKey,
        String baseUrl,
        AiBaseUrlMode baseUrlMode,
        String defaultModel,
        int timeoutMs,
        int maxRetries,
        double temperature,
        int maxTokens,
        AiProviderConfigSource configSource,
        String message
) {
    public boolean isAvailable() {
        return enabled && configured && apiKey != null && apiKey.value() != null && !apiKey.value().isBlank();
    }

    public String resolveModel(String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            return defaultModel;
        }
        return requestedModel;
    }

    public String apiKeyValue() {
        return apiKey == null ? null : apiKey.value();
    }

    public String apiKeyId() {
        return apiKey == null ? null : apiKey.id();
    }

    public String maskedKey() {
        return apiKey == null ? null : apiKey.maskedValue();
    }
}
