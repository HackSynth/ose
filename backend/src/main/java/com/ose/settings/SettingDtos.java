package com.ose.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public final class SettingDtos {
    private SettingDtos() {
    }

    public record SettingResponse(
            Long id,
            LocalDate examDate,
            long daysUntilExam,
            Integer passingScore,
            Integer weeklyStudyHours,
            String learningPreference,
            List<Integer> reviewIntervals,
            Integer dailySessionMinutes
    ) {
    }

    public record UpdateSettingRequest(
            @NotNull(message = "请设置考试日期") LocalDate examDate,
            @NotNull(message = "请设置通过分数") @Min(value = 1, message = "通过分数必须大于 0") Integer passingScore,
            @NotNull(message = "请设置每周学习时长") @Min(value = 1, message = "每周学习时长必须大于 0") Integer weeklyStudyHours,
            @NotBlank(message = "请设置学习偏好") String learningPreference,
            @NotNull(message = "请设置复习周期") List<Integer> reviewIntervals,
            @NotNull(message = "请设置单次学习时长") @Min(value = 15, message = "单次学习时长不能少于 15 分钟") Integer dailySessionMinutes
    ) {
    }
}
