package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.CommentStatusRequest;
import com.blog.dto.FriendLinkRequest;
import com.blog.dto.FriendLinkResponse;
import com.blog.service.AuditLogService;
import com.blog.service.FriendLinkService;
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
@RequestMapping("/api/admin/links")
public class FriendLinkController {

    private final FriendLinkService linkService;
    private final AuditLogService auditLogService;

    public FriendLinkController(FriendLinkService linkService, AuditLogService auditLogService) {
        this.linkService = linkService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<FriendLinkResponse>> list() {
        return ApiResponse.ok(linkService.list());
    }

    @PostMapping
    public ApiResponse<FriendLinkResponse> create(@RequestBody @Valid FriendLinkRequest request, HttpServletRequest httpRequest) {
        FriendLinkResponse response = linkService.create(request);
        auditLogService.record(httpRequest, "友链", "新增", "FRIEND_LINK", response.id(), "新增友链：" + response.name());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<FriendLinkResponse> update(@PathVariable Long id, @RequestBody @Valid FriendLinkRequest request, HttpServletRequest httpRequest) {
        FriendLinkResponse response = linkService.update(id, request);
        auditLogService.record(httpRequest, "友链", "编辑", "FRIEND_LINK", id, "编辑友链：" + response.name());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody CommentStatusRequest request, HttpServletRequest httpRequest) {
        linkService.updateStatus(id, request.getStatus());
        auditLogService.record(httpRequest, "友链", "更新状态", "FRIEND_LINK", id, "更新友链状态为：" + statusText(request.getStatus()));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        linkService.delete(id);
        auditLogService.record(httpRequest, "友链", "删除", "FRIEND_LINK", id, "删除友链 ID：" + id);
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
