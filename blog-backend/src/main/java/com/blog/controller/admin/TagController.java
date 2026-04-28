package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.TagRequest;
import com.blog.dto.TagResponse;
import com.blog.service.AuditLogService;
import com.blog.service.TagService;
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
@RequestMapping("/api/admin/tags")
public class TagController {

    private final TagService tagService;
    private final AuditLogService auditLogService;

    public TagController(TagService tagService, AuditLogService auditLogService) {
        this.tagService = tagService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> list() {
        return ApiResponse.ok(tagService.list());
    }

    @PostMapping
    public ApiResponse<TagResponse> create(@RequestBody @Valid TagRequest request, HttpServletRequest httpRequest) {
        TagResponse response = tagService.create(request);
        auditLogService.record(httpRequest, "标签", "新增", "TAG", response.id(), "新增标签：" + response.name());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<TagResponse> update(@PathVariable Long id, @RequestBody @Valid TagRequest request, HttpServletRequest httpRequest) {
        TagResponse response = tagService.update(id, request);
        auditLogService.record(httpRequest, "标签", "编辑", "TAG", id, "编辑标签：" + response.name());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        tagService.delete(id);
        auditLogService.record(httpRequest, "标签", "删除", "TAG", id, "删除标签 ID：" + id);
        return ApiResponse.ok(null);
    }
}
