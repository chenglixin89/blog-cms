// front.ts 封装前台公开接口和普通用户接口，包括文章、评论、收藏、点赞、登录注册和用户中心。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { Article } from "../types/article";
import type { CommentItem, CommentPayload } from "../types/comment";
import type { FrontAuthResult, FrontLoginPayload, FrontPasswordPayload, FrontProfilePayload, FrontRegisterPayload, FrontUserProfile, UserCenterData } from "../types/frontUser";
import type { Category, Tag } from "../types/taxonomy";

export interface DailyQuote {
  enabled: boolean;
  content: string;
  source: string;
  author: string;
  sourceUrl: string;
  provider: string;
  fallback: boolean;
  fetchedAt: string | null;
}


export interface FrontArticlePageQuery {
  keyword?: string;
  categoryId?: number | null;
  tagId?: number | null;
  sort?: "relevance" | "latest" | "views" | "likes" | "comments";
  page?: number;
  size?: number;
}

const cleanParams = (query: FrontArticlePageQuery = {}) => {
  const params: Record<string, string | number> = {};
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params[key] = value;
    }
  });
  return params;
};

const unwrap = <T>(response: ApiResponse<T> | T): T => {
  if ((response as ApiResponse<T>).data !== undefined) {
    return (response as ApiResponse<T>).data as T;
  }
  return response as T;
};

const getWithFallback = async <T>(primaryUrl: string, fallbackUrl: string): Promise<T> => {
  try {
    return unwrap((await http.get(primaryUrl)) as ApiResponse<T> | T);
  } catch (error) {
    return unwrap((await http.get(fallbackUrl)) as ApiResponse<T> | T);
  }
};


// getFrontArticlePage 调用后端查询接口并返回页面需要的数据。
export const getFrontArticlePage = async (query: FrontArticlePageQuery = {}): Promise<PageResponse<Article>> => {
  const response = (await http.get("/front/articles/page", { params: cleanParams(query) })) as ApiResponse<PageResponse<Article>> | PageResponse<Article>;
  return unwrap(response);
};

// getDailyQuote 调用后端查询接口并返回页面需要的数据。
export const getDailyQuote = async (): Promise<DailyQuote> => {
  const response = (await http.get("/front/daily-quote")) as ApiResponse<DailyQuote> | DailyQuote;
  return unwrap(response);
};

// getFrontArticles 调用后端查询接口并返回页面需要的数据。
export const getFrontArticles = async (): Promise<Article[]> => {
  const response = (await http.get("/front/articles")) as ApiResponse<Article[]> | Article[];
  return unwrap(response);
};

// getFrontArticleDetail 调用后端查询接口并返回页面需要的数据。
export const getFrontArticleDetail = async (id: number): Promise<Article> => {
  const response = (await http.get(`/front/articles/${id}`)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

export const likeFrontArticle = async (id: number): Promise<Article> => {
  const response = (await http.post(`/front/articles/${id}/like`)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

export const unlikeFrontArticle = async (id: number): Promise<Article> => {
  const response = (await http.delete(`/front/articles/${id}/like`)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

// getFrontComments 调用后端查询接口并返回页面需要的数据。
export const getFrontComments = async (articleId: number): Promise<CommentItem[]> => {
  const response = (await http.get(`/front/articles/${articleId}/comments`)) as ApiResponse<CommentItem[]> | CommentItem[];
  return unwrap(response);
};

export const submitFrontComment = async (articleId: number, payload: CommentPayload): Promise<void> => {
  await http.post(`/front/articles/${articleId}/comments`, payload);
};

// getFrontCategories 调用后端查询接口并返回页面需要的数据。
export const getFrontCategories = async (): Promise<Category[]> => {
  return getWithFallback<Category[]>("/front/categories", "/admin/categories");
};

// getFrontTags 调用后端查询接口并返回页面需要的数据。
export const getFrontTags = async (): Promise<Tag[]> => {
  return getWithFallback<Tag[]>("/front/tags", "/admin/tags");
};

// frontLogin 调用前台用户登录接口，成功后返回 token 和用户信息。
export const frontLogin = async (payload: FrontLoginPayload): Promise<FrontAuthResult> => {
  const response = (await http.post("/front/auth/login", payload)) as ApiResponse<FrontAuthResult> | FrontAuthResult;
  return unwrap(response);
};

// frontRegister 调用前台用户注册接口，成功后返回 token 和用户信息。
export const frontRegister = async (payload: FrontRegisterPayload): Promise<FrontAuthResult> => {
  const response = (await http.post("/front/auth/register", payload)) as ApiResponse<FrontAuthResult> | FrontAuthResult;
  return unwrap(response);
};

// getFrontMe 调用后端查询接口并返回页面需要的数据。
export const getFrontMe = async (): Promise<FrontUserProfile> => {
  const response = (await http.get("/front/auth/me")) as ApiResponse<FrontUserProfile> | FrontUserProfile;
  return unwrap(response);
};

// getCloudFavorites 调用后端查询接口并返回页面需要的数据。
export const getCloudFavorites = async (): Promise<Article[]> => {
  const response = (await http.get("/front/favorites")) as ApiResponse<Article[]> | Article[];
  return unwrap(response);
};

// getCloudFavoriteIds 调用后端查询接口并返回页面需要的数据。
export const getCloudFavoriteIds = async (): Promise<number[]> => {
  const response = (await http.get("/front/favorites/ids")) as ApiResponse<number[]> | number[];
  return unwrap(response);
};

// addCloud 调用云端新增接口，用于登录用户的数据同步。
export const addCloudFavorite = async (articleId: number): Promise<void> => {
  await http.post(`/front/favorites/${articleId}`);
};

// removeCloud 调用云端移除接口，用于取消收藏或取消点赞。
export const removeCloudFavorite = async (articleId: number): Promise<void> => {
  await http.delete(`/front/favorites/${articleId}`);
};

// migrateCloud 把未登录时的本地数据迁移到登录后的云端账号。
export const migrateCloudFavorites = async (articleIds: number[]): Promise<number[]> => {
  const response = (await http.post("/front/favorites/migrate", { articleIds })) as ApiResponse<number[]> | number[];
  return unwrap(response);
};

// getCloudLikeIds 调用后端查询接口并返回页面需要的数据。
export const getCloudLikeIds = async (): Promise<number[]> => {
  const response = (await http.get("/front/likes/ids")) as ApiResponse<number[]> | number[];
  return unwrap(response);
};

// addCloud 调用云端新增接口，用于登录用户的数据同步。
export const addCloudLike = async (articleId: number): Promise<Article> => {
  const response = (await http.post(`/front/likes/${articleId}`)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

// removeCloud 调用云端移除接口，用于取消收藏或取消点赞。
export const removeCloudLike = async (articleId: number): Promise<Article> => {
  const response = (await http.delete(`/front/likes/${articleId}`)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

// migrateCloud 把未登录时的本地数据迁移到登录后的云端账号。
export const migrateCloudLikes = async (articleIds: number[]): Promise<number[]> => {
  const response = (await http.post("/front/likes/migrate", { articleIds })) as ApiResponse<number[]> | number[];
  return unwrap(response);
};

// getUserCenter 调用后端查询接口并返回页面需要的数据。
export const getUserCenter = async (): Promise<UserCenterData> => {
  const response = (await http.get("/front/auth/center")) as ApiResponse<UserCenterData> | UserCenterData;
  return unwrap(response);
};

// updateFrontProfile 调用后端更新接口。
export const updateFrontProfile = async (payload: FrontProfilePayload): Promise<FrontUserProfile> => {
  const response = (await http.put("/front/auth/profile", payload)) as ApiResponse<FrontUserProfile> | FrontUserProfile;
  return unwrap(response);
};

// updateFrontPassword 调用后端更新接口。
export const updateFrontPassword = async (payload: FrontPasswordPayload): Promise<void> => {
  await http.put("/front/auth/password", payload);
};
