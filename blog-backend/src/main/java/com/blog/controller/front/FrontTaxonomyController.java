package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.CategoryResponse;
import com.blog.dto.TagResponse;
import com.blog.service.CategoryService;
import com.blog.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/front")
public class FrontTaxonomyController {

    private final CategoryService categoryService;
    private final TagService tagService;

    public FrontTaxonomyController(CategoryService categoryService, TagService tagService) {
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(categoryService.list());
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagResponse>> tags() {
        return ApiResponse.ok(tagService.list());
    }
}
