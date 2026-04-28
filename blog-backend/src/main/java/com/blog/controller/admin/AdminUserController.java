package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.AdminUserPasswordRequest;
import com.blog.dto.AdminUserResponse;
import com.blog.dto.AdminUserStatusRequest;
import com.blog.dto.PageResponse;
import com.blog.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<AdminUserResponse>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(adminUserService.page(keyword, status, page, size));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
        @PathVariable Long id,
        @RequestBody @Valid AdminUserStatusRequest request,
        HttpServletRequest servletRequest
    ) {
        adminUserService.updateStatus(id, request, servletRequest);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(
        @PathVariable Long id,
        @RequestBody @Valid AdminUserPasswordRequest request,
        HttpServletRequest servletRequest
    ) {
        adminUserService.resetPassword(id, request, servletRequest);
        return ApiResponse.ok(null);
    }
}
