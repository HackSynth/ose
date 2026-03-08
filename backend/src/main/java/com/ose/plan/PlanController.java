package com.ose.plan;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping("/plans/current")
    public ApiResponse<PlanDtos.PlanResponse> currentPlan() {
        return ApiResponse.success(planService.currentPlan());
    }

    @PostMapping("/plans/generate")
    public ApiResponse<PlanDtos.PlanResponse> generate() {
        return ApiResponse.success(planService.generateCurrentPlan());
    }

    @PatchMapping("/tasks/{taskId}")
    public ApiResponse<PlanDtos.TaskResponse> updateTask(@PathVariable Long taskId,
                                                         @Valid @RequestBody PlanDtos.UpdateTaskRequest request) {
        return ApiResponse.success(planService.updateTask(taskId, request));
    }

    @PostMapping("/tasks/rebalance")
    public ApiResponse<PlanDtos.PlanResponse> rebalance(@RequestBody(required = false) PlanDtos.RebalanceRequest request) {
        return ApiResponse.success(planService.rebalance(request == null ? new PlanDtos.RebalanceRequest(null) : request));
    }
}
