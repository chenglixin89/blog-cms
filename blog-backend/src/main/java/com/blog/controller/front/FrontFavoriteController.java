package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.ArticleResponse;
import com.blog.dto.FavoriteMigrateRequest;
import com.blog.service.UserFavoriteService;
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
@RequestMapping("/api/front/favorites")
public class FrontFavoriteController {

    private final UserFavoriteService userFavoriteService;

    public FrontFavoriteController(UserFavoriteService userFavoriteService) {
        this.userFavoriteService = userFavoriteService;
    }

    @GetMapping
    public ApiResponse<List<ArticleResponse>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(userFavoriteService.list(authorization));
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> ids(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(userFavoriteService.ids(authorization));
    }

    @PostMapping("/{articleId}")
    public ApiResponse<Void> add(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long articleId
    ) {
        userFavoriteService.add(authorization, articleId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{articleId}")
    public ApiResponse<Void> remove(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long articleId
    ) {
        userFavoriteService.remove(authorization, articleId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/migrate")
    public ApiResponse<List<Long>> migrate(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody @Valid FavoriteMigrateRequest request
    ) {
        return ApiResponse.ok(userFavoriteService.migrate(authorization, request));
    }
}
