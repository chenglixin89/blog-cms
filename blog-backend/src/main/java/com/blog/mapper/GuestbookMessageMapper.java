package com.blog.mapper;

import com.blog.entity.GuestbookMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GuestbookMessageMapper {

    @Select("""
        SELECT id, parent_id, user_id, nickname, email, content, status, is_deleted, created_at, updated_at
        FROM blog_guestbook_message
        WHERE is_deleted = 0
        ORDER BY created_at DESC, id DESC
        """)
    List<GuestbookMessage> selectList();

    @Select("""
        SELECT id, parent_id, user_id, nickname, email, content, status, is_deleted, created_at, updated_at
        FROM blog_guestbook_message
        WHERE id = #{id} AND is_deleted = 0
        """)
    GuestbookMessage selectById(@Param("id") Long id);

    @Select("""
        <script>
        SELECT id, parent_id, user_id, nickname, email, content, status, is_deleted, created_at, updated_at
        FROM blog_guestbook_message
        WHERE is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (nickname LIKE CONCAT('%', #{keyword}, '%')
            OR email LIKE CONCAT('%', #{keyword}, '%')
            OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND status = #{status}
        </if>
        ORDER BY created_at DESC, id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<GuestbookMessage> selectAdminPage(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM blog_guestbook_message
        WHERE is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (nickname LIKE CONCAT('%', #{keyword}, '%')
            OR email LIKE CONCAT('%', #{keyword}, '%')
            OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND status = #{status}
        </if>
        </script>
        """)
    long countAdminPage(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("""
        SELECT id, parent_id, user_id, nickname, email, content, status, is_deleted, created_at, updated_at
        FROM blog_guestbook_message
        WHERE is_deleted = 0 AND status = 1
        ORDER BY created_at ASC, id ASC
        """)
    List<GuestbookMessage> selectApprovedList();

    @Insert("""
        INSERT INTO blog_guestbook_message (parent_id, user_id, nickname, email, content, status)
        VALUES (#{parentId}, #{userId}, #{nickname}, #{email}, #{content}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GuestbookMessage message);

    @Select("""
        SELECT COUNT(*)
        FROM blog_guestbook_message
        WHERE user_id = #{userId}
          AND is_deleted = 0
          AND created_at >= DATE_SUB(NOW(), INTERVAL 60 SECOND)
        """)
    int countRecentByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM blog_guestbook_message
        WHERE user_id IS NULL
          AND nickname = #{nickname}
          AND email = #{email}
          AND is_deleted = 0
          AND created_at >= DATE_SUB(NOW(), INTERVAL 60 SECOND)
        """)
    int countRecentByGuest(@Param("nickname") String nickname, @Param("email") String email);

    @Update("""
        UPDATE blog_guestbook_message
        SET status = #{status},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("""
        UPDATE blog_guestbook_message
        SET is_deleted = 1,
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int logicalDelete(@Param("id") Long id);
}