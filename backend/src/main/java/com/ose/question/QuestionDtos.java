package com.ose.question;

import com.ose.model.AppEnums;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class QuestionDtos {
    private QuestionDtos() {
    }

    public record QuestionOptionDto(Long id, String key, String content) {
    }

    public record QuestionRelationDto(Long id, String code, String name) {
    }

    public record QuestionResponse(
            Long id,
            AppEnums.QuestionType type,
            String title,
            String content,
            List<QuestionOptionDto> options,
            String correctAnswer,
            String explanation,
            String referenceAnswer,
            Integer year,
            Integer difficulty,
            String source,
            List<String> tags,
            List<QuestionRelationDto> knowledgePoints,
            BigDecimal score,
            Boolean active
    ) {
    }

    public record QuestionSaveRequest(
            @NotNull(message = "请选择题型") AppEnums.QuestionType type,
            @NotBlank(message = "请输入题目标题") String title,
            @NotBlank(message = "请输入题目内容") String content,
            List<QuestionOptionInput> options,
            String correctAnswer,
            String explanation,
            String referenceAnswer,
            @NotNull(message = "请输入年份") Integer year,
            @NotNull(message = "请输入难度") @Min(value = 1, message = "难度必须 >= 1") Integer difficulty,
            @NotBlank(message = "请输入来源") String source,
            @NotEmpty(message = "请至少提供一个标签") List<String> tags,
            List<Long> knowledgePointIds,
            @NotNull(message = "请输入分值") BigDecimal score,
            Boolean active
    ) {
    }

    public record QuestionOptionInput(String key, String content) {
    }

    public record QuestionImportResult(int importedCount, List<String> errors) {
    }
}
