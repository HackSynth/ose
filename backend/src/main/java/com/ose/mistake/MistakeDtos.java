package com.ose.mistake;

import com.ose.model.AppEnums;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public final class MistakeDtos {
    private MistakeDtos() {
    }

    public record MistakeView(
            Long id,
            Long questionId,
            String questionTitle,
            String questionType,
            Long knowledgePointId,
            String knowledgePointName,
            String reasonType,
            AppEnums.ReviewStatus reviewStatus,
            LocalDate nextReviewAt,
            Integer reviewCount,
            String note,
            String latestResult
    ) {
    }

    public record UpdateMistakeRequest(
            @NotBlank(message = "请选择错因") String reasonType,
            AppEnums.ReviewStatus reviewStatus,
            LocalDate nextReviewAt,
            String note
    ) {
    }
}
