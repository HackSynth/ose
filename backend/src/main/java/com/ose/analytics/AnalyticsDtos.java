package com.ose.analytics;

import java.math.BigDecimal;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record SummaryCard(String label, String value, String hint) {
    }

    public record KnowledgeStat(String name, Long practiceCount, BigDecimal correctRate) {
    }

    public record TrendPoint(String label, BigDecimal value) {
    }

    public record DistributionItem(String name, Long value) {
    }

    public record AnalyticsSummary(
            List<SummaryCard> cards,
            List<KnowledgeStat> knowledgeStats,
            List<DistributionItem> mistakeDistribution
    ) {
    }

    public record AnalyticsTrends(
            List<TrendPoint> examTrend,
            List<TrendPoint> planCompletionTrend,
            List<TrendPoint> practiceTrend
    ) {
    }
}
