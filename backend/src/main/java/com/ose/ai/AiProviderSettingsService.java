package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import com.ose.model.AiProviderSettingsEntity;
import com.ose.repository.AiProviderSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiProviderSettingsService {

    private final AppProperties appProperties;
    private final AiProviderSettingsRepository repository;
    private final AiProviderConfigurationResolver resolver;
    private final AiSecretCryptoService cryptoService;
    private final AiProviderCatalogService catalogService;

    public AiProviderSettingsDtos.AiSettingsResponse getSettings() {
        List<AiProviderSettingsDtos.AiProviderSettingsSummary> providers = new ArrayList<>();
        for (AiProviderType provider : AiProviderType.values()) {
            providers.add(getProviderSummary(provider));
        }
        providers.sort(Comparator.comparing(item -> item.provider().name()));
        return new AiProviderSettingsDtos.AiSettingsResponse(
                appProperties.getAi().getConfigMode(),
                cryptoService.canPersistSecrets(),
                appProperties.getAi().getConfigMode() != AiConfigMode.ENV,
                providers
        );
    }

    public AiProviderSettingsDtos.AiProviderSettingsSummary getProviderSummary(AiProviderType provider) {
        Optional<AiProviderSettingsEntity> entity = repository.findByProvider(provider);
        ResolvedAiProviderConfig resolved = resolver.resolve(provider);
        String baseUrl = entity.map(AiProviderSettingsEntity::getBaseUrl)
                .filter(item -> item != null && !item.isBlank())
                .orElse(resolved.baseUrl());
        String defaultModel = entity.map(AiProviderSettingsEntity::getDefaultModel)
                .filter(item -> item != null && !item.isBlank())
                .orElse(resolved.defaultModel());
        Integer timeoutMs = entity.map(AiProviderSettingsEntity::getTimeoutMs).orElse(resolved.timeoutMs());
        Integer maxRetries = entity.map(AiProviderSettingsEntity::getMaxRetries).orElse(resolved.maxRetries());
        Double temperature = entity.map(AiProviderSettingsEntity::getTemperature).orElse(resolved.temperature());
        AiProviderHealthStatus healthStatus = entity.map(AiProviderSettingsEntity::getLastHealthStatus)
                .orElse(resolved.isAvailable() ? AiProviderHealthStatus.UNKNOWN : AiProviderHealthStatus.UNAVAILABLE);
        String healthMessage = entity.map(AiProviderSettingsEntity::getLastHealthMessage).orElse(resolved.message());

        return new AiProviderSettingsDtos.AiProviderSettingsSummary(
                provider,
                entity.map(AiProviderSettingsEntity::isEnabled).orElse(false),
                resolved.isAvailable(),
                resolved.maskedKey(),
                entity.map(AiProviderSettingsEntity::getApiKeyMask).orElse(null),
                baseUrl,
                defaultModel,
                timeoutMs,
                maxRetries,
                temperature,
                resolved.configSource(),
                healthStatus,
                healthMessage,
                appProperties.getAi().getConfigMode() != AiConfigMode.ENV,
                resolved.configSource() == AiProviderConfigSource.ENV || resolved.configSource() == AiProviderConfigSource.ENV_FALLBACK,
                entity.map(item -> item.getApiKeyMask() != null && !item.getApiKeyMask().isBlank()).orElse(false)
        );
    }

    @Transactional
    public AiProviderSettingsDtos.AiProviderSettingsSummary update(AiProviderType provider,
                                                                  AiProviderSettingsDtos.UpdateAiProviderSettingsRequest request) {
        if (appProperties.getAi().getConfigMode() == AiConfigMode.ENV) {
            throw new BusinessException("当前配置来自环境变量；如需在页面中托管密钥，请配置数据库加密主密钥并切换到数据库模式");
        }

        AiProviderSettingsEntity entity = repository.findByProvider(provider)
                .orElseGet(() -> AiProviderSettingsEntity.builder()
                        .provider(provider)
                        .enabled(false)
                        .configSource(AiProviderConfigSource.DB)
                        .build());

        boolean enabled = request.enabled() != null ? request.enabled() : entity.isEnabled();
        entity.setEnabled(enabled);
        entity.setBaseUrl(trimOrDefault(request.baseUrl(), entity.getBaseUrl(), catalogService.defaultBaseUrl(provider)));
        entity.setDefaultModel(trimOrDefault(request.defaultModel(), entity.getDefaultModel(), catalogService.defaultModel(provider)));
        entity.setTimeoutMs(request.timeoutMs() != null ? request.timeoutMs() : defaultInt(entity.getTimeoutMs(), catalogService.defaultTimeoutMs()));
        entity.setMaxRetries(request.maxRetries() != null ? request.maxRetries() : defaultInt(entity.getMaxRetries(), catalogService.defaultMaxRetries()));
        entity.setTemperature(request.temperature() != null ? request.temperature() : defaultDouble(entity.getTemperature(), catalogService.defaultTemperature(provider)));
        entity.setConfigSource(AiProviderConfigSource.DB);

        if (request.shouldClearApiKey()) {
            entity.setApiKeyEncrypted(null);
            entity.setApiKeyMask(null);
        } else if (request.hasApiKeyInput()) {
            if (!cryptoService.canPersistSecrets()) {
                throw new BusinessException("未配置 AI_SECRET_ENCRYPTION_KEY，当前实例不能托管数据库密钥");
            }
            String apiKey = request.apiKey().trim();
            entity.setApiKeyEncrypted(cryptoService.encrypt(apiKey));
            entity.setApiKeyMask(cryptoService.mask(apiKey));
        }

        if (entity.isEnabled() && (entity.getApiKeyEncrypted() == null || entity.getApiKeyEncrypted().isBlank())) {
            throw new BusinessException("启用数据库配置时必须提供 API Key，或保留现有已保存的 Key");
        }

        if (!entity.isEnabled() && request.shouldClearApiKey()) {
            entity.setLastHealthStatus(AiProviderHealthStatus.UNAVAILABLE);
            entity.setLastHealthMessage("数据库密钥已清空");
        }

        repository.save(entity);
        return getProviderSummary(provider);
    }

    public AiProviderSettingsDtos.AiProviderModelListResponse models(AiProviderType provider) {
        ResolvedAiProviderConfig resolved = resolver.resolve(provider);
        return new AiProviderSettingsDtos.AiProviderModelListResponse(
                provider,
                resolved.defaultModel(),
                catalogService.models(provider, resolved.defaultModel())
        );
    }

    private String trimOrDefault(String candidate, String current, String fallback) {
        String value = candidate == null ? current : candidate;
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private int defaultInt(Integer candidate, int fallback) {
        return candidate == null ? fallback : candidate;
    }

    private double defaultDouble(Double candidate, double fallback) {
        return candidate == null ? fallback : candidate;
    }
}
