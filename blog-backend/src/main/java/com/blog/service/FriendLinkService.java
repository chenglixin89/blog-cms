package com.blog.service;

import com.blog.dto.FriendLinkRequest;
import com.blog.dto.FriendLinkResponse;
import com.blog.entity.FriendLink;
import com.blog.mapper.FriendLinkMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FriendLinkService {

    private final FriendLinkMapper linkMapper;

    public FriendLinkService(FriendLinkMapper linkMapper) {
        this.linkMapper = linkMapper;
    }

    public List<FriendLinkResponse> list() {
        return linkMapper.selectList().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FriendLinkResponse> approvedList() {
        return linkMapper.selectApprovedList().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public FriendLinkResponse create(FriendLinkRequest request) {
        FriendLink link = new FriendLink();
        fillLink(link, request, normalizeStatus(request.getStatus()));
        linkMapper.insert(link);
        return detail(link.getId());
    }

    @Transactional
    public void apply(FriendLinkRequest request) {
        FriendLink link = new FriendLink();
        fillLink(link, request, 0);
        linkMapper.insert(link);
    }

    public FriendLinkResponse detail(Long id) {
        FriendLink link = findExisting(id);
        return toResponse(link);
    }

    @Transactional
    public FriendLinkResponse update(Long id, FriendLinkRequest request) {
        findExisting(id);
        FriendLink link = new FriendLink();
        link.setId(id);
        fillLink(link, request, normalizeStatus(request.getStatus()));
        int updated = linkMapper.update(link);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend link not found");
        }
        return detail(id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        int updated = linkMapper.updateStatus(id, normalizeStatus(status));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend link not found");
        }
    }

    @Transactional
    public void delete(Long id) {
        int deleted = linkMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend link not found");
        }
    }

    private FriendLink findExisting(Long id) {
        FriendLink link = linkMapper.selectById(id);
        if (link == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend link not found");
        }
        return link;
    }

    private void fillLink(FriendLink link, FriendLinkRequest request, int status) {
        link.setName(request.getName().trim());
        link.setUrl(normalizeUrl(request.getUrl()));
        link.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        link.setLogo(request.getLogo() == null ? "" : request.getLogo().trim());
        link.setStatus(status);
        link.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private int normalizeStatus(Integer status) {
        if (status == null || status < 0 || status > 2) {
            return 0;
        }
        return status;
    }

    private FriendLinkResponse toResponse(FriendLink link) {
        return new FriendLinkResponse(
            link.getId(),
            link.getName(),
            link.getUrl(),
            link.getDescription(),
            link.getLogo(),
            link.getStatus(),
            link.getSortOrder(),
            link.getCreatedAt()
        );
    }
}
