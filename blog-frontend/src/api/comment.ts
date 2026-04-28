// comment.ts 封装后台评论管理接口，包括审核、回复和删除。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { CommentItem } from "../types/comment";


export interface AdminCommentPageQuery {
  keyword?: string;
  status?: number | null;
  page?: number;
  size?: number;
}

const cleanParams = (query: AdminCommentPageQuery = {}) => {
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


// getCommentPage 调用后端查询接口并返回页面需要的数据。
export const getCommentPage = async (query: AdminCommentPageQuery = {}): Promise<PageResponse<CommentItem>> => {
  const response = (await http.get("/admin/comments/page", { params: cleanParams(query) })) as ApiResponse<PageResponse<CommentItem>> | PageResponse<CommentItem>;
  return unwrap(response);
};

// getCommentList 调用后端查询接口并返回页面需要的数据。
export const getCommentList = async (): Promise<CommentItem[]> => {
  const response = (await http.get("/admin/comments")) as ApiResponse<CommentItem[]> | CommentItem[];
  return unwrap(response);
};

// updateCommentStatus 调用后端更新接口。
export const updateCommentStatus = async (id: number, status: number): Promise<void> => {
  await http.put(`/admin/comments/${id}/status`, { status });
};

export const replyComment = async (id: number, content: string): Promise<CommentItem> => {
  const response = (await http.post(`/admin/comments/${id}/reply`, { content })) as ApiResponse<CommentItem> | CommentItem;
  return unwrap(response);
};

// deleteComment 调用后端删除接口。
export const deleteComment = async (id: number): Promise<void> => {
  await http.delete(`/admin/comments/${id}`);
};
