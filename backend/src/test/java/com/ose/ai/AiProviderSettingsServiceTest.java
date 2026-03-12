package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import com.ose.model.AiProviderSettingsEntity;
import com.ose.repository.AiProviderSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderSettingsServiceTest {

    @Mock
    private AiProviderSettingsRepository repository;

    private final Map<AiProviderType, AiProviderSettingsEntity> store = new EnumMap<>(AiProviderType.class);
    private AiProviderSettingsService service;
    private AiSecretCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getAi().setConfigMode(AiConfigMode.HYBRID);
        properties.getAi().setSecretEncryptionKey("service-secret");
        properties.getAi().getOpenai().setApiKey("");

        cryptoService = new AiSecretCryptoService(properties);
        AiProviderCatalogService catalogService = new AiProviderCatalogService(properties);
        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(properties, repository, cryptoService, catalogService);
        service = new AiProviderSettingsService(properties, repository, resolver, cryptoService, catalogService);

        when(repository.findByProvider(any())).thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
        lenient().when(repository.save(any(AiProviderSettingsEntity.class))).thenAnswer(invocation -> {
            AiProviderSettingsEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId((long) (store.size() + 1));
            }
            store.put(entity.getProvider(), entity);
            return entity;
        });
    }

    @Test
    void shouldSaveAndReadMaskedConfiguration() {
        AiProviderSettingsDtos.UpdateAiProviderSettingsRequest request = new AiProviderSettingsDtos.UpdateAiProviderSettingsRequest(
                true,
                "sk-live-1234",
                false,
                "https://proxy.example.com",
                "gpt-4.1-mini",
                10000,
                2,
                0.4d
        );

        AiProviderSettingsDtos.AiProviderSettingsSummary summary = service.update(AiProviderType.OPENAI, request);

        assertTrue(summary.enabled());
        assertTrue(summary.configured());
        assertEquals("sk-***1234", summary.maskedKey());
        assertEquals(AiProviderConfigSource.DB, summary.configSource());
        assertNotEquals("sk-live-1234", store.get(AiProviderType.OPENAI).getApiKeyEncrypted());
    }

    @Test
    void shouldKeepExistingKeyWhenApiKeyNotProvided() {
        service.update(AiProviderType.OPENAI, new AiProviderSettingsDtos.UpdateAiProviderSettingsRequest(
                true,
                "sk-live-1234",
                false,
                null,
                null,
                null,
                null,
                null
        ));
        String encrypted = store.get(AiProviderType.OPENAI).getApiKeyEncrypted();

        AiProviderSettingsDtos.AiProviderSettingsSummary summary = service.update(AiProviderType.OPENAI, new AiProviderSettingsDtos.UpdateAiProviderSettingsRequest(
                true,
                null,
                false,
                "https://new-base.example.com",
                null,
                null,
                null,
                null
        ));

        assertEquals(encrypted, store.get(AiProviderType.OPENAI).getApiKeyEncrypted());
        assertEquals("https://new-base.example.com", summary.baseUrl());
        assertTrue(summary.configured());
    }

    @Test
    void shouldClearApiKeyWhenRequested() {
        service.update(AiProviderType.OPENAI, new AiProviderSettingsDtos.UpdateAiProviderSettingsRequest(
                true,
                "sk-live-1234",
                false,
                null,
                null,
                null,
                null,
                null
        ));

        AiProviderSettingsDtos.AiProviderSettingsSummary summary = service.update(AiProviderType.OPENAI, new AiProviderSettingsDtos.UpdateAiProviderSettingsRequest(
                false,
                null,
                true,
                null,
                null,
                null,
                null,
                null
        ));

        assertNull(store.get(AiProviderType.OPENAI).getApiKeyEncrypted());
        assertNull(store.get(AiProviderType.OPENAI).getApiKeyMask());
        assertFalse(summary.configured());
    }

    @Test
    void shouldRejectPersistingKeyWithoutMasterKey() {
        AppProperties properties = new AppProperties();
        properties.getAi().setConfigMode(AiConfigMode.HYBRID);
        properties.getAi().setSecretEncryptionKey("");
        AiSecretCryptoService localCrypto = new AiSecretCryptoService(properties);
        AiProviderCatalogService catalogService = new AiProviderCatalogService(properties);
        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(properties, repository, localCrypto, catalogService);
        AiProviderSettingsService localService = new AiProviderSettingsService(properties, repository, resolver, localCrypto, catalogService);
        when(repository.findByProvider(eq(AiProviderType.OPENAI))).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> localService.update(
                AiProviderType.OPENAI,
                new AiProviderSettingsDtos.UpdateAiProviderSettingsRequest(true, "sk-live-1234", false, null, null, null, null, null)
        ));

        assertTrue(ex.getMessage().contains("AI_SECRET_ENCRYPTION_KEY"));
    }
}
