package com.blog.mapper;

import com.blog.entity.FriendLink;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FriendLinkMapper {

    @Select("""
        SELECT id, name, url, description, logo, status, sort_order, is_deleted, created_at, updated_at
        FROM blog_friend_link
        WHERE is_deleted = 0
        ORDER BY sort_order DESC, id DESC
        """)
    List<FriendLink> selectList();

    @Select("""
        SELECT id, name, url, description, logo, status, sort_order, is_deleted, created_at, updated_at
        FROM blog_friend_link
        WHERE is_deleted = 0 AND status = 1
        ORDER BY sort_order DESC, id DESC
        """)
    List<FriendLink> selectApprovedList();

    @Select("""
        SELECT id, name, url, description, logo, status, sort_order, is_deleted, created_at, updated_at
        FROM blog_friend_link
        WHERE id = #{id} AND is_deleted = 0
        """)
    FriendLink selectById(@Param("id") Long id);

    @Insert("""
        INSERT INTO blog_friend_link (name, url, description, logo, status, sort_order)
        VALUES (#{name}, #{url}, #{description}, #{logo}, #{status}, #{sortOrder})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FriendLink link);

    @Update("""
        UPDATE blog_friend_link
        SET name = #{name},
            url = #{url},
            description = #{description},
            logo = #{logo},
            status = #{status},
            sort_order = #{sortOrder},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int update(FriendLink link);

    @Update("""
        UPDATE blog_friend_link
        SET status = #{status},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("""
        UPDATE blog_friend_link
        SET is_deleted = 1,
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int logicalDelete(@Param("id") Long id);
}
