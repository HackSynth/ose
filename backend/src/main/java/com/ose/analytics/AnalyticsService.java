package com.ose.analytics;

import com.ose.exam.ExamService;
import com.ose.model.*;
import com.ose.plan.PlanService;
import com.ose.practice.PracticeService;
import com.ose.repository.KnowledgePointRepository;
import com.ose.repository.MistakeRecordRepository;
import com.ose.repository.PracticeRecordRepository;
import com.ose.repository.QuestionRepository;
import com.ose.settings.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalyticsService {

    private final QuestionRepository questionRepository;
    private final PracticeRecordRepository practiceRecordRepository;
    private final MistakeRecordRepository mistakeRecordRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final SettingService settingService;
    private final PlanService planService;
    private final PracticeService practiceService;
    private final ExamService examService;

    public AnalyticsDtos.AnalyticsSummary summary() {
        List<PracticeRecord> records = practiceRecordRepository.findAll();
        List<MistakeRecord> mistakes = mistakeRecordRepository.findAll();
        var setting = settingService.getSetting();
        var plan = planService.currentPlan();
        long doneTasks = plan.tasks().stream().filter(task -> task.status() == AppEnums.TaskStatus.DONE).count();
        BigDecimal planCompletion = percentage(doneTasks, plan.tasks().size());
        BigDecimal avgAccuracy = percentage(records.stream().filter(record -> record.getResult() == AppEnums.PracticeResult.CORRECT).count(), records.size());

        List<AnalyticsDtos.SummaryCard> cards = List.of(
                new AnalyticsDtos.SummaryCard("题库总量", String.valueOf(questionRepository.count()), "含上午题与下午题"),
                new AnalyticsDtos.SummaryCard("练习正确率", avgAccuracy + "%", "基于已评分练习记录"),
                new AnalyticsDtos.SummaryCard("计划完成率", planCompletion + "%", "当前激活计划"),
                new AnalyticsDtos.SummaryCard("距离考试", String.valueOf(setting.daysUntilExam()), "距离目标考试日剩余天数")
        );

        Map<String, List<PracticeRecord>> groupedByKnowledge = records.stream().collect(Collectors.groupingBy(record ->
                record.getQuestion().getKnowledgePoints().stream().findFirst().map(KnowledgePoint::getName).orElse("未分类")
        ));
        List<AnalyticsDtos.KnowledgeStat> knowledgeStats = groupedByKnowledge.entrySet().stream()
                .map(entry -> new AnalyticsDtos.KnowledgeStat(
                        entry.getKey(),
                        (long) entry.getValue().size(),
                        percentage(entry.getValue().stream().filter(record -> record.getResult() == AppEnums.PracticeResult.CORRECT).count(), entry.getValue().size())
                ))
                .sorted(Comparator.comparing(AnalyticsDtos.KnowledgeStat::practiceCount).reversed())
                .limit(8)
                .toList();

        List<AnalyticsDtos.DistributionItem> mistakeDistribution = mistakes.stream()
                .collect(Collectors.groupingBy(item -> item.getKnowledgePoint() == null ? "未分类" : item.getKnowledgePoint().getName(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new AnalyticsDtos.DistributionItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AnalyticsDtos.DistributionItem::value).reversed())
                .toList();

        return new AnalyticsDtos.AnalyticsSummary(cards, knowledgeStats, mistakeDistribution);
    }

    public AnalyticsDtos.AnalyticsTrends trends() {
        List<AnalyticsDtos.TrendPoint> examTrend = examService.recentAttempts().stream()
                .sorted(Comparator.comparing(MockExamAttempt::getStartedAt))
                .map(attempt -> new AnalyticsDtos.TrendPoint(attempt.getStartedAt().toLocalDate().toString(), attempt.getTotalScore()))
                .toList();

        Map<Integer, Long> planWeekly = planService.currentPlan().tasks().stream()
                .filter(task -> task.status() == AppEnums.TaskStatus.DONE)
                .collect(Collectors.groupingBy(task -> task.scheduledDate().get(WeekFields.ISO.weekOfWeekBasedYear()), Collectors.counting()));
        List<AnalyticsDtos.TrendPoint> planCompletionTrend = planWeekly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AnalyticsDtos.TrendPoint("W" + entry.getKey(), BigDecimal.valueOf(entry.getValue())))
                .toList();

        Map<LocalDate, Long> practiceDaily = practiceService.recentRecords().stream()
                .filter(record -> record.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(record -> record.getSubmittedAt().toLocalDate(), Collectors.counting()));
        List<AnalyticsDtos.TrendPoint> practiceTrend = practiceDaily.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AnalyticsDtos.TrendPoint(entry.getKey().toString(), BigDecimal.valueOf(entry.getValue())))
                .toList();

        return new AnalyticsDtos.AnalyticsTrends(examTrend, planCompletionTrend, practiceTrend);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
