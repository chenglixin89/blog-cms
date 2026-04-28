// dashboard.ts 封装后台仪表盘统计接口，包括总览数据和发布趋势。
import http from "./http";
import type { ApiResponse } from "../types/api";

export interface DashboardStats {
  articleCount: number;
  publishedCount: number;
  draftCount: number;
  categoryCount: number;
  tagCount: number;
  pendingCommentCount: number;
}

export interface DashboardTrendPoint {
  key: string;
  label: string;
  value: number;
}

// getDashboardStats 调用后端查询接口并返回页面需要的数据。
export const getDashboardStats = async (): Promise<DashboardStats> => {
  const response = (await http.get("/admin/dashboard/stats")) as ApiResponse<DashboardStats> | DashboardStats;
  if ((response as ApiResponse<DashboardStats>).data !== undefined) {
    return (response as ApiResponse<DashboardStats>).data as DashboardStats;
  }
  return response as DashboardStats;
};

// getDashboardPublishTrend 调用后端查询接口并返回页面需要的数据。
export const getDashboardPublishTrend = async (): Promise<DashboardTrendPoint[]> => {
  const response = (await http.get("/admin/dashboard/publish-trend")) as ApiResponse<DashboardTrendPoint[]> | DashboardTrendPoint[];
  if ((response as ApiResponse<DashboardTrendPoint[]>).data !== undefined) {
    return (response as ApiResponse<DashboardTrendPoint[]>).data as DashboardTrendPoint[];
  }
  return response as DashboardTrendPoint[];
};