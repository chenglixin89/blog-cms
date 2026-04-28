package com.blog.mapper;

import com.blog.entity.MediaCategory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MediaCategoryMapper {

    @Select("""
        SELECT mc.id,
               mc.name,
               mc.code,
               mc.description,
               mc.sort_order,
               (
                   SELECT COUNT(*)
                   FROM blog_media_asset ma
                   WHERE ma.category = mc.code
                     AND ma.is_deleted = 0
               ) AS asset_count,
               mc.is_system,
               mc.is_deleted,
               mc.created_at,
               mc.updated_at
        FROM blog_media_category mc
        WHERE mc.is_deleted = 0
        ORDER BY mc.sort_order DESC, mc.id ASC
        """)
    List<MediaCategory> selectList();

    @Select("""
        SELECT mc.id,
               mc.name,
               mc.code,
               mc.description,
               mc.sort_order,
               (
                   SELECT COUNT(*)
                   FROM blog_media_asset ma
                   WHERE ma.category = mc.code
                     AND ma.is_deleted = 0
               ) AS asset_count,
               mc.is_system,
               mc.is_deleted,
               mc.created_at,
               mc.updated_at
        FROM blog_media_category mc
        WHERE mc.id = #{id} AND mc.is_deleted = 0
        """)
    MediaCategory selectById(@Param("id") Long id);

    @Select("""
        SELECT mc.id,
               mc.name,
               mc.code,
               mc.description,
               mc.sort_order,
               (
                   SELECT COUNT(*)
                   FROM blog_media_asset ma
                   WHERE ma.category = mc.code
                     AND ma.is_deleted = 0
               ) AS asset_count,
               mc.is_system,
               mc.is_deleted,
               mc.created_at,
               mc.updated_at
        FROM blog_media_category mc
        WHERE mc.code = #{code} AND mc.is_deleted = 0
        """)
    MediaCategory selectByCode(@Param("code") String code);

    @Insert("""
        INSERT INTO blog_media_category (name, code, description, sort_order, is_system)
        VALUES (#{name}, #{code}, #{description}, #{sortOrder}, #{isSystem})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MediaCategory category);

    @Update("""
        UPDATE blog_media_category
        SET name = #{name},
            description = #{description},
            sort_order = #{sortOrder},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0
        """)
    int update(MediaCategory category);

    @Update("""
        UPDATE blog_media_category
        SET is_deleted = 1,
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = 0 AND is_system = 0
        """)
    int logicalDelete(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM blog_media_asset WHERE category = #{code} AND is_deleted = 0")
    long countAssetsByCode(@Param("code") String code);
}