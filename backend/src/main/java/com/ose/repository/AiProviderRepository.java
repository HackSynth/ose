package com.ose.repository;

import com.ose.ai.AiProviderType;
import com.ose.model.AiProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiProviderRepository extends JpaRepository<AiProviderEntity, String> {
    List<AiProviderEntity> findByProviderType(AiProviderType providerType);
}
