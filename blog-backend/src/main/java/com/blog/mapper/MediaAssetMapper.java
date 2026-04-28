package com.blog.mapper;

import com.blog.entity.MediaAsset;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MediaAssetMapper {

    @Insert("""
        INSERT INTO blog_media_asset (url, original_name, file_name, extension, content_type, size, category)
        VALUES (#{url}, #{originalName}, #{fileName}, #{extension}, #{contentType}, #{size}, #{category})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MediaAsset mediaAsset);

    @Select("""
        <script>
        SELECT id, url, original_name, file_name, extension, content_type, size, category, is_deleted, created_at, updated_at
        FROM blog_media_asset
        WHERE is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (original_name LIKE CONCAT('%', #{keyword}, '%') OR file_name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="category != null and category != ''">
          AND category = #{category}
        </if>
        ORDER BY created_at DESC, id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<MediaAsset> selectPage(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM blog_media_asset
        WHERE is_deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (original_name LIKE CONCAT('%', #{keyword}, '%') OR file_name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="category != null and category != ''">
          AND category = #{category}
        </if>
        </script>
        """)
    long countPage(@Param("keyword") String keyword, @Param("category") String category);

    @Select("SELECT id, url, original_name, file_name, extension, content_type, size, category, is_deleted, created_at, updated_at FROM blog_media_asset WHERE id = #{id} AND is_deleted = 0")
    MediaAsset selectById(@Param("id") Long id);

    @Update("UPDATE blog_media_asset SET category = #{category}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateCategory(@Param("id") Long id, @Param("category") String category);

    @Update("UPDATE blog_media_asset SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicalDelete(@Param("id") Long id);
}
