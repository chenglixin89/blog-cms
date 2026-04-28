// adminUser.ts 定义后台用户管理相关的数据结构。
// AdminUser 描述前后端交互中的一类业务数据。
export interface AdminUser {
  id: number;
  username: string;
  nickname: string;
  email: string;
  avatar: string;
  bio: string;
  role: string;
  status: number;
  favoriteCount: number;
  likeCount: number;
  commentCount: number;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}
