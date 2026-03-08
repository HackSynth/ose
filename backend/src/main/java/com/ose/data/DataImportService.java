package com.ose.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.knowledge.KnowledgeDtos;
import com.ose.knowledge.KnowledgeService;
import com.ose.model.*;
import com.ose.note.NoteDtos;
import com.ose.note.NoteService;
import com.ose.plan.PlanDtos;
import com.ose.question.QuestionDtos;
import com.ose.question.QuestionService;
import com.ose.repository.*;
import com.ose.settings.SettingDtos;
import com.ose.settings.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DataImportService {

    private final ObjectMapper objectMapper;
    private final SettingService settingService;
    private final KnowledgeService knowledgeService;
    private final QuestionService questionService;
    private final NoteService noteService;
    private final KnowledgePointRepository knowledgePointRepository;
    private final QuestionRepository questionRepository;
    private final NoteRepository noteRepository;
    private final StudyPlanRepository studyPlanRepository;

    @Transactional
    public DataImportDtos.FullImportResult importFull(DataImportDtos.FullImportRequest request,
                                                      DataImportDtos.DuplicateStrategy strategy) {
        MutableSection settings = new MutableSection();
        MutableSection knowledge = new MutableSection();
        MutableSection questions = new MutableSection();
        MutableSection notes = new MutableSection();
        MutableSection plans = new MutableSection();
        List<String> warnings = new ArrayList<>();
        List<DataImportDtos.ImportError> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new DataImportDtos.ImportError("bundle", "root", "导入内容为空"));
            return buildResult(strategy, settings, knowledge, questions, notes, plans, warnings, errors);
        }

        importSettings(request.settings(), settings, errors);
        importKnowledgeTree(request.knowledgeTree(), strategy, knowledge, warnings, errors);
        importQuestions(request.questions(), strategy, questions, warnings, errors);
        importNotes(request.notes(), strategy, notes, errors);
        importPlan(request.plan(), strategy, plans, warnings, errors);

        return buildResult(strategy, settings, knowledge, questions, notes, plans, warnings, errors);
    }

    @Transactional
    public DataImportDtos.FullImportResult importFromFile(MultipartFile file,
                                                          DataImportDtos.DuplicateStrategy strategy) throws IOException {
        JsonNode root = objectMapper.readTree(file.getBytes());
        JsonNode payload = root.has("data") ? root.get("data") : root;
        DataImportDtos.FullImportRequest request = objectMapper.treeToValue(payload, DataImportDtos.FullImportRequest.class);
        return importFull(request, strategy);
    }

    private void importSettings(SettingDtos.SettingResponse settings,
                                MutableSection section,
                                List<DataImportDtos.ImportError> errors) {
        if (settings == null) {
            return;
        }
        if (settings.examDate() == null || settings.passingScore() == null || settings.weeklyStudyHours() == null || settings.dailySessionMinutes() == null) {
            errors.add(new DataImportDtos.ImportError("settings", "system", "设置缺少必要字段"));
            return;
        }
        List<Integer> reviewIntervals = settings.reviewIntervals() == null || settings.reviewIntervals().isEmpty()
                ? List.of(1, 3, 7, 14)
                : settings.reviewIntervals();
        settingService.updateSetting(new SettingDtos.UpdateSettingRequest(
                settings.examDate(),
                settings.passingScore(),
                settings.weeklyStudyHours(),
                Optional.ofNullable(settings.learningPreference()).filter(text -> !text.isBlank()).orElse("导入的学习偏好"),
                reviewIntervals,
                settings.dailySessionMinutes()
        ));
        section.updated++;
    }

    private void importKnowledgeTree(List<KnowledgeDtos.KnowledgeTreeItem> tree,
                                     DataImportDtos.DuplicateStrategy strategy,
                                     MutableSection section,
                                     List<String> warnings,
                                     List<DataImportDtos.ImportError> errors) {
        if (tree == null || tree.isEmpty()) {
            return;
        }
        tree.forEach(item -> importKnowledgeNode(item, null, strategy, section, warnings, errors));
    }

    private void importKnowledgeNode(KnowledgeDtos.KnowledgeTreeItem item,
                                     Long parentId,
                                     DataImportDtos.DuplicateStrategy strategy,
                                     MutableSection section,
                                     List<String> warnings,
                                     List<DataImportDtos.ImportError> errors) {
        if (item.code() == null || item.code().isBlank() || item.name() == null || item.name().isBlank()) {
            errors.add(new DataImportDtos.ImportError("knowledge", String.valueOf(item.code()), "知识点编码或名称不能为空"));
            return;
        }
        KnowledgeDtos.KnowledgeRequest request = new KnowledgeDtos.KnowledgeRequest(
                item.code(),
                item.name(),
                item.level() == null ? 1 : item.level(),
                item.masteryLevel() == null ? 50 : item.masteryLevel(),
                item.weight() == null ? 5 : item.weight(),
                item.note(),
                1,
                parentId
        );
        var existing = knowledgePointRepository.findByCode(item.code());
        Long resolvedId;
        if (existing.isPresent()) {
            if (strategy == DataImportDtos.DuplicateStrategy.SKIP) {
                section.skipped++;
                resolvedId = existing.get().getId();
            } else {
                resolvedId = knowledgeService.update(existing.get().getId(), request).id();
                section.updated++;
            }
        } else {
            resolvedId = knowledgeService.create(request).id();
            section.created++;
        }
        if (item.children() != null) {
            item.children().forEach(child -> importKnowledgeNode(child, resolvedId, strategy, section, warnings, errors));
        }
        if (existing.isPresent() && strategy == DataImportDtos.DuplicateStrategy.SKIP) {
            warnings.add("知识点已存在，已跳过：" + item.code());
        }
    }

    private void importQuestions(List<QuestionDtos.QuestionResponse> questionList,
                                 DataImportDtos.DuplicateStrategy strategy,
                                 MutableSection section,
                                 List<String> warnings,
                                 List<DataImportDtos.ImportError> errors) {
        if (questionList == null || questionList.isEmpty()) {
            return;
        }
        for (QuestionDtos.QuestionResponse question : questionList) {
            if (question.type() == null || question.title() == null || question.title().isBlank() || question.content() == null || question.content().isBlank()) {
                errors.add(new DataImportDtos.ImportError("questions", String.valueOf(question.title()), "题目缺少必要字段"));
                continue;
            }
            List<Long> knowledgeIds = Optional.ofNullable(question.knowledgePoints()).orElse(List.of()).stream()
                    .map(item -> knowledgePointRepository.findByCode(item.code()).orElse(null))
                    .filter(Objects::nonNull)
                    .map(KnowledgePoint::getId)
                    .toList();
            QuestionDtos.QuestionSaveRequest request = new QuestionDtos.QuestionSaveRequest(
                    question.type(),
                    question.title(),
                    question.content(),
                    Optional.ofNullable(question.options()).orElse(List.of()).stream()
                            .map(option -> new QuestionDtos.QuestionOptionInput(option.key(), option.content()))
                            .toList(),
                    question.correctAnswer(),
                    question.explanation(),
                    question.referenceAnswer(),
                    question.year(),
                    question.difficulty(),
                    Optional.ofNullable(question.source()).filter(text -> !text.isBlank()).orElse("导入题目"),
                    Optional.ofNullable(question.tags()).filter(tags -> !tags.isEmpty()).orElse(List.of("导入")),
                    knowledgeIds,
                    question.score(),
                    question.active()
            );
            var existing = questionRepository.findFirstByTypeAndYearAndTitle(question.type(), question.year(), question.title());
            try {
                if (existing.isPresent()) {
                    if (strategy == DataImportDtos.DuplicateStrategy.SKIP) {
                        section.skipped++;
                        warnings.add("题目已存在，已跳过：" + question.title());
                    } else {
                        questionService.update(existing.get().getId(), request);
                        section.updated++;
                    }
                } else {
                    questionService.create(request);
                    section.created++;
                }
            } catch (Exception ex) {
                errors.add(new DataImportDtos.ImportError("questions", question.title(), ex.getMessage()));
            }
        }
    }

    private void importNotes(List<NoteDtos.NoteView> noteViews,
                             DataImportDtos.DuplicateStrategy strategy,
                             MutableSection section,
                             List<DataImportDtos.ImportError> errors) {
        if (noteViews == null || noteViews.isEmpty()) {
            return;
        }
        for (NoteDtos.NoteView note : noteViews) {
            if (note.title() == null || note.title().isBlank() || note.content() == null || note.content().isBlank()) {
                errors.add(new DataImportDtos.ImportError("notes", String.valueOf(note.title()), "笔记标题或内容不能为空"));
                continue;
            }
            NoteDtos.NoteSaveRequest request = new NoteDtos.NoteSaveRequest(
                    note.title(),
                    note.content(),
                    note.summary(),
                    note.favorite(),
                    Optional.ofNullable(note.links()).orElse(List.of()).stream()
                            .map(link -> new NoteDtos.NoteLinkInput(link.linkType(), link.targetId()))
                            .toList()
            );
            var existing = noteRepository.findFirstByTitleIgnoreCase(note.title());
            try {
                if (existing.isPresent()) {
                    if (strategy == DataImportDtos.DuplicateStrategy.SKIP) {
                        section.skipped++;
                    } else {
                        noteService.update(existing.get().getId(), request);
                        section.updated++;
                    }
                } else {
                    noteService.create(request);
                    section.created++;
                }
            } catch (Exception ex) {
                errors.add(new DataImportDtos.ImportError("notes", note.title(), ex.getMessage()));
            }
        }
    }

    private void importPlan(PlanDtos.PlanResponse plan,
                            DataImportDtos.DuplicateStrategy strategy,
                            MutableSection section,
                            List<String> warnings,
                            List<DataImportDtos.ImportError> errors) {
        if (plan == null) {
            return;
        }
        var existingActive = studyPlanRepository.findFirstByStatusOrderByCreatedAtDesc(AppEnums.PlanStatus.ACTIVE);
        if (existingActive.isPresent() && strategy == DataImportDtos.DuplicateStrategy.SKIP) {
            section.skipped++;
            warnings.add("当前已存在激活计划，已跳过整包计划导入");
            return;
        }
        existingActive.ifPresent(existing -> {
            existing.setStatus(AppEnums.PlanStatus.ARCHIVED);
            studyPlanRepository.save(existing);
        });
        try {
            StudyPlan entity = StudyPlan.builder()
                    .name(Optional.ofNullable(plan.name()).filter(text -> !text.isBlank()).orElse("导入学习计划"))
                    .examDate(Optional.ofNullable(plan.examDate()).orElse(LocalDate.now().plusDays(90)))
                    .startDate(Optional.ofNullable(plan.startDate()).orElse(LocalDate.now()))
                    .endDate(Optional.ofNullable(plan.endDate()).orElse(Optional.ofNullable(plan.examDate()).orElse(LocalDate.now().plusDays(90))))
                    .totalHours(Optional.ofNullable(plan.totalHours()).orElse(0))
                    .status(parsePlanStatus(plan.status()))
                    .settingSnapshot(Optional.ofNullable(plan.settingSnapshot()).orElse("导入计划"))
                    .tasks(new ArrayList<>())
                    .build();
            for (PlanDtos.TaskResponse task : Optional.ofNullable(plan.tasks()).orElse(List.of())) {
                KnowledgePoint knowledgePoint = resolveKnowledgeForTask(task, warnings);
                entity.getTasks().add(StudyTask.builder()
                        .plan(entity)
                        .phase(Optional.ofNullable(task.phase()).orElse(AppEnums.PlanPhase.FOUNDATION))
                        .taskType(Optional.ofNullable(task.taskType()).orElse(AppEnums.TaskType.KNOWLEDGE))
                        .status(Optional.ofNullable(task.status()).orElse(AppEnums.TaskStatus.TODO))
                        .title(Optional.ofNullable(task.title()).filter(text -> !text.isBlank()).orElse("导入任务"))
                        .description(task.description())
                        .knowledgePoint(knowledgePoint)
                        .scheduledDate(Optional.ofNullable(task.scheduledDate()).orElse(LocalDate.now()))
                        .estimatedMinutes(Optional.ofNullable(task.estimatedMinutes()).orElse(60))
                        .priority(Optional.ofNullable(task.priority()).orElse(3))
                        .progress(Optional.ofNullable(task.progress()).orElse(0))
                        .postponedTo(task.postponedTo())
                        .build());
            }
            studyPlanRepository.save(entity);
            if (existingActive.isPresent()) {
                section.updated++;
            } else {
                section.created++;
            }
        } catch (Exception ex) {
            errors.add(new DataImportDtos.ImportError("plan", Optional.ofNullable(plan.name()).orElse("current"), ex.getMessage()));
        }
    }

    private KnowledgePoint resolveKnowledgeForTask(PlanDtos.TaskResponse task, List<String> warnings) {
        if (task.knowledgePointId() != null) {
            var byId = knowledgePointRepository.findById(task.knowledgePointId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        if (task.knowledgePointName() != null && !task.knowledgePointName().isBlank()) {
            var byName = knowledgePointRepository.findFirstByNameIgnoreCase(task.knowledgePointName());
            if (byName.isPresent()) {
                return byName.get();
            }
            warnings.add("计划任务未找到匹配知识点，已忽略关联：" + task.knowledgePointName());
        }
        return null;
    }

    private AppEnums.PlanStatus parsePlanStatus(String status) {
        if (status == null || status.isBlank()) {
            return AppEnums.PlanStatus.ACTIVE;
        }
        try {
            return AppEnums.PlanStatus.valueOf(status);
        } catch (Exception ignored) {
            return AppEnums.PlanStatus.ACTIVE;
        }
    }

    private DataImportDtos.FullImportResult buildResult(DataImportDtos.DuplicateStrategy strategy,
                                                        MutableSection settings,
                                                        MutableSection knowledge,
                                                        MutableSection questions,
                                                        MutableSection notes,
                                                        MutableSection plans,
                                                        List<String> warnings,
                                                        List<DataImportDtos.ImportError> errors) {
        return new DataImportDtos.FullImportResult(
                strategy,
                settings.toResult(),
                knowledge.toResult(),
                questions.toResult(),
                notes.toResult(),
                plans.toResult(),
                warnings,
                errors
        );
    }

    private static final class MutableSection {
        private int created;
        private int updated;
        private int skipped;

        private DataImportDtos.SectionResult toResult() {
            return new DataImportDtos.SectionResult(created, updated, skipped);
        }
    }
}
