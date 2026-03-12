package com.ose.ai;

import com.ose.common.exception.BusinessException;
import com.ose.model.AiDefaultModelSettingsEntity;
import com.ose.model.AiModelEntity;
import com.ose.model.AiProviderEntity;
import com.ose.repository.AiDefaultModelSettingsRepository;
import com.ose.repository.AiModelRepository;
import com.ose.repository.AiProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiDefaultModelService {

    private final AiDefaultModelSettingsRepository repository;
    private final AiProviderRepository providerRepository;
    private final AiModelRepository modelRepository;

    public AiDefaultModelService(AiDefaultModelSettingsRepository repository,
                                 AiProviderRepository providerRepository,
                                 AiModelRepository modelRepository) {
        this.repository = repository;
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
    }

    public AiProviderAdminDtos.DefaultModelsResponse getDefaultModels() {
        AiDefaultModelSettingsEntity entity = loadOrCreate();
        return new AiProviderAdminDtos.DefaultModelsResponse(
                toSelection(entity.getQuestionGenerationProviderId(), entity.getQuestionGenerationModelId()),
                toSelection(entity.getReviewSummaryProviderId(), entity.getReviewSummaryModelId()),
                toSelection(entity.getPracticeRecommendationProviderId(), entity.getPracticeRecommendationModelId())
        );
    }

    @Transactional
    public AiProviderAdminDtos.DefaultModelsResponse update(AiProviderAdminDtos.UpdateDefaultModelsRequest request) {
        AiDefaultModelSettingsEntity entity = loadOrCreate();
        validateSelection(request.questionGeneration(), AiModelUseCase.QUESTION_GENERATION);
        validateSelection(request.reviewSummary(), AiModelUseCase.REVIEW_SUMMARY);
        validateSelection(request.practiceRecommendation(), AiModelUseCase.PRACTICE_RECOMMENDATION);

        entity.setQuestionGenerationProviderId(providerIdOf(request.questionGeneration()));
        entity.setQuestionGenerationModelId(modelIdOf(request.questionGeneration()));
        entity.setReviewSummaryProviderId(providerIdOf(request.reviewSummary()));
        entity.setReviewSummaryModelId(modelIdOf(request.reviewSummary()));
        entity.setPracticeRecommendationProviderId(providerIdOf(request.practiceRecommendation()));
        entity.setPracticeRecommendationModelId(modelIdOf(request.practiceRecommendation()));
        repository.save(entity);
        return getDefaultModels();
    }

    public ResolvedAiModelSelection resolveSelection(AiModelUseCase useCase) {
        AiDefaultModelSettingsEntity entity = loadOrCreate();
        String providerId = switch (useCase) {
            case QUESTION_GENERATION -> entity.getQuestionGenerationProviderId();
            case REVIEW_SUMMARY -> entity.getReviewSummaryProviderId();
            case PRACTICE_RECOMMENDATION -> entity.getPracticeRecommendationProviderId();
        };
        String modelId = switch (useCase) {
            case QUESTION_GENERATION -> entity.getQuestionGenerationModelId();
            case REVIEW_SUMMARY -> entity.getReviewSummaryModelId();
            case PRACTICE_RECOMMENDATION -> entity.getPracticeRecommendationModelId();
        };
        if (providerId == null || modelId == null) {
            return null;
        }
        AiProviderEntity provider = providerRepository.findById(providerId).orElse(null);
        if (provider == null) {
            return null;
        }
        AiModelEntity model = modelRepository.findByIdAndProviderId(modelId, providerId).orElse(null);
        if (model == null || !provider.isEnabled() || !model.isEnabled()) {
            return null;
        }
        return new ResolvedAiModelSelection(provider.getId(), provider.getProviderType(), provider.getDisplayName(), model.getModelId());
    }

    public ResolvedAiModelSelection fallbackSelection(List<AiProviderEntity> providers, AiModelUseCase useCase) {
        return providers.stream()
                .filter(AiProviderEntity::isEnabled)
                .sorted(Comparator.comparing(AiProviderEntity::getDisplayName))
                .flatMap(provider -> provider.getModels().stream()
                        .filter(AiModelEntity::isEnabled)
                        .sorted(Comparator.comparing(AiModelEntity::getSortOrder).thenComparing(AiModelEntity::getDisplayName))
                        .map(model -> new ResolvedAiModelSelection(
                                provider.getId(),
                                provider.getProviderType(),
                                provider.getDisplayName(),
                                model.getModelId()
                        )))
                .findFirst()
                .orElse(null);
    }

    private void validateSelection(AiProviderAdminDtos.DefaultModelSelection selection, AiModelUseCase useCase) {
        if (selection == null || selection.providerId() == null || selection.modelId() == null) {
            return;
        }
        AiProviderEntity provider = providerRepository.findById(selection.providerId())
                .orElseThrow(() -> new BusinessException(useCaseLabel(useCase) + "默认模型对应的 Provider 不存在"));
        if (!provider.isEnabled()) {
            throw new BusinessException(useCaseLabel(useCase) + "默认模型要求 Provider 已启用");
        }
        AiModelEntity model = modelRepository.findByIdAndProviderId(selection.modelId(), selection.providerId())
                .orElseThrow(() -> new BusinessException(useCaseLabel(useCase) + "默认模型不存在"));
        if (!model.isEnabled()) {
            throw new BusinessException(useCaseLabel(useCase) + "默认模型必须处于启用状态");
        }
    }

    private String useCaseLabel(AiModelUseCase useCase) {
        return switch (useCase) {
            case QUESTION_GENERATION -> "出题";
            case REVIEW_SUMMARY -> "复盘摘要";
            case PRACTICE_RECOMMENDATION -> "推荐练习";
        };
    }

    private AiDefaultModelSettingsEntity loadOrCreate() {
        return repository.findById(AiDefaultModelSettingsEntity.SINGLETON_ID)
                .orElseGet(() -> repository.save(AiDefaultModelSettingsEntity.builder()
                        .id(AiDefaultModelSettingsEntity.SINGLETON_ID)
                        .build()));
    }

    private AiProviderAdminDtos.DefaultModelSelection toSelection(String providerId, String modelId) {
        if (providerId == null || modelId == null) {
            return null;
        }
        return new AiProviderAdminDtos.DefaultModelSelection(providerId, modelId);
    }

    private String providerIdOf(AiProviderAdminDtos.DefaultModelSelection selection) {
        return selection == null ? null : selection.providerId();
    }

    private String modelIdOf(AiProviderAdminDtos.DefaultModelSelection selection) {
        return selection == null ? null : selection.modelId();
    }
}
