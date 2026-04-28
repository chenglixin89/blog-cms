package com.blog.service;

import com.blog.dto.AdminCommentReplyRequest;
import com.blog.dto.CommentResponse;
import com.blog.dto.FrontCommentRequest;
import com.blog.dto.PageResponse;
import com.blog.entity.BlogUser;
import com.blog.entity.Comment;
import com.blog.mapper.CommentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final FrontUserAuthService frontUserAuthService;

    public CommentService(CommentMapper commentMapper, FrontUserAuthService frontUserAuthService) {
        this.commentMapper = commentMapper;
        this.frontUserAuthService = frontUserAuthService;
    }

    public PageResponse<CommentResponse> page(String keyword, Integer status, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = normalizeKeyword(keyword);
        Integer normalizedStatus = normalizeAdminStatus(status);

        long total = commentMapper.countAdminPage(normalizedKeyword, normalizedStatus);
        List<CommentResponse> records = commentMapper
            .selectAdminPage(normalizedKeyword, normalizedStatus, offset, normalizedSize)
            .stream()
            .map(this::toResponse)
            .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    public List<CommentResponse> list() {
        return commentMapper.selectList().stream().map(this::toResponse).toList();
    }

    public List<CommentResponse> approvedByArticle(Long articleId) {
        return commentMapper.selectApprovedByArticleId(articleId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void submit(Long articleId, FrontCommentRequest request, String authorization) {
        BlogUser user = frontUserAuthService.optionalUser(authorization);
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setParentId(request.getParentId());
        comment.setUserId(user == null ? null : user.getId());
        comment.setNickname(user == null ? request.getNickname().trim() : user.getNickname());
        comment.setEmail(user == null ? normalizeText(request.getEmail()) : normalizeText(user.getEmail()));
        comment.setContent(request.getContent().trim());
        comment.setStatus(0);
        commentMapper.insert(comment);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        int normalized = normalizeStatus(status);
        int updated = commentMapper.updateStatus(id, normalized);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
    }

    @Transactional
    public CommentResponse reply(Long parentId, AdminCommentReplyRequest request) {
        Comment parent = commentMapper.selectById(parentId);
        if (parent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        Comment reply = new Comment();
        reply.setArticleId(parent.getArticleId());
        reply.setParentId(parent.getId());
        reply.setUserId(null);
        reply.setNickname("Admin");
        reply.setEmail("");
        reply.setContent(request.getContent().trim());
        reply.setStatus(1);
        commentMapper.insert(reply);
        return toResponse(commentMapper.selectById(reply.getId()));
    }

    @Transactional
    public void delete(Long id) {
        int deleted = commentMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
    }

    private int normalizePage(Integer page) { return page == null || page < 1 ? 1 : page; }
    private int normalizeSize(Integer size) { return Math.min(size == null || size < 1 ? 10 : size, 50); }
    private String normalizeKeyword(String keyword) { return keyword == null ? "" : keyword.trim(); }
    private String normalizeText(String value) { return value == null ? "" : value.trim(); }
    private Integer normalizeAdminStatus(Integer status) { return status == null || status < 0 || status > 2 ? null : status; }
    private int normalizeStatus(Integer status) { return status == null || status < 0 || status > 2 ? 0 : status; }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getArticleId(),
            comment.getParentId(),
            comment.getArticleTitle(),
            comment.getNickname(),
            comment.getEmail(),
            comment.getContent(),
            comment.getStatus(),
            comment.getCreatedAt()
        );
    }
}
