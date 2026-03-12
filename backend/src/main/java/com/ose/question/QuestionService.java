package com.ose.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.common.exception.BusinessException;
import com.ose.common.exception.NotFoundException;
import com.ose.model.AppEnums;
import com.ose.model.KnowledgePoint;
import com.ose.model.Question;
import com.ose.model.QuestionOption;
import com.ose.repository.KnowledgePointRepository;
import com.ose.repository.QuestionRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final ObjectMapper objectMapper;

    public List<QuestionDtos.QuestionResponse> list(String keyword, AppEnums.QuestionType type, Integer year,
                                                    Integer difficulty, Long knowledgePointId, String source) {
        Specification<Question> specification = Specification.where(null);
        if (keyword != null && !keyword.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(root.get("title"), "%" + keyword + "%"),
                    cb.like(root.get("content"), "%" + keyword + "%")
            ));
        }
        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (year != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("year"), year));
        }
        if (difficulty != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("difficulty"), difficulty));
        }
        if (source != null && !source.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.like(root.get("source"), "%" + source + "%"));
        }
        if (knowledgePointId != null) {
            specification = specification.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("knowledgePoints", JoinType.LEFT).get("id"), knowledgePointId);
            });
        }
        return questionRepository.findAll(specification).stream().map(this::toDto).toList();
    }

    @Transactional
    public QuestionDtos.QuestionResponse create(QuestionDtos.QuestionSaveRequest request) {
        Question question = new Question();
        apply(question, request);
        return toDto(questionRepository.save(question));
    }

    @Transactional
    public QuestionDtos.QuestionResponse update(Long id, QuestionDtos.QuestionSaveRequest request) {
        Question question = questionRepository.findById(id).orElseThrow(() -> new NotFoundException("题目不存在"));
        apply(question, request);
        return toDto(questionRepository.save(question));
    }

    @Transactional
    public void delete(Long id) {
        questionRepository.deleteById(id);
    }

    @Transactional
    public QuestionDtos.QuestionImportResult importQuestions(MultipartFile file) {
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("questions.json").toLowerCase(Locale.ROOT);
        List<String> errors = new ArrayList<>();
        int imported = 0;
        try {
            if (filename.endsWith(".json")) {
                JsonNode root = objectMapper.readTree(file.getInputStream());
                List<QuestionImportItem> items = objectMapper.convertValue(root, new TypeReference<>() {
                });
                for (QuestionImportItem item : items) {
                    int before = errors.size();
                    importSingle(item, errors);
                    if (errors.size() == before) {
                        imported++;
                    }
                }
            } else {
                try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                        .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    for (var record : parser) {
                        try {
                            int before = errors.size();
                            importSingle(new QuestionImportItem(
                                    record.get("type"),
                                    record.get("title"),
                                    record.get("content"),
                                    List.of(
                                            new QuestionDtos.QuestionOptionInput("A", record.get("optionA")),
                                            new QuestionDtos.QuestionOptionInput("B", record.get("optionB")),
                                            new QuestionDtos.QuestionOptionInput("C", record.get("optionC")),
                                            new QuestionDtos.QuestionOptionInput("D", record.get("optionD"))
                                    ),
                                    record.get("correctAnswer"),
                                    record.get("referenceAnswer"),
                                    Integer.parseInt(record.get("year")),
                                    Integer.parseInt(record.get("difficulty")),
                                    record.get("source"),
                                    Arrays.asList(record.get("tags").replace("\"", "").split("\\|")),
                                    Arrays.asList(record.get("knowledgeCodes").split("\\|")),
                                    new BigDecimal(record.get("score"))
                            ), errors);
                            if (errors.size() == before) {
                                imported++;
                            }
                        } catch (Exception ex) {
                            errors.add("CSV 导入失败：" + ex.getMessage());
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new BusinessException("导入文件解析失败");
        }
        return new QuestionDtos.QuestionImportResult(imported, errors);
    }

    public ResponseEntity<byte[]> exportQuestions(String format) throws IOException {
        List<QuestionDtos.QuestionResponse> questions = questionRepository.findAll().stream().map(this::toDto).toList();
        if ("csv".equalsIgnoreCase(format)) {
            StringWriter writer = new StringWriter();
            try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("type", "title", "content", "optionA", "optionB", "optionC", "optionD", "correctAnswer", "year", "difficulty", "source", "tags", "knowledgeCodes", "score", "referenceAnswer")
                    .build())) {
                for (QuestionDtos.QuestionResponse item : questions) {
                    Map<String, String> optionMap = new HashMap<>();
                    item.options().forEach(option -> optionMap.put(option.key(), option.content()));
                    printer.printRecord(
                            item.type(), item.title(), item.content(), optionMap.get("A"), optionMap.get("B"), optionMap.get("C"), optionMap.get("D"),
                            item.correctAnswer(), item.year(), item.difficulty(), item.source(), String.join("|", item.tags()),
                            item.knowledgePoints().stream().map(QuestionDtos.QuestionRelationDto::code).reduce((a, b) -> a + "|" + b).orElse(""),
                            item.score(), item.referenceAnswer()
                    );
                }
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions-export.csv")
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .body(writer.toString().getBytes(StandardCharsets.UTF_8));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(questions));
    }

    public ResponseEntity<byte[]> template(String format) throws IOException {
        String normalized = "csv".equalsIgnoreCase(format) ? "csv" : "json";
        ClassPathResource resource = new ClassPathResource("templates/questions-template." + normalized);
        String mediaType = "csv".equals(normalized) ? "text/csv;charset=UTF-8" : MediaType.APPLICATION_JSON_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions-template." + normalized)
                .contentType(MediaType.parseMediaType(mediaType))
                .body(resource.getInputStream().readAllBytes());
    }

    public List<Question> findAllByIds(List<Long> ids) {
        return questionRepository.findAllById(ids);
    }

    public List<Question> activeByType(AppEnums.QuestionType type) {
        return questionRepository.findByTypeAndActiveTrue(type);
    }

    public Question getEntity(Long id) {
        return questionRepository.findById(id).orElseThrow(() -> new NotFoundException("题目不存在"));
    }

    private void apply(Question question, QuestionDtos.QuestionSaveRequest request) {
        question.setType(request.type());
        question.setTitle(request.title());
        question.setContent(request.content());
        question.setCorrectAnswer(request.correctAnswer());
        question.setExplanation(request.explanation());
        question.setReferenceAnswer(request.referenceAnswer());
        question.setYear(request.year());
        question.setDifficulty(request.difficulty());
        question.setSource(request.source());
        question.setTags(String.join(",", request.tags()));
        question.setScore(request.score());
        question.setActive(request.active() == null || request.active());
        if (question.getAiGenerated() == null) {
            question.setAiGenerated(false);
        }
        question.getOptions().clear();
        if (request.options() != null) {
            request.options().stream()
                    .filter(option -> option.content() != null && !option.content().isBlank())
                    .forEach(option -> question.getOptions().add(QuestionOption.builder()
                            .question(question)
                            .optionKey(option.key())
                            .content(option.content())
                            .build()));
        }
        question.getKnowledgePoints().clear();
        if (request.knowledgePointIds() != null && !request.knowledgePointIds().isEmpty()) {
            question.getKnowledgePoints().addAll(knowledgePointRepository.findAllById(request.knowledgePointIds()));
        }
    }

    private void importSingle(QuestionImportItem item, List<String> errors) {
        try {
            QuestionDtos.QuestionSaveRequest request = new QuestionDtos.QuestionSaveRequest(
                    AppEnums.QuestionType.valueOf(item.type()),
                    item.title(),
                    item.content(),
                    item.options(),
                    item.correctAnswer(),
                    null,
                    item.referenceAnswer(),
                    item.year(),
                    item.difficulty(),
                    item.source(),
                    item.tags(),
                    knowledgePointRepository.findAll().stream().filter(point -> item.knowledgeCodes().contains(point.getCode())).map(KnowledgePoint::getId).toList(),
                    item.score(),
                    true
            );
            create(request);
        } catch (Exception ex) {
            errors.add(item.title() + " 导入失败：" + ex.getMessage());
        }
    }

    private QuestionDtos.QuestionResponse toDto(Question question) {
        return new QuestionDtos.QuestionResponse(
                question.getId(),
                question.getType(),
                question.getTitle(),
                question.getContent(),
                question.getOptions().stream().map(option -> new QuestionDtos.QuestionOptionDto(option.getId(), option.getOptionKey(), option.getContent())).toList(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getReferenceAnswer(),
                question.getYear(),
                question.getDifficulty(),
                question.getSource(),
                Arrays.stream(Optional.ofNullable(question.getTags()).orElse("").split(",")).filter(tag -> !tag.isBlank()).toList(),
                question.getKnowledgePoints().stream().map(point -> new QuestionDtos.QuestionRelationDto(point.getId(), point.getCode(), point.getName())).toList(),
                question.getScore(),
                question.getActive(),
                question.getAiGenerated(),
                question.getAiProvider(),
                question.getAiModel()
        );
    }

    private record QuestionImportItem(
            String type,
            String title,
            String content,
            List<QuestionDtos.QuestionOptionInput> options,
            String correctAnswer,
            String referenceAnswer,
            Integer year,
            Integer difficulty,
            String source,
            List<String> tags,
            List<String> knowledgeCodes,
            BigDecimal score
    ) {
    }
}
