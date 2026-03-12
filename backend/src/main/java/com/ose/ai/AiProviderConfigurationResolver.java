package com.ose.ai;

import com.ose.common.exception.BusinessException;
import com.ose.model.AiModelEntity;
import com.ose.model.AiProviderEntity;
import com.ose.repository.AiProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiProviderConfigurationResolver {

    private final AiProviderRepository providerRepository;
    private final AiProviderCatalogService catalogService;
    private final AiApiKeyRotationService keyRotationService;
    private final AiDefaultModelService defaultModelService;
    private final AiSecretCryptoService cryptoService;

    public AiProviderConfigurationResolver(AiProviderRepository providerRepository,
                                           AiProviderCatalogService catalogService,
                                           AiApiKeyRotationService keyRotationService,
                                           AiDefaultModelService defaultModelService,
                                           AiSecretCryptoService cryptoService) {
        this.providerRepository = providerRepository;
        this.catalogService = catalogService;
        this.keyRotationService = keyRotationService;
        this.defaultModelService = defaultModelService;
        this.cryptoService = cryptoService;
    }

    public ResolvedAiProviderConfig resolve(String providerId) {
        if (catalogService.isEnvProviderId(providerId)) {
            return resolveEnvProvider(catalogService.envProviderType(providerId));
        }
        AiProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException("Provider 不存在"));
        return resolveDbProvider(provider);
    }

    public ResolvedAiModelSelection resolveModelSelection(String providerId, String modelId, AiModelUseCase useCase) {
        if (providerId != null && !providerId.isBlank()) {
            ResolvedAiProviderConfig provider = resolve(providerId);
            String resolvedModel = resolveRequestedModel(providerId, modelId);
            return new ResolvedAiModelSelection(provider.providerId(), provider.providerType(), provider.providerDisplayName(), resolvedModel);
        }

        ResolvedAiModelSelection defaultSelection = defaultModelService.resolveSelection(useCase);
        if (defaultSelection != null) {
            return defaultSelection;
        }

        List<AiProviderEntity> providers = providerRepository.findAll();
        ResolvedAiModelSelection fallback = defaultModelService.fallbackSelection(providers, useCase);
        if (fallback != null) {
            return fallback;
        }

        AiProviderType openAiEnv = catalogService.envApiKey(AiProviderType.OPENAI) == null || catalogService.envModels(AiProviderType.OPENAI).isEmpty()
                ? null
                : AiProviderType.OPENAI;
        if (openAiEnv != null) {
            return new ResolvedAiModelSelection(
                    catalogService.envProviderId(AiProviderType.OPENAI),
                    AiProviderType.OPENAI,
                    AiProviderType.OPENAI.defaultDisplayName(),
                    catalogService.defaultModel(AiProviderType.OPENAI)
            );
        }

        AiProviderType anthropicEnv = catalogService.envApiKey(AiProviderType.ANTHROPIC) == null || catalogService.envModels(AiProviderType.ANTHROPIC).isEmpty()
                ? null
                : AiProviderType.ANTHROPIC;
        if (anthropicEnv != null) {
            return new ResolvedAiModelSelection(
                    catalogService.envProviderId(AiProviderType.ANTHROPIC),
                    AiProviderType.ANTHROPIC,
                    AiProviderType.ANTHROPIC.defaultDisplayName(),
                    catalogService.defaultModel(AiProviderType.ANTHROPIC)
            );
        }

        return null;
    }

    public String resolveRequestedModel(String providerId, String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            ResolvedAiProviderConfig config = resolve(providerId);
            return config.defaultModel();
        }
        if (catalogService.isEnvProviderId(providerId)) {
            List<String> envModels = catalogService.envModels(catalogService.envProviderType(providerId));
            if (!envModels.contains(requestedModel)) {
                throw new BusinessException("指定模型不在当前 Provider 的可用列表中");
            }
            return requestedModel;
        }
        AiProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException("Provider 不存在"));
        return provider.getModels().stream()
                .filter(AiModelEntity::isEnabled)
                .map(AiModelEntity::getModelId)
                .filter(model -> model.equals(requestedModel))
                .findFirst()
                .orElseThrow(() -> new BusinessException("指定模型不在当前 Provider 的可用列表中"));
    }

    public List<AiQuestionDtos.AiModelConfig> availableModels(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return providerRepository.findAll().stream()
                    .filter(AiProviderEntity::isEnabled)
                    .flatMap(provider -> provider.getModels().stream()
                            .filter(AiModelEntity::isEnabled)
                            .map(model -> new AiQuestionDtos.AiModelConfig(model.getModelId(), model.getDisplayName(), false)))
                    .toList();
        }
        if (catalogService.isEnvProviderId(providerId)) {
            return catalogService.envModels(catalogService.envProviderType(providerId)).stream()
                    .map(model -> new AiQuestionDtos.AiModelConfig(model, model, model.equals(resolve(providerId).defaultModel())))
                    .toList();
        }
        ResolvedAiProviderConfig config = resolve(providerId);
        AiProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException("Provider 不存在"));
        return provider.getModels().stream()
                .filter(AiModelEntity::isEnabled)
                .sorted(Comparator.comparing(AiModelEntity::getSortOrder).thenComparing(AiModelEntity::getCreatedAt))
                .map(model -> new AiQuestionDtos.AiModelConfig(model.getModelId(), model.getDisplayName(), model.getModelId().equals(config.defaultModel())))
                .toList();
    }

    private ResolvedAiProviderConfig resolveDbProvider(AiProviderEntity provider) {
        Candidate dbCandidate = dbCandidate(provider);
        boolean hasEnabledModel = provider.getModels().stream().anyMatch(AiModelEntity::isEnabled);
        if (provider.getConfigSource() == AiProviderConfigSource.DB) {
            return dbCandidate.configured()
                    ? toResolved(provider, dbCandidate, AiProviderConfigSource.DB)
                    : unavailable(provider, dbCandidate.message(), AiProviderConfigSource.DB);
        }

        if (provider.getConfigSource() == AiProviderConfigSource.ENV) {
            Candidate envCandidate = envCandidate(provider.getProviderType());
            return envCandidate.configured()
                    ? toResolved(provider, envCandidate, AiProviderConfigSource.ENV)
                    : unavailable(provider, envCandidate.message(), AiProviderConfigSource.ENV);
        }

        Candidate envCandidate = envCandidate(provider.getProviderType());
        if (dbCandidate.configured()) {
            return toResolved(provider, dbCandidate, AiProviderConfigSource.DB);
        }
        if (envCandidate.configured() && hasEnabledModel) {
            return toResolved(provider, merge(provider, envCandidate), AiProviderConfigSource.HYBRID);
        }
        return unavailable(provider, hasEnabledModel ? "未检测到可用的数据库密钥或环境变量密钥" : "未添加启用模型", AiProviderConfigSource.HYBRID);
    }

    private ResolvedAiProviderConfig resolveEnvProvider(AiProviderType providerType) {
        Candidate candidate = envCandidate(providerType);
        String providerId = catalogService.envProviderId(providerType);
        return new ResolvedAiProviderConfig(
                providerId,
                providerType,
                providerType.defaultDisplayName() + "（ENV）",
                candidate.enabled(),
                candidate.configured(),
                candidate.apiKey(),
                candidate.baseUrl(),
                AiBaseUrlMode.ROOT,
                candidate.defaultModel(),
                candidate.timeoutMs(),
                candidate.maxRetries(),
                candidate.temperature(),
                candidate.maxTokens(),
                candidate.configured() ? AiProviderConfigSource.ENV : AiProviderConfigSource.UNAVAILABLE,
                candidate.message()
        );
    }

    private Candidate merge(AiProviderEntity provider, Candidate envCandidate) {
        return new Candidate(
                provider.isEnabled(),
                envCandidate.configured(),
                envCandidate.apiKey(),
                provider.getBaseUrl() == null || provider.getBaseUrl().isBlank() ? envCandidate.baseUrl() : provider.getBaseUrl(),
                provider.getDefaultModel() == null || provider.getDefaultModel().isBlank() ? envCandidate.defaultModel() : provider.getDefaultModel(),
                provider.getTimeoutMs() == null ? envCandidate.timeoutMs() : provider.getTimeoutMs(),
                provider.getMaxRetries() == null ? envCandidate.maxRetries() : provider.getMaxRetries(),
                provider.getTemperature() == null ? envCandidate.temperature() : provider.getTemperature(),
                envCandidate.maxTokens(),
                "数据库 Provider 使用环境变量兜底密钥"
        );
    }

    private Candidate dbCandidate(AiProviderEntity provider) {
        if (!provider.isEnabled()) {
            return new Candidate(
                    false,
                    false,
                    null,
                    provider.getBaseUrl(),
                    providerDefaultModel(provider),
                    providerTimeout(provider),
                    providerMaxRetries(provider),
                    providerTemperature(provider),
                    catalogService.maxTokens(provider.getProviderType()),
                    "Provider 未启用"
            );
        }

        ResolvedAiApiKey apiKey = keyRotationService.selectKey(provider);
        boolean hasEnabledModel = provider.getModels().stream().anyMatch(AiModelEntity::isEnabled);
        String message = apiKey == null ? "未配置可用 API Key" : hasEnabledModel ? "数据库配置可用" : "未添加启用模型";
        return new Candidate(
                true,
                apiKey != null && hasEnabledModel,
                apiKey,
                provider.getBaseUrl(),
                providerDefaultModel(provider),
                providerTimeout(provider),
                providerMaxRetries(provider),
                providerTemperature(provider),
                catalogService.maxTokens(provider.getProviderType()),
                message
        );
    }

    private Candidate envCandidate(AiProviderType providerType) {
        if (providerType == null || !providerType.supportsEnvFallback()) {
            return new Candidate(false, false, null, null, null, 0, 0, 0, 0, "当前 Provider 不支持环境变量兜底");
        }
        String apiKeyValue = catalogService.envApiKey(providerType);
        ResolvedAiApiKey apiKey = apiKeyValue == null || apiKeyValue.isBlank()
                ? null
                : new ResolvedAiApiKey("env-" + providerType.name().toLowerCase() + "-key", apiKeyValue, cryptoService.mask(apiKeyValue));
        List<String> models = catalogService.envModels(providerType);
        return new Candidate(
                true,
                apiKey != null && !models.isEmpty(),
                apiKey,
                catalogService.defaultBaseUrl(providerType),
                catalogService.defaultModel(providerType),
                catalogService.defaultTimeoutMs(),
                catalogService.defaultMaxRetries(),
                catalogService.defaultTemperature(providerType),
                catalogService.maxTokens(providerType),
                apiKey == null ? "环境变量未配置 API Key" : models.isEmpty() ? "环境变量未配置模型列表" : "环境变量配置可用"
        );
    }

    private ResolvedAiProviderConfig toResolved(AiProviderEntity provider, Candidate candidate, AiProviderConfigSource source) {
        return new ResolvedAiProviderConfig(
                provider.getId(),
                provider.getProviderType(),
                provider.getDisplayName(),
                candidate.enabled(),
                candidate.configured(),
                candidate.apiKey(),
                provider.getBaseUrl() == null || provider.getBaseUrl().isBlank() ? candidate.baseUrl() : provider.getBaseUrl(),
                provider.getBaseUrlMode(),
                providerDefaultModel(provider, candidate.defaultModel()),
                candidate.timeoutMs(),
                candidate.maxRetries(),
                candidate.temperature(),
                candidate.maxTokens(),
                source,
                candidate.message()
        );
    }

    private ResolvedAiProviderConfig unavailable(AiProviderEntity provider, String message, AiProviderConfigSource source) {
        return new ResolvedAiProviderConfig(
                provider.getId(),
                provider.getProviderType(),
                provider.getDisplayName(),
                provider.isEnabled(),
                false,
                null,
                provider.getBaseUrl(),
                provider.getBaseUrlMode(),
                providerDefaultModel(provider),
                providerTimeout(provider),
                providerMaxRetries(provider),
                providerTemperature(provider),
                catalogService.maxTokens(provider.getProviderType()),
                source,
                message
        );
    }

    private String providerDefaultModel(AiProviderEntity provider) {
        return providerDefaultModel(provider, catalogService.defaultModel(provider.getProviderType()));
    }

    private String providerDefaultModel(AiProviderEntity provider, String fallback) {
        return provider.getDefaultModel() == null || provider.getDefaultModel().isBlank() ? fallback : provider.getDefaultModel();
    }

    private int providerTimeout(AiProviderEntity provider) {
        return provider.getTimeoutMs() == null ? catalogService.defaultTimeoutMs() : provider.getTimeoutMs();
    }

    private int providerMaxRetries(AiProviderEntity provider) {
        return provider.getMaxRetries() == null ? catalogService.defaultMaxRetries() : provider.getMaxRetries();
    }

    private double providerTemperature(AiProviderEntity provider) {
        return provider.getTemperature() == null ? catalogService.defaultTemperature(provider.getProviderType()) : provider.getTemperature();
    }

    private record Candidate(
            boolean enabled,
            boolean configured,
            ResolvedAiApiKey apiKey,
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
