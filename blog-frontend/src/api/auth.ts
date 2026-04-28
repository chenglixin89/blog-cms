// auth.ts 封装后台管理员登录和修改密码接口。
import http from "./http";
import type { ApiResponse, LoginRequest, LoginResult } from "../types/api";

interface LoginPayload {
  token?: string;
  accessToken?: string;
  jwt?: string;
  nickname?: string;
  user?: {
    nickname?: string;
  };
  userInfo?: {
    nickname?: string;
  };
}

export interface AdminPasswordChangePayload {
  oldPassword: string;
  newPassword: string;
}

const SUCCESS_CODES = new Set([0, 200, "0", "200", "00000"]);

const getMessage = (response: ApiResponse<LoginPayload> | undefined): string =>
  response?.message ?? response?.msg ?? "\u767b\u5f55\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u8d26\u53f7\u6216\u5bc6\u7801";

const getToken = (payload: LoginPayload | undefined): string =>
  payload?.token ?? payload?.accessToken ?? payload?.jwt ?? "";

const getNickname = (payload: LoginPayload | undefined): string | undefined =>
  payload?.nickname ?? payload?.user?.nickname ?? payload?.userInfo?.nickname;

export const loginByPassword = async (params: LoginRequest): Promise<LoginResult> => {
  let response: ApiResponse<LoginPayload> | LoginPayload;
  try {
    response = (await http.post("/admin/login", params)) as ApiResponse<LoginPayload> | LoginPayload;
  } catch (error: any) {
    if (error instanceof Error && !(error as any).response) {
      throw error;
    }

    const status = error?.response?.status;
    const backendMessage =
      error?.response?.data?.message ??
      error?.response?.data?.msg ??
      error?.response?.data?.error;

    if (error?.response?.status === 404) {
      throw new Error("\u767b\u5f55\u63a5\u53e3 404\uff1a\u8bf7\u786e\u8ba4\u540e\u7aef\u5df2\u542f\u52a8\uff0c\u5e76\u68c0\u67e5\u63a5\u53e3\u662f\u5426\u4e3a /api/admin/login");
    }
    if (backendMessage) {
      throw new Error(`\u767b\u5f55\u5931\u8d25\uff08${status}\uff09\uff1a${backendMessage}`);
    }
    if (status) {
      throw new Error(`\u767b\u5f55\u5931\u8d25\uff08${status}\uff09\uff1a\u540e\u7aef\u670d\u52a1\u5f02\u5e38\uff0c\u8bf7\u67e5\u770b\u540e\u7aef\u65e5\u5fd7`);
    }
    throw new Error("\u767b\u5f55\u5931\u8d25\uff1a\u65e0\u6cd5\u8fde\u63a5\u5230\u540e\u7aef\u670d\u52a1");
  }

  const normalized = (response as ApiResponse<LoginPayload>).data
    ? (response as ApiResponse<LoginPayload>)
    : ({ code: 200, data: response as LoginPayload } as ApiResponse<LoginPayload>);

  if (normalized.code !== undefined && !SUCCESS_CODES.has(normalized.code)) {
    throw new Error(getMessage(normalized));
  }

  const token = getToken(normalized.data);
  if (!token) {
    throw new Error("\u767b\u5f55\u6210\u529f\u4f46\u672a\u83b7\u53d6\u5230 token\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u8fd4\u56de\u7ed3\u6784");
  }

  return {
    token,
    nickname: getNickname(normalized.data)
  };
};

export const changeAdminPassword = async (params: AdminPasswordChangePayload): Promise<void> => {
  await http.put("/admin/auth/password", params);
};
