package com.ose.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AiProviderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAiProviderClient openAiProviderClient;

    @MockBean
    private AnthropicProviderClient anthropicProviderClient;

    @MockBean
    private OpenAiCompatibleProviderClient openAiCompatibleProviderClient;

    @BeforeEach
    void setUp() {
        when(openAiProviderClient.providerType()).thenReturn(AiProviderType.OPENAI);
        when(anthropicProviderClient.providerType()).thenReturn(AiProviderType.ANTHROPIC);
        when(openAiCompatibleProviderClient.providerType()).thenReturn(AiProviderType.OPENAI_COMPATIBLE);
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
    void shouldCreateProviderManageKeysAndHidePlaintext() throws Exception {
        String providerId = createProvider("""
                {
                  "displayName":"OpenAI 主站",
                  "providerType":"OPENAI",
                  "baseUrl":"https://api.openai.com",
                  "enabled":true,
                  "configSource":"DB"
                }
                """);

        mockMvc.perform(post("/api/ai/providers/{id}/keys", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "apiKey":"sk-secret-1234",
                                  "enabled":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maskedKey").value("sk-***1234"))
                .andExpect(content().string(not(containsString("sk-secret-1234"))));

        mockMvc.perform(get("/api/ai/providers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sk-***1234")));
    }

    @Test
    @WithMockUser
    void shouldEnableDisableDeleteAndTestProvider() throws Exception {
        String providerId = createProvider("""
                {
                  "displayName":"OpenAI 主站",
                  "providerType":"OPENAI",
                  "baseUrl":"https://api.openai.com",
                  "enabled":false,
                  "configSource":"DB"
                }
                """);
        mockMvc.perform(post("/api/ai/providers/{id}/models", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId":"gpt-4.1-mini",
                                  "displayName":"gpt-4.1-mini",
                                  "modelType":"CHAT",
                                  "enabled":true,
                                  "sortOrder":0
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ai/providers/{id}/keys", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "apiKey":"sk-secret-1234",
                                  "enabled":true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ai/providers/{id}/enable", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(post("/api/ai/providers/{id}/test", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.providerType").value("OPENAI"));

        mockMvc.perform(post("/api/ai/providers/{id}/disable", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(delete("/api/ai/providers/{id}", providerId))
                .andExpect(status().isOk());
    }

    private String createProvider(String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ai/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asText();
    }
}
