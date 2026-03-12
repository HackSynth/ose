package com.ose.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.common.config.AppProperties;
import com.ose.common.exception.BusinessException;
import com.ose.model.AiGenerationRecord;
import com.ose.model.AppEnums;
import com.ose.model.KnowledgePoint;
import com.ose.repository.AiGenerationRecordRepository;
import com.ose.repository.KnowledgePointRepository;
import com.ose.repository.MistakeRecordRepository;
import com.ose.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGenerationServiceTest {

    @Mock
    private AiProviderClient openAiClient;
    @Mock
    private AiProviderClient compatibleClient;
    @Mock
    private AiProviderService providerService;
    @Mock
    private AiProviderConfigurationResolver resolver;
    @Mock
    private AiApiKeyRotationService keyRotationService;
    @Mock
    private KnowledgePointRepository knowledgePointRepository;
    @Mock
    private MistakeRecordRepository mistakeRecordRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AiGenerationRecordRepository aiGenerationRecordRepository;

    private AiGenerationService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        service = new AiGenerationService(
                List.of(openAiClient, compatibleClient),
                providerService,
                resolver,
                keyRotationService,
                knowledgePointRepository,
                mistakeRecordRepository,
                questionRepository,
                aiGenerationRecordRepository,
                new AiPromptBuilder(),
                new AiQuestionValidationService(new ObjectMapper(), new AiPromptBuilder(), questionRepository),
                new ObjectMapper(),
                appProperties
        );
        lenient().when(aiGenerationRecordRepository.save(any(AiGenerationRecord.class))).thenAnswer(invocation -> {
            AiGenerationRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(1L);
            }
            return record;
        });
    }

    @Test
    void shouldGenerateWithExplicitProviderIdAndModel() {
        when(openAiClient.providerType()).thenReturn(AiProviderType.OPENAI);
        when(resolver.resolveModelSelection("provider-openai", "gpt-4.1-mini", AiModelUseCase.QUESTION_GENERATION))
                .thenReturn(new ResolvedAiModelSelection("provider-openai", AiProviderType.OPENAI, "OpenAI 主站", "gpt-4.1-mini"));
        when(resolver.resolveRequestedModel("provider-openai", "gpt-4.1-mini")).thenReturn("gpt-4.1-mini");
        when(resolver.resolve("provider-openai")).thenReturn(new ResolvedAiProviderConfig(
                "provider-openai",
                AiProviderType.OPENAI,
                "OpenAI 主站",
                true,
                true,
                new ResolvedAiApiKey("key-1", "sk-live-1234", "sk-***1234"),
                "https://api.openai.com",
                AiBaseUrlMode.ROOT,
                "gpt-4.1-mini",
                10000,
                1,
                0.2d,
                0,
                AiProviderConfigSource.DB,
                "数据库配置可用"
        ));

        KnowledgePoint point = new KnowledgePoint();
        point.setId(1L);
        point.setName("事务管理");
        when(knowledgePointRepository.findAllById(List.of(1L))).thenReturn(List.of(point));
        when(openAiClient.generate(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(
                new AiQuestionDtos.ProviderGenerationPayload(
                        AppEnums.QuestionType.MORNING_SINGLE,
                        List.of(new AiQuestionDtos.ProviderQuestionPayload(
                                "事务题",
                                "关于事务一致性的说法，正确的是？",
                                List.of(
                                        new AiQuestionDtos.QuestionOptionDraft("A", "A"),
                                        new AiQuestionDtos.QuestionOptionDraft("B", "B"),
                                        new AiQuestionDtos.QuestionOptionDraft("C", "C"),
                                        new AiQuestionDtos.QuestionOptionDraft("D", "D")
                                ),
                                "A",
                                "解析",
                                null,
                                null,
                                List.of("事务管理"),
                                "MEDIUM"
                        ))
                )
        );

        AiQuestionDtos.AiQuestionGenerationRequest request = new AiQuestionDtos.AiQuestionGenerationRequest(
                "provider-openai",
                null,
                "gpt-4.1-mini",
                AppEnums.QuestionType.MORNING_SINGLE,
                AiQuestionDtos.AiQuestionTopicType.KNOWLEDGE_POINT,
                List.of(1L),
                AiQuestionDtos.AiQuestionDifficulty.MEDIUM,
                1,
                true,
                true,
                false,
                "中文",
                AiQuestionDtos.AiStyleType.EXAM,
                null
        );

        AiQuestionDtos.AiQuestionGenerationResult result = service.generate(request);

        assertEquals("provider-openai", result.providerId());
        assertEquals("OpenAI 主站", result.providerDisplayName());
        assertEquals(1, result.drafts().size());
        verify(openAiClient).generate(any(), anyString(), anyString(), anyString(), anyString());
        verify(compatibleClient, never()).generate(any(), anyString(), anyString(), anyString(), anyString());
        verify(keyRotationService).recordSuccess("key-1");
    }

    @Test
    void shouldRouteToOpenAiCompatibleClient() {
        when(openAiClient.providerType()).thenReturn(AiProviderType.OPENAI);
        when(compatibleClient.providerType()).thenReturn(AiProviderType.OPENAI_COMPATIBLE);
        when(resolver.resolveModelSelection("provider-compatible", "qwen-plus", AiModelUseCase.QUESTION_GENERATION))
                .thenReturn(new ResolvedAiModelSelection("provider-compatible", AiProviderType.OPENAI_COMPATIBLE, "网关", "qwen-plus"));
        when(resolver.resolveRequestedModel("provider-compatible", "qwen-plus")).thenReturn("qwen-plus");
        when(resolver.resolve("provider-compatible")).thenReturn(new ResolvedAiProviderConfig(
                "provider-compatible",
                AiProviderType.OPENAI_COMPATIBLE,
                "网关",
                true,
                true,
                new ResolvedAiApiKey("key-2", "sk-compatible", "sk-***ible"),
                "https://gateway.example.com",
                AiBaseUrlMode.ROOT,
                "qwen-plus",
                10000,
                0,
                0.2d,
                0,
                AiProviderConfigSource.DB,
                "数据库配置可用"
        ));
        KnowledgePoint point = new KnowledgePoint();
        point.setId(1L);
        point.setName("事务管理");
        when(knowledgePointRepository.findAllById(List.of(1L))).thenReturn(List.of(point));
        when(compatibleClient.generate(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(
                new AiQuestionDtos.ProviderGenerationPayload(AppEnums.QuestionType.MORNING_SINGLE, List.of())
        );

        service.generate(new AiQuestionDtos.AiQuestionGenerationRequest(
                "provider-compatible",
                null,
                "qwen-plus",
                AppEnums.QuestionType.MORNING_SINGLE,
                AiQuestionDtos.AiQuestionTopicType.KNOWLEDGE_POINT,
                List.of(1L),
                AiQuestionDtos.AiQuestionDifficulty.MEDIUM,
                1,
                true,
                true,
                false,
                "中文",
                AiQuestionDtos.AiStyleType.EXAM,
                null
        ));

        verify(compatibleClient).generate(any(), anyString(), anyString(), anyString(), anyString());
        verify(openAiClient, never()).generate(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldFailWhenNoAvailableDefaultModel() {
        when(resolver.resolveModelSelection(null, null, AiModelUseCase.QUESTION_GENERATION)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.generate(
                new AiQuestionDtos.AiQuestionGenerationRequest(
                        null,
                        null,
                        null,
                        AppEnums.QuestionType.MORNING_SINGLE,
                        AiQuestionDtos.AiQuestionTopicType.KNOWLEDGE_POINT,
                        List.of(1L),
                        AiQuestionDtos.AiQuestionDifficulty.MEDIUM,
                        1,
                        true,
                        true,
                        false,
                        "中文",
                        AiQuestionDtos.AiStyleType.EXAM,
                        null
                )
        ));

        assertEquals("当前没有可用的 AI Provider 或模型，请先在模型服务页完成配置", ex.getMessage());
    }
}
