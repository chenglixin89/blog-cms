package com.blog.service;

import com.blog.dto.SiteSettingRequest;
import com.blog.dto.SiteSettingResponse;
import com.blog.entity.SiteSetting;
import com.blog.mapper.SiteSettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettingService {

    public static final String DEFAULT_DAILY_QUOTE_API_URL = "https://v1.hitokoto.cn/?encode=json&max_length=60";
    public static final String DEFAULT_ROADMAP_JSON = "[{\"title\":\"\u4e00\u671f\",\"description\":\"\u82f9\u679c\u7cfb\u7edf\u98ce\u683c\u540e\u53f0\u89c6\u89c9\u4e0e\u57fa\u7840\u9875\u9762\",\"active\":true},{\"title\":\"\u4e8c\u671f\",\"description\":\"\u771f\u5b9e\u767b\u5f55\u3001\u6587\u7ae0\u6301\u4e45\u5316\u3001\u5206\u7c7b\u6807\u7b7e\u8bc4\u8bba\",\"active\":true},{\"title\":\"\u4e09\u671f\",\"description\":\"\u524d\u53f0\u9605\u8bfb\u3001\u70b9\u8d5e\u3001\u8bc4\u8bba\u3001\u4e13\u9898\u3001\u7559\u8a00\u677f\u4e0e\u53cb\u94fe\",\"active\":true},{\"title\":\"\u5f53\u524d\",\"description\":\"AI \u80fd\u529b\u3001\u5a92\u4f53\u5e93\u3001\u7528\u6237\u4e2d\u5fc3\u3001\u7ad9\u70b9\u8bbe\u7f6e\u4e0e\u90e8\u7f72\u4f18\u5316\",\"active\":true}]";

    private final SiteSettingMapper settingMapper;

    public SiteSettingService(SiteSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    public SiteSettingResponse current() {
        SiteSetting setting = settingMapper.selectCurrent();
        if (setting == null) {
            setting = defaultSetting();
            settingMapper.insert(setting);
            setting = settingMapper.selectCurrent();
        }
        return toResponse(setting);
    }

    @Transactional
    public SiteSettingResponse update(SiteSettingRequest request) {
        SiteSetting setting = new SiteSetting();
        setting.setId(1L);
        setting.setSiteName(request.getSiteName().trim());
        setting.setSiteSubtitle(trimOrEmpty(request.getSiteSubtitle()));
        setting.setLogo(trimOrEmpty(request.getLogo()));
        setting.setAvatar(trimOrEmpty(request.getAvatar()));
        setting.setHeroTitle(request.getHeroTitle().trim());
        setting.setHeroSubtitle(trimOrEmpty(request.getHeroSubtitle()));
        setting.setAuthorName(trimOrEmpty(request.getAuthorName()));
        setting.setAuthorBio(trimOrEmpty(request.getAuthorBio()));
        setting.setFooterText(trimOrEmpty(request.getFooterText()));
        setting.setDailyQuoteEnabled(request.getDailyQuoteEnabled() != null && request.getDailyQuoteEnabled() == 0 ? 0 : 1);
        setting.setDailyQuoteApiUrl(normalizeDailyQuoteApiUrl(request.getDailyQuoteApiUrl()));
        setting.setRoadmapJson(normalizeRoadmapJson(request.getRoadmapJson()));

        int updated = settingMapper.update(setting);
        if (updated == 0) {
            settingMapper.insert(setting);
        }
        return current();
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeRoadmapJson(String value) {
        String roadmap = trimOrEmpty(value);
        return roadmap.isBlank() ? DEFAULT_ROADMAP_JSON : roadmap;
    }

    private String normalizeDailyQuoteApiUrl(String value) {
        String url = trimOrEmpty(value);
        if (url.isBlank()) {
            return DEFAULT_DAILY_QUOTE_API_URL;
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            return DEFAULT_DAILY_QUOTE_API_URL;
        }
        return url.length() > 500 ? url.substring(0, 500) : url;
    }

    private SiteSetting defaultSetting() {
        SiteSetting setting = new SiteSetting();
        setting.setId(1L);
        setting.setSiteName("\u4e2a\u4eba\u535a\u5ba2");
        setting.setSiteSubtitle("Personal Blog CMS");
        setting.setLogo("");
        setting.setAvatar("");
        setting.setHeroTitle("\u8bb0\u5f55\u6280\u672f\u3001\u9879\u76ee\u4e0e\u6301\u7eed\u751f\u957f\u7684\u601d\u8003\u3002");
        setting.setHeroSubtitle("\u8fd9\u91cc\u662f\u535a\u5ba2\u524d\u53f0\u5c55\u793a\u533a\uff1a\u652f\u6301\u6587\u7ae0\u68c0\u7d22\u3001\u5206\u7c7b\u7b5b\u9009\u3001\u6807\u7b7e\u7b5b\u9009\u3001\u5f52\u6863\u6d4f\u89c8\u3001\u9605\u8bfb\u7edf\u8ba1\u3001\u70b9\u8d5e\u4e0e\u8bc4\u8bba\u4e92\u52a8\u3002");
        setting.setAuthorName("Admin");
        setting.setAuthorBio("\u7528\u4ee3\u7801\u548c\u6587\u5b57\u8bb0\u5f55\u65e5\u5e38\u63a2\u7d22\u3002");
        setting.setFooterText("Powered by Blog CMS");
        setting.setDailyQuoteEnabled(1);
        setting.setDailyQuoteApiUrl(DEFAULT_DAILY_QUOTE_API_URL);
        setting.setRoadmapJson(DEFAULT_ROADMAP_JSON);
        return setting;
    }

    private SiteSettingResponse toResponse(SiteSetting setting) {
        return new SiteSettingResponse(
            setting.getId(),
            setting.getSiteName(),
            setting.getSiteSubtitle(),
            setting.getLogo(),
            setting.getAvatar(),
            setting.getHeroTitle(),
            setting.getHeroSubtitle(),
            setting.getAuthorName(),
            setting.getAuthorBio(),
            setting.getFooterText(),
            setting.getDailyQuoteEnabled() == null ? 1 : setting.getDailyQuoteEnabled(),
            normalizeDailyQuoteApiUrl(setting.getDailyQuoteApiUrl()),
            normalizeRoadmapJson(setting.getRoadmapJson()),
            setting.getUpdatedAt()
        );
    }
}