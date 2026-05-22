package com.blog.mapper;

import com.blog.entity.Article;
import com.blog.entity.Tag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper {

    @Select("""
        SELECT a.id, a.title, a.summary, a.seo_title, a.seo_description, a.seo_keywords, a.content, a.cover_image, a.category_id, c.name AS category_name,
               a.status, a.is_pinned, a.view_count, a.like_count,
               (SELECT COUNT(*) FROM blog_comment cm WHERE cm.article_id = a.id AND cm.status = 1 AND cm.is_deleted = 0) AS comment_count,
               a.is_deleted, a.created_at, a.published_at, a.updated_at
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE a.is_deleted = 0
        ORDER BY a.is_pinned DESC, a.updated_at DESC, a.id DESC
        """)
    List<Article> selectList();

    @Select("""
        SELECT a.id, a.title, a.summary, a.seo_title, a.seo_description, a.seo_keywords, a.content, a.cover_image, a.category_id, c.name AS category_name,
               a.status, a.is_pinned, a.view_count, a.like_count,
               (SELECT COUNT(*) FROM blog_comment cm WHERE cm.article_id = a.id AND cm.status = 1 AND cm.is_deleted = 0) AS comment_count,
               a.is_deleted, a.created_at, a.published_at, a.updated_at
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE a.is_deleted = 0 AND a.status = 1
        ORDER BY a.is_pinned DESC, COALESCE(a.published_at, a.updated_at) DESC, a.id DESC
        """)
    List<Article> selectPublishedList();

    @Select("""
        SELECT a.id, a.title, a.summary, a.seo_title, a.seo_description, a.seo_keywords, a.content, a.cover_image, a.category_id, c.name AS category_name,
               a.status, a.is_pinned, a.view_count, a.like_count,
               (SELECT COUNT(*) FROM blog_comment cm WHERE cm.article_id = a.id AND cm.status = 1 AND cm.is_deleted = 0) AS comment_count,
               a.is_deleted, a.created_at, a.published_at, a.updated_at
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE a.id = #{id} AND a.is_deleted = 0
        """)
    Article selectById(@Param("id") Long id);

    @Select("""
        SELECT a.id, a.title, a.summary, a.seo_title, a.seo_description, a.seo_keywords, a.content, a.cover_image, a.category_id, c.name AS category_name,
               a.status, a.is_pinned, a.view_count, a.like_count,
               (SELECT COUNT(*) FROM blog_comment cm WHERE cm.article_id = a.id AND cm.status = 1 AND cm.is_deleted = 0) AS comment_count,
               a.is_deleted, a.created_at, a.published_at, a.updated_at
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE a.id = #{id} AND a.is_deleted = 0 AND a.status = 1
        """)
    Article selectPublishedById(@Param("id") Long id);

    @Insert("""
        INSERT INTO blog_article (title, summary, seo_title, seo_description, seo_keywords, content, cover_image, category_id, status, published_at, is_pinned)
        VALUES (#{title}, #{summary}, #{seoTitle}, #{seoDescription}, #{seoKeywords}, #{content}, #{coverImage}, #{categoryId}, #{status}, #{publishedAt}, #{isPinned})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @Update("""
        UPDATE blog_article
        SET title = #{title},
            summary = #{summary},
            content = #{content},
            cover_image = #{coverImage},
            category_id = #{categoryId},
            status = #{status},
            published_at = #{publishedAt},
            is_pinned = #{isPinned},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int update(Article article);

    @Update("""
        UPDATE blog_article
        SET is_deleted = 1,
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int logicalDelete(@Param("id") Long id);

    @Delete("DELETE FROM blog_article WHERE is_deleted = 1")
    int clearDeleted();

    @Select("""
        <script>
        SELECT a.id, a.title, a.summary, a.seo_title, a.seo_description, a.seo_keywords, a.content, a.cover_image, a.category_id, c.name AS category_name,
               a.status, a.is_pinned, a.view_count, a.like_count,
               (SELECT COUNT(*) FROM blog_comment cm WHERE cm.article_id = a.id AND cm.status = 1 AND cm.is_deleted = 0) AS comment_count,
               a.is_deleted, a.created_at, a.published_at, a.updated_at
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE a.is_deleted = 0 AND a.status = 1
        <if test="keyword != null and keyword != '' and keyword.length() >= 2">
          AND MATCH(a.title, a.summary, a.content) AGAINST(#{keyword})
        </if>
        <if test="keyword != null and keyword != '' and keyword.length() &lt; 2">
          AND (a.title LIKE CONCAT('%', #{keyword}, '%')
            OR a.summary LIKE CONCAT('%', #{keyword}, '%')
            OR a.content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="categoryId != null">
          AND a.category_id = #{categoryId}
        </if>
        <if test="tagId != null">
          AND EXISTS (
            SELECT 1 FROM blog_article_tag at
            WHERE at.article_id = a.id AND at.tag_id = #{tagId}
          )
        </if>
        <choose>
          <when test="sort == 'relevance'">
            ORDER BY (
              CASE WHEN a.title LIKE CONCAT('%', #{keyword}, '%') THEN 5 ELSE 0 END +
              CASE WHEN a.summary LIKE CONCAT('%', #{keyword}, '%') THEN 3 ELSE 0 END +
              CASE WHEN a.content LIKE CONCAT('%', #{keyword}, '%') THEN 1 ELSE 0 END
            ) DESC, COALESCE(a.published_at, a.updated_at) DESC, a.id DESC
          </when>
          <when test="sort == 'views'">
            ORDER BY a.view_count DESC, COALESCE(a.published_at, a.updated_at) DESC, a.id DESC
          </when>
          <when test="sort == 'likes'">
            ORDER BY a.like_count DESC, COALESCE(a.published_at, a.updated_at) DESC, a.id DESC
          </when>
          <when test="sort == 'comments'">
            ORDER BY comment_count DESC, COALESCE(a.published_at, a.updated_at) DESC, a.id DESC
          </when>
          <otherwise>
            ORDER BY a.is_pinned DESC, COALESCE(a.published_at, a.updated_at) DESC, a.id DESC
          </otherwise>
        </choose>
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<Article> selectPublishedPage(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("tagId") Long tagId,
        @Param("sort") String sort,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM blog_article a
        WHERE a.is_deleted = 0 AND a.status = 1
        <if test="keyword != null and keyword != '' and keyword.length() >= 2">
          AND MATCH(a.title, a.summary, a.content) AGAINST(#{keyword})
        </if>
        <if test="keyword != null and keyword != '' and keyword.length() &lt; 2">
          AND (a.title LIKE CONCAT('%', #{keyword}, '%')
            OR a.summary LIKE CONCAT('%', #{keyword}, '%')
            OR a.content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="categoryId != null">
          AND a.category_id = #{categoryId}
        </if>
        <if test="tagId != null">
          AND EXISTS (
            SELECT 1 FROM blog_article_tag at
            WHERE at.article_id = a.id AND at.tag_id = #{tagId}
          )
        </if>
        </script>
        """)
    long countPublishedPage(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("tagId") Long tagId
    );

    @Select("""
        <script>
        SELECT a.id, a.title, a.summary, a.seo_title, a.seo_description, a.seo_keywords, a.content, a.cover_image, a.category_id, c.name AS category_name,
               a.status, a.is_pinned, a.view_count, a.like_count,
               (SELECT COUNT(*) FROM blog_comment cm WHERE cm.article_id = a.id AND cm.status = 1 AND cm.is_deleted = 0) AS comment_count,
               a.is_deleted, a.created_at, a.published_at, a.updated_at
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE a.is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (a.title LIKE CONCAT('%', #{keyword}, '%')
            OR a.summary LIKE CONCAT('%', #{keyword}, '%')
            OR a.content LIKE CONCAT('%', #{keyword}, '%')
            OR c.name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND a.status = #{status}
        </if>
        ORDER BY a.is_pinned DESC, a.updated_at DESC, a.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<Article> selectAdminPage(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE a.is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (a.title LIKE CONCAT('%', #{keyword}, '%')
            OR a.summary LIKE CONCAT('%', #{keyword}, '%')
            OR a.content LIKE CONCAT('%', #{keyword}, '%')
            OR c.name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND a.status = #{status}
        </if>
        </script>
        """)
    long countAdminPage(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("""
        SELECT t.id, t.name, t.color, t.article_count, t.is_deleted, t.created_at, t.updated_at
        FROM blog_article_tag at
        JOIN blog_tag t ON t.id = at.tag_id AND t.is_deleted = 0
        WHERE at.article_id = #{articleId}
        ORDER BY t.id DESC
        """)
    List<Tag> selectTagsByArticleId(@Param("articleId") Long articleId);

    @Insert("""
        INSERT INTO blog_article_tag (article_id, tag_id)
        VALUES (#{articleId}, #{tagId})
        """)
    int insertArticleTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM blog_article_tag WHERE article_id = #{articleId}")
    int deleteArticleTags(@Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM blog_article WHERE is_deleted = 0")
    long countAll();

    @Select("SELECT COUNT(*) FROM blog_article WHERE is_deleted = 0 AND status = #{status}")
    long countByStatus(@Param("status") Integer status);

    @Select("""
        SELECT DATE_FORMAT(COALESCE(published_at, created_at, updated_at), '%Y-%m-%d') AS dayKey,
               COUNT(*) AS publishCount
        FROM blog_article
        WHERE is_deleted = 0
          AND status = 1
          AND COALESCE(published_at, created_at, updated_at) >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)
          AND COALESCE(published_at, created_at, updated_at) < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
        GROUP BY DATE_FORMAT(COALESCE(published_at, created_at, updated_at), '%Y-%m-%d')
        ORDER BY dayKey ASC
        """)
    List<Map<String, Object>> selectRecentPublishTrend();

    @Update("""
        UPDATE blog_article
        SET view_count = view_count + 1
        WHERE id = #{id} AND is_deleted = 0 AND status = 1
        """)
    int increaseViewCount(@Param("id") Long id);

    @Update("""
        UPDATE blog_article
        SET like_count = like_count + 1
        WHERE id = #{id} AND is_deleted = 0 AND status = 1
        """)
    int increaseLikeCount(@Param("id") Long id);

    @Update("""
        UPDATE blog_article
        SET like_count = GREATEST(like_count - 1, 0)
        WHERE id = #{id} AND is_deleted = 0 AND status = 1
        """)
    int decreaseLikeCount(@Param("id") Long id);
}
