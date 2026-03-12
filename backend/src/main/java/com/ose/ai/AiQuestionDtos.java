package com.ose.ai;

import com.ose.model.AppEnums;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AiQuestionDtos {
    private AiQuestionDtos() {
    }

    public enum AiQuestionTopicType {
        KNOWLEDGE_POINT,
        DIFFICULTY,
        QUESTION_TYPE,
        EXAM_PHASE,
        WEAK_KNOWLEDGE,
        MISTAKE_SIMILAR,
        MORNING_SET,
        AFTERNOON_SET
    }

    public enum AiQuestionDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public enum AiStyleType {
        EXAM,
        FOUNDATION,
        COMPREHENSIVE
    }

    public record AiModelConfig(String model, String displayName, boolean isDefault) {
    }

    public record AiQuestionGenerationRequest(
            String providerId,
            AiProviderType provider,
            String model,
            @NotNull(message = "请选择题型") AppEnums.QuestionType questionType,
            @NotNull(message = "请选择出题场景") AiQuestionTopicType topicType,
            @NotEmpty(message = "请至少选择一个知识点") List<Long> knowledgePointIds,
            @NotNull(message = "请选择难度") AiQuestionDifficulty difficulty,
            @NotNull(message = "请输入数量") @Min(value = 1, message = "数量最少为 1") @Max(value = 20, message = "数量不能超过 20") Integer count,
            Boolean includeExplanation,
            Boolean includeAnswer,
            Boolean saveToBank,
            String language,
            @NotNull(message = "请选择出题风格") AiStyleType styleType,
            String additionalRequirement
    ) {
    }

    public record AiQuestionDraft(
            String draftId,
            AppEnums.QuestionType questionType,
            @NotBlank String title,
            @NotBlank String content,
            List<QuestionOptionDraft> options,
            String correctAnswer,
            String explanation,
            String referenceAnswer,
            List<String> scoringPoints,
            List<Long> knowledgePointIds,
            List<String> knowledgePointNames,
            AiQuestionDifficulty difficulty,
            String providerId,
            AiProviderType provider,
            String providerDisplayName,
            String model,
            String source,
            List<String> tags
    ) {
    }

    public record QuestionOptionDraft(String key, String content) {
    }

    public record AiQuestionGenerationResult(
            Long generationId,
            String providerId,
            AiProviderType provider,
            String providerDisplayName,
            String model,
            AppEnums.QuestionType questionType,
            String disclaimer,
            boolean saveAllowed,
            List<String> validationErrors,
            List<AiQuestionDraft> drafts
    ) {
    }

    public record AiQuestionSaveRequest(
            Long generationId,
            String providerId,
            AiProviderType provider,
            @NotBlank(message = "请指定模型") String model,
            @NotNull(message = "请选择题型") AppEnums.QuestionType questionType,
            @NotEmpty(message = "保存列表不能为空") @Valid List<AiQuestionDraftInput> drafts
    ) {
    }

    public record AiQuestionDraftInput(
            @NotBlank(message = "题目标题不能为空") String title,
            @NotBlank(message = "题干不能为空") String content,
            List<QuestionOptionDraftInput> options,
            String correctAnswer,
            String explanation,
            String referenceAnswer,
            List<String> scoringPoints,
            @NotEmpty(message = "至少关联一个知识点") List<Long> knowledgePointIds,
            @NotNull(message = "难度不能为空") AiQuestionDifficulty difficulty,
            List<String> tags
    ) {
    }

    public record QuestionOptionDraftInput(String key, String content) {
    }

    public record AiSaveResult(Long generationId, int savedCount, String status, List<Long> questionIds) {
    }

    public record AiProviderStatus(
            String providerId,
            AiProviderType provider,
            String displayName,
            boolean configured,
            String statusMessage,
            List<AiModelConfig> models
    ) {
    }

    public record AiHealthResponse(boolean enabled, int configuredProviders, List<AiProviderStatus> providers) {
    }

    public record AiGenerationHistoryItem(
            Long id,
            String status,
            String errorMessage,
            AiProviderType provider,
            String model,
            AppEnums.QuestionType questionType,
            Integer requestedCount,
            Integer successCount,
            String createdAt
    ) {
    }

    public record ProviderGenerationPayload(
            AppEnums.QuestionType questionType,
            List<ProviderQuestionPayload> questions
    ) {
    }

    public record ProviderQuestionPayload(
            String title,
            String content,
            List<QuestionOptionDraft> options,
            String correctAnswer,
            String explanation,
            String referenceAnswer,
            List<String> scoringPoints,
            List<String> knowledgePointNames,
            String difficulty
    ) {
    }
}
