package com.ose.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AiProviderSettingsDtos {
    private AiProviderSettingsDtos() {
    }

    public record AiSettingsResponse(
            AiConfigMode configMode,
            boolean encryptionKeyConfigured,
            boolean databaseConfigWritable,
            List<AiProviderSettingsSummary> providers
    ) {
    }

    public record AiProviderSettingsSummary(
            AiProviderType provider,
            boolean enabled,
            boolean configured,
            String maskedKey,
            String storedMaskedKey,
            String baseUrl,
            String defaultModel,
            Integer timeoutMs,
            Integer maxRetries,
            Double temperature,
            AiProviderConfigSource configSource,
            AiProviderHealthStatus healthStatus,
            String healthMessage,
            boolean editable,
            boolean keyManagedByEnv,
            boolean hasStoredApiKey
    ) {
    }

    public record UpdateAiProviderSettingsRequest(
            Boolean enabled,
            @Size(max = 512, message = "API Key 长度不能超过 512") String apiKey,
            Boolean clearApiKey,
            @Size(max = 255, message = "Base URL 长度不能超过 255") String baseUrl,
            @Size(max = 120, message = "默认模型长度不能超过 120") String defaultModel,
            @Min(value = 1000, message = "请求超时不能低于 1000ms") @Max(value = 120000, message = "请求超时不能超过 120000ms") Integer timeoutMs,
            @Min(value = 0, message = "最大重试次数不能小于 0") @Max(value = 10, message = "最大重试次数不能超过 10") Integer maxRetries,
            @Min(value = 0, message = "温度不能小于 0") @Max(value = 2, message = "温度不能超过 2") Double temperature
    ) {
        public boolean shouldClearApiKey() {
            return Boolean.TRUE.equals(clearApiKey);
        }

        public boolean hasApiKeyInput() {
            return apiKey != null && !apiKey.isBlank();
        }

        public boolean hasChanges() {
            return enabled != null
                    || apiKey != null
                    || Boolean.TRUE.equals(clearApiKey)
                    || baseUrl != null
                    || defaultModel != null
                    || timeoutMs != null
                    || maxRetries != null
                    || temperature != null;
        }
    }

    public record AiProviderConnectionTestResponse(
            boolean success,
            AiProviderType provider,
            String model,
            Long latencyMs,
            String message,
            AiProviderConfigSource configSource
    ) {
    }

    public record AiProviderModelListResponse(
            AiProviderType provider,
            String suggestedDefaultModel,
            List<AiQuestionDtos.AiModelConfig> models
    ) {
    }
}
