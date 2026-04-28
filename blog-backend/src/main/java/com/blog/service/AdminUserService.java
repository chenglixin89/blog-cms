package com.blog.service;

import com.blog.dto.AdminUserPasswordRequest;
import com.blog.dto.AdminUserResponse;
import com.blog.dto.AdminUserStatusRequest;
import com.blog.dto.PageResponse;
import com.blog.entity.BlogUser;
import com.blog.mapper.BlogUserMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserLikeMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminUserService {

    private final BlogUserMapper blogUserMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserLikeMapper userLikeMapper;
    private final CommentMapper commentMapper;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public AdminUserService(
        BlogUserMapper blogUserMapper,
        UserFavoriteMapper userFavoriteMapper,
        UserLikeMapper userLikeMapper,
        CommentMapper commentMapper,
        AuditLogService auditLogService
    ) {
        this.blogUserMapper = blogUserMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userLikeMapper = userLikeMapper;
        this.commentMapper = commentMapper;
        this.auditLogService = auditLogService;
    }

    public PageResponse<AdminUserResponse> page(String keyword, Integer status, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = normalizeText(keyword);
        Integer normalizedStatus = normalizeStatusFilter(status);
        long total = blogUserMapper.countAdminPage(normalizedKeyword, normalizedStatus);
        List<AdminUserResponse> records = blogUserMapper.selectAdminPage(normalizedKeyword, normalizedStatus, offset, normalizedSize)
            .stream()
            .map(this::toResponse)
            .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    @Transactional
    public void updateStatus(Long id, AdminUserStatusRequest request, HttpServletRequest servletRequest) {
        BlogUser user = findUser(id);
        int status = request.getStatus() != null && request.getStatus() == 1 ? 1 : 0;
        blogUserMapper.updateStatus(id, status);
        auditLogService.record(servletRequest, "User", "Update status", "USER", id, "Update user status: " + user.getUsername() + " -> " + status);
    }

    @Transactional
    public void resetPassword(Long id, AdminUserPasswordRequest request, HttpServletRequest servletRequest) {
        BlogUser user = findUser(id);
        blogUserMapper.updatePassword(id, bCryptPasswordEncoder.encode(request.getPassword()));
        auditLogService.record(servletRequest, "User", "Reset password", "USER", id, "Reset password for user: " + user.getUsername());
    }

    private BlogUser findUser(Long id) {
        BlogUser user = blogUserMapper.selectById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private AdminUserResponse toResponse(BlogUser user) {
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getEmail() == null ? "" : user.getEmail(),
            user.getAvatar() == null ? "" : user.getAvatar(),
            user.getBio() == null ? "" : user.getBio(),
            user.getRole(),
            user.getStatus(),
            userFavoriteMapper.countByUserId(user.getId()),
            userLikeMapper.countByUserId(user.getId()),
            commentMapper.countByUserId(user.getId()),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return Math.min(size == null || size < 1 ? 10 : size, 100);
    }

    private Integer normalizeStatusFilter(Integer status) {
        if (status == null || status < 0 || status > 1) {
            return null;
        }
        return status;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
