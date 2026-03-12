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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiGenerationServiceTest {

    @Mock
    private AiProviderClient openAiClient;
    @Mock
    private AiProviderClient anthropicClient;
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
        appProperties.getAi().getOpenai().setDefaultModel("gpt-4.1-mini");
        appProperties.getAi().getAnthropic().setDefaultModel("claude-3-5-sonnet-latest");
        service = new AiGenerationService(
                List.of(openAiClient, anthropicClient),
                knowledgePointRepository,
                mistakeRecordRepository,
                questionRepository,
                aiGenerationRecordRepository,
                new AiPromptBuilder(),
                new AiQuestionValidationService(new ObjectMapper(), new AiPromptBuilder(), questionRepository),
                new ObjectMapper(),
                appProperties
        );
        when(aiGenerationRecordRepository.save(any(AiGenerationRecord.class))).thenAnswer(invocation -> {
            AiGenerationRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(1L);
            }
            return record;
        });
    }

    @Test
    void shouldRouteToOpenAiProvider() {
        when(openAiClient.provider()).thenReturn(AiProviderType.OPENAI);
        when(openAiClient.isConfigured()).thenReturn(true);
        when(questionRepository.existsByTitleAndContent(anyString(), anyString())).thenReturn(false);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setId(1L);
        kp.setName("事务管理");
        when(knowledgePointRepository.findAllById(List.of(1L))).thenReturn(List.of(kp));
        when(openAiClient.generate(any(), anyString(), anyString(), anyString())).thenReturn(new AiQuestionDtos.ProviderGenerationPayload(
                AppEnums.QuestionType.MORNING_SINGLE,
                List.of(new AiQuestionDtos.ProviderQuestionPayload(
                        "题目",
                        "题干",
                        List.of(
                                new AiQuestionDtos.QuestionOptionDraft("A", "1"),
                                new AiQuestionDtos.QuestionOptionDraft("B", "2"),
                                new AiQuestionDtos.QuestionOptionDraft("C", "3"),
                                new AiQuestionDtos.QuestionOptionDraft("D", "4")
                        ),
                        "A",
                        "解析",
                        null,
                        null,
                        List.of("事务管理"),
                        "MEDIUM"
                ))
        ));

        var request = new AiQuestionDtos.AiQuestionGenerationRequest(
                AiProviderType.OPENAI,
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
        );

        var result = service.generate(request);
        assertEquals(1, result.drafts().size());
        verify(openAiClient, times(1)).generate(any(), anyString(), anyString(), anyString());
        verify(anthropicClient, never()).generate(any(), anyString(), anyString(), anyString());

        ArgumentCaptor<AiGenerationRecord> captor = ArgumentCaptor.forClass(AiGenerationRecord.class);
        verify(aiGenerationRecordRepository, atLeastOnce()).save(captor.capture());
        assertEquals(1L, captor.getAllValues().get(0).getId());
    }

    @Test
    void shouldReturnBusinessExceptionWhenProviderUnavailable() {
        when(openAiClient.provider()).thenReturn(AiProviderType.OPENAI);
        when(openAiClient.isConfigured()).thenReturn(true);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setId(1L);
        kp.setName("事务管理");
        when(knowledgePointRepository.findAllById(List.of(1L))).thenReturn(List.of(kp));
        when(openAiClient.generate(any(), anyString(), anyString(), anyString())).thenThrow(new AiProviderException("超时"));

        var request = new AiQuestionDtos.AiQuestionGenerationRequest(
                AiProviderType.OPENAI,
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

        assertThrows(BusinessException.class, () -> service.generate(request));
    }
}
