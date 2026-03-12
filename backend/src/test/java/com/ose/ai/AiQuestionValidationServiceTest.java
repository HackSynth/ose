package com.ose.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.model.AppEnums;
import com.ose.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuestionValidationServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    private final AiPromptBuilder promptBuilder = new AiPromptBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRejectInvalidMorningOptions() {
        when(questionRepository.existsByTitleAndContent(anyString(), anyString())).thenReturn(false);
        AiQuestionValidationService service = new AiQuestionValidationService(objectMapper, promptBuilder, questionRepository);

        var payload = new AiQuestionDtos.ProviderGenerationPayload(
                AppEnums.QuestionType.MORNING_SINGLE,
                List.of(new AiQuestionDtos.ProviderQuestionPayload(
                        "题目1",
                        "题干1",
                        List.of(new AiQuestionDtos.QuestionOptionDraft("A", "1")),
                        "E",
                        "解析",
                        null,
                        null,
                        List.of("事务管理"),
                        "MEDIUM"
                ))
        );

        List<String> errors = service.validateBusiness(payload);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(item -> item.contains("选项必须为 4 个")));
    }

    @Test
    void shouldRejectSchemaWhenMissingRequiredField() {
        AiQuestionValidationService service = new AiQuestionValidationService(objectMapper, promptBuilder, questionRepository);

        var payload = new AiQuestionDtos.ProviderGenerationPayload(AppEnums.QuestionType.AFTERNOON_CASE, List.of());
        List<String> errors = service.validateSchema(payload);
        assertFalse(errors.isEmpty());
    }
}
