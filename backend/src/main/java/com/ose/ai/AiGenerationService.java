package com.ose.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import com.ose.common.exception.NotFoundException;
import com.ose.model.AiGenerationRecord;
import com.ose.model.AppEnums;
import com.ose.model.KnowledgePoint;
import com.ose.model.Question;
import com.ose.model.QuestionOption;
import com.ose.repository.AiGenerationRecordRepository;
import com.ose.repository.KnowledgePointRepository;
import com.ose.repository.MistakeRecordRepository;
import com.ose.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiGenerationService {

    private static final String DISCLAIMER = "AI 生成题目仅供辅助学习，内容需人工确认后使用。";

    private final List<AiProviderClient> providerClients;
    private final AiProviderService providerService;
    private final AiProviderConfigurationResolver resolver;
    private final AiApiKeyRotationService keyRotationService;
    private final KnowledgePointRepository knowledgePointRepository;
    private final MistakeRecordRepository mistakeRecordRepository;
    private final QuestionRepository questionRepository;
    private final AiGenerationRecordRepository aiGenerationRecordRepository;
    private final AiPromptBuilder promptBuilder;
    private final AiQuestionValidationService validationService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public List<AiQuestionDtos.AiProviderStatus> providers() {
        return providerService.listProviders().stream()
                .map(provider -> {
                    ResolvedAiProviderConfig config = safeResolve(provider.id());
                    return new AiQuestionDtos.AiProviderStatus(
                            provider.id(),
                            provider.providerType(),
                            provider.displayName(),
                            config != null && config.isAvailable(),
                            config == null ? "未配置" : config.message(),
                            resolver.availableModels(provider.id())
                    );
                })
                .toList();
    }

    public List<AiQuestionDtos.AiModelConfig> models(AiProviderType providerType) {
        if (providerType == null) {
            return providerService.listProviders().stream()
                    .flatMap(provider -> resolver.availableModels(provider.id()).stream())
                    .toList();
        }
        return providerService.listProviders().stream()
                .filter(provider -> provider.providerType() == providerType)
                .findFirst()
                .map(provider -> resolver.availableModels(provider.id()))
                .orElse(List.of());
    }

    public AiQuestionDtos.AiHealthResponse health() {
        List<AiQuestionDtos.AiProviderStatus> providers = providers();
        int configuredCount = (int) providers.stream().filter(AiQuestionDtos.AiProviderStatus::configured).count();
        return new AiQuestionDtos.AiHealthResponse(configuredCount > 0, configuredCount, providers);
    }

    @Transactional
    public AiQuestionDtos.AiQuestionGenerationResult generate(AiQuestionDtos.AiQuestionGenerationRequest request) {
        ResolvedAiModelSelection selection = resolveRequestedSelection(request);
        if (selection == null) {
            throw new BusinessException("当前没有可用的 AI Provider 或模型，请先在模型服务页完成配置");
        }

        ResolvedAiProviderConfig config = resolver.resolve(selection.providerId());
        if (!config.isAvailable()) {
            throw new BusinessException(config.providerDisplayName() + " 不可用: " + config.message());
        }

        AiProviderClient providerClient = findProviderClient(config.providerType());
        List<KnowledgePoint> knowledgePoints = resolveKnowledgePoints(request);
        List<String> knowledgeNames = knowledgePoints.stream().map(KnowledgePoint::getName).toList();
        String systemPrompt = promptBuilder.buildSystemPrompt(request);
        String userPrompt = promptBuilder.buildUserPrompt(request, knowledgeNames);
        String jsonSchema = promptBuilder.batchSchema(request.questionType());

        AiGenerationRecord record = AiGenerationRecord.builder()
                .providerId(selection.providerId())
                .provider(selection.providerType())
                .providerDisplayName(selection.providerDisplayName())
                .model(selection.modelId())
                .questionType(request.questionType())
                .topicType(request.topicType().name())
                .difficulty(request.difficulty().name())
                .requestedCount(request.count())
                .successCount(0)
                .status("PENDING")
                .promptHash(hashPrompt(systemPrompt + userPrompt))
                .promptSummary(userPrompt.substring(0, Math.min(280, userPrompt.length())))
                .requestPayload(writeJson(request))
                .build();
        aiGenerationRecordRepository.save(record);

        try {
            AiQuestionDtos.ProviderGenerationPayload payload = providerClient.generate(
                    config,
                    selection.modelId(),
                    systemPrompt,
                    userPrompt,
                    jsonSchema
            );
            keyRotationService.recordSuccess(config.apiKeyId());
            List<String> schemaErrors = validationService.validateSchema(payload);
            List<String> businessErrors = validationService.validateBusiness(payload);
            List<String> errors = new ArrayList<>();
            errors.addAll(schemaErrors);
            errors.addAll(businessErrors);

            List<AiQuestionDtos.AiQuestionDraft> drafts = toDrafts(payload, request, knowledgePoints, selection);
            record.setStatus(errors.isEmpty() ? "SUCCESS" : "REJECTED");
            record.setSuccessCount(errors.isEmpty() ? drafts.size() : 0);
            record.setResponsePayload(writeJson(payload));
            if (!errors.isEmpty()) {
                record.setErrorMessage(String.join("；", errors));
            }
            aiGenerationRecordRepository.save(record);

            return new AiQuestionDtos.AiQuestionGenerationResult(
                    record.getId(),
                    selection.providerId(),
                    selection.providerType(),
                    selection.providerDisplayName(),
                    selection.modelId(),
                    request.questionType(),
                    DISCLAIMER,
                    errors.isEmpty(),
                    errors,
                    drafts
            );
        } catch (AiProviderException ex) {
            keyRotationService.recordFailure(config.apiKeyId());
            record.setStatus("FAILED");
            record.setErrorMessage(ex.getMessage());
            aiGenerationRecordRepository.save(record);
            throw new BusinessException(ex.getMessage());
        } catch (Exception ex) {
            keyRotationService.recordFailure(config.apiKeyId());
            record.setStatus("FAILED");
            record.setErrorMessage("AI 生成失败: " + ex.getMessage());
            aiGenerationRecordRepository.save(record);
            throw new BusinessException("AI 生成失败，请稍后重试");
        }
    }

    @Transactional
    public AiQuestionDtos.AiSaveResult save(AiQuestionDtos.AiQuestionSaveRequest request) {
        ResolvedAiModelSelection selection = resolveSaveSelection(request);
        List<Long> savedIds = new ArrayList<>();
        List<Question> questions = request.drafts().stream()
                .map(draft -> toQuestion(draft, selection.providerDisplayName(), selection.modelId(), request.questionType()))
                .toList();
        questionRepository.saveAll(questions).forEach(item -> savedIds.add(item.getId()));

        if (request.generationId() != null) {
            AiGenerationRecord record = aiGenerationRecordRepository.findById(request.generationId())
                    .orElseThrow(() -> new NotFoundException("AI 生成记录不存在"));
            String status = appProperties.getAi().isEnableSaveReview() ? "PENDING_REVIEW" : "SAVED";
            record.setFinishStatus(status);
            record.setStatus("SAVED");
            record.setSuccessCount(savedIds.size());
            aiGenerationRecordRepository.save(record);
        }

        String saveStatus = appProperties.getAi().isEnableSaveReview() ? "PENDING_REVIEW" : "SAVED";
        return new AiQuestionDtos.AiSaveResult(request.generationId(), savedIds.size(), saveStatus, savedIds);
    }

    public List<AiQuestionDtos.AiGenerationHistoryItem> history() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return aiGenerationRecordRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(item -> new AiQuestionDtos.AiGenerationHistoryItem(
                        item.getId(),
                        item.getStatus(),
                        item.getErrorMessage(),
                        item.getProvider(),
                        item.getModel(),
                        item.getQuestionType(),
                        item.getRequestedCount(),
                        item.getSuccessCount(),
                        item.getCreatedAt().format(formatter)
                )).toList();
    }

    private ResolvedAiModelSelection resolveRequestedSelection(AiQuestionDtos.AiQuestionGenerationRequest request) {
        String requestedProviderId = request.providerId();
        if ((requestedProviderId == null || requestedProviderId.isBlank()) && request.provider() != null) {
            requestedProviderId = providerService.listProviders().stream()
                    .filter(provider -> provider.providerType() == request.provider())
                    .findFirst()
                    .map(AiProviderAdminDtos.ProviderDetail::id)
                    .orElse(null);
        }
        ResolvedAiModelSelection selection = resolver.resolveModelSelection(requestedProviderId, request.model(), AiModelUseCase.QUESTION_GENERATION);
        if (selection == null) {
            return null;
        }
        resolver.resolveRequestedModel(selection.providerId(), selection.modelId());
        return selection;
    }

    private ResolvedAiModelSelection resolveSaveSelection(AiQuestionDtos.AiQuestionSaveRequest request) {
        String providerId = request.providerId();
        if ((providerId == null || providerId.isBlank()) && request.provider() != null) {
            providerId = providerService.listProviders().stream()
                    .filter(provider -> provider.providerType() == request.provider())
                    .findFirst()
                    .map(AiProviderAdminDtos.ProviderDetail::id)
                    .orElse(null);
        }
        if (providerId == null || providerId.isBlank()) {
            throw new BusinessException("保存 AI 题目时缺少 Provider 标识");
        }
        ResolvedAiProviderConfig config = resolver.resolve(providerId);
        String modelId = resolver.resolveRequestedModel(providerId, request.model());
        return new ResolvedAiModelSelection(providerId, config.providerType(), config.providerDisplayName(), modelId);
    }

    private AiProviderClient findProviderClient(AiProviderType providerType) {
        return providerClients.stream()
                .filter(client -> client.providerType() == providerType)
                .findFirst()
                .orElseThrow(() -> new BusinessException("不支持的 AI Provider"));
    }

    private ResolvedAiProviderConfig safeResolve(String providerId) {
        try {
            return resolver.resolve(providerId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private List<KnowledgePoint> resolveKnowledgePoints(AiQuestionDtos.AiQuestionGenerationRequest request) {
        if (request.topicType() == AiQuestionDtos.AiQuestionTopicType.WEAK_KNOWLEDGE) {
            return knowledgePointRepository.findAllByOrderByLevelAscSortOrderAsc().stream()
                    .sorted(Comparator.comparingInt(KnowledgePoint::getMasteryLevel))
                    .limit(Math.max(1, request.count() / 2))
                    .toList();
        }
        if (request.topicType() == AiQuestionDtos.AiQuestionTopicType.MISTAKE_SIMILAR) {
            Set<Long> ids = mistakeRecordRepository.findTop10ByOrderByUpdatedAtDesc().stream()
                    .map(item -> item.getKnowledgePoint() == null ? null : item.getKnowledgePoint().getId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                return knowledgePointRepository.findAllById(ids);
            }
        }
        List<KnowledgePoint> points = knowledgePointRepository.findAllById(request.knowledgePointIds());
        if (points.isEmpty()) {
            throw new BusinessException("未找到有效知识点，请先维护知识点后再生成");
        }
        return points;
    }

    private List<AiQuestionDtos.AiQuestionDraft> toDrafts(AiQuestionDtos.ProviderGenerationPayload payload,
                                                          AiQuestionDtos.AiQuestionGenerationRequest request,
                                                          List<KnowledgePoint> knowledgePoints,
                                                          ResolvedAiModelSelection selection) {
        Map<String, Long> knowledgeNameMap = new LinkedHashMap<>();
        for (KnowledgePoint point : knowledgePoints) {
            knowledgeNameMap.put(point.getName(), point.getId());
        }
        return payload.questions().stream().map(item -> {
            List<Long> knowledgeIds = item.knowledgePointNames() == null ? List.of() : item.knowledgePointNames().stream()
                    .map(name -> knowledgeNameMap.getOrDefault(name, null))
                    .filter(Objects::nonNull)
                    .toList();
            if (knowledgeIds.isEmpty()) {
                knowledgeIds = List.copyOf(request.knowledgePointIds());
            }
            return new AiQuestionDtos.AiQuestionDraft(
                    UUID.randomUUID().toString(),
                    request.questionType(),
                    item.title(),
                    item.content(),
                    item.options(),
                    item.correctAnswer(),
                    item.explanation(),
                    item.referenceAnswer(),
                    item.scoringPoints(),
                    knowledgeIds,
                    item.knowledgePointNames(),
                    parseDifficulty(item.difficulty(), request.difficulty()),
                    selection.providerId(),
                    selection.providerType(),
                    selection.providerDisplayName(),
                    selection.modelId(),
                    "AI",
                    List.of("AI生成", selection.providerDisplayName())
            );
        }).toList();
    }

    private Question toQuestion(AiQuestionDtos.AiQuestionDraftInput draft,
                                String providerDisplayName,
                                String model,
                                AppEnums.QuestionType questionType) {
        Question question = new Question();
        question.setType(questionType);
        question.setTitle(draft.title());
        question.setContent(draft.content());
        question.setCorrectAnswer(draft.correctAnswer());
        question.setExplanation(draft.explanation());
        if (questionType == AppEnums.QuestionType.AFTERNOON_CASE) {
            String reference = draft.referenceAnswer();
            if (draft.scoringPoints() != null && !draft.scoringPoints().isEmpty()) {
                reference = (reference == null ? "" : reference + "\n") + "评分点:\n- " + String.join("\n- ", draft.scoringPoints());
            }
            question.setReferenceAnswer(reference);
        } else {
            question.setReferenceAnswer(draft.referenceAnswer());
        }
        question.setYear(java.time.LocalDate.now().getYear());
        question.setDifficulty(toDifficultyScore(draft.difficulty()));
        question.setSource("AI-" + providerDisplayName + "-" + model);
        List<String> tags = new ArrayList<>();
        tags.add("AI生成");
        if (draft.tags() != null) {
            tags.addAll(draft.tags());
        }
        question.setTags(tags.stream().distinct().collect(Collectors.joining(",")));
        question.setScore(questionType == AppEnums.QuestionType.MORNING_SINGLE ? BigDecimal.ONE : new BigDecimal("15"));
        question.setActive(!appProperties.getAi().isEnableSaveReview());
        question.setAiGenerated(true);
        question.setAiProvider(providerDisplayName);
        question.setAiModel(model);

        if (questionType == AppEnums.QuestionType.MORNING_SINGLE && draft.options() != null) {
            for (AiQuestionDtos.QuestionOptionDraftInput option : draft.options()) {
                if (option.key() == null || option.content() == null || option.content().isBlank()) {
                    continue;
                }
                question.getOptions().add(QuestionOption.builder()
                        .question(question)
                        .optionKey(option.key())
                        .content(option.content())
                        .build());
            }
        }
        question.getKnowledgePoints().addAll(knowledgePointRepository.findAllById(draft.knowledgePointIds()));
        return question;
    }

    private int toDifficultyScore(AiQuestionDtos.AiQuestionDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 2;
            case MEDIUM -> 3;
            case HARD -> 5;
        };
    }

    private AiQuestionDtos.AiQuestionDifficulty parseDifficulty(String value, AiQuestionDtos.AiQuestionDifficulty fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return AiQuestionDtos.AiQuestionDifficulty.valueOf(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String hashPrompt(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("序列化失败", ex);
            return null;
        }
    }
}
