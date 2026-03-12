package com.ose.model;

import com.ose.ai.AiProviderConfigSource;
import com.ose.ai.AiProviderHealthStatus;
import com.ose.ai.AiProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_provider_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_provider_settings_provider", columnNames = "provider")
})
public class AiProviderSettingsEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiProviderType provider;

    @Column(nullable = false)
    private boolean enabled;

    @Lob
    @Column(name = "api_key_encrypted")
    private String apiKeyEncrypted;

    @Column(name = "api_key_mask", length = 32)
    private String apiKeyMask;

    @Column(length = 255)
    private String baseUrl;

    @Column(length = 120)
    private String defaultModel;

    private Integer timeoutMs;

    private Integer maxRetries;

    private Double temperature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiProviderConfigSource configSource;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AiProviderHealthStatus lastHealthStatus;

    @Column(length = 255)
    private String lastHealthMessage;

    private LocalDateTime lastHealthCheckedAt;
}
