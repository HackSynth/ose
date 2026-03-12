package com.ose.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.model.AiModelEntity;
import com.ose.model.AiProviderApiKeyEntity;
import com.ose.model.AiProviderEntity;
import com.ose.model.KnowledgePoint;
import com.ose.repository.AiModelRepository;
import com.ose.repository.AiProviderApiKeyRepository;
import com.ose.repository.AiProviderRepository;
import com.ose.repository.KnowledgePointRepository;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AiQuestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiProviderRepository providerRepository;

    @Autowired
    private AiProviderApiKeyRepository apiKeyRepository;

    @Autowired
    private AiModelRepository modelRepository;

    @Autowired
    private KnowledgePointRepository knowledgePointRepository;

    @Autowired
    private AiSecretCryptoService cryptoService;

    @MockBean
    private OpenAiProviderClient openAiProviderClient;

    @MockBean
    private AnthropicProviderClient anthropicProviderClient;

    @MockBean
    private OpenAiCompatibleProviderClient openAiCompatibleProviderClient;

    private KnowledgePoint knowledgePoint;

    @BeforeEach
    void setUp() {
        providerRepository.deleteAll();
        knowledgePoint = knowledgePointRepository.findAllByOrderByLevelAscSortOrderAsc().stream().findFirst().orElseThrow();
        when(openAiProviderClient.providerType()).thenReturn(AiProviderType.OPENAI);
        when(anthropicProviderClient.providerType()).thenReturn(AiProviderType.ANTHROPIC);
        when(openAiCompatibleProviderClient.providerType()).thenReturn(AiProviderType.OPENAI_COMPATIBLE);
    }

    @Test
    @WithMockUser
    void shouldGenerateAndSaveMorningQuestionsWithProviderId() throws Exception {
        String providerId = persistProvider(AiProviderType.OPENAI, "OpenAI 主站", "gpt-4.1-mini");
        when(openAiProviderClient.generate(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(
                new AiQuestionDtos.ProviderGenerationPayload(
                        com.ose.model.AppEnums.QuestionType.MORNING_SINGLE,
                        List.of(new AiQuestionDtos.ProviderQuestionPayload(
                                "事务一致性",
                                "关于事务一致性，以下说法正确的是？",
                                List.of(
                                        new AiQuestionDtos.QuestionOptionDraft("A", "原子性"),
                                        new AiQuestionDtos.QuestionOptionDraft("B", "一致性"),
                                        new AiQuestionDtos.QuestionOptionDraft("C", "隔离性"),
                                        new AiQuestionDtos.QuestionOptionDraft("D", "持久性")
                                ),
                                "B",
                                "一致性约束结果合法",
                                null,
                                null,
                                List.of(knowledgePoint.getName()),
                                "MEDIUM"
                        ))
                )
        );

        MvcResult generate = mockMvc.perform(post("/api/ai/questions/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId":"%s",
                                  "model":"gpt-4.1-mini",
                                  "questionType":"MORNING_SINGLE",
                                  "topicType":"KNOWLEDGE_POINT",
                                  "knowledgePointIds":[%d],
                                  "difficulty":"MEDIUM",
                                  "count":1,
                                  "includeExplanation":true,
                                  "includeAnswer":true,
                                  "saveToBank":false,
                                  "language":"中文",
                                  "styleType":"EXAM"
                                }
                                """.formatted(providerId, knowledgePoint.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerId").value(providerId))
                .andExpect(jsonPath("$.data.provider").value("OPENAI"))
                .andExpect(jsonPath("$.data.drafts[0].providerId").value(providerId))
                .andReturn();

        JsonNode result = objectMapper.readTree(generate.getResponse().getContentAsString()).path("data");
        Long generationId = result.path("generationId").asLong();

        mockMvc.perform(post("/api/ai/questions/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "generationId":%d,
                                  "providerId":"%s",
                                  "model":"gpt-4.1-mini",
                                  "questionType":"MORNING_SINGLE",
                                  "drafts":[{
                                    "title":"事务一致性",
                                    "content":"关于事务一致性，以下说法正确的是？",
                                    "options":[
                                      {"key":"A","content":"原子性"},
                                      {"key":"B","content":"一致性"},
                                      {"key":"C","content":"隔离性"},
                                      {"key":"D","content":"持久性"}
                                    ],
                                    "correctAnswer":"B",
                                    "explanation":"一致性约束结果合法",
                                    "knowledgePointIds":[%d],
                                    "difficulty":"MEDIUM",
                                    "tags":["AI生成"]
                                  }]
                                }
                                """.formatted(generationId, providerId, knowledgePoint.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedCount").value(1));
    }

    @Test
    @WithMockUser
    void shouldGenerateAfternoonQuestionWithAnthropicProvider() throws Exception {
        String providerId = persistProvider(AiProviderType.ANTHROPIC, "Anthropic", "claude-3-5-sonnet-latest");
        when(anthropicProviderClient.generate(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(
                new AiQuestionDtos.ProviderGenerationPayload(
                        com.ose.model.AppEnums.QuestionType.AFTERNOON_CASE,
                        List.of(new AiQuestionDtos.ProviderQuestionPayload(
                                "电商系统重构案例",
                                "某电商平台在双十一期间出现性能瓶颈，请给出分析。",
                                List.of(),
                                null,
                                "从架构、缓存、数据库分层分析",
                                "指出瓶颈、给出改造方案、评估风险",
                                List.of("识别瓶颈", "方案设计", "风险控制"),
                                List.of(knowledgePoint.getName()),
                                "HARD"
                        ))
                )
        );

        mockMvc.perform(post("/api/ai/questions/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId":"%s",
                                  "model":"claude-3-5-sonnet-latest",
                                  "questionType":"AFTERNOON_CASE",
                                  "topicType":"AFTERNOON_SET",
                                  "knowledgePointIds":[%d],
                                  "difficulty":"HARD",
                                  "count":1,
                                  "includeExplanation":true,
                                  "includeAnswer":true,
                                  "saveToBank":false,
                                  "language":"中文",
                                  "styleType":"COMPREHENSIVE"
                                }
                                """.formatted(providerId, knowledgePoint.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("ANTHROPIC"))
                .andExpect(jsonPath("$.data.drafts[0].referenceAnswer").exists());

        mockMvc.perform(get("/api/ai/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String persistProvider(AiProviderType providerType, String displayName, String modelId) {
        String providerId = UUID.randomUUID().toString();
        AiProviderEntity provider = providerRepository.save(AiProviderEntity.builder()
                .id(providerId)
                .providerType(providerType)
                .displayName(displayName)
                .enabled(true)
                .keyRotationStrategy(AiKeyRotationStrategy.SEQUENTIAL_ROUND_ROBIN)
                .baseUrl(providerType == AiProviderType.ANTHROPIC ? "https://api.anthropic.com" : "https://api.openai.com")
                .baseUrlMode(AiBaseUrlMode.ROOT)
                .defaultModel(modelId)
                .timeoutMs(10000)
                .maxRetries(0)
                .temperature(0.2d)
                .configSource(AiProviderConfigSource.DB)
                .build());
        apiKeyRepository.save(AiProviderApiKeyEntity.builder()
                .id(UUID.randomUUID().toString())
                .provider(provider)
                .keyEncrypted(cryptoService.encrypt("sk-secret-1234"))
                .keyMask("sk-***1234")
                .enabled(true)
                .sortOrder(0)
                .consecutiveFailures(0)
                .build());
        modelRepository.save(AiModelEntity.builder()
                .id(UUID.randomUUID().toString())
                .provider(provider)
                .modelId(modelId)
                .displayName(modelId)
                .modelType(AiModelType.CHAT)
                .enabled(true)
                .sortOrder(0)
                .build());
        return providerId;
    }
}
