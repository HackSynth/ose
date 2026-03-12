package com.ose.ai;

public record ResolvedAiModelSelection(
        String providerId,
        AiProviderType providerType,
        String providerDisplayName,
        String modelId
) {
}
