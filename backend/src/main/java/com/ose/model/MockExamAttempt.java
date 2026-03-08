package com.ose.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mock_exam_attempts")
public class MockExamAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_id", nullable = false)
    private MockExam mockExam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppEnums.AttemptStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal objectiveScore;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subjectiveScore;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(columnDefinition = "text")
    private String selfReviewSummary;

    @Builder.Default
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MockExamAttemptAnswer> answers = new ArrayList<>();
}
