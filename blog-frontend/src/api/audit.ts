// audit.ts 封装后台操作日志接口，用于查看管理员关键操作记录。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { AuditLog } from "../types/audit";

export interface AuditLogPageQuery {
  keyword?: string;
  module?: string;
  action?: string;
  page?: number;
  size?: number;
}

const cleanParams = (query: AuditLogPageQuery = {}) => {
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

// getAuditLogPage 调用后端查询接口并返回页面需要的数据。
export const getAuditLogPage = async (query: AuditLogPageQuery = {}): Promise<PageResponse<AuditLog>> => {
  const response = (await http.get("/admin/audit-logs/page", { params: cleanParams(query) })) as
    | ApiResponse<PageResponse<AuditLog>>
    | PageResponse<AuditLog>;
  return unwrap(response);
};
