package com.ose.repository;

import com.ose.model.AiModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModelEntity, String> {
    List<AiModelEntity> findByProviderIdOrderBySortOrderAscCreatedAtAsc(String providerId);

    Optional<AiModelEntity> findByIdAndProviderId(String id, String providerId);
}
