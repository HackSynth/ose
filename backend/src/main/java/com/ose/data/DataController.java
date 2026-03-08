package com.ose.data;

import com.ose.common.api.ApiResponse;
import com.ose.knowledge.KnowledgeService;
import com.ose.note.NoteService;
import com.ose.plan.PlanService;
import com.ose.question.QuestionService;
import com.ose.settings.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DataController {

    private final SettingService settingService;
    private final KnowledgeService knowledgeService;
    private final QuestionService questionService;
    private final PlanService planService;
    private final NoteService noteService;
    private final DataImportService dataImportService;

    @GetMapping("/api/export/full")
    public ApiResponse<Map<String, Object>> fullExport() {
        return ApiResponse.success(Map.of(
                "settings", settingService.getSetting(),
                "knowledgeTree", knowledgeService.tree(),
                "questions", questionService.list(null, null, null, null, null, null),
                "plan", planService.currentPlan(),
                "notes", noteService.list(null)
        ));
    }

    @PostMapping("/api/import/full")
    public ApiResponse<DataImportDtos.FullImportResult> fullImport(
            @RequestParam(defaultValue = "OVERWRITE") DataImportDtos.DuplicateStrategy strategy,
            @RequestBody DataImportDtos.FullImportRequest request) {
        return ApiResponse.success(dataImportService.importFull(request, strategy));
    }

    @PostMapping(value = "/api/import/full-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DataImportDtos.FullImportResult> fullImportFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "OVERWRITE") DataImportDtos.DuplicateStrategy strategy) throws IOException {
        return ApiResponse.success(dataImportService.importFromFile(file, strategy));
    }

    @GetMapping("/api/import/template")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/full-import-template.json");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=full-import-template.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource.getInputStream().readAllBytes());
    }
}
