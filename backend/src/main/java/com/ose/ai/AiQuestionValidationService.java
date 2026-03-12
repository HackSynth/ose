package com.ose.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.ose.model.AppEnums;
import com.ose.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiQuestionValidationService {

    private final ObjectMapper objectMapper;
    private final AiPromptBuilder promptBuilder;
    private final QuestionRepository questionRepository;

    public List<String> validateSchema(AiQuestionDtos.ProviderGenerationPayload payload) {
        String schemaText = promptBuilder.batchSchema(payload.questionType());
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaText);
            JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
            JsonNode payloadNode = objectMapper.valueToTree(payload);
            Set<ValidationMessage> messages = schema.validate(payloadNode);
            return messages.stream().map(ValidationMessage::getMessage).toList();
        } catch (Exception ex) {
            return List.of("结构化校验失败: " + ex.getMessage());
        }
    }

    public List<String> validateBusiness(AiQuestionDtos.ProviderGenerationPayload payload) {
        List<String> errors = new ArrayList<>();
        Set<String> duplicateCheck = new HashSet<>();
        for (int i = 0; i < payload.questions().size(); i++) {
            AiQuestionDtos.ProviderQuestionPayload q = payload.questions().get(i);
            String prefix = "第" + (i + 1) + "题";
            if (q.title() == null || q.title().isBlank()) {
                errors.add(prefix + "标题缺失");
            }
            if (q.content() == null || q.content().isBlank()) {
                errors.add(prefix + "题干缺失");
            }
            if (q.knowledgePointNames() == null || q.knowledgePointNames().isEmpty()) {
                errors.add(prefix + "知识点缺失");
            }
            if (payload.questionType() == AppEnums.QuestionType.MORNING_SINGLE) {
                if (q.options() == null || q.options().size() != 4) {
                    errors.add(prefix + "选项必须为 4 个");
                }
                if (q.correctAnswer() == null || !Set.of("A", "B", "C", "D").contains(q.correctAnswer())) {
                    errors.add(prefix + "正确答案无效");
                }
            } else {
                if (q.referenceAnswer() == null || q.referenceAnswer().isBlank()) {
                    errors.add(prefix + "参考答案缺失");
                }
                if (q.scoringPoints() == null || q.scoringPoints().isEmpty()) {
                    errors.add(prefix + "评分点缺失");
                }
            }
            String dedupKey = (q.title() + "#" + q.content()).trim();
            if (!duplicateCheck.add(dedupKey)) {
                errors.add(prefix + "与本批次其他题重复");
            }
            if (questionRepository.existsByTitleAndContent(q.title(), q.content())) {
                errors.add(prefix + "与题库已有题重复");
            }
        }
        return errors;
    }
}
