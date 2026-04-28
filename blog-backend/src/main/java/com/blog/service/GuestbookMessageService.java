package com.blog.service;

import com.blog.dto.FrontGuestbookMessageRequest;
import com.blog.dto.FrontGuestbookMessageResponse;
import com.blog.dto.GuestbookMessageResponse;
import com.blog.dto.GuestbookReplyRequest;
import com.blog.dto.PageResponse;
import com.blog.entity.BlogUser;
import com.blog.entity.GuestbookMessage;
import com.blog.mapper.GuestbookMessageMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GuestbookMessageService {
private static final int SPAM_LIMIT = 3;

    private final GuestbookMessageMapper messageMapper;
    private final FrontUserAuthService frontUserAuthService;

    public GuestbookMessageService(GuestbookMessageMapper messageMapper, FrontUserAuthService frontUserAuthService) {
        this.messageMapper = messageMapper;
        this.frontUserAuthService = frontUserAuthService;
    }

    public PageResponse<GuestbookMessageResponse> page(String keyword, Integer status, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = normalizeKeyword(keyword);
        Integer normalizedStatus = normalizeAdminStatus(status);

        long total = messageMapper.countAdminPage(normalizedKeyword, normalizedStatus);
        List<GuestbookMessageResponse> records = messageMapper
            .selectAdminPage(normalizedKeyword, normalizedStatus, offset, normalizedSize)
            .stream()
            .map(this::toResponse)
            .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    public List<GuestbookMessageResponse> list() {
        return messageMapper.selectList().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FrontGuestbookMessageResponse> approvedList() {
        return messageMapper.selectApprovedList().stream()
            .map(this::toFrontResponse)
            .toList();
    }

    @Transactional
    public void submit(FrontGuestbookMessageRequest request, String authorization) {
        BlogUser user = frontUserAuthService.optionalUser(authorization);
        Long parentId = normalizeParentId(request.getParentId());
        validateParent(parentId);

        String nickname = user == null ? normalizeText(request.getNickname()) : normalizeText(user.getNickname());
        String email = user == null ? normalizeText(request.getEmail()) : normalizeText(user.getEmail());
        String content = normalizeText(request.getContent());

        if (nickname.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname is required");
        }
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }

        ensureNotSpamming(user, nickname, email);

        GuestbookMessage message = new GuestbookMessage();
        message.setParentId(parentId);
        message.setUserId(user == null ? null : user.getId());
        message.setNickname(nickname);
        message.setEmail(email);
        message.setContent(content);
        message.setStatus(0);
        messageMapper.insert(message);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        int normalized = normalizeStatus(status);
        int updated = messageMapper.updateStatus(id, normalized);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }
    }

    @Transactional
    public GuestbookMessageResponse reply(Long parentId, GuestbookReplyRequest request) {
        GuestbookMessage parent = messageMapper.selectById(parentId);
        if (parent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }
        if (parent.getStatus() == null || parent.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only reply to approved messages");
        }

        GuestbookMessage reply = new GuestbookMessage();
        reply.setParentId(parent.getId());
        reply.setUserId(null);
        reply.setNickname("Admin");
        reply.setEmail("");
        reply.setContent(request.getContent().trim());
        reply.setStatus(1);
        messageMapper.insert(reply);
        return toResponse(messageMapper.selectById(reply.getId()));
    }

    @Transactional
    public void delete(Long id) {
        int deleted = messageMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }
    }

    private void validateParent(Long parentId) {
        if (parentId == null) {
            return;
        }
        GuestbookMessage parent = messageMapper.selectById(parentId);
        if (parent == null || parent.getStatus() == null || parent.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply target does not exist or is not approved");
        }
    }

    private void ensureNotSpamming(BlogUser user, String nickname, String email) {
        int recentCount = user == null
            ? messageMapper.countRecentByGuest(nickname, email)
            : messageMapper.countRecentByUserId(user.getId());
        if (recentCount >= SPAM_LIMIT) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many messages, please try again later");
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? null : parentId;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        return keyword.trim();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer normalizeAdminStatus(Integer status) {
        if (status == null || status < 0 || status > 2) {
            return null;
        }
        return status;
    }

    private int normalizeStatus(Integer status) {
        if (status == null || status < 0 || status > 2) {
            return 0;
        }
        return status;
    }

    private GuestbookMessageResponse toResponse(GuestbookMessage message) {
        return new GuestbookMessageResponse(
            message.getId(),
            message.getParentId(),
            message.getNickname(),
            message.getEmail(),
            message.getContent(),
            message.getStatus(),
            message.getCreatedAt()
        );
    }

    private FrontGuestbookMessageResponse toFrontResponse(GuestbookMessage message) {
        return new FrontGuestbookMessageResponse(
            message.getId(),
            message.getParentId(),
            message.getNickname(),
            message.getContent(),
            message.getStatus(),
            message.getCreatedAt(),
            message.getUserId() != null
        );
    }
}