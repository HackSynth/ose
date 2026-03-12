package com.ose.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiProviderUrlBuilderTest {

    private final AiProviderUrlBuilder builder = new AiProviderUrlBuilder();

    @Test
    void shouldAppendStandardPathInRootMode() {
        String url = builder.build(config("https://api.openai.com/", AiBaseUrlMode.ROOT), AiProviderUrlBuilder.Endpoint.CHAT_COMPLETIONS);
        assertEquals("https://api.openai.com/v1/chat/completions", url);
    }

    @Test
    void shouldKeepExactUrlInFullOverrideMode() {
        String url = builder.build(config("https://gateway.example.com/custom/chat", AiBaseUrlMode.FULL_OVERRIDE), AiProviderUrlBuilder.Endpoint.CHAT_COMPLETIONS);
        assertEquals("https://gateway.example.com/custom/chat", url);
    }

    private ResolvedAiProviderConfig config(String baseUrl, AiBaseUrlMode mode) {
        return new ResolvedAiProviderConfig(
                "provider-openai",
                AiProviderType.OPENAI,
                "OpenAI",
                true,
                true,
                new ResolvedAiApiKey("key-1", "sk", "sk***"),
                baseUrl,
                mode,
                "gpt-4.1-mini",
                1000,
                0,
                0.2d,
                0,
                AiProviderConfigSource.DB,
                "ok"
        );
    }
}
