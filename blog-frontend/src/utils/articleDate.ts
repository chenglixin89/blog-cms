import type { Article } from "../types/article";

type ArticleDateSource = Pick<Article, "publishedAt" | "updatedAt" | "createdAt">;

export const getArticleDateValue = (article?: Partial<ArticleDateSource> | null) => {
  return article?.publishedAt || article?.updatedAt || article?.createdAt || "";
};

export const getArticleTimestamp = (article?: Partial<ArticleDateSource> | null) => {
  const value = getArticleDateValue(article);
  const timestamp = value ? new Date(value).getTime() : 0;
  return Number.isNaN(timestamp) ? 0 : timestamp;
};

export const formatArticleDate = (article?: Partial<ArticleDateSource> | null) => {
  const value = getArticleDateValue(article);
  if (!value) {
    return "暂无日期";
  }
  return new Date(value).toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  });
};

export const formatArticleShortDate = (article?: Partial<ArticleDateSource> | null) => {
  const value = getArticleDateValue(article);
  if (!value) {
    return "--";
  }
  return new Date(value).toLocaleDateString("zh-CN", {
    month: "2-digit",
    day: "2-digit"
  });
};

export const formatArticleDateTime = (article?: Partial<ArticleDateSource> | null) => {
  const value = getArticleDateValue(article);
  if (!value) {
    return "暂无日期";
  }
  return new Date(value).toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
};
