package com.blog.mapper;

import com.blog.entity.Tag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TagMapper {

    @Select("""
        SELECT t.id,
               t.name,
               t.color,
               (
                   SELECT COUNT(*)
                   FROM blog_article_tag at
                   JOIN blog_article a ON a.id = at.article_id
                   WHERE at.tag_id = t.id
                     AND a.is_deleted = 0
                     AND a.status = 1
               ) AS article_count,
               t.is_deleted,
               t.created_at,
               t.updated_at
        FROM blog_tag t
        WHERE t.is_deleted = 0
        ORDER BY t.id DESC
        """)
    List<Tag> selectList();

    @Select("""
        SELECT t.id,
               t.name,
               t.color,
               (
                   SELECT COUNT(*)
                   FROM blog_article_tag at
                   JOIN blog_article a ON a.id = at.article_id
                   WHERE at.tag_id = t.id
                     AND a.is_deleted = 0
                     AND a.status = 1
               ) AS article_count,
               t.is_deleted,
               t.created_at,
               t.updated_at
        FROM blog_tag t
        WHERE t.id = #{id} AND t.is_deleted = 0
        """)
    Tag selectById(@Param("id") Long id);

    @Insert("""
        INSERT INTO blog_tag (name, color)
        VALUES (#{name}, #{color})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    @Update("""
        UPDATE blog_tag
        SET name = #{name},
            color = #{color},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int update(Tag tag);

    @Update("""
        UPDATE blog_tag
        SET is_deleted = 1,
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
    """)
    int logicalDelete(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM blog_tag WHERE is_deleted = 0")
    long countAll();
}
