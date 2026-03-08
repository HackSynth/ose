package com.ose.plan;

import com.ose.model.AppEnums;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.List;

public final class PlanDtos {
    private PlanDtos() {
    }

    public record TaskResponse(
            Long id,
            AppEnums.PlanPhase phase,
            AppEnums.TaskType taskType,
            AppEnums.TaskStatus status,
            String title,
            String description,
            Long knowledgePointId,
            String knowledgePointName,
            LocalDate scheduledDate,
            Integer estimatedMinutes,
            Integer priority,
            Integer progress,
            LocalDate postponedTo
    ) {
    }

    public record PlanResponse(
            Long id,
            String name,
            LocalDate examDate,
            LocalDate startDate,
            LocalDate endDate,
            Integer totalHours,
            String status,
            String settingSnapshot,
            List<TaskResponse> tasks
    ) {
    }

    public record UpdateTaskRequest(
            AppEnums.TaskStatus status,
            @Min(value = 0, message = "完成度不能小于 0") @Max(value = 100, message = "完成度不能大于 100") Integer progress,
            @Min(value = 1, message = "优先级至少为 1") Integer priority,
            LocalDate postponedTo
    ) {
    }

    public record RebalanceRequest(LocalDate fromDate) {
    }
}
