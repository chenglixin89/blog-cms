// comment.ts 定义评论列表项和评论提交表单的数据结构。
// CommentItem 描述前后端交互中的一类业务数据。
export interface CommentItem {
  id: number;
  articleId: number;
  parentId: number | null;
  articleTitle: string;
  nickname: string;
  email: string;
  content: string;
  status: number;
  createdAt: string;
}

// CommentPayload 描述前后端交互中的一类业务数据。
export interface CommentPayload {
  parentId?: number | null;
  nickname: string;
  email: string;
  content: string;
}
