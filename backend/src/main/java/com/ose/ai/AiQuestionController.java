package com.ose.ai;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiGenerationService aiGenerationService;

    @GetMapping("/provider-options")
    public ApiResponse<List<AiQuestionDtos.AiProviderStatus>> providers() {
        return ApiResponse.success(aiGenerationService.providers());
    }

    @GetMapping("/models")
    public ApiResponse<List<AiQuestionDtos.AiModelConfig>> models(@RequestParam(required = false) AiProviderType provider) {
        return ApiResponse.success(aiGenerationService.models(provider));
    }

    @GetMapping("/health")
    public ApiResponse<AiQuestionDtos.AiHealthResponse> health() {
        return ApiResponse.success(aiGenerationService.health());
    }

    @GetMapping("/history")
    public ApiResponse<List<AiQuestionDtos.AiGenerationHistoryItem>> history() {
        return ApiResponse.success(aiGenerationService.history());
    }

    @PostMapping("/questions/generate")
    public ApiResponse<AiQuestionDtos.AiQuestionGenerationResult> generate(@Valid @RequestBody AiQuestionDtos.AiQuestionGenerationRequest request) {
        return ApiResponse.success(aiGenerationService.generate(request));
    }

    @PostMapping("/questions/save")
    public ApiResponse<AiQuestionDtos.AiSaveResult> save(@Valid @RequestBody AiQuestionDtos.AiQuestionSaveRequest request) {
        return ApiResponse.success(aiGenerationService.save(request));
    }
}
