// message.ts 定义留言板留言项和留言提交表单的数据结构。
// GuestbookMessage 描述前后端交互中的一类业务数据。
export interface GuestbookMessage {
  id: number;
  parentId: number | null;
  nickname: string;
  email?: string;
  content: string;
  status: number;
  createdAt: string;
  loggedInUser?: boolean;
}

// GuestbookMessagePayload 描述前后端交互中的一类业务数据。
export interface GuestbookMessagePayload {
  parentId?: number | null;
  nickname?: string;
  email?: string;
  content: string;
}