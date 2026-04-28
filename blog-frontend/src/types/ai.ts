// ai.ts 定义 AI 技能、模型配置、运行结果和调用日志的数据结构。
// AiProvider 描述前后端交互中的一类业务数据。
export interface AiProvider {
  id: number;
  name: string;
  baseUrl: string;
  maskedApiKey: string;
  model: string;
  temperature: number;
  maxTokens: number;
  enabled: number;
  updatedAt: string;
}

// AiProviderPayload 描述前后端交互中的一类业务数据。
export interface AiProviderPayload {
  name: string;
  baseUrl: string;
  apiKey?: string;
  model: string;
  temperature: number;
  maxTokens: number;
  enabled: number;
}

// AiSkill 描述前后端交互中的一类业务数据。
export interface AiSkill {
  id: number;
  name: string;
  code: string;
  description: string;
  scene: string;
  promptTemplate: string;
  enabled: number;
  sortOrder: number;
  updatedAt: string;
}

// AiSkillPayload 描述前后端交互中的一类业务数据。
export interface AiSkillPayload {
  name: string;
  code: string;
  description: string;
  scene: string;
  promptTemplate: string;
  enabled: number;
  sortOrder: number;
}

// AiSkillRunPayload 描述前后端交互中的一类业务数据。
export interface AiSkillRunPayload {
  title?: string;
  summary?: string;
  content?: string;
  existingTags?: string[];
}

// AiSkillRunResult 描述前后端交互中的一类业务数据。
export interface AiSkillRunResult {
  skillCode: string;
  rawText: string;
  titles: string[];
  summary: string;
  existingTags: string[];
  newTags: string[];
  content: string;
  seoTitle: string;
  seoDescription: string;
  seoKeywords: string[];
  elapsedMs: number;
}

// AiCallLog 描述前后端交互中的一类业务数据。
export interface AiCallLog {
  id: number;
  providerName: string;
  model: string;
  skillCode: string;
  skillName: string;
  inputPreview: string;
  outputPreview: string;
  success: number;
  errorMessage: string;
  elapsedMs: number;
  promptTokens: number | null;
  completionTokens: number | null;
  totalTokens: number | null;
  createdAt: string;
}
// AiCallLogDetail 描述前后端交互中的一类业务数据。
export interface AiCallLogDetail extends AiCallLog {
  inputText: string;
  outputText: string;
}