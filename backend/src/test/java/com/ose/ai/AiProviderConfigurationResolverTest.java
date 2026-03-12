package com.ose.ai;

import com.ose.common.config.AppProperties;
import com.ose.model.AiProviderSettingsEntity;
import com.ose.repository.AiProviderSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderConfigurationResolverTest {

    @Mock
    private AiProviderSettingsRepository repository;

    private AppProperties properties;
    private AiSecretCryptoService cryptoService;
    private AiProviderCatalogService catalogService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getAi().setSecretEncryptionKey("resolver-secret");
        properties.getAi().getOpenai().setApiKey("env-openai-1234");
        properties.getAi().getAnthropic().setApiKey("env-anthropic-5678");
        cryptoService = new AiSecretCryptoService(properties);
        catalogService = new AiProviderCatalogService(properties);
    }

    @Test
    void shouldPreferDatabaseConfigInHybridMode() {
        properties.getAi().setConfigMode(AiConfigMode.HYBRID);
        AiProviderSettingsEntity entity = AiProviderSettingsEntity.builder()
                .provider(AiProviderType.OPENAI)
                .enabled(true)
                .apiKeyEncrypted(cryptoService.encrypt("db-openai-9999"))
                .apiKeyMask(cryptoService.mask("db-openai-9999"))
                .baseUrl("https://db-openai.example.com")
                .defaultModel("gpt-db")
                .timeoutMs(12000)
                .maxRetries(2)
                .temperature(0.3d)
                .configSource(AiProviderConfigSource.DB)
                .build();
        when(repository.findByProvider(AiProviderType.OPENAI)).thenReturn(Optional.of(entity));

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(properties, repository, cryptoService, catalogService);
        ResolvedAiProviderConfig resolved = resolver.resolve(AiProviderType.OPENAI);

        assertEquals(AiProviderConfigSource.DB, resolved.configSource());
        assertEquals("gpt-db", resolved.defaultModel());
        assertEquals("db-***9999", resolved.maskedKey());
    }

    @Test
    void shouldFallbackToEnvironmentInHybridModeWhenDbConfigMissing() {
        properties.getAi().setConfigMode(AiConfigMode.HYBRID);
        when(repository.findByProvider(AiProviderType.OPENAI)).thenReturn(Optional.empty());

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(properties, repository, cryptoService, catalogService);
        ResolvedAiProviderConfig resolved = resolver.resolve(AiProviderType.OPENAI);

        assertEquals(AiProviderConfigSource.ENV_FALLBACK, resolved.configSource());
        assertEquals("env***1234", resolved.maskedKey());
    }

    @Test
    void shouldUseEnvironmentModeWhenConfigured() {
        properties.getAi().setConfigMode(AiConfigMode.ENV);
        when(repository.findByProvider(AiProviderType.ANTHROPIC)).thenReturn(Optional.empty());

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(properties, repository, cryptoService, catalogService);
        ResolvedAiProviderConfig resolved = resolver.resolve(AiProviderType.ANTHROPIC);

        assertEquals(AiProviderConfigSource.ENV, resolved.configSource());
        assertEquals("env***5678", resolved.maskedKey());
    }

    @Test
    void shouldReturnUnavailableWhenDbModeHasNoSavedConfig() {
        properties.getAi().setConfigMode(AiConfigMode.DB);
        when(repository.findByProvider(any())).thenReturn(Optional.empty());

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(properties, repository, cryptoService, catalogService);
        ResolvedAiProviderConfig resolved = resolver.resolve(AiProviderType.OPENAI);

        assertEquals(AiProviderConfigSource.UNAVAILABLE, resolved.configSource());
        assertFalse(resolved.isAvailable());
    }

    @Test
    void shouldNotFallbackToEnvWhenEnabledDbConfigIsBroken() {
        properties.getAi().setConfigMode(AiConfigMode.HYBRID);
        properties.getAi().setSecretEncryptionKey("");
        AiSecretCryptoService brokenCrypto = new AiSecretCryptoService(properties);
        AiProviderSettingsEntity entity = AiProviderSettingsEntity.builder()
                .provider(AiProviderType.OPENAI)
                .enabled(true)
                .apiKeyEncrypted("encrypted-value")
                .apiKeyMask("db-***9999")
                .configSource(AiProviderConfigSource.DB)
                .build();
        when(repository.findByProvider(AiProviderType.OPENAI)).thenReturn(Optional.of(entity));

        AiProviderConfigurationResolver resolver = new AiProviderConfigurationResolver(properties, repository, brokenCrypto, catalogService);
        ResolvedAiProviderConfig resolved = resolver.resolve(AiProviderType.OPENAI);

        assertEquals(AiProviderConfigSource.UNAVAILABLE, resolved.configSource());
        assertFalse(resolved.isAvailable());
    }
}
