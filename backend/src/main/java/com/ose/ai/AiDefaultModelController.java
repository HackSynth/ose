package com.ose.ai;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/default-models")
@RequiredArgsConstructor
public class AiDefaultModelController {

    private final AiDefaultModelService defaultModelService;

    @GetMapping
    public ApiResponse<AiProviderAdminDtos.DefaultModelsResponse> getDefaultModels() {
        return ApiResponse.success(defaultModelService.getDefaultModels());
    }

    @PutMapping
    public ApiResponse<AiProviderAdminDtos.DefaultModelsResponse> updateDefaultModels(
            @Valid @RequestBody AiProviderAdminDtos.UpdateDefaultModelsRequest request) {
        return ApiResponse.success(defaultModelService.update(request));
    }
}
