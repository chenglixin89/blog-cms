package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.DashboardStatsResponse;
import com.blog.dto.DashboardTrendPointResponse;
import com.blog.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> stats() {
        return ApiResponse.ok(dashboardService.stats());
    }

    @GetMapping("/publish-trend")
    public ApiResponse<List<DashboardTrendPointResponse>> publishTrend() {
        return ApiResponse.ok(dashboardService.publishTrend());
    }
}
