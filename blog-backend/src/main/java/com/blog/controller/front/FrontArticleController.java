package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.ArticleResponse;
import com.blog.dto.CommentResponse;
import com.blog.dto.FrontCommentRequest;
import com.blog.dto.PageResponse;
import com.blog.service.ArticleService;
import com.blog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/front/articles")
public class FrontArticleController {

    private final ArticleService articleService;
    private final CommentService commentService;

    public FrontArticleController(ArticleService articleService, CommentService commentService) {
        this.articleService = articleService;
        this.commentService = commentService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<ArticleResponse>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long tagId,
        @RequestParam(required = false, defaultValue = "latest") String sort,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(articleService.publishedPage(keyword, categoryId, tagId, sort, page, size));
    }

    @GetMapping
    public ApiResponse<List<ArticleResponse>> list() {
        return ApiResponse.ok(articleService.publishedList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(articleService.publishedDetail(id));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<ArticleResponse> like(@PathVariable Long id) {
        return ApiResponse.ok(articleService.like(id));
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<ArticleResponse> unlike(@PathVariable Long id) {
        return ApiResponse.ok(articleService.unlike(id));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<CommentResponse>> comments(@PathVariable Long id) {
        return ApiResponse.ok(commentService.approvedByArticle(id));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<Void> submitComment(
        @PathVariable Long id,
        @RequestBody @Valid FrontCommentRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        commentService.submit(id, request, authorization);
        return ApiResponse.ok(null);
    }
}
