package com.ose.exam;

import com.ose.model.AppEnums;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ExamDtos {
    private ExamDtos() {
    }

    public record ExamSummary(
            Long id,
            String name,
            AppEnums.ExamType type,
            Integer durationMinutes,
            BigDecimal totalScore,
            String description,
            Integer questionCount
    ) {
    }

    public record CreateExamRequest(
            @NotBlank(message = "请输入模拟卷名称") String name,
            @NotNull(message = "请选择模拟类型") AppEnums.ExamType type,
            @NotNull(message = "请输入考试时长") @Min(value = 30, message = "考试时长至少 30 分钟") Integer durationMinutes,
            String description,
            @NotEmpty(message = "请至少选择一道题目") List<Long> questionIds
    ) {
    }

    public record AttemptAnswerView(
            Long answerId,
            Long questionId,
            String title,
            String content,
            AppEnums.QuestionType questionType,
            List<com.ose.question.QuestionDtos.QuestionOptionDto> options,
            String answerText,
            BigDecimal autoScore,
            BigDecimal subjectiveScore,
            AppEnums.PracticeResult result,
            String referenceAnswer,
            String feedback,
            BigDecimal score
    ) {
    }

    public record AttemptView(
            Long id,
            Long examId,
            String examName,
            AppEnums.ExamType examType,
            AppEnums.AttemptStatus status,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            BigDecimal objectiveScore,
            BigDecimal subjectiveScore,
            BigDecimal totalScore,
            Integer durationSeconds,
            String selfReviewSummary,
            String systemReviewSummary,
            List<AttemptAnswerView> answers
    ) {
    }

    public record AttemptAnswerInput(Long questionId, String answerText, BigDecimal subjectiveScore, String feedback) {
    }

    public record SubmitAttemptRequest(Integer durationSeconds, String selfReviewSummary, List<AttemptAnswerInput> answers) {
    }

    public record ScoreAfternoonRequest(String selfReviewSummary, List<AttemptAnswerInput> answers) {
    }
}
