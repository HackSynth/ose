package com.ose.repository;

import com.ose.model.AiProviderApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiProviderApiKeyRepository extends JpaRepository<AiProviderApiKeyEntity, String> {
    List<AiProviderApiKeyEntity> findByProviderIdOrderBySortOrderAscCreatedAtAsc(String providerId);

    Optional<AiProviderApiKeyEntity> findByIdAndProviderId(String id, String providerId);
}
