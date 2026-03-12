package com.ose.ai;

public record AiProviderHealthResult(
        boolean success,
        AiProviderType provider,
        String model,
        long latencyMs,
        String message,
        AiProviderConfigSource configSource,
        AiProviderHealthStatus healthStatus
) {
}
