package com.ose.data;

import com.ose.knowledge.KnowledgeDtos;
import com.ose.note.NoteDtos;
import com.ose.plan.PlanDtos;
import com.ose.question.QuestionDtos;
import com.ose.repository.NoteRepository;
import com.ose.repository.StudyPlanRepository;
import com.ose.settings.SettingDtos;
import com.ose.model.AppEnums;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataImportServiceTest {

    @Autowired
    private DataImportService dataImportService;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Test
    void fullImportShouldCreateBundleContent() {
        String suffix = String.valueOf(System.currentTimeMillis());
        var request = new DataImportDtos.FullImportRequest(
                new SettingDtos.SettingResponse(null, LocalDate.of(2026, 6, 30), 0, 45, 14, "导入测试偏好", List.of(1, 3, 7, 14), 90),
                List.of(new KnowledgeDtos.KnowledgeTreeItem(
                        null,
                        "TEST.BUNDLE." + suffix,
                        "测试导入主题" + suffix,
                        1,
                        40,
                        8,
                        "导入测试",
                        null,
                        List.of(new KnowledgeDtos.KnowledgeTreeItem(
                                null,
                                "TEST.BUNDLE.CHILD." + suffix,
                                "测试导入子主题" + suffix,
                                2,
                                35,
                                7,
                                "导入测试子节点",
                                null,
                                List.of()
                        ))
                )),
                List.of(new QuestionDtos.QuestionResponse(
                        null,
                        AppEnums.QuestionType.MORNING_SINGLE,
                        "测试导入上午题 " + suffix,
                        "以下哪项最符合抽象思想？",
                        List.of(
                                new QuestionDtos.QuestionOptionDto(null, "A", "暴露稳定接口，隐藏变化点"),
                                new QuestionDtos.QuestionOptionDto(null, "B", "复制实现细节"),
                                new QuestionDtos.QuestionOptionDto(null, "C", "全部 public"),
                                new QuestionDtos.QuestionOptionDto(null, "D", "省略建模")
                        ),
                        "A",
                        "导入测试解析",
                        null,
                        2026,
                        2,
                        "导入测试",
                        List.of("导入", "测试"),
                        List.of(new QuestionDtos.QuestionRelationDto(null, "TEST.BUNDLE.CHILD." + suffix, "测试导入子主题" + suffix)),
                        BigDecimal.ONE,
                        true,
                        false,
                        null,
                        null
                )),
                new PlanDtos.PlanResponse(
                        null,
                        "导入测试计划 " + suffix,
                        LocalDate.of(2026, 6, 30),
                        LocalDate.of(2026, 3, 8),
                        LocalDate.of(2026, 6, 30),
                        100,
                        "ACTIVE",
                        "导入测试计划快照",
                        List.of(new PlanDtos.TaskResponse(
                                null,
                                AppEnums.PlanPhase.FOUNDATION,
                                AppEnums.TaskType.KNOWLEDGE,
                                AppEnums.TaskStatus.TODO,
                                "导入测试任务",
                                "导入后的计划任务",
                                null,
                                "测试导入子主题" + suffix,
                                LocalDate.of(2026, 3, 10),
                                90,
                                3,
                                0,
                                null
                        ))
                ),
                List.of(new NoteDtos.NoteView(
                        null,
                        "导入测试笔记 " + suffix,
                        "# 导入测试\n\n- 记录一个导入验证笔记",
                        "导入测试摘要",
                        false,
                        List.of(),
                        LocalDateTime.now()
                ))
        );

        var result = dataImportService.importFull(request, DataImportDtos.DuplicateStrategy.OVERWRITE);
        assertTrue(result.errors().isEmpty());
        assertEquals(1, result.questions().created());
        assertTrue(result.knowledgePoints().created() >= 2);
        assertEquals(1, result.notes().created());
        assertEquals("导入测试计划 " + suffix,
                studyPlanRepository.findFirstByStatusOrderByCreatedAtDesc(AppEnums.PlanStatus.ACTIVE).orElseThrow().getName());
        assertTrue(noteRepository.findFirstByTitleIgnoreCase("导入测试笔记 " + suffix).isPresent());
    }

    @Test
    void skipStrategyShouldReportSkippedForDuplicates() {
        var request = new DataImportDtos.FullImportRequest(
                null,
                List.of(new KnowledgeDtos.KnowledgeTreeItem(
                        null,
                        "OO",
                        "面向对象与设计原则",
                        1,
                        58,
                        10,
                        "重复知识点",
                        null,
                        List.of()
                )),
                List.of(new QuestionDtos.QuestionResponse(
                        null,
                        AppEnums.QuestionType.MORNING_SINGLE,
                        "事务一致性概念",
                        "重复题目内容",
                        List.of(
                                new QuestionDtos.QuestionOptionDto(null, "A", "原子性"),
                                new QuestionDtos.QuestionOptionDto(null, "B", "一致性")
                        ),
                        "B",
                        "重复题目解析",
                        null,
                        2026,
                        2,
                        "重复导入",
                        List.of("数据库", "事务"),
                        List.of(new QuestionDtos.QuestionRelationDto(null, "DB.TXN", "事务与并发控制")),
                        BigDecimal.ONE,
                        true,
                        false,
                        null,
                        null
                )),
                null,
                List.of(new NoteDtos.NoteView(
                        null,
                        "分层架构复盘",
                        "重复笔记内容",
                        "重复摘要",
                        false,
                        List.of(),
                        LocalDateTime.now()
                ))
        );

        var result = dataImportService.importFull(request, DataImportDtos.DuplicateStrategy.SKIP);
        assertTrue(result.errors().isEmpty());
        assertEquals(1, result.knowledgePoints().skipped());
        assertEquals(1, result.questions().skipped());
        assertEquals(1, result.notes().skipped());
    }
}
