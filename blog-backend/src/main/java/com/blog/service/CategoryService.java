package com.blog.service;

import com.blog.dto.CategoryRequest;
import com.blog.dto.CategoryResponse;
import com.blog.entity.Category;
import com.blog.mapper.CategoryMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> list() {
        return categoryMapper.selectList().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        fillCategory(category, request);
        categoryMapper.insert(category);
        return detail(category.getId());
    }

    public CategoryResponse detail(Long id) {
        return toResponse(findExisting(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        findExisting(id);
        Category category = new Category();
        category.setId(id);
        fillCategory(category, request);
        int updated = categoryMapper.update(category);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        int deleted = categoryMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
    }

    private Category findExisting(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        return category;
    }

    private void fillCategory(Category category, CategoryRequest request) {
        String name = request.getName().trim();
        category.setName(name);
        category.setSlug(normalizeSlug(request.getSlug(), name));
        category.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private String normalizeSlug(String slug, String fallback) {
        String source = slug == null || slug.isBlank() ? fallback : slug;
        String normalized = source.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
            .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "category" : normalized;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getSortOrder(),
            category.getArticleCount(),
            category.getUpdatedAt()
        );
    }
}
