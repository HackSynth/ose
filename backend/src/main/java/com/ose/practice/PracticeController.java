package com.ose.practice;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/sessions")
    public ApiResponse<PracticeDtos.PracticeSessionResponse> createSession(@Valid @RequestBody PracticeDtos.PracticeSessionRequest request) {
        return ApiResponse.success(practiceService.createSession(request));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<PracticeDtos.PracticeSessionResponse> getSession(@PathVariable Long id) {
        return ApiResponse.success(practiceService.getSession(id));
    }

    @PostMapping("/sessions/{id}/submit")
    public ApiResponse<PracticeDtos.PracticeSessionResponse> submit(@PathVariable Long id,
                                                                    @Valid @RequestBody PracticeDtos.SubmitPracticeRequest request) {
        return ApiResponse.success(practiceService.submit(id, request));
    }

    @PostMapping("/records/{id}/review")
    public ApiResponse<PracticeDtos.PracticeRecordView> review(@PathVariable Long id,
                                                               @RequestBody PracticeDtos.ReviewRecordRequest request) {
        return ApiResponse.success(practiceService.review(id, request));
    }

    @PatchMapping("/records/{id}/flags")
    public ApiResponse<PracticeDtos.PracticeRecordView> flags(@PathVariable Long id,
                                                              @RequestBody PracticeDtos.UpdateFlagsRequest request) {
        return ApiResponse.success(practiceService.updateFlags(id, request));
    }
}
