package com.ose.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_default_model_settings")
public class AiDefaultModelSettingsEntity extends BaseEntity {

    public static final String SINGLETON_ID = "DEFAULT";

    @Id
    @Column(length = 32)
    private String id;

    @Column(length = 64)
    private String questionGenerationProviderId;

    @Column(length = 64)
    private String questionGenerationModelId;

    @Column(length = 64)
    private String reviewSummaryProviderId;

    @Column(length = 64)
    private String reviewSummaryModelId;

    @Column(length = 64)
    private String practiceRecommendationProviderId;

    @Column(length = 64)
    private String practiceRecommendationModelId;
}
