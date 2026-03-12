package com.ose.ai;

import com.ose.common.exception.BusinessException;
import com.ose.common.exception.NotFoundException;
import com.ose.model.AiModelEntity;
import com.ose.model.AiProviderEntity;
import com.ose.repository.AiModelRepository;
import com.ose.repository.AiProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AiModelRegistryService {

    private final AiProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final AiDefaultModelService defaultModelService;

    public AiModelRegistryService(AiProviderRepository providerRepository,
                                  AiModelRepository modelRepository,
                                  AiDefaultModelService defaultModelService) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.defaultModelService = defaultModelService;
    }

    public List<AiProviderAdminDtos.ModelDetail> list(String providerId) {
        AiProviderEntity provider = findProvider(providerId);
        return provider.getModels().stream()
                .sorted(Comparator.comparing(AiModelEntity::getSortOrder).thenComparing(AiModelEntity::getCreatedAt))
                .map(this::toDetail)
                .toList();
    }

    @Transactional
    public AiProviderAdminDtos.ModelDetail create(String providerId, AiProviderAdminDtos.CreateModelRequest request) {
        AiProviderEntity provider = findProvider(providerId);
        AiModelEntity entity = AiModelEntity.builder()
                .id(UUID.randomUUID().toString())
                .provider(provider)
                .modelId(request.modelId().trim())
                .displayName(request.displayName().trim())
                .modelType(request.modelType() == null ? AiModelType.CHAT : request.modelType())
                .capabilityTags(joinTags(request.capabilityTags()))
                .enabled(request.enabled() == null || request.enabled())
                .sortOrder(resolveSortOrder(request.sortOrder(), provider))
                .build();
        modelRepository.save(entity);
        return toDetail(entity);
    }

    @Transactional
    public AiProviderAdminDtos.ModelDetail update(String providerId, String modelId, AiProviderAdminDtos.UpdateModelRequest request) {
        AiModelEntity entity = modelRepository.findByIdAndProviderId(modelId, providerId)
                .orElseThrow(() -> new NotFoundException("模型不存在"));
        if (request.modelId() != null && !request.modelId().isBlank()) {
            entity.setModelId(request.modelId().trim());
        }
        if (request.displayName() != null && !request.displayName().isBlank()) {
            entity.setDisplayName(request.displayName().trim());
        }
        if (request.modelType() != null) {
            entity.setModelType(request.modelType());
        }
        if (request.capabilityTags() != null) {
            entity.setCapabilityTags(joinTags(request.capabilityTags()));
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        return toDetail(modelRepository.save(entity));
    }

    @Transactional
    public void delete(String providerId, String modelId) {
        AiModelEntity entity = modelRepository.findByIdAndProviderId(modelId, providerId)
                .orElseThrow(() -> new NotFoundException("模型不存在"));
        modelRepository.delete(entity);
    }

    public AiProviderAdminDtos.ModelDetail validateEnabledModel(String providerId, String modelId, String sceneLabel) {
        AiProviderEntity provider = findProvider(providerId);
        if (!provider.isEnabled()) {
            throw new BusinessException(sceneLabel + "指定的 Provider 未启用");
        }
        AiModelEntity model = modelRepository.findByIdAndProviderId(modelId, providerId)
                .orElseThrow(() -> new BusinessException(sceneLabel + "指定的模型不存在"));
        if (!model.isEnabled()) {
            throw new BusinessException(sceneLabel + "指定的模型未启用");
        }
        return toDetail(model);
    }

    private AiProviderEntity findProvider(String providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider 不存在"));
    }

    private Integer resolveSortOrder(Integer requestedSortOrder, AiProviderEntity provider) {
        if (requestedSortOrder != null) {
            return requestedSortOrder;
        }
        return provider.getModels().stream()
                .map(AiModelEntity::getSortOrder)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private AiProviderAdminDtos.ModelDetail toDetail(AiModelEntity entity) {
        AiProviderAdminDtos.DefaultModelsResponse defaults = defaultModelService.getDefaultModels();
        return new AiProviderAdminDtos.ModelDetail(
                entity.getId(),
                entity.getProvider().getId(),
                entity.getModelId(),
                entity.getDisplayName(),
                entity.getModelType(),
                splitTags(entity.getCapabilityTags()),
                entity.isEnabled(),
                isDefault(defaults.questionGeneration(), entity),
                isDefault(defaults.reviewSummary(), entity),
                isDefault(defaults.practiceRecommendation(), entity),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private boolean isDefault(AiProviderAdminDtos.DefaultModelSelection selection, AiModelEntity entity) {
        return selection != null
                && entity.getProvider().getId().equals(selection.providerId())
                && entity.getId().equals(selection.modelId());
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream().map(String::trim).filter(item -> !item.isBlank()).distinct().reduce((a, b) -> a + "," + b).orElse(null);
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String tag : tags.split(",")) {
            String trimmed = tag.trim();
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return values;
    }
}
