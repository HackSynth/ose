package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.model.AiModelEntity;
import com.ose.model.AiProviderEntity;
import com.ose.repository.AiProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderConfigurationResolverTest {

    @Mock
    private AiProviderRepository providerRepository;
    @Mock
    private AiApiKeyRotationService rotationService;
    @Mock
    private AiDefaultModelService defaultModelService;

    private AppProperties properties;
    private AiSecretCryptoService cryptoService;
    private AiProviderCatalogService catalogService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getAi().setSecretEncryptionKey("resolver-secret");
        properties.getAi().getOpenai().setApiKey("env-openai-1234");
        properties.getAi().getOpenai().setModels("gpt-4.1-mini,gpt-4.1");
        properties.getAi().getAnthropic().setApiKey("env-anthropic-5678");
        properties.getAi().getAnthropic().setModels("claude-3-5-sonnet-latest");
        cryptoService = new AiSecretCryptoService(properties);
        catalogService = new AiProviderCatalogService(properties);
    }

    @Test
    void shouldUseDatabaseKeyWhenHybridProviderConfigured() {
        AiProviderEntity provider = provider("provider-openai", AiProviderType.OPENAI, AiProviderConfigSource.HYBRID);
        when(providerRepository.findById("provider-openai")).thenReturn(Optional.of(provider));
        when(rotationService.selectKey(provider)).thenReturn(new ResolvedAiApiKey("key-1", "db-openai-9999", "db-***9999"));

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(
                providerRepository,
                catalogService,
                rotationService,
                defaultModelService,
                cryptoService
        );
        ResolvedAiProviderConfig resolved = resolver.resolve("provider-openai");

        assertEquals(AiProviderConfigSource.DB, resolved.configSource());
        assertEquals("gpt-db", resolved.defaultModel());
        assertEquals("db-***9999", resolved.maskedKey());
        assertTrue(resolved.isAvailable());
    }

    @Test
    void shouldFallbackToEnvironmentInHybridModeWhenDbKeyMissing() {
        AiProviderEntity provider = provider("provider-openai", AiProviderType.OPENAI, AiProviderConfigSource.HYBRID);
        when(providerRepository.findById("provider-openai")).thenReturn(Optional.of(provider));
        when(rotationService.selectKey(provider)).thenReturn(null);

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(
                providerRepository,
                catalogService,
                rotationService,
                defaultModelService,
                cryptoService
        );
        ResolvedAiProviderConfig resolved = resolver.resolve("provider-openai");

        assertEquals(AiProviderConfigSource.HYBRID, resolved.configSource());
        assertEquals("env***1234", resolved.maskedKey());
        assertTrue(resolved.isAvailable());
    }

    @Test
    void shouldResolveEnvironmentProviderAlias() {
        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(
                providerRepository,
                catalogService,
                rotationService,
                defaultModelService,
                cryptoService
        );

        ResolvedAiProviderConfig resolved = resolver.resolve("env-openai");

        assertEquals(AiProviderConfigSource.ENV, resolved.configSource());
        assertEquals("env***1234", resolved.maskedKey());
        assertTrue(resolved.isAvailable());
    }

    @Test
    void shouldBeUnavailableWhenProviderHasNoEnabledModel() {
        AiProviderEntity provider = provider("provider-openai", AiProviderType.OPENAI, AiProviderConfigSource.DB);
        provider.getModels().clear();
        when(providerRepository.findById("provider-openai")).thenReturn(Optional.of(provider));
        when(rotationService.selectKey(provider)).thenReturn(new ResolvedAiApiKey("key-1", "db-openai-9999", "db-***9999"));

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(
                providerRepository,
                catalogService,
                rotationService,
                defaultModelService,
                cryptoService
        );
        ResolvedAiProviderConfig resolved = resolver.resolve("provider-openai");

        assertFalse(resolved.isAvailable());
        assertEquals(AiProviderConfigSource.DB, resolved.configSource());
    }

    private AiProviderEntity provider(String id, AiProviderType providerType, AiProviderConfigSource source) {
        AiProviderEntity provider = AiProviderEntity.builder()
                .id(id)
                .providerType(providerType)
                .displayName(providerType.defaultDisplayName())
                .enabled(true)
                .keyRotationStrategy(AiKeyRotationStrategy.SEQUENTIAL_ROUND_ROBIN)
                .baseUrl("https://api.example.com")
                .baseUrlMode(AiBaseUrlMode.ROOT)
                .defaultModel("gpt-db")
                .timeoutMs(12000)
                .maxRetries(2)
                .temperature(0.3d)
                .configSource(source)
                .models(new ArrayList<>(List.of(
                        AiModelEntity.builder()
                                .id("model-1")
                                .modelId("gpt-db")
                                .displayName("gpt-db")
                                .modelType(AiModelType.CHAT)
                                .enabled(true)
                                .sortOrder(0)
                                .build()
                )))
                .build();
        provider.getModels().forEach(model -> model.setProvider(provider));
        return provider;
    }
}
