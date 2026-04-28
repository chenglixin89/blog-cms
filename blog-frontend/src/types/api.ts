// api.ts 定义后端统一响应结构和分页响应结构。
﻿// ApiResponse 描述前后端交互中的一类业务数据。
export interface ApiResponse<T> {
  code?: number | string;
  message?: string;
  msg?: string;
  data?: T;
}

// LoginRequest 描述前后端交互中的一类业务数据。
export interface LoginRequest {
  username: string;
  password: string;
}

// LoginResult 描述前后端交互中的一类业务数据。
export interface LoginResult {
  token: string;
  nickname?: string;
}

// PageResponse 描述前后端交互中的一类业务数据。
export interface PageResponse<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}
