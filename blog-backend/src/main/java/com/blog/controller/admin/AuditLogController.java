package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.AuditLogResponse;
import com.blog.dto.PageResponse;
import com.blog.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<AuditLogResponse>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String module,
        @RequestParam(required = false) String action,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(auditLogService.page(keyword, module, action, page, size));
    }
}
