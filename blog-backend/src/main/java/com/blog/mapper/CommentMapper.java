package com.blog.mapper;

import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper {

    String COMMENT_COLUMNS = "c.id, c.article_id, c.parent_id, c.user_id, a.title AS article_title, c.nickname, c.email, c.content, c.status, c.is_deleted, c.created_at, c.updated_at";

    @Select("""
        SELECT c.id, c.article_id, c.parent_id, c.user_id, a.title AS article_title, c.nickname, c.email,
               c.content, c.status, c.is_deleted, c.created_at, c.updated_at
        FROM blog_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.is_deleted = 0
        ORDER BY c.created_at DESC, c.id DESC
        """)
    List<Comment> selectList();

    @Select("""
        SELECT c.id, c.article_id, c.parent_id, c.user_id, a.title AS article_title, c.nickname, c.email,
               c.content, c.status, c.is_deleted, c.created_at, c.updated_at
        FROM blog_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.id = #{id} AND c.is_deleted = 0
        """)
    Comment selectById(@Param("id") Long id);

    @Select("""
        SELECT c.id, c.article_id, c.parent_id, c.user_id, a.title AS article_title, c.nickname, c.email,
               c.content, c.status, c.is_deleted, c.created_at, c.updated_at
        FROM blog_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.is_deleted = 0 AND c.status = 1 AND c.article_id = #{articleId}
        ORDER BY c.created_at ASC, c.id ASC
        """)
    List<Comment> selectApprovedByArticleId(@Param("articleId") Long articleId);

    @Select("""
        SELECT c.id, c.article_id, c.parent_id, c.user_id, a.title AS article_title, c.nickname, c.email,
               c.content, c.status, c.is_deleted, c.created_at, c.updated_at
        FROM blog_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.is_deleted = 0 AND c.user_id = #{userId}
        ORDER BY c.created_at DESC, c.id DESC
        LIMIT #{limit}
        """)
    List<Comment> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM blog_comment WHERE is_deleted = 0 AND user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM blog_comment WHERE is_deleted = 0 AND user_id = #{userId} AND status = #{status}")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Insert("""
        INSERT INTO blog_comment (article_id, parent_id, user_id, nickname, email, content, status)
        VALUES (#{articleId}, #{parentId}, #{userId}, #{nickname}, #{email}, #{content}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Update("""
        UPDATE blog_comment
        SET status = #{status},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("""
        UPDATE blog_comment
        SET is_deleted = 1,
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int logicalDelete(@Param("id") Long id);

    @Select("""
        <script>
        SELECT c.id, c.article_id, c.parent_id, c.user_id, a.title AS article_title, c.nickname, c.email,
               c.content, c.status, c.is_deleted, c.created_at, c.updated_at
        FROM blog_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (c.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR c.email LIKE CONCAT('%', #{keyword}, '%')
            OR c.content LIKE CONCAT('%', #{keyword}, '%')
            OR a.title LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND c.status = #{status}
        </if>
        ORDER BY c.created_at DESC, c.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<Comment> selectAdminPage(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM blog_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (c.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR c.email LIKE CONCAT('%', #{keyword}, '%')
            OR c.content LIKE CONCAT('%', #{keyword}, '%')
            OR a.title LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND c.status = #{status}
        </if>
        </script>
        """)
    long countAdminPage(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM blog_comment WHERE is_deleted = 0 AND status = 0")
    long countPending();
}
