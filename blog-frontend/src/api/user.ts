// user.ts 封装后台用户管理接口，用于管理员查看、启用或禁用前台用户。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { AdminUser } from "../types/adminUser";

export interface AdminUserPageQuery {
  keyword?: string;
  status?: number | null;
  page?: number;
  size?: number;
}

const cleanParams = (query: AdminUserPageQuery = {}) => {
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

// getAdminUserPage 调用后端查询接口并返回页面需要的数据。
export const getAdminUserPage = async (query: AdminUserPageQuery = {}): Promise<PageResponse<AdminUser>> => {
  const response = (await http.get("/admin/users/page", { params: cleanParams(query) })) as ApiResponse<PageResponse<AdminUser>> | PageResponse<AdminUser>;
  return unwrap(response);
};

// updateAdminUserStatus 调用后端更新接口。
export const updateAdminUserStatus = async (id: number, status: number): Promise<void> => {
  await http.put("/admin/users/" + id + "/status", { status });
};

export const resetAdminUserPassword = async (id: number, password: string): Promise<void> => {
  await http.put("/admin/users/" + id + "/password", { password });
};
