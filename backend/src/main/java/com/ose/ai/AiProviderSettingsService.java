package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiProviderSettingsService {

    private final AppProperties appProperties;
    private final AiProviderService providerService;
    private final AiProviderHealthService healthService;
    private final AiSecretCryptoService cryptoService;

    public AiProviderSettingsService(AppProperties appProperties,
                                     AiProviderService providerService,
                                     AiProviderHealthService healthService,
                                     AiSecretCryptoService cryptoService) {
        this.appProperties = appProperties;
        this.providerService = providerService;
        this.healthService = healthService;
        this.cryptoService = cryptoService;
    }

    public AiProviderSettingsDtos.AiSettingsResponse getSettings() {
        List<AiProviderSettingsDtos.AiProviderSettingsSummary> providers = List.of(
                getProviderSummary(AiProviderType.OPENAI),
                getProviderSummary(AiProviderType.ANTHROPIC)
        ).stream().sorted(Comparator.comparing(item -> item.provider().name())).toList();
        return new AiProviderSettingsDtos.AiSettingsResponse(
                appProperties.getAi().getConfigMode(),
                cryptoService.canPersistSecrets(),
                appProperties.getAi().getConfigMode() != AiConfigMode.ENV,
                providers
        );
    }

    public AiProviderSettingsDtos.AiProviderSettingsSummary getProviderSummary(AiProviderType providerType) {
        AiProviderAdminDtos.ProviderDetail detail = providerService.listProviders().stream()
                .filter(provider -> provider.providerType() == providerType)
                .findFirst()
                .orElseGet(() -> new AiProviderAdminDtos.ProviderDetail(
                        null,
                        providerType,
                        providerType.defaultDisplayName(),
                        false,
                        List.of(),
                        AiKeyRotationStrategy.SEQUENTIAL_ROUND_ROBIN,
                        null,
                        AiBaseUrlMode.ROOT,
                        null,
                        null,
                        null,
                        null,
                        null,
                        AiProviderConfigSource.UNAVAILABLE,
                        AiProviderHealthStatus.UNAVAILABLE,
                        "未配置",
                        null,
                        null,
                        null,
                        false,
                        false,
                        List.of()
                ));

        String maskedKey = detail.apiKeys().isEmpty() ? null : detail.apiKeys().get(0).maskedKey();
        return new AiProviderSettingsDtos.AiProviderSettingsSummary(
                providerType,
                detail.enabled(),
                detail.enabled() && !detail.apiKeys().isEmpty(),
                maskedKey,
                maskedKey,
                detail.baseUrl(),
                detail.defaultModel(),
                detail.timeoutMs(),
                detail.maxRetries(),
                detail.temperature(),
                detail.configSource(),
                detail.healthStatus(),
                detail.healthMessage(),
                detail.editable(),
                detail.configSource() == AiProviderConfigSource.ENV,
                !detail.apiKeys().isEmpty()
        );
    }

    @Transactional
    public AiProviderSettingsDtos.AiProviderSettingsSummary update(AiProviderType providerType,
                                                                  AiProviderSettingsDtos.UpdateAiProviderSettingsRequest request) {
        AiProviderAdminDtos.ProviderDetail detail = providerService.listProviders().stream()
                .filter(provider -> provider.providerType() == providerType && provider.deletable())
                .findFirst()
                .orElseGet(() -> providerService.create(new AiProviderAdminDtos.CreateProviderRequest(
                        providerType.defaultDisplayName(),
                        providerType,
                        null,
                        AiBaseUrlMode.ROOT,
                        false,
                        providerType.supportsEnvFallback() ? AiProviderConfigSource.HYBRID : AiProviderConfigSource.DB
                )));

        providerService.update(detail.id(), new AiProviderAdminDtos.UpdateProviderRequest(
                null,
                request.enabled(),
                AiKeyRotationStrategy.SEQUENTIAL_ROUND_ROBIN,
                request.baseUrl(),
                AiBaseUrlMode.ROOT,
                request.defaultModel(),
                request.timeoutMs(),
                request.maxRetries(),
                request.temperature(),
                null,
                providerType.supportsEnvFallback() ? AiProviderConfigSource.HYBRID : AiProviderConfigSource.DB
        ));

        if (request.shouldClearApiKey()) {
            detail.apiKeys().forEach(key -> providerService.deleteKey(detail.id(), key.id()));
        } else if (request.hasApiKeyInput()) {
            if (detail.apiKeys().isEmpty()) {
                providerService.addKey(detail.id(), new AiProviderAdminDtos.CreateApiKeyRequest(request.apiKey(), true));
            } else {
                providerService.updateKey(detail.id(), detail.apiKeys().get(0).id(), new AiProviderAdminDtos.UpdateApiKeyRequest(
                        AiProviderAdminDtos.SecretAction.REPLACE,
                        request.apiKey(),
                        true,
                        detail.apiKeys().get(0).sortOrder()
                ));
            }
        }
        return getProviderSummary(providerType);
    }

    public AiProviderSettingsDtos.AiProviderModelListResponse models(AiProviderType providerType) {
        AiProviderAdminDtos.ProviderDetail detail = providerService.listProviders().stream()
                .filter(provider -> provider.providerType() == providerType)
                .findFirst()
                .orElse(null);
        if (detail == null) {
            return new AiProviderSettingsDtos.AiProviderModelListResponse(
                    providerType,
                    null,
                    List.of()
            );
        }
        return new AiProviderSettingsDtos.AiProviderModelListResponse(
                providerType,
                detail.defaultModel(),
                resolverModels(detail.id())
        );
    }

    public AiProviderHealthResult test(AiProviderType providerType) {
        AiProviderAdminDtos.ProviderDetail detail = providerService.listProviders().stream()
                .filter(provider -> provider.providerType() == providerType)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Provider 未配置"));
        return healthService.test(detail.id());
    }

    private List<AiQuestionDtos.AiModelConfig> resolverModels(String providerId) {
        return providerService.listProviders().stream()
                .filter(provider -> provider.id() != null && provider.id().equals(providerId))
                .findFirst()
                .map(provider -> provider.models().stream()
                        .map(model -> new AiQuestionDtos.AiModelConfig(
                                model.modelId(),
                                model.displayName(),
                                model.modelId().equals(provider.defaultModel())
                        ))
                        .toList())
                .orElse(List.of());
    }
}
