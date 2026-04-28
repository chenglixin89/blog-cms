package com.blog.mapper;

import com.blog.entity.BlogUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BlogUserMapper {

    String BASE_COLUMNS = "id, username, password_hash, nickname, email, avatar, bio, role, status, last_login_at, created_at, updated_at";

    @Select("SELECT " + BASE_COLUMNS + " FROM blog_user WHERE username = #{username} LIMIT 1")
    BlogUser selectByUsername(@Param("username") String username);

    @Select("SELECT " + BASE_COLUMNS + " FROM blog_user WHERE id = #{id} LIMIT 1")
    BlogUser selectById(@Param("id") Long id);

    @Insert("""
        INSERT INTO blog_user (username, password_hash, nickname, email, avatar, bio, role, status)
        VALUES (#{username}, #{passwordHash}, #{nickname}, #{email}, #{avatar}, #{bio}, #{role}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BlogUser user);

    @Select("""
        <script>
        SELECT id, username, password_hash, nickname, email, avatar, bio, role, status, last_login_at, created_at, updated_at
        FROM blog_user
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (username LIKE CONCAT('%', #{keyword}, '%')
            OR nickname LIKE CONCAT('%', #{keyword}, '%')
            OR email LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND status = #{status}
        </if>
        ORDER BY created_at DESC, id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<BlogUser> selectAdminPage(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM blog_user
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (username LIKE CONCAT('%', #{keyword}, '%')
            OR nickname LIKE CONCAT('%', #{keyword}, '%')
            OR email LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND status = #{status}
        </if>
        </script>
        """)
    long countAdminPage(@Param("keyword") String keyword, @Param("status") Integer status);

    @Update("""
        UPDATE blog_user
        SET nickname = #{nickname},
            email = #{email},
            avatar = #{avatar},
            bio = #{bio},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int updateProfile(BlogUser user);

    @Update("""
        UPDATE blog_user
        SET status = #{status},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("""
        UPDATE blog_user
        SET password_hash = #{passwordHash},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Update("UPDATE blog_user SET last_login_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int updateLastLoginAt(@Param("id") Long id);
}
