package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.model.AiProviderSettingsEntity;
import com.ose.repository.AiProviderSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiProviderConfigurationResolver {

    private final AppProperties appProperties;
    private final AiProviderSettingsRepository repository;
    private final AiSecretCryptoService cryptoService;
    private final AiProviderCatalogService catalogService;

    public ResolvedAiProviderConfig resolve(AiProviderType provider) {
        return resolve(provider, null);
    }

    public ResolvedAiProviderConfig resolve(AiProviderType provider,
                                            AiProviderSettingsDtos.UpdateAiProviderSettingsRequest overrideRequest) {
        Optional<AiProviderSettingsEntity> entity = repository.findByProvider(provider);
        Candidate envCandidate = envCandidate(provider);
        Candidate dbCandidate = dbCandidate(provider, entity.orElse(null), overrideRequest);

        return switch (appProperties.getAi().getConfigMode()) {
            case ENV -> envCandidate.configured
                    ? toResolved(provider, envCandidate, AiProviderConfigSource.ENV)
                    : unavailable(provider, envCandidate, "当前实例通过环境变量管理，未配置 API Key");
            case DB -> dbCandidate.configured
                    ? toResolved(provider, dbCandidate, AiProviderConfigSource.DB)
                    : unavailable(provider, dbCandidate, dbCandidate.message);
            case HYBRID -> {
                if (dbCandidate.enabled) {
                    yield dbCandidate.configured
                            ? toResolved(provider, dbCandidate, AiProviderConfigSource.DB)
                            : unavailable(provider, dbCandidate, dbCandidate.message);
                }
                if (envCandidate.configured) {
                    yield toResolved(provider, envCandidate, AiProviderConfigSource.ENV_FALLBACK);
                }
                yield unavailable(provider, envCandidate, "未检测到可用的数据库配置或环境变量配置");
            }
        };
    }

    private Candidate envCandidate(AiProviderType provider) {
        String apiKey = trimToNull(catalogService.envApiKey(provider));
        String defaultModel = catalogService.defaultModel(provider);
        String baseUrl = catalogService.defaultBaseUrl(provider);
        return new Candidate(
                true,
                apiKey != null,
                apiKey,
                cryptoService.mask(apiKey),
                baseUrl,
                defaultModel,
                catalogService.defaultTimeoutMs(),
                catalogService.defaultMaxRetries(),
                catalogService.defaultTemperature(provider),
                catalogService.maxTokens(provider),
                apiKey == null ? "环境变量未配置 API Key" : "环境变量配置可用"
        );
    }

    private Candidate dbCandidate(AiProviderType provider,
                                  AiProviderSettingsEntity entity,
                                  AiProviderSettingsDtos.UpdateAiProviderSettingsRequest overrideRequest) {
        boolean enabled = overrideRequest != null && overrideRequest.enabled() != null
                ? overrideRequest.enabled()
                : entity != null && entity.isEnabled();
        String baseUrl = trimToNull(overrideRequest != null ? overrideRequest.baseUrl() : null);
        if (baseUrl == null) {
            baseUrl = trimToNull(entity == null ? null : entity.getBaseUrl());
        }
        if (baseUrl == null) {
            baseUrl = catalogService.defaultBaseUrl(provider);
        }

        String defaultModel = trimToNull(overrideRequest != null ? overrideRequest.defaultModel() : null);
        if (defaultModel == null) {
            defaultModel = trimToNull(entity == null ? null : entity.getDefaultModel());
        }
        if (defaultModel == null) {
            defaultModel = catalogService.defaultModel(provider);
        }

        int timeoutMs = overrideRequest != null && overrideRequest.timeoutMs() != null
                ? overrideRequest.timeoutMs()
                : entity != null && entity.getTimeoutMs() != null
                ? entity.getTimeoutMs()
                : catalogService.defaultTimeoutMs();
        int maxRetries = overrideRequest != null && overrideRequest.maxRetries() != null
                ? overrideRequest.maxRetries()
                : entity != null && entity.getMaxRetries() != null
                ? entity.getMaxRetries()
                : catalogService.defaultMaxRetries();
        double temperature = overrideRequest != null && overrideRequest.temperature() != null
                ? overrideRequest.temperature()
                : entity != null && entity.getTemperature() != null
                ? entity.getTemperature()
                : catalogService.defaultTemperature(provider);

        String apiKey = null;
        String maskedKey = null;
        String message = enabled ? "数据库配置已启用但未保存 API Key" : "数据库配置未启用";

        if (overrideRequest != null && overrideRequest.shouldClearApiKey()) {
            message = enabled ? "数据库配置已启用但当前请求要求清空 API Key" : "数据库密钥已清空";
        } else if (overrideRequest != null && overrideRequest.hasApiKeyInput()) {
            apiKey = overrideRequest.apiKey().trim();
            maskedKey = cryptoService.mask(apiKey);
            message = "使用待保存的数据库配置进行测试";
        } else if (entity != null && entity.getApiKeyEncrypted() != null && !entity.getApiKeyEncrypted().isBlank()) {
            maskedKey = entity.getApiKeyMask();
            try {
                apiKey = cryptoService.decrypt(entity.getApiKeyEncrypted());
                message = "数据库配置可用";
            } catch (RuntimeException ex) {
                message = ex.getMessage();
            }
        }

        boolean configured = enabled && apiKey != null && !apiKey.isBlank();
        return new Candidate(
                enabled,
                configured,
                apiKey,
                maskedKey,
                baseUrl,
                defaultModel,
                timeoutMs,
                maxRetries,
                temperature,
                catalogService.maxTokens(provider),
                message
        );
    }

    private ResolvedAiProviderConfig toResolved(AiProviderType provider, Candidate candidate, AiProviderConfigSource source) {
        List<AiQuestionDtos.AiModelConfig> models = catalogService.models(provider, candidate.defaultModel);
        return new ResolvedAiProviderConfig(
                provider,
                candidate.enabled,
                candidate.configured,
                candidate.apiKey,
                candidate.maskedKey,
                candidate.baseUrl,
                candidate.defaultModel,
                candidate.timeoutMs,
                candidate.maxRetries,
                candidate.temperature,
                candidate.maxTokens,
                source,
                candidate.message,
                models
        );
    }

    private ResolvedAiProviderConfig unavailable(AiProviderType provider, Candidate candidate, String message) {
        return new ResolvedAiProviderConfig(
                provider,
                candidate.enabled,
                false,
                null,
                candidate.maskedKey,
                candidate.baseUrl,
                candidate.defaultModel,
                candidate.timeoutMs,
                candidate.maxRetries,
                candidate.temperature,
                candidate.maxTokens,
                AiProviderConfigSource.UNAVAILABLE,
                message,
                catalogService.models(provider, candidate.defaultModel)
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Candidate(
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
            String message
    ) {
    }
}
