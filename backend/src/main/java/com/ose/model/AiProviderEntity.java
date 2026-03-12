package com.ose.model;

import com.ose.ai.AiBaseUrlMode;
import com.ose.ai.AiKeyRotationStrategy;
import com.ose.ai.AiProviderConfigSource;
import com.ose.ai.AiProviderHealthStatus;
import com.ose.ai.AiProviderType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_providers")
public class AiProviderEntity extends BaseEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiProviderType providerType;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiKeyRotationStrategy keyRotationStrategy;

    @Column(length = 255)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiBaseUrlMode baseUrlMode;

    @Column(length = 120)
    private String defaultModel;

    private Integer timeoutMs;

    private Integer maxRetries;

    private Double temperature;

    @Column(length = 500)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiProviderConfigSource configSource;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AiProviderHealthStatus healthStatus;

    private LocalDateTime lastCheckedAt;

    @Column(length = 255)
    private String healthMessage;

    @Builder.Default
    @OrderBy("sortOrder ASC, createdAt ASC")
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiProviderApiKeyEntity> apiKeys = new ArrayList<>();

    @Builder.Default
    @OrderBy("sortOrder ASC, createdAt ASC")
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiModelEntity> models = new ArrayList<>();
}
