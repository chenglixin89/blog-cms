package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.CommentStatusRequest;
import com.blog.dto.CommentResponse;
import com.blog.dto.PageResponse;
import com.blog.dto.AdminCommentReplyRequest;
import com.blog.service.AuditLogService;
import com.blog.service.CommentService;
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
@RequestMapping("/api/admin/comments")
public class CommentController {

    private final CommentService commentService;
    private final AuditLogService auditLogService;

    public CommentController(CommentService commentService, AuditLogService auditLogService) {
        this.commentService = commentService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<CommentResponse>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(commentService.page(keyword, status, page, size));
    }

    @GetMapping
    public ApiResponse<List<CommentResponse>> list() {
        return ApiResponse.ok(commentService.list());
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody CommentStatusRequest request, HttpServletRequest httpRequest) {
        commentService.updateStatus(id, request.getStatus());
        auditLogService.record(httpRequest, "评论", "审核", "COMMENT", id, "更新评论状态为：" + statusText(request.getStatus()));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<CommentResponse> reply(@PathVariable Long id, @RequestBody @Valid AdminCommentReplyRequest request, HttpServletRequest httpRequest) {
        CommentResponse response = commentService.reply(id, request);
        auditLogService.record(httpRequest, "评论", "回复", "COMMENT", id, "回复评论，回复 ID：" + response.id());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        commentService.delete(id);
        auditLogService.record(httpRequest, "评论", "删除", "COMMENT", id, "删除评论 ID：" + id);
        return ApiResponse.ok(null);
    }

    private String statusText(Integer status) {
        if (status != null && status == 1) {
            return "已通过";
        }
        if (status != null && status == 2) {
            return "已拒绝";
        }
        return "待审核";
    }
}
