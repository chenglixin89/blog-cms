package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.CategoryRequest;
import com.blog.dto.CategoryResponse;
import com.blog.service.AuditLogService;
import com.blog.service.CategoryService;
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
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuditLogService auditLogService;

    public CategoryController(CategoryService categoryService, AuditLogService auditLogService) {
        this.categoryService = categoryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(categoryService.list());
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@RequestBody @Valid CategoryRequest request, HttpServletRequest httpRequest) {
        CategoryResponse response = categoryService.create(request);
        auditLogService.record(httpRequest, "分类", "新增", "CATEGORY", response.id(), "新增分类：" + response.name());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request, HttpServletRequest httpRequest) {
        CategoryResponse response = categoryService.update(id, request);
        auditLogService.record(httpRequest, "分类", "编辑", "CATEGORY", id, "编辑分类：" + response.name());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        categoryService.delete(id);
        auditLogService.record(httpRequest, "分类", "删除", "CATEGORY", id, "删除分类 ID：" + id);
        return ApiResponse.ok(null);
    }
}
