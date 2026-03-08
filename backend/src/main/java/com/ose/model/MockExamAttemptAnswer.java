package com.ose.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mock_exam_attempt_answers")
public class MockExamAttemptAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private MockExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "text")
    private String answerText;

    @Column(precision = 10, scale = 2)
    private BigDecimal autoScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal subjectiveScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AppEnums.PracticeResult result;

    @Column(columnDefinition = "text")
    private String feedback;
}
