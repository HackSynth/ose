package com.ose.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AiProviderAdminDtos {
    private AiProviderAdminDtos() {
    }

    public record ProviderDetail(
            String id,
            AiProviderType providerType,
            String displayName,
            boolean enabled,
            List<ApiKeySummary> apiKeys,
            AiKeyRotationStrategy keyRotationStrategy,
            String baseUrl,
            AiBaseUrlMode baseUrlMode,
            String defaultModel,
            Integer timeoutMs,
            Integer maxRetries,
            Double temperature,
            String remark,
            AiProviderConfigSource configSource,
            AiProviderHealthStatus healthStatus,
            String healthMessage,
            LocalDateTime lastCheckedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean editable,
            boolean deletable,
            List<ModelDetail> models
    ) {
    }

    public record ApiKeySummary(
            String id,
            String maskedKey,
            boolean enabled,
            Integer sortOrder,
            Integer consecutiveFailures,
            LocalDateTime lastUsedAt,
            LocalDateTime lastFailedAt
    ) {
    }

    public record ModelDetail(
            String id,
            String providerId,
            String modelId,
            String displayName,
            AiModelType modelType,
            List<String> capabilityTags,
            boolean enabled,
            boolean defaultForQuestionGeneration,
            boolean defaultForReviewSummary,
            boolean defaultForPracticeRecommendation,
            Integer sortOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CreateProviderRequest(
            @NotBlank(message = "显示名称不能为空") @Size(max = 120, message = "显示名称不能超过 120 个字符") String displayName,
            @NotNull(message = "请选择 Provider 类型") AiProviderType providerType,
            @Size(max = 255, message = "Base URL 不能超过 255 个字符") String baseUrl,
            AiBaseUrlMode baseUrlMode,
            Boolean enabled,
            AiProviderConfigSource configSource
    ) {
    }

    public record UpdateProviderRequest(
            @Size(max = 120, message = "显示名称不能超过 120 个字符") String displayName,
            Boolean enabled,
            AiKeyRotationStrategy keyRotationStrategy,
            @Size(max = 255, message = "Base URL 不能超过 255 个字符") String baseUrl,
            AiBaseUrlMode baseUrlMode,
            @Size(max = 120, message = "默认模型不能超过 120 个字符") String defaultModel,
            @Min(value = 1000, message = "请求超时不能低于 1000ms") @Max(value = 120000, message = "请求超时不能超过 120000ms") Integer timeoutMs,
            @Min(value = 0, message = "最大重试次数不能小于 0") @Max(value = 10, message = "最大重试次数不能超过 10") Integer maxRetries,
            @Min(value = 0, message = "温度不能小于 0") @Max(value = 2, message = "温度不能超过 2") Double temperature,
            @Size(max = 500, message = "备注不能超过 500 个字符") String remark,
            AiProviderConfigSource configSource
    ) {
    }

    public enum SecretAction {
        KEEP,
        REPLACE,
        CLEAR
    }

    public record CreateApiKeyRequest(
            @NotBlank(message = "API Key 不能为空") @Size(max = 512, message = "API Key 长度不能超过 512") String apiKey,
            Boolean enabled
    ) {
    }

    public record UpdateApiKeyRequest(
            SecretAction secretAction,
            @Size(max = 512, message = "API Key 长度不能超过 512") String apiKey,
            Boolean enabled,
            Integer sortOrder
    ) {
    }

    public record CreateModelRequest(
            @NotBlank(message = "模型 ID 不能为空") @Size(max = 120, message = "模型 ID 不能超过 120 个字符") String modelId,
            @NotBlank(message = "模型显示名不能为空") @Size(max = 120, message = "模型显示名不能超过 120 个字符") String displayName,
            AiModelType modelType,
            List<String> capabilityTags,
            Boolean enabled,
            Integer sortOrder
    ) {
    }

    public record UpdateModelRequest(
            @Size(max = 120, message = "模型 ID 不能超过 120 个字符") String modelId,
            @Size(max = 120, message = "模型显示名不能超过 120 个字符") String displayName,
            AiModelType modelType,
            List<String> capabilityTags,
            Boolean enabled,
            Integer sortOrder
    ) {
    }

    public record DiscoverModelsResponse(
            boolean success,
            String message,
            List<ModelDetail> models
    ) {
    }

    public record DefaultModelSelection(
            String providerId,
            String modelId
    ) {
    }

    public record DefaultModelsResponse(
            DefaultModelSelection questionGeneration,
            DefaultModelSelection reviewSummary,
            DefaultModelSelection practiceRecommendation
    ) {
    }

    public record UpdateDefaultModelsRequest(
            DefaultModelSelection questionGeneration,
            DefaultModelSelection reviewSummary,
            DefaultModelSelection practiceRecommendation
    ) {
    }

    public record ProviderTestResponse(
            boolean success,
            String providerId,
            AiProviderType providerType,
            String model,
            Long latencyMs,
            String message,
            AiProviderConfigSource configSource
    ) {
    }
}
