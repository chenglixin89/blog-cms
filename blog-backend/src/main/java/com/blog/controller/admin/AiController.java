package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.AiCallLogDetailResponse;
import com.blog.dto.AiCallLogResponse;
import com.blog.dto.AiProviderRequest;
import com.blog.dto.AiProviderResponse;
import com.blog.dto.AiSkillRequest;
import com.blog.dto.AiSkillResponse;
import com.blog.dto.AiSkillRunRequest;
import com.blog.dto.AiSkillRunResponse;
import com.blog.dto.PageResponse;
import com.blog.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/provider")
    public ApiResponse<AiProviderResponse> provider() {
        return ApiResponse.ok(aiService.getProvider());
    }

    @PutMapping("/provider")
    public ApiResponse<AiProviderResponse> updateProvider(@RequestBody @Valid AiProviderRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(aiService.updateProvider(request, servletRequest));
    }

    @PostMapping("/provider/test")
    public ApiResponse<AiSkillRunResponse> testProvider(HttpServletRequest servletRequest) {
        return ApiResponse.ok(aiService.testProvider(servletRequest));
    }

    @GetMapping("/skills")
    public ApiResponse<List<AiSkillResponse>> skills() {
        return ApiResponse.ok(aiService.listSkills());
    }

    @PostMapping("/skills")
    public ApiResponse<AiSkillResponse> createSkill(@RequestBody @Valid AiSkillRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(aiService.createSkill(request, servletRequest));
    }

    @PutMapping("/skills/{id}")
    public ApiResponse<AiSkillResponse> updateSkill(@PathVariable Long id, @RequestBody @Valid AiSkillRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(aiService.updateSkill(id, request, servletRequest));
    }

    @DeleteMapping("/skills/{id}")
    public ApiResponse<Void> deleteSkill(@PathVariable Long id, HttpServletRequest servletRequest) {
        aiService.deleteSkill(id, servletRequest);
        return ApiResponse.ok(null);
    }

    @PostMapping("/skills/{code}/run")
    public ApiResponse<AiSkillRunResponse> runSkill(@PathVariable String code, @RequestBody AiSkillRunRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(aiService.runSkill(code, request, servletRequest));
    }

    @GetMapping("/logs/{id}")
    public ApiResponse<AiCallLogDetailResponse> logDetail(@PathVariable Long id) {
        return ApiResponse.ok(aiService.detailLog(id));
    }

    @GetMapping("/logs/page")
    public ApiResponse<PageResponse<AiCallLogResponse>> logs(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer success,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(aiService.pageLogs(keyword, success, page, size));
    }
}