package com.ose.ai;

import com.ose.model.AppEnums;
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

import java.util.List;

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

    @MockBean
    private OpenAiProviderClient openAiProviderClient;

    @MockBean
    private AnthropicProviderClient anthropicProviderClient;

    @BeforeEach
    void setUp() {
        when(openAiProviderClient.provider()).thenReturn(AiProviderType.OPENAI);
        when(anthropicProviderClient.provider()).thenReturn(AiProviderType.ANTHROPIC);
        when(openAiProviderClient.isConfigured()).thenReturn(true);
        when(anthropicProviderClient.isConfigured()).thenReturn(true);
        when(openAiProviderClient.models()).thenReturn(List.of(new AiQuestionDtos.AiModelConfig("gpt-4.1-mini", "gpt-4.1-mini", true)));
        when(anthropicProviderClient.models()).thenReturn(List.of(new AiQuestionDtos.AiModelConfig("claude-3-5-sonnet-latest", "claude-3-5-sonnet-latest", true)));
    }

    @Test
    @WithMockUser
    void shouldGenerateAndSaveMorningQuestions() throws Exception {
        when(openAiProviderClient.generate(any(), anyString(), anyString(), anyString())).thenReturn(new AiQuestionDtos.ProviderGenerationPayload(
                AppEnums.QuestionType.MORNING_SINGLE,
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
                        List.of("事务管理"),
                        "MEDIUM"
                ))
        ));

        String generatePayload = """
                {
                  "provider":"OPENAI",
                  "model":"gpt-4.1-mini",
                  "questionType":"MORNING_SINGLE",
                  "topicType":"KNOWLEDGE_POINT",
                  "knowledgePointIds":[1],
                  "difficulty":"MEDIUM",
                  "count":1,
                  "includeExplanation":true,
                  "includeAnswer":true,
                  "saveToBank":false,
                  "language":"中文",
                  "styleType":"EXAM"
                }
                """;

        String savePayload = """
                {
                  "provider":"OPENAI",
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
                    "knowledgePointIds":[1],
                    "difficulty":"MEDIUM",
                    "tags":["AI生成"]
                  }]
                }
                """;

        mockMvc.perform(post("/api/ai/questions/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("OPENAI"))
                .andExpect(jsonPath("$.data.drafts[0].title").value("事务一致性"));

        mockMvc.perform(post("/api/ai/questions/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(savePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.savedCount").value(1));
    }

    @Test
    @WithMockUser
    void shouldGenerateAfternoonQuestionWithAnthropic() throws Exception {
        when(anthropicProviderClient.generate(any(), anyString(), anyString(), anyString())).thenReturn(new AiQuestionDtos.ProviderGenerationPayload(
                AppEnums.QuestionType.AFTERNOON_CASE,
                List.of(new AiQuestionDtos.ProviderQuestionPayload(
                        "电商系统重构案例",
                        "某电商平台在双十一期间出现性能瓶颈，请给出分析。",
                        List.of(),
                        null,
                        "从架构、缓存、数据库分层分析",
                        "指出瓶颈、给出改造方案、评估风险",
                        List.of("识别瓶颈", "方案设计", "风险控制"),
                        List.of("软件架构设计"),
                        "HARD"
                ))
        ));

        mockMvc.perform(post("/api/ai/questions/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"ANTHROPIC",
                                  "model":"claude-3-5-sonnet-latest",
                                  "questionType":"AFTERNOON_CASE",
                                  "topicType":"AFTERNOON_SET",
                                  "knowledgePointIds":[1],
                                  "difficulty":"HARD",
                                  "count":1,
                                  "includeExplanation":true,
                                  "includeAnswer":true,
                                  "saveToBank":false,
                                  "language":"中文",
                                  "styleType":"COMPREHENSIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("ANTHROPIC"))
                .andExpect(jsonPath("$.data.drafts[0].referenceAnswer").exists());

        mockMvc.perform(get("/api/ai/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
