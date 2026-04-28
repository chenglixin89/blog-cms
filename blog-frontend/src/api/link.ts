// link.ts 封装友情链接接口，包括前台展示、友链申请和后台审核管理。
import http from "./http";
import type { ApiResponse } from "../types/api";
import type { FriendLink, FriendLinkPayload } from "../types/link";

const unwrap = <T>(response: ApiResponse<T> | T): T => {
  if ((response as ApiResponse<T>).data !== undefined) {
    return (response as ApiResponse<T>).data as T;
  }
  return response as T;
};

// getFrontLinks 调用后端查询接口并返回页面需要的数据。
export const getFrontLinks = async (): Promise<FriendLink[]> => {
  const response = (await http.get("/front/links")) as ApiResponse<FriendLink[]> | FriendLink[];
  return unwrap(response);
};

export const applyFriendLink = async (payload: FriendLinkPayload): Promise<void> => {
  await http.post("/front/links", payload);
};

// getAdminLinks 调用后端查询接口并返回页面需要的数据。
export const getAdminLinks = async (): Promise<FriendLink[]> => {
  const response = (await http.get("/admin/links")) as ApiResponse<FriendLink[]> | FriendLink[];
  return unwrap(response);
};

// createFriendLink 调用后端新增接口。
export const createFriendLink = async (payload: FriendLinkPayload): Promise<FriendLink> => {
  const response = (await http.post("/admin/links", payload)) as ApiResponse<FriendLink> | FriendLink;
  return unwrap(response);
};

// updateFriendLink 调用后端更新接口。
export const updateFriendLink = async (id: number, payload: FriendLinkPayload): Promise<FriendLink> => {
  const response = (await http.put(`/admin/links/${id}`, payload)) as ApiResponse<FriendLink> | FriendLink;
  return unwrap(response);
};

// updateFriendLinkStatus 调用后端更新接口。
export const updateFriendLinkStatus = async (id: number, status: number): Promise<void> => {
  await http.put(`/admin/links/${id}/status`, { status });
};

// deleteFriendLink 调用后端删除接口。
export const deleteFriendLink = async (id: number): Promise<void> => {
  await http.delete(`/admin/links/${id}`);
};
