// media.ts 定义媒体库图片资源和媒体分类的数据结构。
// MediaAsset 描述前后端交互中的一类业务数据。
export interface MediaAsset {
  id: number;
  url: string;
  originalName: string;
  fileName: string;
  extension: string;
  contentType: string;
  size: number;
  category: string;
  createdAt: string;
}

// MediaCategory 描述前后端交互中的一类业务数据。
export interface MediaCategory {
  id: number;
  name: string;
  code: string;
  description: string;
  sortOrder: number;
  assetCount: number;
  isSystem: number;
  updatedAt: string;
}