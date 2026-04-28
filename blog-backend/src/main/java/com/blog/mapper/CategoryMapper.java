package com.blog.mapper;

import com.blog.entity.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("""
        SELECT c.id,
               c.name,
               c.slug,
               c.description,
               c.sort_order,
               (
                   SELECT COUNT(*)
                   FROM blog_article a
                   WHERE a.category_id = c.id
                     AND a.is_deleted = 0
                     AND a.status = 1
               ) AS article_count,
               c.is_deleted,
               c.created_at,
               c.updated_at
        FROM blog_category c
        WHERE c.is_deleted = 0
        ORDER BY sort_order DESC, id DESC
        """)
    List<Category> selectList();

    @Select("""
        SELECT c.id,
               c.name,
               c.slug,
               c.description,
               c.sort_order,
               (
                   SELECT COUNT(*)
                   FROM blog_article a
                   WHERE a.category_id = c.id
                     AND a.is_deleted = 0
                     AND a.status = 1
               ) AS article_count,
               c.is_deleted,
               c.created_at,
               c.updated_at
        FROM blog_category c
        WHERE c.id = #{id} AND c.is_deleted = 0
        """)
    Category selectById(@Param("id") Long id);

    @Insert("""
        INSERT INTO blog_category (name, slug, description, sort_order)
        VALUES (#{name}, #{slug}, #{description}, #{sortOrder})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Update("""
        UPDATE blog_category
        SET name = #{name},
            slug = #{slug},
            description = #{description},
            sort_order = #{sortOrder},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int update(Category category);

    @Update("""
        UPDATE blog_category
        SET is_deleted = 1,
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
    """)
    int logicalDelete(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM blog_category WHERE is_deleted = 0")
    long countAll();
}
