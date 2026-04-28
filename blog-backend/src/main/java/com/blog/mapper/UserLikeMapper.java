package com.blog.mapper;

import com.blog.entity.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserLikeMapper {

    @Insert("""
        INSERT IGNORE INTO blog_user_like (user_id, article_id)
        VALUES (#{userId}, #{articleId})
        """)
    int insert(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Delete("DELETE FROM blog_user_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int delete(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM blog_user_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int countByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM blog_user_like WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    @Select("SELECT article_id FROM blog_user_like WHERE user_id = #{userId}")
    List<Long> selectArticleIdsByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT a.id, a.title, a.summary, a.content, a.cover_image, a.category_id, c.name AS category_name,
               a.status, a.is_pinned, a.view_count, a.like_count,
               (SELECT COUNT(*) FROM blog_comment cm WHERE cm.article_id = a.id AND cm.status = 1 AND cm.is_deleted = 0) AS comment_count,
               a.is_deleted, a.created_at, a.published_at, a.updated_at
        FROM blog_user_like l
        JOIN blog_article a ON a.id = l.article_id
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.is_deleted = 0
        WHERE l.user_id = #{userId}
          AND a.is_deleted = 0
          AND a.status = 1
        ORDER BY l.created_at DESC, l.id DESC
        """)
    List<Article> selectPublishedLikesByUserId(@Param("userId") Long userId);
}
