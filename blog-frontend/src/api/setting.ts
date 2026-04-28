// setting.ts wraps site setting APIs used by the admin settings page and front pages.
import http from "./http";
import type { ApiResponse } from "../types/api";
import type { SiteSetting, SiteSettingPayload } from "../types/setting";

const unwrap = <T>(response: ApiResponse<T> | T): T => {
  if ((response as ApiResponse<T>).data !== undefined) {
    return (response as ApiResponse<T>).data as T;
  }
  return response as T;
};

export const defaultRoadmapJson = JSON.stringify([
  { title: "一期", description: "苹果系统风格后台视觉与基础页面", active: true },
  { title: "二期", description: "真实登录、文章持久化、分类标签评论", active: true },
  { title: "三期", description: "前台阅读、点赞、评论、专题、留言板与友链", active: true },
  { title: "当前", description: "Skills 管理、媒体库、用户中心、站点设置与部署优化", active: true }
], null, 2);

export const defaultSiteSetting: SiteSetting = {
  id: 1,
  siteName: "个人博客",
  siteSubtitle: "Personal Blog CMS",
  logo: "",
  avatar: "",
  heroTitle: "记录技术、项目与持续生长的思考。",
  heroSubtitle: "这里是博客前台展示区：支持文章检索、分类筛选、标签筛选、归档浏览、阅读统计、点赞与评论互动。",
  authorName: "Admin",
  authorBio: "用代码和文字记录日常探索。",
  footerText: "Powered by Blog CMS",
  dailyQuoteEnabled: 1,
  dailyQuoteApiUrl: "https://v1.hitokoto.cn/?encode=json&max_length=60",
  roadmapJson: defaultRoadmapJson,
  updatedAt: ""
};

const normalizeSetting = (setting: SiteSetting): SiteSetting => ({
  ...defaultSiteSetting,
  ...setting,
  dailyQuoteEnabled: setting.dailyQuoteEnabled ?? defaultSiteSetting.dailyQuoteEnabled,
  dailyQuoteApiUrl: setting.dailyQuoteApiUrl || defaultSiteSetting.dailyQuoteApiUrl,
  roadmapJson: setting.roadmapJson || defaultSiteSetting.roadmapJson
});

export const getFrontSetting = async (): Promise<SiteSetting> => {
  try {
    const response = (await http.get("/front/settings")) as ApiResponse<SiteSetting> | SiteSetting;
    return normalizeSetting(unwrap(response));
  } catch {
    return defaultSiteSetting;
  }
};

export const getAdminSetting = async (): Promise<SiteSetting> => {
  const response = (await http.get("/admin/settings")) as ApiResponse<SiteSetting> | SiteSetting;
  return normalizeSetting(unwrap(response));
};

export const updateSiteSetting = async (payload: SiteSettingPayload): Promise<SiteSetting> => {
  const response = (await http.put("/admin/settings", payload)) as ApiResponse<SiteSetting> | SiteSetting;
  return normalizeSetting(unwrap(response));
};
