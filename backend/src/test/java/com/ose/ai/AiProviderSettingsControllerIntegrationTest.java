package com.ose.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AiProviderSettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAiProviderClient openAiProviderClient;

    @MockBean
    private AnthropicProviderClient anthropicProviderClient;

    @BeforeEach
    void setUp() {
        when(openAiProviderClient.provider()).thenReturn(AiProviderType.OPENAI);
        when(anthropicProviderClient.provider()).thenReturn(AiProviderType.ANTHROPIC);
        when(openAiProviderClient.testConnection(any())).thenReturn(new AiProviderHealthResult(
                true,
                AiProviderType.OPENAI,
                "gpt-4.1-mini",
                120L,
                "OpenAI 连通性测试通过",
                AiProviderConfigSource.DB,
                AiProviderHealthStatus.SUCCESS
        ));
    }

    @Test
    @WithMockUser
    void shouldUpdateSettingsWithoutReturningPlaintextKey() throws Exception {
        String payload = """
                {
                  "enabled": true,
                  "apiKey": "sk-secret-1234",
                  "baseUrl": "https://proxy.example.com",
                  "defaultModel": "gpt-4.1-mini",
                  "timeoutMs": 10000,
                  "maxRetries": 2,
                  "temperature": 0.4
                }
                """;

        mockMvc.perform(put("/api/ai/settings/OPENAI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.maskedKey").value("sk-***1234"))
                .andExpect(jsonPath("$.data.storedMaskedKey").value("sk-***1234"))
                .andExpect(content().string(not(containsString("sk-secret-1234"))));
    }

    @Test
    @WithMockUser
    void shouldTestConnectionWithPreviewConfig() throws Exception {
        String payload = """
                {
                  "enabled": true,
                  "apiKey": "sk-preview-5678",
                  "baseUrl": "https://proxy.example.com",
                  "defaultModel": "gpt-4.1-mini",
                  "timeoutMs": 10000,
                  "maxRetries": 1,
                  "temperature": 0.2
                }
                """;

        mockMvc.perform(post("/api/ai/settings/OPENAI/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.provider").value("OPENAI"))
                .andExpect(jsonPath("$.data.model").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.configSource").value("DB"));
    }
}
