package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.ArticleResponse;
import com.blog.dto.FavoriteMigrateRequest;
import com.blog.service.UserLikeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/front/likes")
public class FrontLikeController {

    private final UserLikeService userLikeService;

    public FrontLikeController(UserLikeService userLikeService) {
        this.userLikeService = userLikeService;
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> ids(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(userLikeService.ids(authorization));
    }

    @PostMapping("/{articleId}")
    public ApiResponse<ArticleResponse> add(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long articleId
    ) {
        return ApiResponse.ok(userLikeService.add(authorization, articleId));
    }

    @DeleteMapping("/{articleId}")
    public ApiResponse<ArticleResponse> remove(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long articleId
    ) {
        return ApiResponse.ok(userLikeService.remove(authorization, articleId));
    }

    @PostMapping("/migrate")
    public ApiResponse<List<Long>> migrate(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody @Valid FavoriteMigrateRequest request
    ) {
        return ApiResponse.ok(userLikeService.migrate(authorization, request));
    }
}
