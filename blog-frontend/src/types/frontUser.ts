// frontUser.ts 定义前台普通用户登录、注册、用户中心和个人资料的数据结构。
import type { Article } from "./article";
import type { CommentItem } from "./comment";

// FrontUserProfile 描述前后端交互中的一类业务数据。
export interface FrontUserProfile {
  id: number;
  username: string;
  nickname: string;
  email: string;
  avatar: string;
  bio: string;
}

// FrontLoginPayload 描述前后端交互中的一类业务数据。
export interface FrontLoginPayload {
  username: string;
  password: string;
}

// FrontRegisterPayload 描述前后端交互中的一类业务数据。
export interface FrontRegisterPayload extends FrontLoginPayload {
  nickname: string;
  email?: string;
}

// FrontAuthResult 描述前后端交互中的一类业务数据。
export interface FrontAuthResult {
  token: string;
  nickname: string;
}

// FrontProfilePayload 描述前后端交互中的一类业务数据。
export interface FrontProfilePayload {
  nickname: string;
  email: string;
  avatar: string;
  bio: string;
}

// FrontPasswordPayload 描述前后端交互中的一类业务数据。
export interface FrontPasswordPayload {
  oldPassword: string;
  newPassword: string;
}

// UserActivityStats 描述前后端交互中的一类业务数据。
export interface UserActivityStats {
  favoriteCount: number;
  likeCount: number;
  commentCount: number;
  pendingCommentCount: number;
  approvedCommentCount: number;
}

// UserCenterData 描述前后端交互中的一类业务数据。
export interface UserCenterData {
  profile: FrontUserProfile;
  stats: UserActivityStats;
  favorites: Article[];
  likes: Article[];
  comments: CommentItem[];
}
