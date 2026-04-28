// taxonomy.ts 封装分类和标签接口，用于文章归类、筛选和后台管理。
import http from "./http";
import type { ApiResponse } from "../types/api";
import type { Category, CategoryPayload, Tag, TagPayload } from "../types/taxonomy";

const unwrap = <T>(response: ApiResponse<T> | T): T => {
  if ((response as ApiResponse<T>).data !== undefined) {
    return (response as ApiResponse<T>).data as T;
  }
  return response as T;
};

// getCategoryList 调用后端查询接口并返回页面需要的数据。
export const getCategoryList = async (): Promise<Category[]> => {
  const response = (await http.get("/admin/categories")) as ApiResponse<Category[]> | Category[];
  return unwrap(response);
};

// createCategory 调用后端新增接口。
export const createCategory = async (payload: CategoryPayload): Promise<Category> => {
  const response = (await http.post("/admin/categories", payload)) as ApiResponse<Category> | Category;
  return unwrap(response);
};

// updateCategory 调用后端更新接口。
export const updateCategory = async (id: number, payload: CategoryPayload): Promise<Category> => {
  const response = (await http.put(`/admin/categories/${id}`, payload)) as ApiResponse<Category> | Category;
  return unwrap(response);
};

// deleteCategory 调用后端删除接口。
export const deleteCategory = async (id: number): Promise<void> => {
  await http.delete(`/admin/categories/${id}`);
};

// getTagList 调用后端查询接口并返回页面需要的数据。
export const getTagList = async (): Promise<Tag[]> => {
  const response = (await http.get("/admin/tags")) as ApiResponse<Tag[]> | Tag[];
  return unwrap(response);
};

// createTag 调用后端新增接口。
export const createTag = async (payload: TagPayload): Promise<Tag> => {
  const response = (await http.post("/admin/tags", payload)) as ApiResponse<Tag> | Tag;
  return unwrap(response);
};

// updateTag 调用后端更新接口。
export const updateTag = async (id: number, payload: TagPayload): Promise<Tag> => {
  const response = (await http.put(`/admin/tags/${id}`, payload)) as ApiResponse<Tag> | Tag;
  return unwrap(response);
};

// deleteTag 调用后端删除接口。
export const deleteTag = async (id: number): Promise<void> => {
  await http.delete(`/admin/tags/${id}`);
};
