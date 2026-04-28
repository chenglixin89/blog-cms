// audit.ts 定义后台操作日志的数据结构。
// AuditLog 描述前后端交互中的一类业务数据。
export interface AuditLog {
  id: number;
  operator: string;
  module: string;
  action: string;
  targetType: string;
  targetId?: number | null;
  detail: string;
  ip: string;
  userAgent: string;
  createdAt: string;
}
