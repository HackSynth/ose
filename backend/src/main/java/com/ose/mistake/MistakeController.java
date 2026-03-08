package com.ose.mistake;

import com.ose.common.api.ApiResponse;
import com.ose.model.AppEnums;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mistakes")
@RequiredArgsConstructor
public class MistakeController {

    private final MistakeService mistakeService;

    @GetMapping
    public ApiResponse<List<MistakeDtos.MistakeView>> list(@RequestParam(required = false) AppEnums.ReviewStatus status,
                                                           @RequestParam(required = false) Long knowledgePointId) {
        return ApiResponse.success(mistakeService.list(status, knowledgePointId));
    }

    @PatchMapping("/{id}")
    public ApiResponse<MistakeDtos.MistakeView> update(@PathVariable Long id,
                                                       @Valid @RequestBody MistakeDtos.UpdateMistakeRequest request) {
        return ApiResponse.success(mistakeService.update(id, request));
    }
}
