package com.blog.service;

import com.blog.dto.TagRequest;
import com.blog.dto.TagResponse;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TagService {

    private static final String DEFAULT_COLOR = "#0a84ff";

    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public List<TagResponse> list() {
        return tagMapper.selectList().stream()
            .map(this::toResponse)
            .toList();
    }

    public TagResponse detail(Long id) {
        return toResponse(findExisting(id));
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        Tag tag = new Tag();
        fillTag(tag, request);
        tagMapper.insert(tag);
        return detail(tag.getId());
    }

    @Transactional
    public TagResponse update(Long id, TagRequest request) {
        findExisting(id);
        Tag tag = new Tag();
        tag.setId(id);
        fillTag(tag, request);
        int updated = tagMapper.update(tag);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        int deleted = tagMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
    }

    private Tag findExisting(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
        return tag;
    }

    private void fillTag(Tag tag, TagRequest request) {
        tag.setName(request.getName().trim());
        tag.setColor(normalizeColor(request.getColor()));
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return DEFAULT_COLOR;
        }
        return color.trim();
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            tag.getArticleCount(),
            tag.getUpdatedAt()
        );
    }
}
