package com.ose.exam;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping("/exams")
    public ApiResponse<List<ExamDtos.ExamSummary>> listExams() {
        return ApiResponse.success(examService.listExams());
    }

    @GetMapping("/exam-attempts")
    public ApiResponse<List<ExamDtos.AttemptView>> listAttempts() {
        return ApiResponse.success(examService.listAttempts());
    }

    @PostMapping("/exams")
    public ApiResponse<ExamDtos.ExamSummary> createExam(@Valid @RequestBody ExamDtos.CreateExamRequest request) {
        return ApiResponse.success(examService.createExam(request));
    }

    @PostMapping("/exams/{id}/attempts")
    public ApiResponse<ExamDtos.AttemptView> startAttempt(@PathVariable Long id) {
        return ApiResponse.success(examService.startAttempt(id));
    }

    @GetMapping("/exam-attempts/{id}")
    public ApiResponse<ExamDtos.AttemptView> getAttempt(@PathVariable Long id) {
        return ApiResponse.success(examService.getAttempt(id));
    }

    @PostMapping("/exam-attempts/{id}/submit")
    public ApiResponse<ExamDtos.AttemptView> submit(@PathVariable Long id,
                                                    @RequestBody ExamDtos.SubmitAttemptRequest request) {
        return ApiResponse.success(examService.submitAttempt(id, request));
    }

    @PostMapping("/exam-attempts/{id}/score-afternoon")
    public ApiResponse<ExamDtos.AttemptView> scoreAfternoon(@PathVariable Long id,
                                                            @RequestBody ExamDtos.ScoreAfternoonRequest request) {
        return ApiResponse.success(examService.scoreAfternoon(id, request));
    }
}
