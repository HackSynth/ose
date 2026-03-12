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
    private final KnowledgePointRepository knowledgePointRepository;
    private final MistakeRecordRepository mistakeRecordRepository;
    private final QuestionRepository questionRepository;
    private final AiGenerationRecordRepository aiGenerationRecordRepository;
    private final AiPromptBuilder promptBuilder;
    private final AiQuestionValidationService validationService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public List<AiQuestionDtos.AiProviderStatus> providers() {
        return providerClients.stream()
                .sorted(Comparator.comparing(item -> item.provider().name()))
                .map(client -> new AiQuestionDtos.AiProviderStatus(
                        client.provider(),
                        client.isConfigured(),
                        client.isConfigured() ? "可用" : "未配置 API Key",
                        client.models()))
                .toList();
    }

    public List<AiQuestionDtos.AiModelConfig> models(AiProviderType provider) {
        if (provider == null) {
            return providerClients.stream().flatMap(item -> item.models().stream()).toList();
        }
        return findProvider(provider).models();
    }

    public AiQuestionDtos.AiHealthResponse health() {
        List<AiQuestionDtos.AiProviderStatus> providers = providers();
        int configuredCount = (int) providers.stream().filter(AiQuestionDtos.AiProviderStatus::configured).count();
        return new AiQuestionDtos.AiHealthResponse(configuredCount > 0, configuredCount, providers);
    }

    @Transactional
    public AiQuestionDtos.AiQuestionGenerationResult generate(AiQuestionDtos.AiQuestionGenerationRequest request) {
        AiProviderClient providerClient = findProvider(request.provider());
        if (!providerClient.isConfigured()) {
            throw new BusinessException(providerClient.provider().name() + " 未配置 API Key，当前为优雅降级模式");
        }

        List<KnowledgePoint> knowledgePoints = resolveKnowledgePoints(request);
        List<String> knowledgeNames = knowledgePoints.stream().map(KnowledgePoint::getName).toList();
        String systemPrompt = promptBuilder.buildSystemPrompt(request);
        String userPrompt = promptBuilder.buildUserPrompt(request, knowledgeNames);
        String jsonSchema = promptBuilder.batchSchema(request.questionType());

        AiGenerationRecord record = AiGenerationRecord.builder()
                .provider(request.provider())
                .model(resolveModel(request))
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
            AiQuestionDtos.ProviderGenerationPayload payload = providerClient.generate(request, systemPrompt, userPrompt, jsonSchema);
            List<String> schemaErrors = validationService.validateSchema(payload);
            List<String> businessErrors = validationService.validateBusiness(payload);
            List<String> errors = new ArrayList<>();
            errors.addAll(schemaErrors);
            errors.addAll(businessErrors);

            List<AiQuestionDtos.AiQuestionDraft> drafts = toDrafts(payload, request, knowledgePoints);
            record.setStatus(errors.isEmpty() ? "SUCCESS" : "REJECTED");
            record.setSuccessCount(errors.isEmpty() ? drafts.size() : 0);
            record.setResponsePayload(writeJson(payload));
            if (!errors.isEmpty()) {
                record.setErrorMessage(String.join("；", errors));
            }
            aiGenerationRecordRepository.save(record);

            return new AiQuestionDtos.AiQuestionGenerationResult(
                    record.getId(),
                    request.provider(),
                    resolveModel(request),
                    request.questionType(),
                    DISCLAIMER,
                    errors.isEmpty(),
                    errors,
                    drafts
            );
        } catch (AiProviderException ex) {
            record.setStatus("FAILED");
            record.setErrorMessage(ex.getMessage());
            aiGenerationRecordRepository.save(record);
            throw new BusinessException(ex.getMessage());
        } catch (Exception ex) {
            record.setStatus("FAILED");
            record.setErrorMessage("AI 生成失败: " + ex.getMessage());
            aiGenerationRecordRepository.save(record);
            throw new BusinessException("AI 生成失败，请稍后重试");
        }
    }

    @Transactional
    public AiQuestionDtos.AiSaveResult save(AiQuestionDtos.AiQuestionSaveRequest request) {
        List<Long> savedIds = new ArrayList<>();
        List<Question> questions = request.drafts().stream().map(draft -> toQuestion(draft, request.provider(), request.model(), request.questionType())).toList();
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

    private AiProviderClient findProvider(AiProviderType providerType) {
        return providerClients.stream()
                .filter(client -> client.provider() == providerType)
                .findFirst()
                .orElseThrow(() -> new BusinessException("不支持的 AI Provider"));
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
                                                           List<KnowledgePoint> knowledgePoints) {
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
                    request.provider(),
                    resolveModel(request),
                    "AI",
                    List.of("AI生成", request.provider().name())
            );
        }).toList();
    }

    private Question toQuestion(AiQuestionDtos.AiQuestionDraftInput draft,
                                AiProviderType provider,
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
        question.setSource("AI-" + provider.name() + "-" + model);
        List<String> tags = new ArrayList<>();
        tags.add("AI生成");
        if (draft.tags() != null) {
            tags.addAll(draft.tags());
        }
        question.setTags(tags.stream().distinct().collect(Collectors.joining(",")));
        question.setScore(questionType == AppEnums.QuestionType.MORNING_SINGLE ? BigDecimal.ONE : new BigDecimal("15"));
        question.setActive(!appProperties.getAi().isEnableSaveReview());
        question.setAiGenerated(true);
        question.setAiProvider(provider.name());
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

    private String resolveModel(AiQuestionDtos.AiQuestionGenerationRequest request) {
        if (request.model() != null && !request.model().isBlank()) {
            return request.model();
        }
        return request.provider() == AiProviderType.OPENAI
                ? appProperties.getAi().getOpenai().getDefaultModel()
                : appProperties.getAi().getAnthropic().getDefaultModel();
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
