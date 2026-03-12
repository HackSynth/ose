package com.ose.question;

import com.ose.model.AppEnums;
import com.ose.model.MockExam;
import com.ose.model.MockExamAttempt;
import com.ose.model.MockExamAttemptAnswer;
import com.ose.model.Question;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class QuestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Test
    @WithMockUser
    void deleteShouldReturnBusinessErrorWhenQuestionIsReferenced() throws Exception {
        Question question = Question.builder()
                .type(AppEnums.QuestionType.MORNING_SINGLE)
                .title("删除测试题")
                .content("删除测试题干")
                .correctAnswer("A")
                .year(2026)
                .difficulty(2)
                .source("TEST")
                .tags("test")
                .score(BigDecimal.valueOf(1))
                .active(true)
                .aiGenerated(false)
                .build();
        entityManager.persist(question);

        MockExam exam = MockExam.builder()
                .name("删除测试模考")
                .type(AppEnums.ExamType.MORNING)
                .durationMinutes(90)
                .totalScore(BigDecimal.valueOf(75))
                .description("test")
                .build();
        entityManager.persist(exam);

        MockExamAttempt attempt = MockExamAttempt.builder()
                .mockExam(exam)
                .status(AppEnums.AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .objectiveScore(BigDecimal.ZERO)
                .subjectiveScore(BigDecimal.ZERO)
                .totalScore(BigDecimal.ZERO)
                .durationSeconds(0)
                .build();
        entityManager.persist(attempt);

        MockExamAttemptAnswer answer = MockExamAttemptAnswer.builder()
                .attempt(attempt)
                .question(question)
                .answerText("A")
                .autoScore(BigDecimal.ZERO)
                .subjectiveScore(BigDecimal.ZERO)
                .result(AppEnums.PracticeResult.CORRECT)
                .feedback("test")
                .build();
        entityManager.persist(answer);
        entityManager.flush();

        mockMvc.perform(delete("/api/questions/{id}", question.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("题目已被练习或模考记录引用，无法删除"));
    }
}
