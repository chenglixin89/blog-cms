package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class SiteSettingRequest {

    @NotBlank(message = "Site name is required")
    private String siteName;

    private String siteSubtitle;
    private String logo;
    private String avatar;

    @NotBlank(message = "Hero title is required")
    private String heroTitle;

    private String heroSubtitle;
    private String authorName;
    private String authorBio;
    private String footerText;
    private Integer dailyQuoteEnabled;
    private String dailyQuoteApiUrl;
    private String roadmapJson;

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getSiteSubtitle() { return siteSubtitle; }
    public void setSiteSubtitle(String siteSubtitle) { this.siteSubtitle = siteSubtitle; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorBio() { return authorBio; }
    public void setAuthorBio(String authorBio) { this.authorBio = authorBio; }
    public String getFooterText() { return footerText; }
    public void setFooterText(String footerText) { this.footerText = footerText; }
    public Integer getDailyQuoteEnabled() { return dailyQuoteEnabled; }
    public void setDailyQuoteEnabled(Integer dailyQuoteEnabled) { this.dailyQuoteEnabled = dailyQuoteEnabled; }
    public String getDailyQuoteApiUrl() { return dailyQuoteApiUrl; }
    public void setDailyQuoteApiUrl(String dailyQuoteApiUrl) { this.dailyQuoteApiUrl = dailyQuoteApiUrl; }
    public String getRoadmapJson() { return roadmapJson; }
    public void setRoadmapJson(String roadmapJson) { this.roadmapJson = roadmapJson; }
}