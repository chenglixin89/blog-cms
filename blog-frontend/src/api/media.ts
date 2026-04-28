// media.ts 封装媒体库接口，包括图片上传、分页查询、分类管理和图片分类修改。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { MediaAsset, MediaCategory } from "../types/media";

export interface MediaCategoryPayload {
  name: string;
  code?: string;
  description?: string;
  sortOrder?: number;
}

export interface MediaPageQuery {
  keyword?: string;
  category?: string;
  page?: number;
  size?: number;
}

const cleanParams = (query: MediaPageQuery = {}) => {
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

// getMediaPage 调用后端查询接口并返回页面需要的数据。
export const getMediaPage = async (query: MediaPageQuery = {}): Promise<PageResponse<MediaAsset>> => {
  const response = (await http.get("/admin/media/page", { params: cleanParams(query) })) as ApiResponse<PageResponse<MediaAsset>> | PageResponse<MediaAsset>;
  return unwrap(response);
};

// uploadMedia 使用 FormData 上传文件到后端。
export const uploadMedia = async (file: File, category = "content"): Promise<MediaAsset> => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("category", category);
  const response = (await http.post("/admin/media/upload", formData)) as ApiResponse<MediaAsset> | MediaAsset;
  return unwrap(response);
};

// deleteMedia 调用后端删除接口。
export const deleteMedia = async (id: number): Promise<void> => {
  await http.delete("/admin/media/" + id);
};

// getMediaCategories 调用后端查询接口并返回页面需要的数据。
export const getMediaCategories = async (): Promise<MediaCategory[]> => {
  const response = (await http.get("/admin/media/categories")) as ApiResponse<MediaCategory[]> | MediaCategory[];
  return unwrap(response);
};

// createMediaCategory 调用后端新增接口。
export const createMediaCategory = async (payload: MediaCategoryPayload): Promise<MediaCategory> => {
  const response = (await http.post("/admin/media/categories", payload)) as ApiResponse<MediaCategory> | MediaCategory;
  return unwrap(response);
};

// updateMediaCategory 调用后端更新接口。
export const updateMediaCategory = async (id: number, payload: MediaCategoryPayload): Promise<MediaCategory> => {
  const response = (await http.put("/admin/media/categories/" + id, payload)) as ApiResponse<MediaCategory> | MediaCategory;
  return unwrap(response);
};

// deleteMediaCategory 调用后端删除接口。
export const deleteMediaCategory = async (id: number): Promise<void> => {
  await http.delete("/admin/media/categories/" + id);
};
// updateMediaAssetCategory 调用后端更新接口。
export const updateMediaAssetCategory = async (id: number, category: string): Promise<MediaAsset> => {
  const response = (await http.put("/admin/media/" + id + "/category", { category })) as ApiResponse<MediaAsset> | MediaAsset;
  return unwrap(response);
};