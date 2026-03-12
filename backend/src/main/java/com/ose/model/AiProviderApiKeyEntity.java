package com.ose.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "ai_provider_api_keys")
public class AiProviderApiKeyEntity extends BaseEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private AiProviderEntity provider;

    @Column(name = "key_encrypted", nullable = false, columnDefinition = "longtext")
    private String keyEncrypted;

    @Column(name = "key_mask", nullable = false, length = 32)
    private String keyMask;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Integer consecutiveFailures;

    private LocalDateTime lastUsedAt;

    private LocalDateTime lastFailedAt;
}
