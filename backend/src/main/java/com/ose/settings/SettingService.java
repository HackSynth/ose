package com.ose.settings;

import com.ose.model.SystemSetting;
import com.ose.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SettingService {

    private final SystemSettingRepository settingRepository;

    public SettingDtos.SettingResponse getSetting() {
        return toDto(getOrCreateDefault());
    }

    @Transactional
    public SettingDtos.SettingResponse updateSetting(SettingDtos.UpdateSettingRequest request) {
        SystemSetting setting = getOrCreateDefault();
        setting.setExamDate(request.examDate());
        setting.setPassingScore(request.passingScore());
        setting.setWeeklyStudyHours(request.weeklyStudyHours());
        setting.setLearningPreference(request.learningPreference());
        setting.setReviewIntervals(request.reviewIntervals().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("1,3,7,14"));
        setting.setDailySessionMinutes(request.dailySessionMinutes());
        return toDto(settingRepository.save(setting));
    }

    public SystemSetting getOrCreateDefault() {
        return settingRepository.findAll().stream().findFirst().orElseGet(() -> settingRepository.save(SystemSetting.builder()
                .examDate(LocalDate.of(2026, 5, 23))
                .passingScore(45)
                .weeklyStudyHours(12)
                .learningPreference("工作日碎片化学习 + 周末整块模拟")
                .reviewIntervals("1,3,7,14")
                .dailySessionMinutes(90)
                .build()));
    }

    public List<Integer> parseReviewIntervals(SystemSetting setting) {
        return Arrays.stream(setting.getReviewIntervals().split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(Integer::parseInt)
                .toList();
    }

    private SettingDtos.SettingResponse toDto(SystemSetting setting) {
        long daysUntilExam = ChronoUnit.DAYS.between(LocalDate.now(), setting.getExamDate());
        return new SettingDtos.SettingResponse(
                setting.getId(),
                setting.getExamDate(),
                daysUntilExam,
                setting.getPassingScore(),
                setting.getWeeklyStudyHours(),
                setting.getLearningPreference(),
                parseReviewIntervals(setting),
                setting.getDailySessionMinutes()
        );
    }
}
