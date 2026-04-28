<script setup lang="ts">
// ArticleDetail.vue 是前台文章详情页：负责文章渲染、阅读进度、点赞收藏、评论和相关推荐。
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import {
  addCloudFavorite,
  addCloudLike,
  getCloudFavoriteIds,
  getCloudLikeIds,
  getFrontArticleDetail,
  getFrontArticles,
  getFrontComments,
  likeFrontArticle,
  removeCloudFavorite,
  removeCloudLike,
  submitFrontComment,
  unlikeFrontArticle
} from "../../api/front";
import { getArticleDetail } from "../../api/article";
import { useFrontUserStore } from "../../stores/frontUser";
import type { Article } from "../../types/article";
import { formatArticleDate, formatArticleDateTime, getArticleTimestamp } from "../../utils/articleDate";
import type { CommentItem, CommentPayload } from "../../types/comment";

// ContentSegment 是把 Markdown 正文解析后的结构化片段，方便渲染标题、段落、引用、列表和代码块。
type ContentSegment =
  | { type: "heading"; id: string; level: number; text: string }
  | { type: "paragraph"; text: string }
  | { type: "quote"; text: string }
  | { type: "list"; ordered: boolean; items: string[] }
  | { type: "hr" }
  | { type: "code"; text: string };

interface ThreadComment extends CommentItem {
  depth: number;
}

// TEXT 集中管理页面中文文案，便于后续维护和国际化扩展。
const TEXT = {
  loading: "\u52a0\u8f7d\u4e2d...",
  pinned: "\u7f6e\u9876",
  uncategorized: "\u672a\u5206\u7c7b",
  noSummary: "\u6682\u65e0\u6458\u8981",
  readEstimate: "\u9884\u8ba1\u9605\u8bfb",
  minutes: "\u5206\u949f",
  reads: "\u9605\u8bfb",
  likes: "\u70b9\u8d5e",
  comments: "\u8bc4\u8bba",
  processing: "\u5904\u7406\u4e2d...",
  liked: "\u5df2\u70b9\u8d5e",
  likeArticle: "\u70b9\u8d5e\u6587\u7ae0",
  bookmarked: "\u5df2\u6536\u85cf",
  favoriteCloud: "\u6536\u85cf\u5230\u4e91\u7aef",
  favoriteLocal: "\u672c\u5730\u6536\u85cf",
  previous: "\u4e0a\u4e00\u7bc7\uff1a",
  next: "\u4e0b\u4e00\u7bc7\uff1a",
  noMore: "\u6ca1\u6709\u4e86",
  related: "\u76f8\u5173\u63a8\u8350",
  basedOn: "\u57fa\u4e8e\u5206\u7c7b\u4e0e\u6807\u7b7e",
  noArticleSummary: "\u8fd9\u7bc7\u6587\u7ae0\u8fd8\u6ca1\u6709\u6458\u8981\u3002",
  displayed: "\u6761\u5df2\u5c55\u793a",
  noApprovedComments: "\u8fd8\u6ca1\u6709\u5df2\u5ba1\u6838\u8bc4\u8bba\uff0c\u6765\u5199\u7b2c\u4e00\u6761\u5427\u3002",
  reply: "\u56de\u590d",
  replying: "\u6b63\u5728\u56de\u590d",
  cancelReply: "\u53d6\u6d88\u56de\u590d",
  nicknamePlaceholder: "\u6635\u79f0\uff08\u5fc5\u586b\uff09",
  emailPlaceholder: "\u90ae\u7bb1\uff08\u53ef\u9009\uff0c\u4e0d\u516c\u5f00\uff09",
  commentPlaceholder: "\u5199\u4e0b\u4f60\u7684\u8bc4\u8bba\uff0c\u5ba1\u6838\u540e\u5c55\u793a",
  submitting: "\u63d0\u4ea4\u4e2d...",
  submitComment: "\u63d0\u4ea4\u8bc4\u8bba",
  toc: "\u76ee\u5f55",
  backTop: "\u56de\u5230\u9876\u90e8",
  badArticleUrl: "\u6587\u7ae0\u5730\u5740\u4e0d\u6b63\u786e",
  favoriteFail: "\u6536\u85cf\u64cd\u4f5c\u5931\u8d25",
  likeFail: "\u70b9\u8d5e\u64cd\u4f5c\u5931\u8d25",
  detailLoadFail: "\u52a0\u8f7d\u6587\u7ae0\u8be6\u60c5\u5931\u8d25",
  commentLoadFail: "\u8bc4\u8bba\u52a0\u8f7d\u5931\u8d25",
  commentSubmitFail: "\u8bc4\u8bba\u63d0\u4ea4\u5931\u8d25",
  nickContentRequired: "\u6635\u79f0\u548c\u8bc4\u8bba\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a",
  commentSuccess: "\u8bc4\u8bba\u5df2\u63d0\u4ea4\uff0c\u7b49\u5f85\u7ba1\u7406\u5458\u5ba1\u6838\u540e\u5c55\u793a\u3002",
  commentApiMissing: "\u5f53\u524d\u8fd0\u884c\u7684\u540e\u7aef\u8fd8\u6ca1\u6709\u52a0\u8f7d\u8bc4\u8bba\u63a5\u53e3\uff0c\u8bf7\u91cd\u65b0\u7f16\u8bd1\u5e76\u91cd\u542f\u540e\u7aef\u3002",
  commentSubmitApiMissing: "\u5f53\u524d\u8fd0\u884c\u7684\u540e\u7aef\u8fd8\u6ca1\u6709\u52a0\u8f7d\u8bc4\u8bba\u63d0\u4ea4\u63a5\u53e3\uff0c\u8bf7\u91cd\u65b0\u7f16\u8bd1\u5e76\u91cd\u542f\u540e\u7aef\u3002",
  adminPreview: "\u7ba1\u7406\u5458\u9884\u89c8\uff1a\u5f53\u524d\u6b63\u5728\u4f7f\u7528\u540e\u53f0\u6570\u636e\u9884\u89c8\uff0c\u8349\u7a3f\u548c\u672a\u53d1\u5e03\u6587\u7ae0\u4e5f\u53ef\u89c1\u3002",
  backAdmin: "\u8fd4\u56de\u540e\u53f0"
};

const route = useRoute();
const router = useRouter();
const frontUserStore = useFrontUserStore();
const loading = ref(false);
const liking = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const article = ref<Article | null>(null);
const allArticles = ref<Article[]>([]);
const comments = ref<CommentItem[]>([]);
const readingProgress = ref(0);
const liked = ref(false);
const bookmarked = ref(false);
const bookmarking = ref(false);
const replyTarget = ref<CommentItem | null>(null);
const commentErrorText = ref("");
const activeHeadingId = ref("");
const tocPanelRef = ref<HTMLElement | null>(null);
const showBackToTop = ref(false);

// 从路由参数中读取文章 id，详情接口和评论接口都依赖它。
const articleId = computed(() => Number(route.params.id));
const isAdminPreview = computed(() => route.meta.adminPreview === true || route.query.preview === "admin" || route.path.startsWith("/admin/articles/"));
const likedKey = computed(() => "liked_article_" + articleId.value);
const commentDraftKey = computed(() => "comment_draft_" + articleId.value);
const bookmarkIdsKey = "bookmarked_article_ids";

// commentForm 保存评论表单内容，支持普通评论和回复评论。
const commentForm = reactive<CommentPayload>({
  parentId: null,
  nickname: "",
  email: "",
  content: ""
});

// 从 localStorage 恢复评论草稿，避免用户刷新页面后已输入内容丢失。
const loadCommentDraft = () => {
  try {
    const raw = localStorage.getItem(commentDraftKey.value);
    if (!raw) return;
    const draft = JSON.parse(raw) as Partial<CommentPayload>;
    commentForm.parentId = typeof draft.parentId === "number" ? draft.parentId : null;
    commentForm.nickname = String(draft.nickname ?? "");
    commentForm.email = String(draft.email ?? "");
    commentForm.content = String(draft.content ?? "");
  } catch {
    // Ignore invalid local draft.
  }
};

// 保存评论草稿，用户输入过程中自动持久化到本地。
const saveCommentDraft = () => {
  localStorage.setItem(commentDraftKey.value, JSON.stringify({
    parentId: commentForm.parentId,
    nickname: commentForm.nickname,
    email: commentForm.email,
    content: commentForm.content
  }));
};

const clearCommentDraft = () => {
  localStorage.removeItem(commentDraftKey.value);
};

// 未登录用户的收藏保存在浏览器本地，这里负责读取本地收藏 id 列表。
const getBookmarkedIds = () => {
  try {
    const raw = localStorage.getItem(bookmarkIdsKey);
    const parsed = raw ? (JSON.parse(raw) as number[]) : [];
    return parsed.filter((value) => Number.isFinite(value));
  } catch {
    return [];
  }
};

// 同步点赞状态：登录用户走云端接口，未登录用户走 localStorage。
const syncLikeState = async () => {
  if (!frontUserStore.isLoggedIn) {
    liked.value = localStorage.getItem(likedKey.value) === "1";
    return;
  }
  try {
    const ids = await getCloudLikeIds();
    liked.value = ids.includes(articleId.value);
  } catch {
    liked.value = false;
  }
};

// 同步收藏状态：登录用户走云端收藏，未登录用户走本地收藏。
const syncBookmarkState = async () => {
  if (!frontUserStore.isLoggedIn) {
    bookmarked.value = getBookmarkedIds().includes(articleId.value);
    return;
  }
  try {
    const ids = await getCloudFavoriteIds();
    bookmarked.value = ids.includes(articleId.value);
  } catch {
    bookmarked.value = false;
  }
};

// toggleBookmark 处理收藏和取消收藏，同时兼容登录和未登录两种状态。
const toggleBookmark = async () => {
  if (bookmarking.value) return;
  bookmarking.value = true;
  errorText.value = "";
  try {
    if (frontUserStore.isLoggedIn) {
      if (bookmarked.value) {
        await removeCloudFavorite(articleId.value);
        bookmarked.value = false;
      } else {
        await addCloudFavorite(articleId.value);
        bookmarked.value = true;
      }
      return;
    }
    const ids = getBookmarkedIds();
    const next = ids.includes(articleId.value)
      ? ids.filter((id) => id !== articleId.value)
      : [articleId.value, ...ids];
    localStorage.setItem(bookmarkIdsKey, JSON.stringify(next.slice(0, 200)));
    bookmarked.value = next.includes(articleId.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.favoriteFail;
  } finally {
    bookmarking.value = false;
  }
};

const formatDateTime = (value: string) =>
  new Date(value).toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });


// escapeHtml 对用户内容做转义，降低 Markdown 渲染时的 XSS 风险。
const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

// safeUrl 只允许 http、https、站内路径和锚点，避免危险链接进入页面。
const safeUrl = (value: string) => {
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (/^(https?:\/\/|\/|#)/i.test(trimmed)) {
    return escapeHtml(trimmed);
  }
  return "";
};

const stripMarkdown = (value: string) =>
  value
    .replace(/!\[([^\]]*)\]\([^)]+\)/g, "$1")
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/[\x60*_~]/g, "")
    .trim();

// renderInline 渲染行内 Markdown，例如图片、链接、加粗、斜体和代码。
const renderInline = (value: string) => {
  const codeStart = "@@CODE@@";
  const codeEnd = "@@ENDCODE@@";
  const normalized = value.replace(/`([^`]+)`/g, codeStart + "$1" + codeEnd);
  const pattern = /!\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)|\[([^\]]+)\]\(([^)\s]+)(?:\s+"[^"]*")?\)|@@CODE@@([\s\S]*?)@@ENDCODE@@|\*\*([^*]+)\*\*|__([^_]+)__|\*([^*]+)\*|_([^_]+)_/g;
  let html = "";
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(normalized)) !== null) {
    html += escapeHtml(normalized.slice(lastIndex, match.index));
    if (match[1] !== undefined) {
      const url = safeUrl(match[2]);
      html += url ? '<img src="' + url + '" alt="' + escapeHtml(match[1]) + '" loading="lazy" />' : escapeHtml(match[0]);
    } else if (match[3] !== undefined) {
      const url = safeUrl(match[4]);
      html += url ? '<a href="' + url + '" target="_blank" rel="noreferrer">' + renderInline(match[3]) + '</a>' : escapeHtml(match[3]);
    } else if (match[5] !== undefined) {
      html += '<code>' + escapeHtml(match[5]) + '</code>';
    } else if (match[6] !== undefined || match[7] !== undefined) {
      html += '<strong>' + renderInline(match[6] ?? match[7]) + '</strong>';
    } else if (match[8] !== undefined || match[9] !== undefined) {
      html += '<em>' + renderInline(match[8] ?? match[9]) + '</em>';
    } else {
      html += escapeHtml(match[0]);
    }
    lastIndex = pattern.lastIndex;
  }

  html += escapeHtml(normalized.slice(lastIndex));
  return html;
};

const parseContent = (content: string): ContentSegment[] => {
  const segments: ContentSegment[] = [];
  const lines = content.split(/\r?\n/);
  let paragraph: string[] = [];
  let code: string[] = [];
  let list: string[] = [];
  let listOrdered = false;
  let inCode = false;
  let headingIndex = 0;

  const flushParagraph = () => {
    if (paragraph.length) {
      segments.push({ type: "paragraph", text: paragraph.join(" ") });
      paragraph = [];
    }
  };

  const flushList = () => {
    if (list.length) {
      segments.push({ type: "list", ordered: listOrdered, items: list });
      list = [];
      listOrdered = false;
    }
  };

  lines.forEach((rawLine) => {
    const trimmed = rawLine.trim();
    if (trimmed.startsWith("```")) {
      flushParagraph();
      flushList();
      if (inCode) {
        segments.push({ type: "code", text: code.join("\n") });
        code = [];
        inCode = false;
      } else {
        inCode = true;
      }
      return;
    }
    if (inCode) {
      code.push(rawLine);
      return;
    }
    if (!trimmed) {
      flushParagraph();
      flushList();
      return;
    }
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      flushParagraph();
      flushList();
      segments.push({ type: "hr" });
      return;
    }
    const headingMatch = /^(#{1,4})\s+(.+)$/.exec(trimmed);
    if (headingMatch) {
      flushParagraph();
      flushList();
      segments.push({ type: "heading", id: "heading-" + headingIndex, level: headingMatch[1].length, text: stripMarkdown(headingMatch[2]) });
      headingIndex += 1;
      return;
    }
    const unorderedMatch = /^[-*+]\s+(.+)$/.exec(trimmed);
    const orderedMatch = /^\d+[.)]\s+(.+)$/.exec(trimmed);
    if (unorderedMatch || orderedMatch) {
      flushParagraph();
      const isOrdered = Boolean(orderedMatch);
      if (list.length && listOrdered !== isOrdered) {
        flushList();
      }
      listOrdered = isOrdered;
      list.push((unorderedMatch ?? orderedMatch)?.[1] ?? "");
      return;
    }
    if (trimmed.startsWith(">")) {
      flushParagraph();
      flushList();
      segments.push({ type: "quote", text: trimmed.replace(/^>\s?/, "") });
      return;
    }
    flushList();
    paragraph.push(trimmed);
  });

  flushParagraph();
  flushList();
  if (inCode && code.length) {
    segments.push({ type: "code", text: code.join("\n") });
  }
  return segments;
};
const contentSegments = computed(() => parseContent(article.value?.content ?? ""));
const tocItems = computed(() => contentSegments.value.filter((item) => item.type === "heading"));
const estimatedReadMinutes = computed(() => {
  const content = article.value?.content ?? "";
  const cjkCount = (content.match(/[\u4e00-\u9fff]/g) ?? []).length;
  const latinWordCount = (content.match(/[A-Za-z0-9_]+/g) ?? []).length;
  return Math.max(1, Math.ceil(cjkCount / 320 + latinWordCount / 180));
});
const rootComments = computed(() => comments.value.filter((item) => !item.parentId));
const currentIndex = computed(() => allArticles.value.findIndex((item) => item.id === articleId.value));
const previousArticle = computed(() => (currentIndex.value > 0 ? allArticles.value[currentIndex.value - 1] : null));
const nextArticle = computed(() => currentIndex.value >= 0 && currentIndex.value < allArticles.value.length - 1 ? allArticles.value[currentIndex.value + 1] : null);
const relatedArticles = computed(() => {
  if (!article.value) return [];
  const current = article.value;
  const currentTagIds = new Set(current.tags.map((tag) => tag.id));
  const scored = allArticles.value
    .filter((item) => item.id !== current.id)
    .map((item) => {
      const overlapTags = item.tags.filter((tag) => currentTagIds.has(tag.id)).length;
      const sameCategory = item.categoryId === current.categoryId ? 1 : 0;
      const score = sameCategory * 3 + overlapTags * 2 + (item.isPinned === 1 ? 0.5 : 0) + (item.viewCount ?? 0) / 10000;
      return { item, score };
    })
    .sort((a, b) => b.score - a.score || getArticleTimestamp(b.item) - getArticleTimestamp(a.item));
  const positive = scored.filter((entry) => entry.score > 0).slice(0, 4).map((entry) => entry.item);
  if (positive.length >= 3) return positive;
  const fallback = allArticles.value
    .filter((item) => item.id !== current.id)
    .sort((a, b) => getArticleTimestamp(b) - getArticleTimestamp(a))
    .slice(0, 4);
  return positive.length ? positive : fallback;
});

const descendantsOf = (parentId: number, depth = 1): ThreadComment[] => {
  return comments.value
    .filter((item) => item.parentId === parentId)
    .flatMap((item) => [{ ...item, depth }, ...descendantsOf(item.id, depth + 1)]);
};

const loadPage = async () => {
  if (!Number.isFinite(articleId.value)) {
    errorText.value = TEXT.badArticleUrl;
    return;
  }
  loading.value = true;
  errorText.value = "";
  successText.value = "";
  commentErrorText.value = "";
  replyTarget.value = null;
  try {
    if (isAdminPreview.value) {
      const detail = await getArticleDetail(articleId.value);
      article.value = detail;
      allArticles.value = [];
      comments.value = [];
      liked.value = false;
      bookmarked.value = false;
      return;
    }

    await Promise.all([syncLikeState(), syncBookmarkState()]);
    const [detail, articleList] = await Promise.all([getFrontArticleDetail(articleId.value), getFrontArticles()]);
    article.value = detail;
    allArticles.value = articleList;
    try {
      comments.value = await getFrontComments(articleId.value);
    } catch (error) {
      comments.value = [];
      commentErrorText.value = error instanceof Error && error.message.includes("No static resource")
        ? TEXT.commentApiMissing
        : error instanceof Error
          ? error.message
          : TEXT.commentLoadFail;
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.detailLoadFail;
  } finally {
    loading.value = false;
    loadCommentDraft();
    updateProgress();
    requestAnimationFrame(updateActiveHeading);
  }
};

const updateActiveHeading = () => {
  if (!tocItems.value.length) {
    activeHeadingId.value = "";
    return;
  }
  const offsetTop = 120;
  let current = tocItems.value[0]?.id ?? "";
  tocItems.value.forEach((item) => {
    const element = document.getElementById(item.id);
    if (element && element.getBoundingClientRect().top <= offsetTop) {
      current = item.id;
    }
  });
  if (activeHeadingId.value !== current) {
    activeHeadingId.value = current;
    requestAnimationFrame(() => {
      const activeButton = tocPanelRef.value?.querySelector<HTMLButtonElement>("button.active");
      activeButton?.scrollIntoView({ block: "nearest", inline: "nearest" });
    });
  }
};

const updateProgress = () => {
  const scrollTop = window.scrollY;
  const available = document.documentElement.scrollHeight - window.innerHeight;
  readingProgress.value = available <= 0 ? 0 : Math.min(100, Math.round((scrollTop / available) * 100));
  showBackToTop.value = scrollTop > 420;
  updateActiveHeading();
};

const scrollToHeading = (id: string) => {
  activeHeadingId.value = id;
  document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
};

const backToTop = () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
};

const toggleLike = async () => {
  if (liking.value) return;
  liking.value = true;
  errorText.value = "";
  try {
    if (frontUserStore.isLoggedIn) {
      if (liked.value) {
        article.value = await removeCloudLike(articleId.value);
        liked.value = false;
      } else {
        article.value = await addCloudLike(articleId.value);
        liked.value = true;
      }
      return;
    }
    if (liked.value) {
      article.value = await unlikeFrontArticle(articleId.value);
      localStorage.removeItem(likedKey.value);
      liked.value = false;
    } else {
      article.value = await likeFrontArticle(articleId.value);
      localStorage.setItem(likedKey.value, "1");
      liked.value = true;
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.likeFail;
  } finally {
    liking.value = false;
  }
};

const startReply = (comment: CommentItem) => {
  replyTarget.value = comment;
  commentForm.parentId = comment.id;
  if (!commentForm.content.trim()) {
    commentForm.content = "@" + comment.nickname + "\uff1a";
  }
  saveCommentDraft();
  document.getElementById("comment-form")?.scrollIntoView({ behavior: "smooth", block: "center" });
};

const cancelReply = () => {
  replyTarget.value = null;
  commentForm.parentId = null;
  saveCommentDraft();
};

const submitComment = async () => {
  successText.value = "";
  if (!commentForm.nickname.trim() || !commentForm.content.trim()) {
    errorText.value = TEXT.nickContentRequired;
    return;
  }
  submitting.value = true;
  errorText.value = "";
  commentErrorText.value = "";
  try {
    await submitFrontComment(articleId.value, commentForm);
    commentForm.parentId = null;
    commentForm.nickname = "";
    commentForm.email = "";
    commentForm.content = "";
    replyTarget.value = null;
    clearCommentDraft();
    successText.value = TEXT.commentSuccess;
  } catch (error) {
    commentErrorText.value = error instanceof Error && error.message.includes("No static resource")
      ? TEXT.commentSubmitApiMissing
      : error instanceof Error
        ? error.message
        : TEXT.commentSubmitFail;
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadPage();
  updateProgress();
  window.addEventListener("scroll", updateProgress, { passive: true });
});

watch(articleId, () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
  activeHeadingId.value = "";
  loadPage();
});

watch(
  () => [commentForm.parentId, commentForm.nickname, commentForm.email, commentForm.content],
  () => {
    saveCommentDraft();
  }
);

onBeforeUnmount(() => {
  window.removeEventListener("scroll", updateProgress);
});
</script>

<template>
  <main class="front-page">
    <div class="reading-progress" :style="{ width: readingProgress + '%' }"></div>
    <FrontHeader />
    <div v-if="isAdminPreview" class="admin-preview-banner glass">
      <span>{{ TEXT.adminPreview }}</span>
      <button class="ghost-btn" type="button" @click="router.push('/admin/articles')">{{ TEXT.backAdmin }}</button>
    </div>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading" class="muted">{{ TEXT.loading }}</p>
    <section v-else-if="article" class="front-detail-layout">
      <article class="front-detail">
        <img v-if="article.coverImage" class="detail-cover" :src="article.coverImage" :alt="article.title" />
        <p class="front-category"><span v-if="article.isPinned === 1" class="pin-badge">{{ TEXT.pinned }}</span>{{ article.categoryName || TEXT.uncategorized }}</p>
        <h1>{{ article.title }}</h1>
        <p class="front-summary">{{ article.summary || TEXT.noSummary }}</p>
        <div class="front-card-footer">
          <span>{{ formatArticleDateTime(article) }}</span>
          <span>{{ TEXT.readEstimate }} {{ estimatedReadMinutes }} {{ TEXT.minutes }}</span>
          <span>{{ article.viewCount }} {{ TEXT.reads }} &middot; {{ article.likeCount }} {{ TEXT.likes }} &middot; {{ article.commentCount }} {{ TEXT.comments }}</span>
          <div class="mini-tag-row">
            <RouterLink v-for="tag in article.tags" :key="tag.id" class="mini-tag" :style="{ borderColor: tag.color }" :to="'/tags/' + tag.id">{{ tag.name }}</RouterLink>
          </div>
        </div>
        <div v-if="!isAdminPreview" class="detail-action-row">
          <button class="primary-btn like-btn" :class="{ liked }" :disabled="liking" @click="toggleLike">{{ liking ? TEXT.processing : liked ? TEXT.liked : TEXT.likeArticle }}</button>
          <button class="ghost-btn bookmark-btn" :class="{ bookmarked }" type="button" :disabled="bookmarking" @click="toggleBookmark">{{ bookmarking ? TEXT.processing : bookmarked ? TEXT.bookmarked : frontUserStore.isLoggedIn ? TEXT.favoriteCloud : TEXT.favoriteLocal }}</button>
        </div>
        <div class="markdown-body">
          <template v-for="(segment, index) in contentSegments" :key="segment.type + '-' + index">
            <h1 v-if="segment.type === 'heading' && segment.level === 1" :id="segment.id">{{ segment.text }}</h1>
            <h2 v-else-if="segment.type === 'heading' && segment.level === 2" :id="segment.id">{{ segment.text }}</h2>
            <h3 v-else-if="segment.type === 'heading' && segment.level === 3" :id="segment.id">{{ segment.text }}</h3>
            <h4 v-else-if="segment.type === 'heading'" :id="segment.id">{{ segment.text }}</h4>
            <p v-else-if="segment.type === 'paragraph'" v-html="renderInline(segment.text)"></p>
            <blockquote v-else-if="segment.type === 'quote'" v-html="renderInline(segment.text)"></blockquote>
            <ol v-else-if="segment.type === 'list' && segment.ordered"><li v-for="item in segment.items" :key="item" v-html="renderInline(item)"></li></ol>
            <ul v-else-if="segment.type === 'list'"><li v-for="item in segment.items" :key="item" v-html="renderInline(item)"></li></ul>
            <hr v-else-if="segment.type === 'hr'" />
            <pre v-else-if="segment.type === 'code'"><code>{{ segment.text }}</code></pre>
          </template>
        </div>
        <nav v-if="!isAdminPreview" class="article-neighbor">
          <button class="ghost-btn" :disabled="!previousArticle" @click="previousArticle && router.push('/articles/' + previousArticle.id)">{{ TEXT.previous }}{{ previousArticle?.title || TEXT.noMore }}</button>
          <button class="ghost-btn" :disabled="!nextArticle" @click="nextArticle && router.push('/articles/' + nextArticle.id)">{{ TEXT.next }}{{ nextArticle?.title || TEXT.noMore }}</button>
        </nav>
        <section v-if="!isAdminPreview && relatedArticles.length" class="related-section">
          <div class="section-title-row"><h2>{{ TEXT.related }}</h2><span>{{ TEXT.basedOn }}</span></div>
          <div class="related-grid">
            <article v-for="item in relatedArticles" :key="item.id" class="related-card" @click="router.push('/articles/' + item.id)">
              <p class="front-category">{{ item.categoryName || TEXT.uncategorized }}</p>
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary || TEXT.noArticleSummary }}</p>
              <div class="front-card-footer"><span>{{ formatArticleDate(item) }}</span><span>{{ item.viewCount }} {{ TEXT.reads }} &middot; {{ item.likeCount }} {{ TEXT.likes }}</span></div>
            </article>
          </div>
        </section>
        <section v-if="!isAdminPreview" class="front-comments">
          <div class="section-title-row"><h2>{{ TEXT.comments }}</h2><span>{{ comments.length }} {{ TEXT.displayed }}</span></div>
          <p v-if="commentErrorText" class="error-text">{{ commentErrorText }}</p>
          <p v-if="comments.length === 0" class="muted">{{ TEXT.noApprovedComments }}</p>
          <article v-for="item in rootComments" :key="item.id" class="front-comment">
            <div class="comment-line"><strong>{{ item.nickname }}</strong><span>{{ formatDateTime(item.createdAt) }}</span></div>
            <p>{{ item.content }}</p>
            <button class="text-btn" @click="startReply(item)">{{ TEXT.reply }}</button>
            <div v-if="descendantsOf(item.id).length" class="comment-replies">
              <article v-for="child in descendantsOf(item.id)" :key="child.id" class="front-comment child-comment" :style="{ marginLeft: Math.min(child.depth - 1, 3) * 14 + 'px' }">
                <div class="comment-line"><strong>{{ child.nickname }}</strong><span>{{ formatDateTime(child.createdAt) }}</span><small>{{ TEXT.reply }} #{{ child.parentId }}</small></div>
                <p>{{ child.content }}</p>
                <button class="text-btn" @click="startReply(child)">{{ TEXT.reply }}</button>
              </article>
            </div>
          </article>
          <form id="comment-form" class="comment-form" @submit.prevent="submitComment">
            <div v-if="replyTarget" class="reply-banner">{{ TEXT.replying }} {{ replyTarget.nickname }}<button type="button" class="text-btn" @click="cancelReply">{{ TEXT.cancelReply }}</button></div>
            <input v-model="commentForm.nickname" type="text" :placeholder="TEXT.nicknamePlaceholder" />
            <input v-model="commentForm.email" type="email" :placeholder="TEXT.emailPlaceholder" />
            <textarea v-model="commentForm.content" :placeholder="TEXT.commentPlaceholder"></textarea>
            <p v-if="successText" class="success-text">{{ successText }}</p>
            <button class="primary-btn" type="submit" :disabled="submitting">{{ submitting ? TEXT.submitting : TEXT.submitComment }}</button>
          </form>
        </section>
      </article>
      <aside ref="tocPanelRef" class="toc-panel" v-if="tocItems.length">
        <h3>{{ TEXT.toc }}</h3>
        <button v-for="item in tocItems" :key="item.id" :class="['toc-level-' + item.level, { active: activeHeadingId === item.id }]" @click="scrollToHeading(item.id)">{{ item.text }}</button>
      </aside>
    </section>
    <button v-show="showBackToTop" class="back-to-top" type="button" @click="backToTop">{{ TEXT.backTop }}</button>
  </main>
</template>
