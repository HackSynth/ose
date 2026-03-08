package com.ose.analytics;

import com.ose.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ApiResponse<AnalyticsDtos.AnalyticsSummary> summary() {
        return ApiResponse.success(analyticsService.summary());
    }

    @GetMapping("/trends")
    public ApiResponse<AnalyticsDtos.AnalyticsTrends> trends() {
        return ApiResponse.success(analyticsService.trends());
    }
}
