package com.blog.mapper;

import com.blog.entity.SiteSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SiteSettingMapper {

    @Select("""
        SELECT id, site_name, site_subtitle, logo, avatar, hero_title, hero_subtitle,
               author_name, author_bio, footer_text, daily_quote_enabled, daily_quote_api_url, roadmap_json, updated_at
        FROM blog_site_setting
        WHERE id = 1
        """)
    SiteSetting selectCurrent();

    @Insert("""
        INSERT INTO blog_site_setting
            (id, site_name, site_subtitle, logo, avatar, hero_title, hero_subtitle, author_name, author_bio, footer_text, daily_quote_enabled, daily_quote_api_url, roadmap_json)
        VALUES
            (1, #{siteName}, #{siteSubtitle}, #{logo}, #{avatar}, #{heroTitle}, #{heroSubtitle}, #{authorName}, #{authorBio}, #{footerText}, #{dailyQuoteEnabled}, #{dailyQuoteApiUrl}, #{roadmapJson})
        """)
    int insert(SiteSetting setting);

    @Update("""
        UPDATE blog_site_setting
        SET site_name = #{siteName},
            site_subtitle = #{siteSubtitle},
            logo = #{logo},
            avatar = #{avatar},
            hero_title = #{heroTitle},
            hero_subtitle = #{heroSubtitle},
            author_name = #{authorName},
            author_bio = #{authorBio},
            footer_text = #{footerText},
            daily_quote_enabled = #{dailyQuoteEnabled},
            daily_quote_api_url = #{dailyQuoteApiUrl},
            roadmap_json = #{roadmapJson},
            updated_at = NOW()
        WHERE id = 1
        """)
    int update(SiteSetting setting);
}