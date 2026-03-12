package com.ose.ai;

import com.ose.model.AppEnums;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiPromptBuilder {

    public String buildSystemPrompt(AiQuestionDtos.AiQuestionGenerationRequest request) {
        return "你是软件设计师考试出题助手。输出必须是严格 JSON，且仅输出 JSON。"
                + "必须使用中文。题目风格贴近软考软件设计师。"
                + "不得编造不合理考试规则，不得照搬版权内容。";
    }

    public String buildUserPrompt(AiQuestionDtos.AiQuestionGenerationRequest request, List<String> knowledgeNames) {
        String topic = switch (request.topicType()) {
            case KNOWLEDGE_POINT -> "按知识点出题";
            case DIFFICULTY -> "按难度出题";
            case QUESTION_TYPE -> "按题型出题";
            case EXAM_PHASE -> "按考试阶段出题";
            case WEAK_KNOWLEDGE -> "按薄弱知识点强化";
            case MISTAKE_SIMILAR -> "按错题相似知识点强化";
            case MORNING_SET -> "生成上午选择题";
            case AFTERNOON_SET -> "生成下午案例题";
        };
        String style = switch (request.styleType()) {
            case EXAM -> "偏考试风格";
            case FOUNDATION -> "偏基础巩固";
            case COMPREHENSIVE -> "偏综合应用";
        };
        String difficulty = switch (request.difficulty()) {
            case EASY -> "简单";
            case MEDIUM -> "中等";
            case HARD -> "困难";
        };
        String questionType = request.questionType() == AppEnums.QuestionType.MORNING_SINGLE ? "上午单选题" : "下午案例题";
        String knowledgeText = knowledgeNames.isEmpty() ? "未提供" : String.join("、", knowledgeNames);
        String includeExplanation = Boolean.TRUE.equals(request.includeExplanation()) ? "是" : "否";
        String includeAnswer = Boolean.TRUE.equals(request.includeAnswer()) ? "是" : "否";
        String additionalRequirement = request.additionalRequirement() == null ? "无" : request.additionalRequirement();
        return "请按以下要求生成题目：\n"
                + "场景=" + topic + "\n"
                + "题型=" + questionType + "\n"
                + "知识点=" + knowledgeText + "\n"
                + "难度=" + difficulty + "\n"
                + "数量=" + request.count() + "\n"
                + "语言=" + (request.language() == null || request.language().isBlank() ? "中文" : request.language()) + "\n"
                + "风格=" + style + "\n"
                + "是否生成解析=" + includeExplanation + "\n"
                + "是否生成答案=" + includeAnswer + "\n"
                + "额外要求=" + additionalRequirement + "\n"
                + "知识点名称清单=" + knowledgeNames.stream().collect(Collectors.joining("|"));
    }

    public String batchSchema(AppEnums.QuestionType questionType) {
        return questionType == AppEnums.QuestionType.MORNING_SINGLE ? morningBatchSchema() : afternoonBatchSchema();
    }

    public String morningQuestionSchema() {
        return """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "required":["title","content","options","correctAnswer","explanation","knowledgePointNames","difficulty"],
                  "properties":{
                    "title":{"type":"string","minLength":1},
                    "content":{"type":"string","minLength":1},
                    "options":{
                      "type":"array",
                      "minItems":4,
                      "maxItems":4,
                      "items":{
                        "type":"object",
                        "required":["key","content"],
                        "properties":{
                          "key":{"type":"string","enum":["A","B","C","D"]},
                          "content":{"type":"string","minLength":1}
                        }
                      }
                    },
                    "correctAnswer":{"type":"string","enum":["A","B","C","D"]},
                    "explanation":{"type":"string"},
                    "referenceAnswer":{"type":"string"},
                    "scoringPoints":{"type":"array","items":{"type":"string"}},
                    "knowledgePointNames":{"type":"array","minItems":1,"items":{"type":"string"}},
                    "difficulty":{"type":"string","enum":["EASY","MEDIUM","HARD"]}
                  }
                }
                """;
    }

    public String afternoonQuestionSchema() {
        return """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "required":["title","content","referenceAnswer","scoringPoints","explanation","knowledgePointNames","difficulty"],
                  "properties":{
                    "title":{"type":"string","minLength":1},
                    "content":{"type":"string","minLength":1},
                    "options":{"type":"array","items":{"type":"object"}},
                    "correctAnswer":{"type":"string"},
                    "explanation":{"type":"string","minLength":1},
                    "referenceAnswer":{"type":"string","minLength":1},
                    "scoringPoints":{"type":"array","minItems":1,"items":{"type":"string","minLength":1}},
                    "knowledgePointNames":{"type":"array","minItems":1,"items":{"type":"string"}},
                    "difficulty":{"type":"string","enum":["EASY","MEDIUM","HARD"]}
                  }
                }
                """;
    }

    public String morningBatchSchema() {
        return batchSchemaWrapper(morningQuestionSchema());
    }

    public String afternoonBatchSchema() {
        return batchSchemaWrapper(afternoonQuestionSchema());
    }

    private String batchSchemaWrapper(String questionSchema) {
        return """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "required":["questionType","questions"],
                  "properties":{
                    "questionType":{"type":"string","enum":["MORNING_SINGLE","AFTERNOON_CASE"]},
                    "questions":{
                      "type":"array",
                      "minItems":1,
                      "items":%s
                    }
                  }
                }
                """.formatted(questionSchema);
    }
}
