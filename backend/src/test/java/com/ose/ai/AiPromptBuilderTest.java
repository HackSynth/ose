package com.ose.ai;

import com.ose.model.AppEnums;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptBuilderTest {

    private final AiPromptBuilder promptBuilder = new AiPromptBuilder();

    @Test
    void shouldBuildMorningPromptInChinese() {
        var request = new AiQuestionDtos.AiQuestionGenerationRequest(
                "provider-openai",
                AiProviderType.OPENAI,
                "gpt-4.1-mini",
                AppEnums.QuestionType.MORNING_SINGLE,
                AiQuestionDtos.AiQuestionTopicType.KNOWLEDGE_POINT,
                List.of(1L),
                AiQuestionDtos.AiQuestionDifficulty.MEDIUM,
                3,
                true,
                true,
                false,
                "中文",
                AiQuestionDtos.AiStyleType.EXAM,
                ""
        );

        String systemPrompt = promptBuilder.buildSystemPrompt(request);
        String userPrompt = promptBuilder.buildUserPrompt(request, List.of("事务管理"));

        assertTrue(systemPrompt.contains("严格 JSON"));
        assertTrue(systemPrompt.contains("软件设计师"));
        assertTrue(userPrompt.contains("知识点=事务管理"));
        assertTrue(userPrompt.contains("题型=上午单选题"));
    }

    @Test
    void shouldContainBatchSchema() {
        String schema = promptBuilder.batchSchema(AppEnums.QuestionType.AFTERNOON_CASE);
        assertTrue(schema.contains("questionType"));
        assertTrue(schema.contains("scoringPoints"));
    }
}
