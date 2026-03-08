package com.ose.bootstrap;

import com.ose.common.config.AppProperties;
import com.ose.exam.ExamDtos;
import com.ose.exam.ExamService;
import com.ose.knowledge.KnowledgeDtos;
import com.ose.knowledge.KnowledgeService;
import com.ose.model.AppEnums;
import com.ose.model.AppUser;
import com.ose.note.NoteDtos;
import com.ose.note.NoteService;
import com.ose.plan.PlanService;
import com.ose.practice.PracticeDtos;
import com.ose.practice.PracticeService;
import com.ose.question.QuestionDtos;
import com.ose.question.QuestionService;
import com.ose.repository.*;
import com.ose.settings.SettingDtos;
import com.ose.settings.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository userRepository;
    private final KnowledgeService knowledgeService;
    private final QuestionService questionService;
    private final SettingService settingService;
    private final PlanService planService;
    private final PracticeService practiceService;
    private final ExamService examService;
    private final NoteService noteService;
    private final KnowledgePointRepository knowledgePointRepository;
    private final QuestionRepository questionRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final MockExamRepository mockExamRepository;
    private final NoteRepository noteRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedSettings();
        seedKnowledgePoints();
        seedQuestions();
        seedPlan();
        seedPractice();
        seedExams();
        seedNotes();
        log.info("OSE seed data initialization finished");
    }

    private void seedAdmin() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(AppUser.builder()
                .username(appProperties.getDefaultAdmin().getUsername())
                .passwordHash(passwordEncoder.encode(appProperties.getDefaultAdmin().getPassword()))
                .displayName(appProperties.getDefaultAdmin().getDisplayName())
                .role(AppEnums.Role.ADMIN)
                .build());
    }

    private void seedSettings() {
        if (systemSettingRepository.count() > 0) {
            return;
        }
        settingService.updateSetting(new SettingDtos.UpdateSettingRequest(
                LocalDate.of(2026, 5, 23),
                45,
                12,
                "工作日专题学习，周末整卷模拟",
                List.of(1, 3, 7, 14),
                90
        ));
    }

    private void seedKnowledgePoints() {
        if (knowledgePointRepository.count() > 0) {
            return;
        }
        Map<String, Long> ids = new HashMap<>();
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("OO", "面向对象与设计原则", 1, 58, 10, "OO 基础、SOLID 与设计质量", 1, null));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("OO.ENCAP", "封装与抽象", 2, 62, 9, "关注类职责边界与接口设计", 1, ids.get("OO")));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("OO.ENCAP.ACCESS", "访问控制与信息隐藏", 3, 55, 7, "掌握 private/protected/public 的设计意义", 1, ids.get("OO.ENCAP")));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("OO.PATTERN", "常见设计模式", 2, 48, 9, "重点掌握工厂、单例、观察者", 2, ids.get("OO")));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("ARCH", "软件架构设计", 1, 50, 10, "关注分层、模块与非功能需求", 2, null));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("ARCH.LAYER", "分层架构", 2, 52, 9, "掌握职责分配、接口与依赖方向", 1, ids.get("ARCH")));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("ARCH.LAYER.DDD", "分层与领域建模协同", 3, 44, 8, "关注应用层与领域层边界", 1, ids.get("ARCH.LAYER")));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("DB", "数据库与事务", 1, 64, 10, "事务、索引、并发控制", 3, null));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("DB.TXN", "事务与并发控制", 2, 68, 9, "掌握 ACID、锁、隔离级别", 1, ids.get("DB")));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("REQ", "需求工程", 1, 57, 8, "需求建模与验证", 4, null));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("REQ.USECASE", "用例建模", 2, 60, 8, "识别参与者、主流程和异常流程", 1, ids.get("REQ")));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("NET", "网络与系统性能", 1, 46, 7, "常见协议与性能指标", 5, null));
        createKnowledge(ids, new KnowledgeDtos.KnowledgeRequest("NET.CACHE", "缓存与性能优化", 2, 42, 7, "缓存一致性、击穿和雪崩", 1, ids.get("NET")));
    }

    private void seedQuestions() {
        if (questionRepository.count() > 0) {
            return;
        }
        questionService.create(new QuestionDtos.QuestionSaveRequest(
                AppEnums.QuestionType.MORNING_SINGLE,
                "面向对象特性辨析",
                "下列做法中，最能体现封装思想的是哪一项？",
                List.of(
                        new QuestionDtos.QuestionOptionInput("A", "隐藏对象内部实现细节，仅暴露稳定接口"),
                        new QuestionDtos.QuestionOptionInput("B", "通过继承复用父类全部属性"),
                        new QuestionDtos.QuestionOptionInput("C", "把所有属性都声明为 public"),
                        new QuestionDtos.QuestionOptionInput("D", "在多个类中复制同样的逻辑")
                ),
                "A",
                "封装强调隐藏实现细节、控制访问边界。",
                null,
                2026,
                2,
                "OSE 示例题",
                List.of("面向对象", "封装"),
                idsOf("OO.ENCAP", "OO.ENCAP.ACCESS"),
                BigDecimal.ONE,
                true
        ));
        questionService.create(new QuestionDtos.QuestionSaveRequest(
                AppEnums.QuestionType.MORNING_SINGLE,
                "事务一致性概念",
                "在事务 ACID 特性中，用于保证事务前后约束不被破坏的是哪一项？",
                List.of(
                        new QuestionDtos.QuestionOptionInput("A", "原子性"),
                        new QuestionDtos.QuestionOptionInput("B", "一致性"),
                        new QuestionDtos.QuestionOptionInput("C", "隔离性"),
                        new QuestionDtos.QuestionOptionInput("D", "持久性")
                ),
                "B",
                "一致性保证事务前后满足业务规则和完整性约束。",
                null,
                2026,
                2,
                "OSE 示例题",
                List.of("数据库", "事务"),
                idsOf("DB.TXN"),
                BigDecimal.ONE,
                true
        ));
        questionService.create(new QuestionDtos.QuestionSaveRequest(
                AppEnums.QuestionType.MORNING_SINGLE,
                "设计模式应用",
                "当系统希望根据输入条件返回不同产品对象，但客户端不依赖具体实现类时，更适合使用哪种模式？",
                List.of(
                        new QuestionDtos.QuestionOptionInput("A", "单例模式"),
                        new QuestionDtos.QuestionOptionInput("B", "工厂方法模式"),
                        new QuestionDtos.QuestionOptionInput("C", "适配器模式"),
                        new QuestionDtos.QuestionOptionInput("D", "装饰器模式")
                ),
                "B",
                "工厂方法通过抽象工厂接口隔离对象创建细节。",
                null,
                2026,
                3,
                "OSE 示例题",
                List.of("设计模式", "创建型"),
                idsOf("OO.PATTERN"),
                BigDecimal.ONE,
                true
        ));
        questionService.create(new QuestionDtos.QuestionSaveRequest(
                AppEnums.QuestionType.MORNING_SINGLE,
                "缓存场景判断",
                "在高并发系统中，为降低数据库压力又尽量保持热点数据读取性能，通常优先采用哪项措施？",
                List.of(
                        new QuestionDtos.QuestionOptionInput("A", "所有请求都绕过缓存直接查库"),
                        new QuestionDtos.QuestionOptionInput("B", "引入只写缓存不落库策略"),
                        new QuestionDtos.QuestionOptionInput("C", "对热点数据使用缓存并设计一致性策略"),
                        new QuestionDtos.QuestionOptionInput("D", "提高数据库日志级别")
                ),
                "C",
                "缓存能承接热点读流量，但需要处理一致性与失效策略。",
                null,
                2026,
                2,
                "OSE 示例题",
                List.of("缓存", "性能优化"),
                idsOf("NET.CACHE", "ARCH.LAYER"),
                BigDecimal.ONE,
                true
        ));
        questionService.create(new QuestionDtos.QuestionSaveRequest(
                AppEnums.QuestionType.AFTERNOON_CASE,
                "订单系统分层架构设计",
                "围绕高并发订单系统，说明表示层、应用层、领域层、基础设施层的职责，并给出接口设计要点。",
                List.of(),
                null,
                null,
                "要点：职责清晰、接口幂等、事务边界、缓存/消息解耦、异常处理与观测性。",
                2026,
                3,
                "OSE 示例题",
                List.of("架构设计", "分层"),
                idsOf("ARCH.LAYER", "ARCH.LAYER.DDD"),
                new BigDecimal("15"),
                true
        ));
        questionService.create(new QuestionDtos.QuestionSaveRequest(
                AppEnums.QuestionType.AFTERNOON_CASE,
                "在线考试系统需求分析",
                "请结合在线考试系统，说明如何识别核心参与者、建立用例图，并补充关键非功能需求。",
                List.of(),
                null,
                null,
                "要点：参与者识别、主成功场景、异常流、性能、安全、可用性、审计追踪。",
                2026,
                2,
                "OSE 示例题",
                List.of("需求分析", "用例"),
                idsOf("REQ.USECASE"),
                new BigDecimal("15"),
                true
        ));
        questionService.create(new QuestionDtos.QuestionSaveRequest(
                AppEnums.QuestionType.AFTERNOON_CASE,
                "事务一致性方案设计",
                "说明在支付与订单两个服务协同场景下，如何设计可靠的事务一致性与补偿方案。",
                List.of(),
                null,
                null,
                "要点：本地事务、消息最终一致性、幂等、防重、补偿、对账。",
                2026,
                3,
                "OSE 示例题",
                List.of("事务", "分布式"),
                idsOf("DB.TXN", "ARCH.LAYER"),
                new BigDecimal("15"),
                true
        ));
    }

    private void seedPlan() {
        if (planService.currentPlan().tasks().isEmpty()) {
            planService.generateCurrentPlan();
        }
    }

    private void seedPractice() {
        if (practiceSessionRepository.count() > 0) {
            return;
        }
        Long ooKnowledgeId = knowledgePointRepository.findByCode("OO.ENCAP").map(item -> item.getId()).orElse(null);
        var morningSession = practiceService.createSession(new PracticeDtos.PracticeSessionRequest(
                AppEnums.SessionType.RANDOM,
                AppEnums.QuestionType.MORNING_SINGLE,
                ooKnowledgeId,
                2
        ));
        practiceService.submit(morningSession.id(), new PracticeDtos.SubmitPracticeRequest(List.of(
                new PracticeDtos.PracticeAnswerInput(morningSession.records().get(0).recordId(), "A", 60, null, "CONCEPT"),
                new PracticeDtos.PracticeAnswerInput(morningSession.records().get(1).recordId(), "A", 75, null, "CARELESS")
        )));
        var afternoonSession = practiceService.createSession(new PracticeDtos.PracticeSessionRequest(
                AppEnums.SessionType.RANDOM,
                AppEnums.QuestionType.AFTERNOON_CASE,
                null,
                1
        ));
        practiceService.submit(afternoonSession.id(), new PracticeDtos.SubmitPracticeRequest(List.of(
                new PracticeDtos.PracticeAnswerInput(afternoonSession.records().get(0).recordId(), "先划分层次，再说明领域对象与接口，再补充异常与缓存策略。", 900, new BigDecimal("9"), "CONCEPT")
        )));
    }

    private void seedExams() {
        if (mockExamRepository.count() > 0) {
            return;
        }
        var morningExam = examService.createExam(new ExamDtos.CreateExamRequest(
                "上午卷示例模拟 1",
                AppEnums.ExamType.MORNING,
                150,
                "含 4 道上午选择题示例",
                questionRepository.findByTypeAndActiveTrue(AppEnums.QuestionType.MORNING_SINGLE).stream().map(item -> item.getId()).limit(4).toList()
        ));
        var afternoonExam = examService.createExam(new ExamDtos.CreateExamRequest(
                "下午卷示例模拟 1",
                AppEnums.ExamType.AFTERNOON,
                150,
                "含 2 道下午案例题示例",
                questionRepository.findByTypeAndActiveTrue(AppEnums.QuestionType.AFTERNOON_CASE).stream().map(item -> item.getId()).limit(2).toList()
        ));
        var morningAttempt = examService.startAttempt(morningExam.id());
        examService.submitAttempt(morningAttempt.id(), new ExamDtos.SubmitAttemptRequest(
                3200,
                "上午卷主要错在缓存与事务细节。",
                morningAttempt.answers().stream()
                        .map(answer -> new ExamDtos.AttemptAnswerInput(answer.questionId(), answer.options().isEmpty() ? "A" : answer.options().get(0).key(), null, null))
                        .toList()
        ));
        var afternoonAttempt = examService.startAttempt(afternoonExam.id());
        examService.submitAttempt(afternoonAttempt.id(), new ExamDtos.SubmitAttemptRequest(
                5400,
                "下午卷先完成主干，再针对要点逐项补充。",
                afternoonAttempt.answers().stream()
                        .map(answer -> new ExamDtos.AttemptAnswerInput(answer.questionId(), "先明确场景，再按职责/流程/约束拆解答案。", new BigDecimal("10"), "结构完整但细节略少"))
                        .toList()
        ));
    }

    private void seedNotes() {
        if (noteRepository.count() > 0) {
            return;
        }
        noteService.create(new NoteDtos.NoteSaveRequest(
                "分层架构复盘",
                "# 分层架构\n\n- 表示层关注输入输出\n- 应用层负责流程编排\n- 领域层沉淀核心规则\n- 基础设施层处理数据库、缓存、消息\n",
                "整理分层架构职责边界。",
                true,
                List.of(new NoteDtos.NoteLinkInput(AppEnums.NoteLinkType.KNOWLEDGE, knowledgePointRepository.findByCode("ARCH.LAYER").orElseThrow().getId()))
        ));
        noteService.create(new NoteDtos.NoteSaveRequest(
                "事务一致性答题模板",
                "# 事务一致性\n\n1. 识别一致性目标\n2. 说明事务边界\n3. 给出消息/补偿/幂等策略\n4. 说明对账与监控\n",
                "下午题可复用的答题模板。",
                false,
                List.of(new NoteDtos.NoteLinkInput(AppEnums.NoteLinkType.QUESTION, questionRepository.findByTypeAndActiveTrue(AppEnums.QuestionType.AFTERNOON_CASE).get(0).getId()))
        ));
    }

    private void createKnowledge(Map<String, Long> ids, KnowledgeDtos.KnowledgeRequest request) {
        var created = knowledgeService.create(request);
        ids.put(created.code(), created.id());
    }

    private List<Long> idsOf(String... codes) {
        return java.util.Arrays.stream(codes)
                .map(code -> knowledgePointRepository.findByCode(code).orElseThrow().getId())
                .toList();
    }
}
