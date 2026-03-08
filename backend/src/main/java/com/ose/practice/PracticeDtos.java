package com.ose.practice;

import com.ose.model.AppEnums;
import com.ose.question.QuestionDtos;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PracticeDtos {
    private PracticeDtos() {
    }

    public record PracticeSessionRequest(
            @NotNull(message = "请选择练习模式") AppEnums.SessionType sessionType,
            @NotNull(message = "请选择题型") AppEnums.QuestionType questionType,
            Long knowledgePointId,
            @Min(value = 1, message = "题量至少为 1") Integer count
    ) {
    }

    public record PracticeQuestionView(
            Long questionId,
            String title,
            String content,
            AppEnums.QuestionType type,
            List<QuestionDtos.QuestionOptionDto> options,
            BigDecimal score,
            List<QuestionDtos.QuestionRelationDto> knowledgePoints,
            String referenceAnswer
    ) {
    }

    public record PracticeRecordView(
            Long recordId,
            PracticeQuestionView question,
            String userAnswer,
            BigDecimal autoScore,
            BigDecimal subjectiveScore,
            AppEnums.PracticeResult result,
            Boolean favorite,
            Boolean markedUnknown,
            Boolean addedToReview,
            Integer durationSeconds,
            LocalDateTime submittedAt
    ) {
    }

    public record PracticeSessionResponse(
            Long id,
            AppEnums.SessionType sessionType,
            AppEnums.QuestionType questionType,
            AppEnums.SessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            Integer questionCount,
            List<PracticeRecordView> records
    ) {
    }

    public record PracticeAnswerInput(
            Long recordId,
            String answer,
            Integer durationSeconds,
            BigDecimal subjectiveScore,
            String reasonType
    ) {
    }

    public record SubmitPracticeRequest(List<PracticeAnswerInput> answers) {
    }

    public record ReviewRecordRequest(BigDecimal subjectiveScore, String reasonType) {
    }

    public record UpdateFlagsRequest(Boolean favorite, Boolean markedUnknown, Boolean addedToReview) {
    }
}
