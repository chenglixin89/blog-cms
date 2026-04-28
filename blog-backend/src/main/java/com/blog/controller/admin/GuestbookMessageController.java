package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.CommentStatusRequest;
import com.blog.dto.GuestbookMessageResponse;
import com.blog.dto.PageResponse;
import com.blog.dto.GuestbookReplyRequest;
import com.blog.service.AuditLogService;
import com.blog.service.GuestbookMessageService;
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
@RequestMapping("/api/admin/messages")
public class GuestbookMessageController {

    private final GuestbookMessageService messageService;
    private final AuditLogService auditLogService;

    public GuestbookMessageController(GuestbookMessageService messageService, AuditLogService auditLogService) {
        this.messageService = messageService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<GuestbookMessageResponse>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(messageService.page(keyword, status, page, size));
    }

    @GetMapping
    public ApiResponse<List<GuestbookMessageResponse>> list() {
        return ApiResponse.ok(messageService.list());
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody CommentStatusRequest request, HttpServletRequest httpRequest) {
        messageService.updateStatus(id, request.getStatus());
        auditLogService.record(httpRequest, "留言", "审核", "MESSAGE", id, "更新留言状态为：" + statusText(request.getStatus()));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<GuestbookMessageResponse> reply(@PathVariable Long id, @RequestBody @Valid GuestbookReplyRequest request, HttpServletRequest httpRequest) {
        GuestbookMessageResponse response = messageService.reply(id, request);
        auditLogService.record(httpRequest, "留言", "回复", "MESSAGE", id, "回复留言，回复 ID：" + response.id());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        messageService.delete(id);
        auditLogService.record(httpRequest, "留言", "删除", "MESSAGE", id, "删除留言 ID：" + id);
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
