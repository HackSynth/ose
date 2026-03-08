package com.ose.settings;

import com.ose.common.api.ApiResponse;
import com.ose.plan.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;
    private final PlanService planService;

    @GetMapping
    public ApiResponse<SettingDtos.SettingResponse> getSetting() {
        return ApiResponse.success(settingService.getSetting());
    }

    @PutMapping
    public ApiResponse<SettingDtos.SettingResponse> updateSetting(@Valid @RequestBody SettingDtos.UpdateSettingRequest request) {
        SettingDtos.SettingResponse response = settingService.updateSetting(request);
        planService.generateCurrentPlan();
        return ApiResponse.success(response);
    }
}
