package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.model.AiProviderApiKeyEntity;
import com.ose.model.AiProviderEntity;
import com.ose.repository.AiProviderApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiApiKeyRotationServiceTest {

    @Mock
    private AiProviderApiKeyRepository repository;

    private AiSecretCryptoService cryptoService;
    private AiApiKeyRotationService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getAi().setSecretEncryptionKey("rotation-secret");
        cryptoService = new AiSecretCryptoService(properties);
        service = new AiApiKeyRotationService(repository, cryptoService);
        lenient().when(repository.save(any(AiProviderApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRotateKeysSequentially() {
        AiProviderEntity provider = provider(
                key("key-1", "sk-live-1111", 0, 0, null),
                key("key-2", "sk-live-2222", 1, 0, null)
        );

        ResolvedAiApiKey first = service.selectKey(provider);
        ResolvedAiApiKey second = service.selectKey(provider);

        assertEquals("key-1", first.id());
        assertEquals("key-2", second.id());
    }

    @Test
    void shouldSkipOpenCircuitKey() {
        AiProviderEntity provider = provider(
                key("key-1", "sk-live-1111", 0, 3, LocalDateTime.now()),
                key("key-2", "sk-live-2222", 1, 0, null)
        );

        ResolvedAiApiKey selected = service.selectKey(provider);

        assertEquals("key-2", selected.id());
    }

    @Test
    void shouldRecordFailureCount() {
        AiProviderApiKeyEntity entity = key("key-1", "sk-live-1111", 0, 0, null);
        when(repository.findById("key-1")).thenReturn(Optional.of(entity));

        service.recordFailure("key-1");

        assertEquals(1, entity.getConsecutiveFailures());
    }

    private AiProviderEntity provider(AiProviderApiKeyEntity... keys) {
        AiProviderEntity provider = AiProviderEntity.builder()
                .id("provider-1")
                .providerType(AiProviderType.OPENAI)
                .displayName("OpenAI")
                .enabled(true)
                .keyRotationStrategy(AiKeyRotationStrategy.SEQUENTIAL_ROUND_ROBIN)
                .baseUrl("https://api.openai.com")
                .baseUrlMode(AiBaseUrlMode.ROOT)
                .configSource(AiProviderConfigSource.DB)
                .apiKeys(List.of(keys))
                .build();
        provider.getApiKeys().forEach(key -> key.setProvider(provider));
        return provider;
    }

    private AiProviderApiKeyEntity key(String id, String value, int sortOrder, int failures, LocalDateTime lastFailedAt) {
        return AiProviderApiKeyEntity.builder()
                .id(id)
                .keyEncrypted(cryptoService.encrypt(value))
                .keyMask(cryptoService.mask(value))
                .enabled(true)
                .sortOrder(sortOrder)
                .consecutiveFailures(failures)
                .lastFailedAt(lastFailedAt)
                .build();
    }
}
