package com.blog.service;

import com.blog.dto.MediaCategoryRequest;
import com.blog.dto.MediaCategoryResponse;
import com.blog.entity.MediaCategory;
import com.blog.mapper.MediaCategoryMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class MediaCategoryService {

    private final MediaCategoryMapper mediaCategoryMapper;

    public MediaCategoryService(MediaCategoryMapper mediaCategoryMapper) {
        this.mediaCategoryMapper = mediaCategoryMapper;
    }

    public List<MediaCategoryResponse> list() {
        return mediaCategoryMapper.selectList().stream().map(this::toResponse).toList();
    }

    @Transactional
    public MediaCategoryResponse create(MediaCategoryRequest request) {
        MediaCategory category = new MediaCategory();
        fillForCreate(category, request);
        category.setIsSystem(0);
        try {
            mediaCategoryMapper.insert(category);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media category code already exists");
        }
        return detail(category.getId());
    }

    public MediaCategoryResponse detail(Long id) {
        return toResponse(findExisting(id));
    }

    @Transactional
    public MediaCategoryResponse update(Long id, MediaCategoryRequest request) {
        MediaCategory existing = findExisting(id);
        MediaCategory category = new MediaCategory();
        category.setId(id);
        category.setName(request.getName().trim());
        category.setCode(existing.getCode());
        category.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        category.setSortOrder(request.getSortOrder() == null ? existing.getSortOrder() : request.getSortOrder());
        int updated = mediaCategoryMapper.update(category);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media category not found");
        }
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        MediaCategory category = findExisting(id);
        if (category.getIsSystem() != null && category.getIsSystem() == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System media category cannot be deleted");
        }
        if (mediaCategoryMapper.countAssetsByCode(category.getCode()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media category still has images");
        }
        int deleted = mediaCategoryMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media category not found");
        }
    }

    public String normalizeExistingCode(String value) {
        String code = value == null || value.isBlank() ? "general" : value.trim();
        if (mediaCategoryMapper.selectByCode(code) == null) {
            return "general";
        }
        return code;
    }

    private MediaCategory findExisting(Long id) {
        MediaCategory category = mediaCategoryMapper.selectById(id);
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media category not found");
        }
        return category;
    }

    private void fillForCreate(MediaCategory category, MediaCategoryRequest request) {
        String name = request.getName().trim();
        category.setName(name);
        category.setCode(normalizeCode(request.getCode(), name));
        category.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private String normalizeCode(String code, String fallback) {
        String source = code == null || code.isBlank() ? fallback : code;
        String normalized = source.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5_-]+", "-")
            .replaceAll("(^-+|-+$)", "");
        if (normalized.isBlank()) {
            normalized = "media";
        }
        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    private MediaCategoryResponse toResponse(MediaCategory category) {
        return new MediaCategoryResponse(
            category.getId(),
            category.getName(),
            category.getCode(),
            category.getDescription(),
            category.getSortOrder(),
            category.getAssetCount() == null ? 0 : category.getAssetCount(),
            category.getIsSystem() == null ? 0 : category.getIsSystem(),
            category.getUpdatedAt()
        );
    }
}