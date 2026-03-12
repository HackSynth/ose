package com.ose.ai;

import com.ose.model.AiProviderEntity;
import com.ose.repository.AiProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiProviderHealthService {

    private final AiProviderConfigurationResolver resolver;
    private final AiProviderRepository providerRepository;
    private final AiApiKeyRotationService keyRotationService;
    private final List<AiProviderClient> providerClients;

    public AiProviderHealthService(AiProviderConfigurationResolver resolver,
                                   AiProviderRepository providerRepository,
                                   AiApiKeyRotationService keyRotationService,
                                   List<AiProviderClient> providerClients) {
        this.resolver = resolver;
        this.providerRepository = providerRepository;
        this.keyRotationService = keyRotationService;
        this.providerClients = providerClients;
    }

    @Transactional
    public AiProviderHealthResult test(String providerId) {
        ResolvedAiProviderConfig resolved = resolver.resolve(providerId);
        if (!resolved.isAvailable()) {
            AiProviderHealthResult result = new AiProviderHealthResult(
                    false,
                    resolved.providerType(),
                    resolved.defaultModel(),
                    0L,
                    resolved.message(),
                    resolved.configSource(),
                    AiProviderHealthStatus.UNAVAILABLE
            );
            updateHealthSnapshot(providerId, result);
            return result;
        }

        AiProviderClient client = providerClients.stream()
                .filter(item -> item.providerType() == resolved.providerType())
                .findFirst()
                .orElseThrow(() -> new AiProviderException("未找到对应的 AI Provider Client"));
        AiProviderHealthResult result = client.testConnection(resolved);
        if (result.success()) {
            keyRotationService.recordSuccess(resolved.apiKeyId());
        } else {
            keyRotationService.recordFailure(resolved.apiKeyId());
        }
        updateHealthSnapshot(providerId, result);
        return result;
    }

    private void updateHealthSnapshot(String providerId, AiProviderHealthResult result) {
        providerRepository.findById(providerId).ifPresent(entity -> {
            entity.setHealthStatus(result.healthStatus());
            entity.setHealthMessage(truncate(result.message(), 255));
            entity.setLastCheckedAt(LocalDateTime.now());
            providerRepository.save(entity);
        });
    }

    public void updateUnavailable(AiProviderEntity provider, String message) {
        provider.setHealthStatus(AiProviderHealthStatus.UNAVAILABLE);
        provider.setHealthMessage(truncate(message, 255));
        provider.setLastCheckedAt(LocalDateTime.now());
        providerRepository.save(provider);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
