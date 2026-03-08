package com.ose.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mistake_records")
public class MistakeRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_record_id", nullable = false)
    private PracticeRecord practiceRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_point_id")
    private KnowledgePoint knowledgePoint;

    @Column(nullable = false, length = 40)
    private String reasonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppEnums.ReviewStatus reviewStatus;

    @Column(nullable = false)
    private LocalDate nextReviewAt;

    @Column(nullable = false)
    private Integer reviewCount;

    @Column(columnDefinition = "text")
    private String note;
}
