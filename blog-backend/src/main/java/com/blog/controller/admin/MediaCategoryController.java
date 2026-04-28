package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.MediaCategoryRequest;
import com.blog.dto.MediaCategoryResponse;
import com.blog.service.AuditLogService;
import com.blog.service.MediaCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/media/categories")
public class MediaCategoryController {

    private final MediaCategoryService mediaCategoryService;
    private final AuditLogService auditLogService;

    public MediaCategoryController(MediaCategoryService mediaCategoryService, AuditLogService auditLogService) {
        this.mediaCategoryService = mediaCategoryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<MediaCategoryResponse>> list() {
        return ApiResponse.ok(mediaCategoryService.list());
    }

    @PostMapping
    public ApiResponse<MediaCategoryResponse> create(@RequestBody @Valid MediaCategoryRequest request, HttpServletRequest httpRequest) {
        MediaCategoryResponse response = mediaCategoryService.create(request);
        auditLogService.record(httpRequest, "Media", "Create category", "MEDIA_CATEGORY", response.id(), "Create media category: " + response.name());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<MediaCategoryResponse> update(@PathVariable Long id, @RequestBody @Valid MediaCategoryRequest request, HttpServletRequest httpRequest) {
        MediaCategoryResponse response = mediaCategoryService.update(id, request);
        auditLogService.record(httpRequest, "Media", "Update category", "MEDIA_CATEGORY", id, "Update media category: " + response.name());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        mediaCategoryService.delete(id);
        auditLogService.record(httpRequest, "Media", "Delete category", "MEDIA_CATEGORY", id, "Delete media category: " + id);
        return ApiResponse.ok(null);
    }
}