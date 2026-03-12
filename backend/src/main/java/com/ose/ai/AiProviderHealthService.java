package com.ose.ai;

import com.ose.repository.AiProviderSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiProviderHealthService {

    private final AiProviderConfigurationResolver resolver;
    private final AiProviderSettingsRepository repository;
    private final List<AiProviderClient> providerClients;

    public AiProviderHealthService(AiProviderConfigurationResolver resolver,
                                   AiProviderSettingsRepository repository,
                                   List<AiProviderClient> providerClients) {
        this.resolver = resolver;
        this.repository = repository;
        this.providerClients = providerClients;
    }

    @Transactional
    public AiProviderHealthResult test(AiProviderType provider,
                                       AiProviderSettingsDtos.UpdateAiProviderSettingsRequest request) {
        ResolvedAiProviderConfig resolved = resolver.resolve(provider, request);
        if (!resolved.isAvailable()) {
            AiProviderHealthResult result = new AiProviderHealthResult(
                    false,
                    provider,
                    resolved.defaultModel(),
                    0L,
                    resolved.message(),
                    resolved.configSource(),
                    AiProviderHealthStatus.UNAVAILABLE
            );
            updateHealthSnapshot(provider, request, result);
            return result;
        }

        AiProviderClient client = providerClients.stream()
                .filter(item -> item.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new AiProviderException("未找到对应的 AI Provider"));
        AiProviderHealthResult result = client.testConnection(resolved);
        updateHealthSnapshot(provider, request, result);
        return result;
    }

    private void updateHealthSnapshot(AiProviderType provider,
                                      AiProviderSettingsDtos.UpdateAiProviderSettingsRequest request,
                                      AiProviderHealthResult result) {
        if (request != null && request.hasChanges()) {
            return;
        }
        repository.findByProvider(provider).ifPresent(entity -> {
            entity.setLastHealthStatus(result.healthStatus());
            entity.setLastHealthMessage(truncate(result.message(), 255));
            entity.setLastHealthCheckedAt(LocalDateTime.now());
            repository.save(entity);
        });
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
