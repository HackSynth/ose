package com.ose.data;

import com.ose.knowledge.KnowledgeDtos;
import com.ose.note.NoteDtos;
import com.ose.plan.PlanDtos;
import com.ose.question.QuestionDtos;
import com.ose.settings.SettingDtos;

import java.util.List;

public final class DataImportDtos {
    private DataImportDtos() {
    }

    public enum DuplicateStrategy {
        SKIP,
        OVERWRITE
    }

    public record FullImportRequest(
            SettingDtos.SettingResponse settings,
            List<KnowledgeDtos.KnowledgeTreeItem> knowledgeTree,
            List<QuestionDtos.QuestionResponse> questions,
            PlanDtos.PlanResponse plan,
            List<NoteDtos.NoteView> notes
    ) {
    }

    public record SectionResult(int created, int updated, int skipped) {
    }

    public record ImportError(String scope, String identifier, String message) {
    }

    public record FullImportResult(
            DuplicateStrategy strategy,
            SectionResult settings,
            SectionResult knowledgePoints,
            SectionResult questions,
            SectionResult notes,
            SectionResult studyPlans,
            List<String> warnings,
            List<ImportError> errors
    ) {
    }
}
