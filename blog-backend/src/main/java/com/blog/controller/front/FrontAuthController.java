package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.FrontUserPasswordUpdateRequest;
import com.blog.dto.FrontUserProfileResponse;
import com.blog.dto.FrontUserProfileUpdateRequest;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.RegisterRequest;
import com.blog.dto.UserCenterResponse;
import com.blog.service.FrontUserAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front/auth")
public class FrontAuthController {

    private final FrontUserAuthService frontUserAuthService;

    public FrontAuthController(FrontUserAuthService frontUserAuthService) {
        this.frontUserAuthService = frontUserAuthService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.ok(frontUserAuthService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(frontUserAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<FrontUserProfileResponse> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(frontUserAuthService.me(authorization));
    }

    @GetMapping("/center")
    public ApiResponse<UserCenterResponse> center(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(frontUserAuthService.center(authorization));
    }

    @PutMapping("/profile")
    public ApiResponse<FrontUserProfileResponse> updateProfile(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody @Valid FrontUserProfileUpdateRequest request
    ) {
        return ApiResponse.ok(frontUserAuthService.updateProfile(authorization, request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody @Valid FrontUserPasswordUpdateRequest request
    ) {
        frontUserAuthService.updatePassword(authorization, request);
        return ApiResponse.ok(null);
    }
}
