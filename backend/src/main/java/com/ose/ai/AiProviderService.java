package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import com.ose.common.exception.NotFoundException;
import com.ose.model.AiModelEntity;
import com.ose.model.AiProviderApiKeyEntity;
import com.ose.model.AiProviderEntity;
import com.ose.repository.AiModelRepository;
import com.ose.repository.AiProviderApiKeyRepository;
import com.ose.repository.AiProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AiProviderService {

    private final AppProperties appProperties;
    private final AiProviderRepository providerRepository;
    private final AiProviderApiKeyRepository apiKeyRepository;
    private final AiModelRepository modelRepository;
    private final AiSecretCryptoService cryptoService;
    private final AiProviderCatalogService catalogService;
    private final AiDefaultModelService defaultModelService;
    private final AiApiKeyRotationService keyRotationService;

    public AiProviderService(AppProperties appProperties,
                             AiProviderRepository providerRepository,
                             AiProviderApiKeyRepository apiKeyRepository,
                             AiModelRepository modelRepository,
                             AiSecretCryptoService cryptoService,
                             AiProviderCatalogService catalogService,
                             AiDefaultModelService defaultModelService,
                             AiApiKeyRotationService keyRotationService) {
        this.appProperties = appProperties;
        this.providerRepository = providerRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.modelRepository = modelRepository;
        this.cryptoService = cryptoService;
        this.catalogService = catalogService;
        this.defaultModelService = defaultModelService;
        this.keyRotationService = keyRotationService;
    }

    public List<AiProviderAdminDtos.ProviderDetail> listProviders() {
        List<AiProviderAdminDtos.ProviderDetail> result = new ArrayList<>();
        List<AiProviderEntity> providers = providerRepository.findAll();
        AiProviderAdminDtos.DefaultModelsResponse defaults = defaultModelService.getDefaultModels();
        providers.stream()
                .sorted(Comparator.comparing(AiProviderEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(provider -> toDetail(provider, defaults))
                .forEach(result::add);

        if (appProperties.getAi().getConfigMode() != AiConfigMode.DB) {
            addEnvFallbackIfMissing(result, providers, defaults, AiProviderType.OPENAI);
            addEnvFallbackIfMissing(result, providers, defaults, AiProviderType.ANTHROPIC);
        }

        result.sort(Comparator.comparing(AiProviderAdminDtos.ProviderDetail::displayName));
        return result;
    }

    public AiProviderAdminDtos.ProviderDetail getProvider(String providerId) {
        if (catalogService.isEnvProviderId(providerId)) {
            AiProviderType providerType = catalogService.envProviderType(providerId);
            if (providerType == null) {
                throw new NotFoundException("Provider 不存在");
            }
            return toEnvDetail(providerType, defaultModelService.getDefaultModels());
        }
        return toDetail(findProvider(providerId), defaultModelService.getDefaultModels());
    }

    @Transactional
    public AiProviderAdminDtos.ProviderDetail create(AiProviderAdminDtos.CreateProviderRequest request) {
        AiProviderType providerType = request.providerType();
        AiProviderEntity entity = AiProviderEntity.builder()
                .id(UUID.randomUUID().toString())
                .providerType(providerType)
                .displayName(request.displayName().trim())
                .enabled(request.enabled() == null || request.enabled())
                .keyRotationStrategy(AiKeyRotationStrategy.SEQUENTIAL_ROUND_ROBIN)
                .baseUrl(normalizeBaseUrl(request.baseUrl(), providerType))
                .baseUrlMode(request.baseUrlMode() == null ? AiBaseUrlMode.ROOT : request.baseUrlMode())
                .defaultModel(catalogService.defaultModel(providerType))
                .timeoutMs(catalogService.defaultTimeoutMs())
                .maxRetries(catalogService.defaultMaxRetries())
                .temperature(catalogService.defaultTemperature(providerType))
                .configSource(resolveCreateConfigSource(providerType, request.configSource()))
                .healthStatus(AiProviderHealthStatus.UNKNOWN)
                .build();
        providerRepository.save(entity);
        return toDetail(entity, defaultModelService.getDefaultModels());
    }

    @Transactional
    public AiProviderAdminDtos.ProviderDetail update(String providerId, AiProviderAdminDtos.UpdateProviderRequest request) {
        AiProviderEntity entity = findProvider(providerId);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            entity.setDisplayName(request.displayName().trim());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.keyRotationStrategy() != null) {
            entity.setKeyRotationStrategy(request.keyRotationStrategy());
        }
        if (request.baseUrlMode() != null) {
            entity.setBaseUrlMode(request.baseUrlMode());
        }
        if (request.baseUrl() != null) {
            entity.setBaseUrl(normalizeBaseUrl(request.baseUrl(), entity.getProviderType()));
        }
        if (request.defaultModel() != null) {
            entity.setDefaultModel(blankToNull(request.defaultModel()));
        }
        if (request.timeoutMs() != null) {
            entity.setTimeoutMs(request.timeoutMs());
        }
        if (request.maxRetries() != null) {
            entity.setMaxRetries(request.maxRetries());
        }
        if (request.temperature() != null) {
            entity.setTemperature(request.temperature());
        }
        if (request.remark() != null) {
            entity.setRemark(blankToNull(request.remark()));
        }
        if (request.configSource() != null) {
            entity.setConfigSource(resolveUpdateConfigSource(entity.getProviderType(), request.configSource()));
        }
        providerRepository.save(entity);
        return toDetail(entity, defaultModelService.getDefaultModels());
    }

    @Transactional
    public void delete(String providerId) {
        AiProviderEntity entity = findProvider(providerId);
        providerRepository.delete(entity);
    }

    @Transactional
    public AiProviderAdminDtos.ProviderDetail enable(String providerId) {
        AiProviderEntity entity = findProvider(providerId);
        entity.setEnabled(true);
        providerRepository.save(entity);
        return toDetail(entity, defaultModelService.getDefaultModels());
    }

    @Transactional
    public AiProviderAdminDtos.ProviderDetail disable(String providerId) {
        AiProviderEntity entity = findProvider(providerId);
        entity.setEnabled(false);
        providerRepository.save(entity);
        return toDetail(entity, defaultModelService.getDefaultModels());
    }

    @Transactional
    public AiProviderAdminDtos.ApiKeySummary addKey(String providerId, AiProviderAdminDtos.CreateApiKeyRequest request) {
        keyRotationService.validateCanPersist();
        AiProviderEntity provider = findProvider(providerId);
        AiProviderApiKeyEntity entity = AiProviderApiKeyEntity.builder()
                .id(UUID.randomUUID().toString())
                .provider(provider)
                .keyEncrypted(cryptoService.encrypt(request.apiKey().trim()))
                .keyMask(cryptoService.mask(request.apiKey()))
                .enabled(request.enabled() == null || request.enabled())
                .sortOrder(nextSortOrder(provider))
                .consecutiveFailures(0)
                .build();
        apiKeyRepository.save(entity);
        return toKeySummary(entity);
    }

    @Transactional
    public AiProviderAdminDtos.ApiKeySummary updateKey(String providerId, String keyId, AiProviderAdminDtos.UpdateApiKeyRequest request) {
        AiProviderApiKeyEntity entity = apiKeyRepository.findByIdAndProviderId(keyId, providerId)
                .orElseThrow(() -> new NotFoundException("API Key 不存在"));
        AiProviderAdminDtos.SecretAction action = request.secretAction() == null ? AiProviderAdminDtos.SecretAction.KEEP : request.secretAction();
        if (action == AiProviderAdminDtos.SecretAction.REPLACE) {
            keyRotationService.validateCanPersist();
            if (request.apiKey() == null || request.apiKey().isBlank()) {
                throw new BusinessException("替换 API Key 时必须提供新值");
            }
            entity.setKeyEncrypted(cryptoService.encrypt(request.apiKey().trim()));
            entity.setKeyMask(cryptoService.mask(request.apiKey()));
            entity.setConsecutiveFailures(0);
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        return toKeySummary(apiKeyRepository.save(entity));
    }

    @Transactional
    public void deleteKey(String providerId, String keyId) {
        AiProviderApiKeyEntity entity = apiKeyRepository.findByIdAndProviderId(keyId, providerId)
                .orElseThrow(() -> new NotFoundException("API Key 不存在"));
        apiKeyRepository.delete(entity);
    }

    public AiProviderEntity findProvider(String providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider 不存在"));
    }

    public AiProviderAdminDtos.ProviderDetail toDetail(AiProviderEntity provider, AiProviderAdminDtos.DefaultModelsResponse defaults) {
        List<AiProviderAdminDtos.ApiKeySummary> keys = provider.getApiKeys().stream()
                .sorted(Comparator.comparing(AiProviderApiKeyEntity::getSortOrder).thenComparing(AiProviderApiKeyEntity::getCreatedAt))
                .map(this::toKeySummary)
                .toList();
        List<AiProviderAdminDtos.ModelDetail> models = provider.getModels().stream()
                .sorted(Comparator.comparing(AiModelEntity::getSortOrder).thenComparing(AiModelEntity::getCreatedAt))
                .map(model -> toModelDetail(provider, model, defaults))
                .toList();
        AiProviderHealthStatus healthStatus = provider.getHealthStatus() == null
                ? (hasConfig(provider) ? AiProviderHealthStatus.UNKNOWN : AiProviderHealthStatus.UNAVAILABLE)
                : provider.getHealthStatus();
        return new AiProviderAdminDtos.ProviderDetail(
                provider.getId(),
                provider.getProviderType(),
                provider.getDisplayName(),
                provider.isEnabled(),
                keys,
                provider.getKeyRotationStrategy(),
                provider.getBaseUrl(),
                provider.getBaseUrlMode(),
                provider.getDefaultModel(),
                provider.getTimeoutMs(),
                provider.getMaxRetries(),
                provider.getTemperature(),
                provider.getRemark(),
                provider.getConfigSource(),
                healthStatus,
                provider.getHealthMessage(),
                provider.getLastCheckedAt(),
                provider.getCreatedAt(),
                provider.getUpdatedAt(),
                appProperties.getAi().getConfigMode() != AiConfigMode.ENV,
                true,
                models
        );
    }

    private void addEnvFallbackIfMissing(List<AiProviderAdminDtos.ProviderDetail> result,
                                         List<AiProviderEntity> providers,
                                         AiProviderAdminDtos.DefaultModelsResponse defaults,
                                         AiProviderType providerType) {
        boolean hasDbProvider = providers.stream().anyMatch(provider -> provider.getProviderType() == providerType);
        if (hasDbProvider) {
            return;
        }
        String envKey = catalogService.envApiKey(providerType);
        List<String> envModels = catalogService.envModels(providerType);
        if ((envKey == null || envKey.isBlank()) && envModels.isEmpty()) {
            return;
        }
        result.add(toEnvDetail(providerType, defaults));
    }

    private AiProviderAdminDtos.ProviderDetail toEnvDetail(AiProviderType providerType,
                                                           AiProviderAdminDtos.DefaultModelsResponse defaults) {
        String providerId = catalogService.envProviderId(providerType);
        String envKey = catalogService.envApiKey(providerType);
        List<AiProviderAdminDtos.ApiKeySummary> keys = envKey == null || envKey.isBlank()
                ? List.of()
                : List.of(new AiProviderAdminDtos.ApiKeySummary(
                        providerId + "-key",
                        cryptoService.mask(envKey),
                        true,
                        0,
                        0,
                        null,
                        null
                ));
        List<AiProviderAdminDtos.ModelDetail> models = catalogService.envModelDetails(providerType, defaults);
        return new AiProviderAdminDtos.ProviderDetail(
                providerId,
                providerType,
                providerType.defaultDisplayName() + "（ENV）",
                envKey != null && !envKey.isBlank(),
                keys,
                AiKeyRotationStrategy.SEQUENTIAL_ROUND_ROBIN,
                catalogService.defaultBaseUrl(providerType),
                AiBaseUrlMode.ROOT,
                catalogService.defaultModel(providerType),
                catalogService.defaultTimeoutMs(),
                catalogService.defaultMaxRetries(),
                catalogService.defaultTemperature(providerType),
                "环境变量兜底 Provider，仅支持只读查看",
                AiProviderConfigSource.ENV,
                envKey == null || envKey.isBlank() ? AiProviderHealthStatus.UNAVAILABLE : AiProviderHealthStatus.UNKNOWN,
                envKey == null || envKey.isBlank() ? "环境变量未配置 API Key" : "环境变量配置可用",
                null,
                null,
                null,
                false,
                false,
                models
        );
    }

    private AiProviderAdminDtos.ApiKeySummary toKeySummary(AiProviderApiKeyEntity entity) {
        return new AiProviderAdminDtos.ApiKeySummary(
                entity.getId(),
                entity.getKeyMask(),
                entity.isEnabled(),
                entity.getSortOrder(),
                entity.getConsecutiveFailures(),
                entity.getLastUsedAt(),
                entity.getLastFailedAt()
        );
    }

    private AiProviderAdminDtos.ModelDetail toModelDetail(AiProviderEntity provider,
                                                          AiModelEntity model,
                                                          AiProviderAdminDtos.DefaultModelsResponse defaults) {
        return new AiProviderAdminDtos.ModelDetail(
                model.getId(),
                provider.getId(),
                model.getModelId(),
                model.getDisplayName(),
                model.getModelType(),
                splitTags(model.getCapabilityTags()),
                model.isEnabled(),
                isDefault(defaults.questionGeneration(), provider.getId(), model.getId()),
                isDefault(defaults.reviewSummary(), provider.getId(), model.getId()),
                isDefault(defaults.practiceRecommendation(), provider.getId(), model.getId()),
                model.getSortOrder(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }

    private boolean isDefault(AiProviderAdminDtos.DefaultModelSelection selection, String providerId, String modelId) {
        return selection != null
                && Objects.equals(selection.providerId(), providerId)
                && Objects.equals(selection.modelId(), modelId);
    }

    private boolean hasConfig(AiProviderEntity provider) {
        return provider.isEnabled()
                && provider.getApiKeys().stream().anyMatch(AiProviderApiKeyEntity::isEnabled)
                && provider.getModels().stream().anyMatch(AiModelEntity::isEnabled);
    }

    private AiProviderConfigSource resolveCreateConfigSource(AiProviderType providerType, AiProviderConfigSource requested) {
        if (requested == null) {
            return providerType.supportsEnvFallback() ? AiProviderConfigSource.HYBRID : AiProviderConfigSource.DB;
        }
        return resolveUpdateConfigSource(providerType, requested);
    }

    private AiProviderConfigSource resolveUpdateConfigSource(AiProviderType providerType, AiProviderConfigSource requested) {
        if (requested == AiProviderConfigSource.UNAVAILABLE) {
            throw new BusinessException("不能手动指定 UNAVAILABLE 作为配置来源");
        }
        if (!providerType.supportsEnvFallback() && requested != AiProviderConfigSource.DB) {
            throw new BusinessException("自定义 OpenAI-Compatible Provider 仅支持数据库托管配置");
        }
        return requested;
    }

    private int nextSortOrder(AiProviderEntity provider) {
        return provider.getApiKeys().stream()
                .map(AiProviderApiKeyEntity::getSortOrder)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private String normalizeBaseUrl(String input, AiProviderType providerType) {
        String value = blankToNull(input);
        if (value == null) {
            return catalogService.defaultBaseUrl(providerType);
        }
        return value;
    }

    private String blankToNull(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : raw.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
