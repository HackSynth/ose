package com.ose.ai;

import com.ose.common.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AiProviderCatalogService {

    private static final String ENV_PREFIX = "env-";

    private final AppProperties appProperties;

    public AiProviderCatalogService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String defaultModel(AiProviderType providerType) {
        return switch (providerType) {
            case OPENAI, OPENAI_COMPATIBLE -> appProperties.getAi().getOpenai().getDefaultModel();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getDefaultModel();
        };
    }

    public String defaultBaseUrl(AiProviderType providerType) {
        return switch (providerType) {
            case OPENAI, OPENAI_COMPATIBLE -> appProperties.getAi().getOpenai().getBaseUrl();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getBaseUrl();
        };
    }

    public int defaultTimeoutMs() {
        return appProperties.getAi().getRequestTimeoutMs();
    }

    public int defaultMaxRetries() {
        return appProperties.getAi().getMaxRetries();
    }

    public double defaultTemperature(AiProviderType providerType) {
        return switch (providerType) {
            case OPENAI, OPENAI_COMPATIBLE -> appProperties.getAi().getOpenai().getTemperature();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getTemperature();
        };
    }

    public int maxTokens(AiProviderType providerType) {
        return switch (providerType) {
            case OPENAI, OPENAI_COMPATIBLE -> 0;
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getMaxTokens();
        };
    }

    public String envApiKey(AiProviderType providerType) {
        return switch (providerType) {
            case OPENAI, OPENAI_COMPATIBLE -> appProperties.getAi().getOpenai().getApiKey();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getApiKey();
        };
    }

    public List<String> envModels(AiProviderType providerType) {
        String raw = switch (providerType) {
            case OPENAI, OPENAI_COMPATIBLE -> appProperties.getAi().getOpenai().getModels();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getModels();
        };
        return splitModels(raw);
    }

    public List<String> splitModels(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    public List<String> splitKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    public String defaultDisplayName(AiProviderType providerType) {
        return providerType.defaultDisplayName();
    }

    public String envProviderId(AiProviderType providerType) {
        return ENV_PREFIX + providerType.name().toLowerCase();
    }

    public boolean isEnvProviderId(String providerId) {
        return providerId != null && providerId.startsWith(ENV_PREFIX);
    }

    public AiProviderType envProviderType(String providerId) {
        if (!isEnvProviderId(providerId)) {
            return null;
        }
        return switch (providerId) {
            case "env-openai" -> AiProviderType.OPENAI;
            case "env-anthropic" -> AiProviderType.ANTHROPIC;
            default -> null;
        };
    }

    public List<AiProviderAdminDtos.ModelDetail> envModelDetails(AiProviderType providerType, AiProviderAdminDtos.DefaultModelsResponse defaults) {
        List<AiProviderAdminDtos.ModelDetail> models = new ArrayList<>();
        List<String> configured = envModels(providerType);
        String providerId = envProviderId(providerType);
        for (int i = 0; i < configured.size(); i++) {
            String modelId = configured.get(i);
            models.add(new AiProviderAdminDtos.ModelDetail(
                    providerId + "::" + modelId,
                    providerId,
                    modelId,
                    modelId,
                    AiModelType.CHAT,
                    List.of("env"),
                    true,
                    isDefault(defaults.questionGeneration(), providerId, modelId),
                    isDefault(defaults.reviewSummary(), providerId, modelId),
                    isDefault(defaults.practiceRecommendation(), providerId, modelId),
                    i,
                    null,
                    null
            ));
        }
        return models;
    }

    private boolean isDefault(AiProviderAdminDtos.DefaultModelSelection selection, String providerId, String modelId) {
        return selection != null
                && providerId.equals(selection.providerId())
                && modelId.equals(selection.modelId());
    }
}
