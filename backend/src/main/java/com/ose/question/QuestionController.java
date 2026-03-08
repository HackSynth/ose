package com.ose.question;

import com.ose.common.api.ApiResponse;
import com.ose.model.AppEnums;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ApiResponse<List<QuestionDtos.QuestionResponse>> list(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) AppEnums.QuestionType type,
                                                                 @RequestParam(required = false) Integer year,
                                                                 @RequestParam(required = false) Integer difficulty,
                                                                 @RequestParam(required = false) Long knowledgePointId,
                                                                 @RequestParam(required = false) String source) {
        return ApiResponse.success(questionService.list(keyword, type, year, difficulty, knowledgePointId, source));
    }

    @PostMapping
    public ApiResponse<QuestionDtos.QuestionResponse> create(@Valid @RequestBody QuestionDtos.QuestionSaveRequest request) {
        return ApiResponse.success(questionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuestionDtos.QuestionResponse> update(@PathVariable Long id,
                                                             @Valid @RequestBody QuestionDtos.QuestionSaveRequest request) {
        return ApiResponse.success(questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }

    @PostMapping("/import")
    public ApiResponse<QuestionDtos.QuestionImportResult> importQuestions(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(questionService.importQuestions(file));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "json") String format) throws IOException {
        return questionService.exportQuestions(format);
    }

    @GetMapping("/templates/{format}")
    public ResponseEntity<byte[]> template(@PathVariable String format) throws IOException {
        return questionService.template(format);
    }
}
