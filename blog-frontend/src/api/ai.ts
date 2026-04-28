// ai.ts 封装后台 Skills 管理接口，包括技能配置、模型调用和调用日志。
import http from "./http";
import type { ApiResponse, PageResponse } from "../types/api";
import type { AiCallLog, AiCallLogDetail, AiProvider, AiProviderPayload, AiSkill, AiSkillPayload, AiSkillRunPayload, AiSkillRunResult } from "../types/ai";

const unwrap = <T>(response: ApiResponse<T> | T): T => {
  if ((response as ApiResponse<T>).data !== undefined) {
    return (response as ApiResponse<T>).data as T;
  }
  return response as T;
};

// getAiProvider 调用后端查询接口并返回页面需要的数据。
export const getAiProvider = async (): Promise<AiProvider> => {
  const response = (await http.get("/admin/ai/provider")) as ApiResponse<AiProvider> | AiProvider;
  return unwrap(response);
};

// updateAiProvider 调用后端更新接口。
export const updateAiProvider = async (payload: AiProviderPayload): Promise<AiProvider> => {
  const response = (await http.put("/admin/ai/provider", payload)) as ApiResponse<AiProvider> | AiProvider;
  return unwrap(response);
};

export const testAiProvider = async (): Promise<AiSkillRunResult> => {
  const response = (await http.post("/admin/ai/provider/test", undefined, { timeout: 120000 })) as ApiResponse<AiSkillRunResult> | AiSkillRunResult;
  return unwrap(response);
};

// getAiSkills 调用后端查询接口并返回页面需要的数据。
export const getAiSkills = async (): Promise<AiSkill[]> => {
  const response = (await http.get("/admin/ai/skills")) as ApiResponse<AiSkill[]> | AiSkill[];
  return unwrap(response);
};

// createAiSkill 调用后端新增接口。
export const createAiSkill = async (payload: AiSkillPayload): Promise<AiSkill> => {
  const response = (await http.post("/admin/ai/skills", payload)) as ApiResponse<AiSkill> | AiSkill;
  return unwrap(response);
};

// updateAiSkill 调用后端更新接口。
export const updateAiSkill = async (id: number, payload: AiSkillPayload): Promise<AiSkill> => {
  const response = (await http.put(`/admin/ai/skills/${id}`, payload)) as ApiResponse<AiSkill> | AiSkill;
  return unwrap(response);
};

// deleteAiSkill 调用后端删除接口。
export const deleteAiSkill = async (id: number): Promise<void> => {
  await http.delete(`/admin/ai/skills/${id}`);
};

// runAiSkill 调用后端 AI 技能运行接口，文章编辑页的一键生成能力依赖它。
export const runAiSkill = async (code: string, payload: AiSkillRunPayload): Promise<AiSkillRunResult> => {
  const response = (await http.post(`/admin/ai/skills/${code}/run`, payload, { timeout: 120000 })) as ApiResponse<AiSkillRunResult> | AiSkillRunResult;
  return unwrap(response);
};

// getAiLogPage 调用后端查询接口并返回页面需要的数据。
export const getAiLogPage = async (query: { keyword?: string; success?: number | null; page?: number; size?: number } = {}): Promise<PageResponse<AiCallLog>> => {
  const params: Record<string, string | number> = {};
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") params[key] = value;
  });
  const response = (await http.get("/admin/ai/logs/page", { params })) as ApiResponse<PageResponse<AiCallLog>> | PageResponse<AiCallLog>;
  return unwrap(response);
};
// getAiLogDetail 调用后端查询接口并返回页面需要的数据。
export const getAiLogDetail = async (id: number): Promise<AiCallLogDetail> => {
  const response = (await http.get(`/admin/ai/logs/${id}`)) as ApiResponse<AiCallLogDetail> | AiCallLogDetail;
  return unwrap(response);
};
