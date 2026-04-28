package com.blog.service;

import com.blog.dto.ArticleResponse;
import com.blog.dto.FavoriteMigrateRequest;
import com.blog.entity.Article;
import com.blog.entity.BlogUser;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class UserLikeService {

    private final UserLikeMapper userLikeMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final FrontUserAuthService frontUserAuthService;

    public UserLikeService(
        UserLikeMapper userLikeMapper,
        ArticleMapper articleMapper,
        ArticleService articleService,
        FrontUserAuthService frontUserAuthService
    ) {
        this.userLikeMapper = userLikeMapper;
        this.articleMapper = articleMapper;
        this.articleService = articleService;
        this.frontUserAuthService = frontUserAuthService;
    }

    public List<Long> ids(String authorization) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        return userLikeMapper.selectArticleIdsByUserId(user.getId());
    }

    @Transactional
    public ArticleResponse add(String authorization, Long articleId) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        ensurePublishedArticle(articleId);
        int inserted = userLikeMapper.insert(user.getId(), articleId);
        if (inserted > 0) {
            articleMapper.increaseLikeCount(articleId);
        }
        return articleService.toResponse(articleMapper.selectPublishedById(articleId));
    }

    @Transactional
    public ArticleResponse remove(String authorization, Long articleId) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        ensurePublishedArticle(articleId);
        int deleted = userLikeMapper.delete(user.getId(), articleId);
        if (deleted > 0) {
            articleMapper.decreaseLikeCount(articleId);
        }
        return articleService.toResponse(articleMapper.selectPublishedById(articleId));
    }

    @Transactional
    public List<Long> migrate(String authorization, FavoriteMigrateRequest request) {
        BlogUser user = frontUserAuthService.requireUser(authorization);
        new LinkedHashSet<>(request.getArticleIds()).stream()
            .filter(id -> id != null && id > 0)
            .filter(this::isPublishedArticle)
            .forEach(articleId -> {
                int inserted = userLikeMapper.insert(user.getId(), articleId);
                if (inserted > 0) {
                    articleMapper.increaseLikeCount(articleId);
                }
            });
        return userLikeMapper.selectArticleIdsByUserId(user.getId());
    }

    private void ensurePublishedArticle(Long articleId) {
        if (!isPublishedArticle(articleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
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
