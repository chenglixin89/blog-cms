package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.ArticleResponse;
import com.blog.dto.ArticleUpsertRequest;
import com.blog.dto.PageResponse;
import com.blog.service.ArticleService;
import com.blog.service.AuditLogService;
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
@RequestMapping("/api/admin/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final AuditLogService auditLogService;

    public ArticleController(ArticleService articleService, AuditLogService auditLogService) {
        this.articleService = articleService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<ArticleResponse>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(articleService.adminPage(keyword, status, page, size));
    }

    @GetMapping
    public ApiResponse<List<ArticleResponse>> list() {
        return ApiResponse.ok(articleService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(articleService.detail(id));
    }

    @PostMapping
    public ApiResponse<ArticleResponse> create(@RequestBody @Valid ArticleUpsertRequest request, HttpServletRequest httpRequest) {
        ArticleResponse response = articleService.create(request);
        auditLogService.record(httpRequest, "文章", "新增", "ARTICLE", response.id(), "新增文章：" + response.title());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleResponse> update(@PathVariable Long id, @RequestBody @Valid ArticleUpsertRequest request, HttpServletRequest httpRequest) {
        ArticleResponse response = articleService.update(id, request);
        auditLogService.record(httpRequest, "文章", "编辑", "ARTICLE", id, "编辑文章：" + response.title());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        articleService.delete(id);
        auditLogService.record(httpRequest, "文章", "删除", "ARTICLE", id, "删除文章 ID：" + id);
        return ApiResponse.ok(null);
    }
}
