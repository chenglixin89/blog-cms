package com.blog.service;

import com.blog.dto.ArticleResponse;
import com.blog.dto.ArticleTagResponse;
import com.blog.dto.ArticleUpsertRequest;
import com.blog.dto.PageResponse;
import com.blog.entity.Article;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }


    public PageResponse<ArticleResponse> adminPage(String keyword, Integer status, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = normalizeKeyword(keyword);
        Integer normalizedStatus = normalizeAdminStatus(status);

        long total = articleMapper.countAdminPage(normalizedKeyword, normalizedStatus);
        List<ArticleResponse> records = articleMapper
            .selectAdminPage(normalizedKeyword, normalizedStatus, offset, normalizedSize)
            .stream()
            .map(this::toResponse)
            .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    public List<ArticleResponse> list() {
        return articleMapper.selectList().stream()
            .map(this::toResponse)
            .toList();
    }

    public ArticleResponse detail(Long id) {
        return toResponse(findExisting(id));
    }


    public PageResponse<ArticleResponse> publishedPage(String keyword, Long categoryId, Long tagId, String sort, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSort = normalizeSort(sort);

        long total = articleMapper.countPublishedPage(normalizedKeyword, categoryId, tagId);
        List<ArticleResponse> records = articleMapper
            .selectPublishedPage(normalizedKeyword, categoryId, tagId, normalizedSort, offset, normalizedSize)
            .stream()
            .map(this::toResponse)
            .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    public List<ArticleResponse> publishedList() {
        return articleMapper.selectPublishedList().stream()
            .map(this::toResponse)
            .toList();
    }

    public ArticleResponse publishedDetail(Long id) {
        articleMapper.increaseViewCount(id);
        Article article = articleMapper.selectPublishedById(id);
        if (article == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        return toResponse(article);
    }

    @Transactional
    public ArticleResponse like(Long id) {
        int updated = articleMapper.increaseLikeCount(id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        Article article = articleMapper.selectPublishedById(id);
        return toResponse(article);
    }

    @Transactional
    public ArticleResponse unlike(Long id) {
        int updated = articleMapper.decreaseLikeCount(id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        Article article = articleMapper.selectPublishedById(id);
        return toResponse(article);
    }

    @Transactional
    public ArticleResponse create(ArticleUpsertRequest request) {
        Article article = new Article();
        fillArticle(article, request, null);
        articleMapper.insert(article);
        syncTags(article.getId(), request.getTagIds());
        return detail(article.getId());
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleUpsertRequest request) {
        Article existing = findExisting(id);
        Article article = new Article();
        article.setId(id);
        fillArticle(article, request, existing);

        int updated = articleMapper.update(article);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        syncTags(id, request.getTagIds());
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        int deleted = articleMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        articleMapper.deleteArticleTags(id);
    }


    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        return keyword.trim();
    }


    private Integer normalizeAdminStatus(Integer status) {
        if (status == null || status < 0 || status > 2) {
            return null;
        }
        return status;
    }

    private String normalizeSort(String sort) {
        if (sort == null) {
            return "latest";
        }
        return switch (sort) {
            case "relevance", "views", "likes", "comments" -> sort;
            default -> "latest";
        };
    }

    private Article findExisting(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        return article;
    }

    private void fillArticle(Article article, ArticleUpsertRequest request, Article existing) {
        int status = normalizeStatus(request.getStatus());
        article.setTitle(request.getTitle().trim());
        article.setSummary(normalizeSummary(request.getSummary()));
        article.setSeoTitle(normalizeText(request.getSeoTitle()));
        article.setSeoDescription(normalizeText(request.getSeoDescription()));
        article.setSeoKeywords(normalizeText(request.getSeoKeywords()));
        article.setContent(request.getContent().trim());
        article.setCoverImage(normalizeText(request.getCoverImage()));
        article.setCategoryId(request.getCategoryId());
        article.setStatus(status);
        article.setPublishedAt(resolvePublishedAt(status, existing));
        article.setIsPinned(normalizePinned(request.getIsPinned()));
    }

    private LocalDateTime resolvePublishedAt(int status, Article existing) {
        if (status != 1) {
            return existing == null ? null : existing.getPublishedAt();
        }
        if (existing != null && existing.getPublishedAt() != null) {
            return existing.getPublishedAt();
        }
        return LocalDateTime.now();
    }

    private void syncTags(Long articleId, List<Long> tagIds) {
        articleMapper.deleteArticleTags(articleId);
        new LinkedHashSet<>(tagIds).stream()
            .filter(id -> id != null && id > 0)
            .forEach(tagId -> articleMapper.insertArticleTag(articleId, tagId));
    }

    public ArticleResponse toResponse(Article article) {
        List<ArticleTagResponse> tags = articleMapper.selectTagsByArticleId(article.getId()).stream()
            .map(this::toTagResponse)
            .toList();

        return new ArticleResponse(
            article.getId(),
            article.getTitle(),
            article.getSummary(),
            article.getSeoTitle(),
            article.getSeoDescription(),
            article.getSeoKeywords(),
            article.getContent(),
            article.getCoverImage(),
            article.getCategoryId(),
            article.getCategoryName(),
            tags,
            article.getStatus(),
            article.getIsPinned(),
            article.getViewCount(),
            article.getLikeCount(),
            article.getCommentCount(),
            article.getCreatedAt(),
            article.getPublishedAt(),
            article.getUpdatedAt()
        );
    }

    private ArticleTagResponse toTagResponse(Tag tag) {
        return new ArticleTagResponse(tag.getId(), tag.getName(), tag.getColor());
    }

    private String normalizeSummary(String summary) {
        return normalizeText(summary);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private int normalizeStatus(Integer status) {
        if (status == null || status < 0 || status > 2) {
            return 0;
        }
        return status;
    }

    private int normalizePinned(Integer isPinned) {
        return isPinned != null && isPinned == 1 ? 1 : 0;
    }
}
