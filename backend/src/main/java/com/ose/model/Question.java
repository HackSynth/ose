package com.ose.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "questions")
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppEnums.QuestionType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 16)
    private String correctAnswer;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(columnDefinition = "text")
    private String referenceAnswer;

    @Column(name = "question_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer difficulty;

    @Column(nullable = false, length = 120)
    private String source;

    @Column(nullable = false, length = 255)
    private String tags;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aiGenerated = false;

    @Column(length = 20)
    private String aiProvider;

    @Column(length = 120)
    private String aiModel;

    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionKey ASC")
    private List<QuestionOption> options = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "question_knowledge_rel",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "knowledge_point_id"))
    private Set<KnowledgePoint> knowledgePoints = new LinkedHashSet<>();
}
