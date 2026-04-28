package com.blog.service;

import com.blog.dto.ArticleResponse;
import com.blog.dto.FavoriteMigrateRequest;
import com.blog.entity.Article;
import com.blog.entity.BlogUser;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserFavoriteMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class UserFavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final FrontUserAuthService frontUserAuthService;

    public UserFavoriteService(
        UserFavoriteMapper userFavoriteMapper,
        ArticleMapper articleMapper,
        ArticleService articleService,
        FrontUserAuthService frontUserAuthService
    ) {
        this.userFavoriteMapper = userFavoriteMapper;
        this.articleMapper = articleMapper;
        this.articleService = articleService;
        this.frontUserAuthService = frontUserAuthService;
    }

    public List<ArticleResponse> list(String authorization) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        return userFavoriteMapper.selectPublishedFavoritesByUserId(user.getId()).stream()
            .map(articleService::toResponse)
            .toList();
    }

    public List<Long> ids(String authorization) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        return userFavoriteMapper.selectArticleIdsByUserId(user.getId());
    }

    @Transactional
    public void add(String authorization, Long articleId) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        ensurePublishedArticle(articleId);
        userFavoriteMapper.insert(user.getId(), articleId);
    }

    @Transactional
    public void remove(String authorization, Long articleId) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        userFavoriteMapper.delete(user.getId(), articleId);
    }

    @Transactional
    public List<Long> migrate(String authorization, FavoriteMigrateRequest request) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        new LinkedHashSet<>(request.getArticleIds()).stream()
            .filter(id -> id != null && id > 0)
            .filter(this::isPublishedArticle)
            .forEach(articleId -> userFavoriteMapper.insert(user.getId(), articleId));
        return userFavoriteMapper.selectArticleIdsByUserId(user.getId());
    }

    private void ensurePublishedArticle(Long articleId) {
        if (!isPublishedArticle(articleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在或未发布");
        }
    }

    private boolean isPublishedArticle(Long articleId) {
        if (articleId == null || articleId <= 0) {
            return false;
        }
        Article article = articleMapper.selectPublishedById(articleId);
        return article != null;
    }
}
