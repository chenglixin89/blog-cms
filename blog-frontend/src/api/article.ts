// article.ts 封装后台文章管理接口，包括分页、详情、新增、编辑、删除和封面上传。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { Article, ArticlePayload } from "../types/article";


export interface AdminArticlePageQuery {
  keyword?: string;
  status?: number | null;
  page?: number;
  size?: number;
}

const cleanParams = (query: AdminArticlePageQuery = {}) => {
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


// getArticlePage 调用后端查询接口并返回页面需要的数据。
export const getArticlePage = async (query: AdminArticlePageQuery = {}): Promise<PageResponse<Article>> => {
  const response = (await http.get("/admin/articles/page", { params: cleanParams(query) })) as ApiResponse<PageResponse<Article>> | PageResponse<Article>;
  return unwrap(response);
};

// getArticleList 调用后端查询接口并返回页面需要的数据。
export const getArticleList = async (): Promise<Article[]> => {
  const response = (await http.get("/admin/articles")) as ApiResponse<Article[]> | Article[];
  return unwrap(response);
};

// getArticleDetail 调用后端查询接口并返回页面需要的数据。
export const getArticleDetail = async (id: number): Promise<Article> => {
  const response = (await http.get(`/admin/articles/${id}`)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

// createArticle 调用后端新增接口。
export const createArticle = async (payload: ArticlePayload): Promise<Article> => {
  const response = (await http.post("/admin/articles", payload)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

// updateArticle 调用后端更新接口。
export const updateArticle = async (id: number, payload: ArticlePayload): Promise<Article> => {
  const response = (await http.put(`/admin/articles/${id}`, payload)) as ApiResponse<Article> | Article;
  return unwrap(response);
};

// deleteArticle 调用后端删除接口。
export const deleteArticle = async (id: number): Promise<void> => {
  await http.delete(`/admin/articles/${id}`);
};

// uploadArticleCover 使用 FormData 上传文件到后端。
export const uploadArticleCover = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append("file", file);
  const response = (await http.post("/admin/upload/cover", formData)) as ApiResponse<string> | string;
  return unwrap(response);
};
