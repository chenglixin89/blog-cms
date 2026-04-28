// taxonomy.ts 定义分类、标签以及对应表单提交的数据结构。
// Category 描述前后端交互中的一类业务数据。
export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string;
  sortOrder: number;
  articleCount: number;
  updatedAt: string;
}

// CategoryPayload 描述前后端交互中的一类业务数据。
export interface CategoryPayload {
  name: string;
  slug: string;
  description: string;
  sortOrder: number;
}

// Tag 描述前后端交互中的一类业务数据。
export interface Tag {
  id: number;
  name: string;
  color: string;
  articleCount: number;
  updatedAt: string;
}

// TagPayload 描述前后端交互中的一类业务数据。
export interface TagPayload {
  name: string;
  color: string;
}
