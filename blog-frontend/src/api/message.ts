// message.ts 封装留言板接口，包括前台提交留言和后台审核回复。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { GuestbookMessage, GuestbookMessagePayload } from "../types/message";


export interface AdminMessagePageQuery {
  keyword?: string;
  status?: number | null;
  page?: number;
  size?: number;
}

const cleanParams = (query: AdminMessagePageQuery = {}) => {
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

// getFrontMessages 调用后端查询接口并返回页面需要的数据。
export const getFrontMessages = async (): Promise<GuestbookMessage[]> => {
  const response = (await http.get("/front/messages")) as ApiResponse<GuestbookMessage[]> | GuestbookMessage[];
  return unwrap(response);
};

export const submitFrontMessage = async (payload: GuestbookMessagePayload): Promise<void> => {
  await http.post("/front/messages", payload);
};


// getAdminMessagePage 调用后端查询接口并返回页面需要的数据。
export const getAdminMessagePage = async (query: AdminMessagePageQuery = {}): Promise<PageResponse<GuestbookMessage>> => {
  const response = (await http.get("/admin/messages/page", { params: cleanParams(query) })) as ApiResponse<PageResponse<GuestbookMessage>> | PageResponse<GuestbookMessage>;
  return unwrap(response);
};

// getAdminMessages 调用后端查询接口并返回页面需要的数据。
export const getAdminMessages = async (): Promise<GuestbookMessage[]> => {
  const response = (await http.get("/admin/messages")) as ApiResponse<GuestbookMessage[]> | GuestbookMessage[];
  return unwrap(response);
};

// updateMessageStatus 调用后端更新接口。
export const updateMessageStatus = async (id: number, status: number): Promise<void> => {
  await http.put(`/admin/messages/${id}/status`, { status });
};

export const replyMessage = async (id: number, content: string): Promise<GuestbookMessage> => {
  const response = (await http.post(`/admin/messages/${id}/reply`, { content })) as
    | ApiResponse<GuestbookMessage>
    | GuestbookMessage;
  return unwrap(response);
};

// deleteMessage 调用后端删除接口。
export const deleteMessage = async (id: number): Promise<void> => {
  await http.delete(`/admin/messages/${id}`);
};
