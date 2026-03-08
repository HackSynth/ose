package com.ose.dashboard;

import com.ose.analytics.AnalyticsService;
import com.ose.exam.ExamService;
import com.ose.knowledge.KnowledgeService;
import com.ose.mistake.MistakeService;
import com.ose.model.*;
import com.ose.note.NoteService;
import com.ose.plan.PlanService;
import com.ose.repository.MistakeRecordRepository;
import com.ose.repository.PracticeRecordRepository;
import com.ose.settings.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final SettingService settingService;
    private final PlanService planService;
    private final KnowledgeService knowledgeService;
    private final MistakeService mistakeService;
    private final ExamService examService;
    private final NoteService noteService;
    private final AnalyticsService analyticsService;
    private final PracticeRecordRepository practiceRecordRepository;
    private final MistakeRecordRepository mistakeRecordRepository;

    public DashboardDtos.DashboardOverview overview() {
        var setting = settingService.getSetting();
        var plan = planService.currentPlan();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);
        LocalDate monthEnd = today.plusDays(30);
        var todayTasks = plan.tasks().stream().filter(task -> task.scheduledDate().equals(today)).toList();
        long weekDone = plan.tasks().stream()
                .filter(task -> !task.scheduledDate().isBefore(today) && !task.scheduledDate().isAfter(weekEnd) && task.status() == AppEnums.TaskStatus.DONE)
                .count();
        long weekTotal = plan.tasks().stream().filter(task -> !task.scheduledDate().isBefore(today) && !task.scheduledDate().isAfter(weekEnd)).count();
        long monthDone = plan.tasks().stream()
                .filter(task -> !task.scheduledDate().isBefore(today) && !task.scheduledDate().isAfter(monthEnd) && task.status() == AppEnums.TaskStatus.DONE)
                .count();
        long monthTotal = plan.tasks().stream().filter(task -> !task.scheduledDate().isBefore(today) && !task.scheduledDate().isAfter(monthEnd)).count();
        var knowledgeOverview = knowledgeService.allEntities().stream()
                .filter(point -> point.getLevel() == 1)
                .map(point -> new DashboardDtos.KnowledgeOverview(point.getName(), point.getMasteryLevel(), point.getWeight()))
                .toList();
        var recentMistakes = mistakeService.list(null, null).stream().limit(5)
                .map(item -> new DashboardDtos.RecentItem(item.id(), item.questionTitle(), item.reasonType(), item.nextReviewAt().toString()))
                .toList();
        var recentExams = examService.recentAttempts().stream().limit(5)
                .map(item -> new DashboardDtos.RecentItem(item.getId(), item.getMockExam().getName(), item.getMockExam().getType().name(), item.getTotalScore().toString()))
                .toList();
        var recentNotes = noteService.recentNotes().stream().limit(5)
                .map(item -> new DashboardDtos.RecentItem(item.getId(), item.getTitle(), item.getSummary(), item.getUpdatedAt().toLocalDate().toString()))
                .toList();
        List<MistakeRecord> dueMistakes = mistakeService.dueMistakes();
        return new DashboardDtos.DashboardOverview(
                setting.examDate(),
                setting.daysUntilExam(),
                todayTasks,
                new DashboardDtos.CompletionBlock(weekDone, weekTotal),
                new DashboardDtos.CompletionBlock(monthDone, monthTotal),
                knowledgeOverview,
                recentMistakes,
                recentExams,
                recentNotes,
                analyticsService.summary().cards(),
                (long) dueMistakes.size(),
                dueMistakes.stream().limit(5)
                        .map(item -> new DashboardDtos.ReviewReminder(
                                item.getId(),
                                item.getQuestion().getTitle(),
                                item.getKnowledgePoint() == null ? "未分类" : item.getKnowledgePoint().getName(),
                                item.getNextReviewAt(),
                                item.getReasonType()
                        ))
                        .toList(),
                buildPracticeRecommendations()
        );
    }

    private List<DashboardDtos.PracticeRecommendation> buildPracticeRecommendations() {
        Map<Long, List<PracticeRecord>> practiceByKnowledge = practiceRecordRepository.findAll().stream()
                .filter(record -> !record.getQuestion().getKnowledgePoints().isEmpty())
                .collect(Collectors.groupingBy(record -> record.getQuestion().getKnowledgePoints().stream().findFirst().map(KnowledgePoint::getId).orElse(-1L)));
        Map<Long, Long> mistakeCounts = mistakeRecordRepository.findAll().stream()
                .filter(item -> item.getKnowledgePoint() != null)
                .collect(Collectors.groupingBy(item -> item.getKnowledgePoint().getId(), Collectors.counting()));
        return knowledgeService.allEntities().stream()
                .filter(point -> point.getLevel() >= 2)
                .map(point -> {
                    List<PracticeRecord> records = practiceByKnowledge.getOrDefault(point.getId(), List.of());
                    BigDecimal accuracy = records.isEmpty()
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(records.stream().filter(record -> record.getResult() == AppEnums.PracticeResult.CORRECT).count())
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
                    long pendingMistakes = mistakeCounts.getOrDefault(point.getId(), 0L);
                    BigDecimal score = BigDecimal.valueOf(100 - point.getMasteryLevel())
                            .add(BigDecimal.valueOf(100).subtract(accuracy).multiply(new BigDecimal("0.6")))
                            .add(BigDecimal.valueOf(pendingMistakes * 8L));
                    String reason = pendingMistakes > 0 && accuracy.compareTo(new BigDecimal("60")) < 0
                            ? "正确率偏低且仍有错题待复习"
                            : point.getMasteryLevel() < 60
                            ? "掌握度偏低，建议专题强化"
                            : "建议通过专项练习巩固近期薄弱点";
                    return new RecommendationHolder(point, accuracy, pendingMistakes, reason, score);
                })
                .sorted(Comparator.comparing(RecommendationHolder::score).reversed())
                .limit(4)
                .map(item -> new DashboardDtos.PracticeRecommendation(
                        item.point().getId(),
                        item.point().getName(),
                        item.point().getMasteryLevel(),
                        item.accuracy(),
                        item.pendingMistakes(),
                        item.reason()
                ))
                .toList();
    }

    private record RecommendationHolder(KnowledgePoint point, BigDecimal accuracy, Long pendingMistakes, String reason, BigDecimal score) {
    }
}
