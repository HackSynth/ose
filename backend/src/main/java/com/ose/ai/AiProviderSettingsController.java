package com.ose.ai;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/settings")
@RequiredArgsConstructor
public class AiProviderSettingsController {

    private final AiProviderSettingsService settingsService;

    @GetMapping
    public ApiResponse<AiProviderSettingsDtos.AiSettingsResponse> settings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @PutMapping("/{provider}")
    public ApiResponse<AiProviderSettingsDtos.AiProviderSettingsSummary> update(@PathVariable AiProviderType provider,
                                                                                @Valid @RequestBody AiProviderSettingsDtos.UpdateAiProviderSettingsRequest request) {
        return ApiResponse.success(settingsService.update(provider, request));
    }

    @PostMapping("/{provider}/test")
    public ApiResponse<AiProviderSettingsDtos.AiProviderConnectionTestResponse> test(@PathVariable AiProviderType provider) {
        AiProviderHealthResult result = settingsService.test(provider);
        return ApiResponse.success(new AiProviderSettingsDtos.AiProviderConnectionTestResponse(
                result.success(),
                result.provider(),
                result.model(),
                result.latencyMs(),
                result.message(),
                result.configSource()
        ));
    }

    @GetMapping("/{provider}/models")
    public ApiResponse<AiProviderSettingsDtos.AiProviderModelListResponse> models(@PathVariable AiProviderType provider) {
        return ApiResponse.success(settingsService.models(provider));
    }
}
