package com.ose.core;

import com.ose.analytics.AnalyticsService;
import com.ose.model.AppEnums;
import com.ose.plan.PlanService;
import com.ose.practice.PracticeDtos;
import com.ose.practice.PracticeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CoreFlowServiceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private PracticeService practiceService;

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    void currentPlanShouldContainTasks() {
        var plan = planService.currentPlan();
        assertNotNull(plan);
        assertFalse(plan.tasks().isEmpty());
    }

    @Test
    void morningPracticeShouldAutoScore() {
        var session = practiceService.createSession(new PracticeDtos.PracticeSessionRequest(
                AppEnums.SessionType.RANDOM,
                AppEnums.QuestionType.MORNING_SINGLE,
                null,
                1
        ));
        assertEquals(1, session.records().size());
        var record = session.records().get(0);
        var submitted = practiceService.submit(session.id(), new PracticeDtos.SubmitPracticeRequest(List.of(
                new PracticeDtos.PracticeAnswerInput(record.recordId(), "A", 30, null, "CONCEPT")
        )));
        assertEquals(AppEnums.SessionStatus.SUBMITTED, submitted.status());
        assertNotNull(submitted.records().get(0).result());
    }

    @Test
    void analyticsSummaryShouldHaveCards() {
        var summary = analyticsService.summary();
        assertEquals(4, summary.cards().size());
        assertNotNull(summary.knowledgeStats());
    }
}
