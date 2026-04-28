package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.SiteSettingRequest;
import com.blog.dto.SiteSettingResponse;
import com.blog.service.AuditLogService;
import com.blog.service.SiteSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
public class SiteSettingController {

    private final SiteSettingService settingService;
    private final AuditLogService auditLogService;

    public SiteSettingController(SiteSettingService settingService, AuditLogService auditLogService) {
        this.settingService = settingService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<SiteSettingResponse> current() {
        return ApiResponse.ok(settingService.current());
    }

    @PutMapping
    public ApiResponse<SiteSettingResponse> update(@RequestBody @Valid SiteSettingRequest request, HttpServletRequest httpRequest) {
        SiteSettingResponse response = settingService.update(request);
        auditLogService.record(httpRequest, "站点", "更新设置", "SITE_SETTING", response.id(), "更新站点设置：" + response.siteName());
        return ApiResponse.ok(response);
    }
}
