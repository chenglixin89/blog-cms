// article.ts 定义文章、文章标签和文章表单提交的数据结构。
// ArticleTag 描述前后端交互中的一类业务数据。
export interface ArticleTag {
  id: number;
  name: string;
  color: string;
}

// Article 描述前后端交互中的一类业务数据。
export interface Article {
  id: number;
  title: string;
  summary: string;
  seoTitle: string;
  seoDescription: string;
  seoKeywords: string;
  content: string;
  coverImage: string;
  categoryId: number | null;
  categoryName: string | null;
  tags: ArticleTag[];
  status: number;
  isPinned: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createdAt?: string;
  publishedAt?: string | null;
  updatedAt: string;
}

// ArticlePayload 描述前后端交互中的一类业务数据。
export interface ArticlePayload {
  title: string;
  summary: string;
  seoTitle: string;
  seoDescription: string;
  seoKeywords: string;
  content: string;
  coverImage: string;
  categoryId: number | null;
  tagIds: number[];
  status: number;
  isPinned: number;
}
