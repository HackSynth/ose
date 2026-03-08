package com.ose.plan;

import com.ose.common.exception.NotFoundException;
import com.ose.knowledge.KnowledgeService;
import com.ose.model.AppEnums;
import com.ose.model.KnowledgePoint;
import com.ose.model.StudyPlan;
import com.ose.model.StudyTask;
import com.ose.model.SystemSetting;
import com.ose.repository.StudyPlanRepository;
import com.ose.repository.StudyTaskRepository;
import com.ose.settings.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final StudyTaskRepository studyTaskRepository;
    private final SettingService settingService;
    private final KnowledgeService knowledgeService;

    public PlanDtos.PlanResponse currentPlan() {
        StudyPlan plan = studyPlanRepository.findFirstByStatusOrderByCreatedAtDesc(AppEnums.PlanStatus.ACTIVE)
                .orElseGet(this::generatePlan);
        return toDto(plan);
    }

    @Transactional
    public PlanDtos.PlanResponse generateCurrentPlan() {
        return toDto(generatePlan());
    }

    @Transactional
    public PlanDtos.TaskResponse updateTask(Long taskId, PlanDtos.UpdateTaskRequest request) {
        StudyTask task = studyTaskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("任务不存在"));
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.progress() != null) {
            task.setProgress(request.progress());
            if (request.progress() == 100 && task.getStatus() != AppEnums.TaskStatus.DONE) {
                task.setStatus(AppEnums.TaskStatus.DONE);
            }
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.postponedTo() != null) {
            task.setPostponedTo(request.postponedTo());
            task.setStatus(AppEnums.TaskStatus.DELAYED);
        }
        return toTaskDto(studyTaskRepository.save(task));
    }

    @Transactional
    public PlanDtos.PlanResponse rebalance(PlanDtos.RebalanceRequest request) {
        StudyPlan plan = studyPlanRepository.findFirstByStatusOrderByCreatedAtDesc(AppEnums.PlanStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("当前没有激活计划"));
        LocalDate start = request.fromDate() == null ? LocalDate.now().plusDays(1) : request.fromDate();
        List<StudyTask> tasks = studyTaskRepository.findByPlanOrderByScheduledDateAscIdAsc(plan).stream()
                .filter(task -> task.getStatus() == AppEnums.TaskStatus.DELAYED || task.getStatus() == AppEnums.TaskStatus.MISSED)
                .sorted(Comparator.comparing(StudyTask::getScheduledDate))
                .toList();
        AtomicInteger offset = new AtomicInteger(0);
        tasks.forEach(task -> {
            LocalDate candidate = nextStudyDate(start.plusDays(offset.getAndIncrement()));
            task.setScheduledDate(candidate);
            task.setPostponedTo(null);
            task.setStatus(AppEnums.TaskStatus.TODO);
        });
        studyTaskRepository.saveAll(tasks);
        return toDto(plan);
    }

    @Transactional
    protected StudyPlan generatePlan() {
        studyPlanRepository.findFirstByStatusOrderByCreatedAtDesc(AppEnums.PlanStatus.ACTIVE)
                .ifPresent(plan -> {
                    plan.setStatus(AppEnums.PlanStatus.ARCHIVED);
                    studyPlanRepository.save(plan);
                });
        SystemSetting setting = settingService.getOrCreateDefault();
        List<KnowledgePoint> points = knowledgeService.allEntities().stream().filter(point -> point.getLevel() >= 2).toList();
        LocalDate today = LocalDate.now();
        LocalDate examDate = setting.getExamDate();
        long totalDays = Math.max(7, ChronoUnit.DAYS.between(today, examDate));
        LocalDate foundationEnd = today.plusDays((long) (totalDays * 0.5));
        LocalDate intensiveEnd = today.plusDays((long) (totalDays * 0.8));

        StudyPlan plan = StudyPlan.builder()
                .name("2026 软件设计师备考计划")
                .examDate(examDate)
                .startDate(today)
                .endDate(examDate)
                .totalHours((int) Math.ceil(totalDays / 7.0 * setting.getWeeklyStudyHours()))
                .status(AppEnums.PlanStatus.ACTIVE)
                .settingSnapshot("考试日期=" + examDate + ";每周时长=" + setting.getWeeklyStudyHours() + ";偏好=" + setting.getLearningPreference())
                .tasks(new ArrayList<>())
                .build();

        AtomicInteger cursor = new AtomicInteger(0);
        for (LocalDate date = today; !date.isAfter(examDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            AppEnums.PlanPhase phase = date.isAfter(intensiveEnd) ? AppEnums.PlanPhase.SPRINT
                    : date.isAfter(foundationEnd) ? AppEnums.PlanPhase.INTENSIVE
                    : AppEnums.PlanPhase.FOUNDATION;
            KnowledgePoint point = points.isEmpty() ? null : points.get(cursor.getAndIncrement() % points.size());
            AppEnums.TaskType taskType = switch (phase) {
                case FOUNDATION -> date.getDayOfWeek() == DayOfWeek.SATURDAY ? AppEnums.TaskType.PRACTICE : AppEnums.TaskType.KNOWLEDGE;
                case INTENSIVE -> date.getDayOfWeek() == DayOfWeek.SATURDAY ? AppEnums.TaskType.EXAM : AppEnums.TaskType.PRACTICE;
                case SPRINT -> date.getDayOfWeek() == DayOfWeek.SATURDAY ? AppEnums.TaskType.EXAM : AppEnums.TaskType.REVIEW;
            };
            int estimated = date.getDayOfWeek() == DayOfWeek.SATURDAY ? Math.max(120, setting.getDailySessionMinutes()) : setting.getDailySessionMinutes();
            String title = buildTitle(phase, taskType, point);
            String description = buildDescription(phase, point, setting.getLearningPreference());
            StudyTask task = StudyTask.builder()
                    .plan(plan)
                    .phase(phase)
                    .taskType(taskType)
                    .status(AppEnums.TaskStatus.TODO)
                    .title(title)
                    .description(description)
                    .knowledgePoint(point)
                    .scheduledDate(date)
                    .estimatedMinutes(estimated)
                    .priority(phase == AppEnums.PlanPhase.SPRINT ? 5 : phase == AppEnums.PlanPhase.INTENSIVE ? 4 : 3)
                    .progress(0)
                    .build();
            plan.getTasks().add(task);
        }
        return studyPlanRepository.save(plan);
    }

    private String buildTitle(AppEnums.PlanPhase phase, AppEnums.TaskType taskType, KnowledgePoint point) {
        String topic = point == null ? "综合能力" : point.getName();
        return switch (taskType) {
            case KNOWLEDGE -> "夯实知识点：" + topic;
            case PRACTICE -> "刷题训练：" + topic;
            case REVIEW -> "错题复盘：" + topic;
            case EXAM -> phase == AppEnums.PlanPhase.INTENSIVE ? "阶段模拟：上午/下午混合演练" : "冲刺模拟：整卷限时练习";
            case NOTE -> "整理笔记：" + topic;
        };
    }

    private String buildDescription(AppEnums.PlanPhase phase, KnowledgePoint point, String preference) {
        String topic = point == null ? "综合题型" : point.getName();
        return switch (phase) {
            case FOUNDATION -> "基础阶段，以知识理解与例题拆解为主；主题=" + topic + "；偏好=" + preference;
            case INTENSIVE -> "强化阶段，以专题刷题和查漏补缺为主；主题=" + topic + "；偏好=" + preference;
            case SPRINT -> "冲刺阶段，以错题复盘、整卷模拟和时间控制为主；主题=" + topic + "；偏好=" + preference;
        };
    }

    private LocalDate nextStudyDate(LocalDate date) {
        LocalDate cursor = date;
        while (cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    private PlanDtos.PlanResponse toDto(StudyPlan plan) {
        return new PlanDtos.PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getExamDate(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getTotalHours(),
                plan.getStatus().name(),
                plan.getSettingSnapshot(),
                plan.getTasks().stream().map(this::toTaskDto).toList()
        );
    }

    private PlanDtos.TaskResponse toTaskDto(StudyTask task) {
        return new PlanDtos.TaskResponse(
                task.getId(),
                task.getPhase(),
                task.getTaskType(),
                task.getStatus(),
                task.getTitle(),
                task.getDescription(),
                task.getKnowledgePoint() == null ? null : task.getKnowledgePoint().getId(),
                task.getKnowledgePoint() == null ? null : task.getKnowledgePoint().getName(),
                task.getScheduledDate(),
                task.getEstimatedMinutes(),
                task.getPriority(),
                task.getProgress(),
                task.getPostponedTo()
        );
    }
}
