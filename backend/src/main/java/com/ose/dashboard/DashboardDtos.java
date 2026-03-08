package com.ose.dashboard;

import com.ose.analytics.AnalyticsDtos;
import com.ose.plan.PlanDtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record RecentItem(Long id, String title, String subtitle, String extra) {
    }

    public record KnowledgeOverview(String name, Integer masteryLevel, Integer weight) {
    }

    public record CompletionBlock(Long done, Long total) {
    }

    public record ReviewReminder(Long id, String questionTitle, String knowledgePointName, LocalDate nextReviewAt, String reasonType) {
    }

    public record PracticeRecommendation(Long knowledgePointId, String knowledgePointName, Integer masteryLevel, BigDecimal accuracy, Long pendingMistakes, String reason) {
    }

    public record DashboardOverview(
            LocalDate examDate,
            long daysUntilExam,
            List<PlanDtos.TaskResponse> todayTasks,
            CompletionBlock weekCompletion,
            CompletionBlock monthCompletion,
            List<KnowledgeOverview> knowledgeOverview,
            List<RecentItem> recentMistakes,
            List<RecentItem> recentExams,
            List<RecentItem> recentNotes,
            List<AnalyticsDtos.SummaryCard> summaryCards,
            Long dueReviewCount,
            List<ReviewReminder> reviewReminders,
            List<PracticeRecommendation> practiceRecommendations
    ) {
    }
}
