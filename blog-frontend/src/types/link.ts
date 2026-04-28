// link.ts 定义友情链接和友链申请的数据结构。
// FriendLink 描述前后端交互中的一类业务数据。
export interface FriendLink {
  id: number;
  name: string;
  url: string;
  description: string;
  logo: string;
  status: number;
  sortOrder: number;
  createdAt: string;
}

// FriendLinkPayload 描述前后端交互中的一类业务数据。
export interface FriendLinkPayload {
  name: string;
  url: string;
  description: string;
  logo: string;
  status?: number;
  sortOrder?: number;
}
