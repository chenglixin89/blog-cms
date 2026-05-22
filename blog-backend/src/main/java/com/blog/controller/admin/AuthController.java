package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.config.AdminAuthInterceptor;
import com.blog.dto.AdminPasswordChangeRequest;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.service.AuditLogService;
import com.blog.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request);
        auditLogService.record(httpRequest, "System", "Login", "ADMIN", null, "Admin login: " + request.getUsername());
        return ApiResponse.ok(response);
    }

    @PutMapping("/auth/password")
    public ApiResponse<Void> changePassword(
        @RequestBody @Valid AdminPasswordChangeRequest request,
        HttpServletRequest httpRequest
    ) {
        // The interceptor has already validated the JWT and stashed the username on the request.
        String username = (String) httpRequest.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME);
        authService.changePassword(username, request);
        auditLogService.record(httpRequest, "System", "Change password", "ADMIN", null, "Admin changed own password");
        return ApiResponse.ok(null);
    }
}
