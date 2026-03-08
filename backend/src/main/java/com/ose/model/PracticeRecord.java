package com.ose.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "practice_records")
public class PracticeRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "text")
    private String userAnswer;

    @Column(precision = 10, scale = 2)
    private BigDecimal autoScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal subjectiveScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AppEnums.PracticeResult result;

    @Column(nullable = false)
    private Boolean favorite;

    @Column(nullable = false)
    private Boolean markedUnknown;

    @Column(nullable = false)
    private Boolean addedToReview;

    @Column(nullable = false)
    private Integer durationSeconds;

    private LocalDateTime submittedAt;
}
