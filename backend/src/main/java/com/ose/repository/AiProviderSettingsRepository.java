package com.ose.repository;

import com.ose.ai.AiProviderType;
import com.ose.model.AiProviderSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiProviderSettingsRepository extends JpaRepository<AiProviderSettingsEntity, Long> {
    Optional<AiProviderSettingsEntity> findByProvider(AiProviderType provider);
}
