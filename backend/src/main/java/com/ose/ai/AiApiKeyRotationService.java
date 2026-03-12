package com.ose.ai;

import com.ose.common.exception.BusinessException;
import com.ose.model.AiProviderApiKeyEntity;
import com.ose.model.AiProviderEntity;
import com.ose.repository.AiProviderApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(readOnly = true)
public class AiApiKeyRotationService {

    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;
    private static final int CIRCUIT_BREAKER_MINUTES = 10;

    private final AiProviderApiKeyRepository repository;
    private final AiSecretCryptoService cryptoService;
    private final Map<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

    public AiApiKeyRotationService(AiProviderApiKeyRepository repository,
                                   AiSecretCryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    public ResolvedAiApiKey selectKey(AiProviderEntity provider) {
        List<AiProviderApiKeyEntity> candidates = provider.getApiKeys().stream()
                .filter(AiProviderApiKeyEntity::isEnabled)
                .filter(this::isNotOpenCircuit)
                .sorted(Comparator.comparing(AiProviderApiKeyEntity::getSortOrder).thenComparing(AiProviderApiKeyEntity::getCreatedAt))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        int index = switch (provider.getKeyRotationStrategy()) {
            case SEQUENTIAL_ROUND_ROBIN -> nextIndex(provider.getId(), candidates.size());
        };
        AiProviderApiKeyEntity selected = candidates.get(index);
        String decrypted = cryptoService.decrypt(selected.getKeyEncrypted());
        return new ResolvedAiApiKey(selected.getId(), decrypted, selected.getKeyMask());
    }

    @Transactional
    public void recordSuccess(String keyId) {
        if (keyId == null) {
            return;
        }
        repository.findById(keyId).ifPresent(key -> {
            key.setConsecutiveFailures(0);
            key.setLastUsedAt(LocalDateTime.now());
            repository.save(key);
        });
    }

    @Transactional
    public void recordFailure(String keyId) {
        if (keyId == null) {
            return;
        }
        repository.findById(keyId).ifPresent(key -> {
            key.setConsecutiveFailures((key.getConsecutiveFailures() == null ? 0 : key.getConsecutiveFailures()) + 1);
            key.setLastFailedAt(LocalDateTime.now());
            repository.save(key);
        });
    }

    @Transactional
    public void validateCanPersist() {
        if (!cryptoService.canPersistSecrets()) {
            throw new BusinessException("未配置 AI_SECRET_ENCRYPTION_KEY，当前实例不能托管数据库密钥");
        }
    }

    private boolean isNotOpenCircuit(AiProviderApiKeyEntity key) {
        Integer failures = key.getConsecutiveFailures();
        if (failures == null || failures < CIRCUIT_BREAKER_THRESHOLD) {
            return true;
        }
        LocalDateTime lastFailedAt = key.getLastFailedAt();
        return lastFailedAt == null || lastFailedAt.isBefore(LocalDateTime.now().minusMinutes(CIRCUIT_BREAKER_MINUTES));
    }

    private int nextIndex(String providerId, int size) {
        AtomicInteger cursor = cursors.computeIfAbsent(providerId, ignored -> new AtomicInteger(0));
        return Math.floorMod(cursor.getAndIncrement(), size);
    }
}
