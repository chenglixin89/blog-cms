// SiteSetting describes the global site settings shared by admin and front pages.
export interface SiteSetting {
  id: number;
  siteName: string;
  siteSubtitle: string;
  logo: string;
  avatar: string;
  heroTitle: string;
  heroSubtitle: string;
  authorName: string;
  authorBio: string;
  footerText: string;
  dailyQuoteEnabled: number;
  dailyQuoteApiUrl: string;
  roadmapJson: string;
  updatedAt: string;
}

export interface SiteSettingPayload {
  siteName: string;
  siteSubtitle: string;
  logo: string;
  avatar: string;
  heroTitle: string;
  heroSubtitle: string;
  authorName: string;
  authorBio: string;
  footerText: string;
  dailyQuoteEnabled: number;
  dailyQuoteApiUrl: string;
  roadmapJson: string;
}