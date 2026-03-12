package com.ose.model;

import com.ose.ai.AiProviderType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_generation_records")
public class AiGenerationRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiProviderType provider;

    @Column(nullable = false, length = 120)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppEnums.QuestionType questionType;

    @Column(nullable = false, length = 32)
    private String topicType;

    @Column(nullable = false, length = 32)
    private String difficulty;

    @Column(nullable = false)
    private Integer requestedCount;

    @Column(nullable = false)
    private Integer successCount;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "text")
    private String requestPayload;

    @Column(columnDefinition = "longtext")
    private String responsePayload;

    @Column(length = 64)
    private String promptHash;

    @Column(columnDefinition = "text")
    private String promptSummary;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(length = 32)
    private String finishStatus;

    @Column
    private Integer usageInputTokens;

    @Column
    private Integer usageOutputTokens;

    @Column(precision = 10, scale = 4)
    private java.math.BigDecimal estimatedCost;
}
