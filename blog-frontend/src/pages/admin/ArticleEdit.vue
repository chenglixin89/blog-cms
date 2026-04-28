<script setup lang="ts">
// ArticleEdit.vue 是后台文章编辑页：支持文章保存、封面上传、媒体库选图、Markdown 编辑和 AI 写作助手。
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { createArticle, getArticleDetail, updateArticle, uploadArticleCover } from "../../api/article";
import { runAiSkill } from "../../api/ai";
import { getMediaCategories, getMediaPage, uploadMedia } from "../../api/media";
import { createTag, getCategoryList, getTagList } from "../../api/taxonomy";
import type { AiSkillRunResult } from "../../types/ai";
import type { ArticlePayload } from "../../types/article";
import type { MediaAsset, MediaCategory } from "../../types/media";
import type { Category, Tag } from "../../types/taxonomy";

// TEXT 统一管理本页面所有提示文案，避免模板中散落大量字符串。
const TEXT = {
  editArticle: "\u7f16\u8f91\u6587\u7ae0",
  newArticle: "\u65b0\u5efa\u6587\u7ae0",
  desc: "\u652f\u6301\u5c01\u9762\u3001\u5206\u7c7b\u3001\u6807\u7b7e\u3001\u7f6e\u9876\u3001\u004d\u0061\u0072\u006b\u0064\u006f\u0077\u006e \u548c \u0041\u0049 \u5199\u4f5c\u52a9\u624b\u3002",
  openMedia: "\u6253\u5f00\u5a92\u4f53\u5e93",
  backList: "\u8fd4\u56de\u5217\u8868",
  loading: "\u52a0\u8f7d\u4e2d...",
  title: "\u6807\u9898",
  summary: "\u6458\u8981",
  cover: "\u5c01\u9762\u56fe",
  category: "\u5206\u7c7b",
  seoBlock: "SEO \u4fe1\u606f",
  seoTitle: "SEO \u6807\u9898",
  seoDescription: "SEO \u63cf\u8ff0",
  seoKeywords: "SEO \u5173\u952e\u8bcd",
  uncategorized: "\u672a\u5206\u7c7b",
  status: "\u72b6\u6001",
  draft: "\u8349\u7a3f",
  published: "\u5df2\u53d1\u5e03",
  archived: "\u5df2\u5f52\u6863",
  pinned: "\u524d\u53f0\u7f6e\u9876\u5c55\u793a",
  tags: "\u6807\u7b7e",
  body: "\u6b63\u6587\uff08\u004d\u0061\u0072\u006b\u0064\u006f\u0077\u006e\uff09",
  bodyPlaceholder: "\u5728\u8fd9\u91cc\u8f93\u5165 \u004d\u0061\u0072\u006b\u0064\u006f\u0077\u006e \u5185\u5bb9",
  uploadLocal: "\u4e0a\u4f20\u672c\u5730\u56fe\u7247",
  uploading: "\u4e0a\u4f20\u4e2d...",
  uploadCoverOk: "\u5c01\u9762\u56fe\u4e0a\u4f20\u6210\u529f\uff0c\u5df2\u81ea\u52a8\u586b\u5165 URL\u3002",
  uploadContentOk: "\u6b63\u6587\u56fe\u7247\u5df2\u4e0a\u4f20\u5e76\u63d2\u5165\u3002",
  uploadContent: "\u4e0a\u4f20\u63d2\u56fe",
  pickMedia: "\u4ece\u5a92\u4f53\u5e93\u9009\u62e9",
  saveArticle: "\u4fdd\u5b58\u6587\u7ae0",
  saving: "\u4fdd\u5b58\u4e2d...",
  previewTitle: "\u6587\u7ae0\u6807\u9898\u9884\u89c8",
  previewSummary: "\u8fd9\u91cc\u4f1a\u5c55\u793a\u6587\u7ae0\u6458\u8981\uff0c\u65b9\u4fbf\u53d1\u5e03\u524d\u5feb\u901f\u68c0\u67e5\u524d\u53f0\u5361\u7247\u6548\u679c\u3002",
  articlePreview: "\u6587\u7ae0\u9884\u89c8",
  pinnedArticle: "\u7f6e\u9876\u6587\u7ae0",
  coverPreview: "\u5c01\u9762\u9884\u89c8",
  localFileWarn: "\u5f53\u524d\u662f file:/// \u672c\u5730\u8def\u5f84\uff0c\u7f51\u9875\u65e0\u6cd5\u52a0\u8f7d\uff0c\u8bf7\u6539\u7528\u4e0a\u4f20\u540e\u7684 HTTP \u5730\u5740\u3002",
  imageTip: "\u652f\u6301 jpg / jpeg / png / gif / webp\u3002\u4e0a\u4f20\u540e\u7684\u56fe\u7247\u4f1a\u81ea\u52a8\u8fdb\u5165\u5a92\u4f53\u5e93\u3002",
  mediaTitle: "\u9009\u62e9\u5a92\u4f53\u56fe\u7247",
  mediaDesc: "图片较多时采用分页加载，可搜索文件名或按分类筛选。",
  close: "\u5173\u95ed",
  mediaLoading: "\u5a92\u4f53\u52a0\u8f7d\u4e2d...",
  noMedia: "当前条件下没有图片，请尝试更换搜索词或分类。",
  mediaSearchPlaceholder: "搜索图片名称",
  mediaSearch: "搜索",
  allMedia: "全部图片",
  contentMedia: "正文插图",
  coverMedia: "封面图片",
  generalMedia: "通用图片",
  loadMore: "加载更多",
  mediaLoaded: "已显示",
  titleRequired: "\u6807\u9898\u548c\u6b63\u6587\u4e0d\u80fd\u4e3a\u7a7a\u3002",
  fileUrlError: "\u4e0d\u80fd\u76f4\u63a5\u4f7f\u7528 file:/// \u672c\u5730\u8def\u5f84\uff0c\u8bf7\u5148\u4e0a\u4f20\u672c\u5730\u56fe\u7247\u3002",
  saveFail: "\u4fdd\u5b58\u5931\u8d25",
  loadFail: "\u52a0\u8f7d\u6587\u7ae0\u7f16\u8f91\u6570\u636e\u5931\u8d25",
  uploadFail: "\u56fe\u7247\u4e0a\u4f20\u5931\u8d25",
  sizeLimit: "\u56fe\u7247\u4e0d\u80fd\u8d85\u8fc7 5MB\u3002",
  aiAssistant: "AI \u52a9\u624b",
  aiTitle: "\u751f\u6210\u6807\u9898",
  aiSummary: "\u751f\u6210\u6458\u8981",
  aiTags: "\u63a8\u8350\u6807\u7b7e",
  aiPolish: "\u6da6\u8272\u6b63\u6587",
  aiOutline: "\u751f\u6210\u5927\u7eb2",
  aiExpand: "\u6269\u5199\u6b63\u6587",
  aiContinue: "\u7eed\u5199\u5185\u5bb9",
  aiSeo: "\u751f\u6210 SEO",
  aiRunning: "AI \u751f\u6210\u4e2d...",
  aiResult: "AI \u751f\u6210\u7ed3\u679c",
  apply: "\u5e94\u7528",
  append: "\u8ffd\u52a0\u5230\u6b63\u6587",
  applySeo: "\u5e94\u7528 SEO",
  applyExistingTags: "\u5e94\u7528\u5df2\u6709\u6807\u7b7e",
  copy: "\u590d\u5236",
  raw: "\u539f\u59cb\u8f93\u51fa",
  noExistingTags: "\u6ca1\u6709\u5339\u914d\u5230\u5df2\u6709\u6807\u7b7e\u3002",
  newTags: "\u65b0\u6807\u7b7e\u5efa\u8bae",
  createNewTags: "\u521b\u5efa\u5e76\u9009\u4e2d\u65b0\u6807\u7b7e",
  creatingTags: "\u521b\u5efa\u4e2d...",
  createTagsOk: "\u65b0\u6807\u7b7e\u5df2\u521b\u5efa\u5e76\u9009\u4e2d\u3002",
  colon: "\uff1a",
  aiNeedContent: "\u8bf7\u5148\u8f93\u5165\u6807\u9898\u6216\u6b63\u6587\u5185\u5bb9\u3002"
};

const route = useRoute();
const router = useRouter();
// 从路由参数读取文章 id，有 id 表示编辑文章，没有 id 表示新建文章。
const articleId = computed(() => Number(route.params.id));
const isEdit = computed(() => Number.isFinite(articleId.value) && articleId.value > 0);

const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const mediaUploading = ref(false);
const mediaLoading = ref(false);
const mediaPickerOpen = ref(false);
const aiRunning = ref("");
const aiResultOpen = ref(false);
const creatingAiTags = ref(false);
const aiResultTitle = ref("");
const aiResultType = ref("");
const aiResult = ref<AiSkillRunResult | null>(null);
const errorText = ref("");
const uploadText = ref("");
const maxCoverSize = 5 * 1024 * 1024;
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);
const mediaList = ref<MediaAsset[]>([]);
const mediaCategories = ref<MediaCategory[]>([]);
const mediaKeyword = ref("");
const mediaCategory = ref("");
const mediaPage = ref(1);
const mediaTotal = ref(0);
const mediaTotalPages = ref(0);
const mediaPageSize = 24;
const imageExtensions = new Set(["jpg", "jpeg", "png", "gif", "webp"]);
const textareaRef = ref<HTMLTextAreaElement | null>(null);

// form 是文章编辑表单，和后端 ArticlePayload 字段保持一致。
const form = reactive<ArticlePayload>({ title: "", summary: "", seoTitle: "", seoDescription: "", seoKeywords: "", content: "", coverImage: "", categoryId: null, tagIds: [], status: 0, isPinned: 0 });

const tagPalette = ["#0a84ff", "#34c759", "#ff9500", "#ff2d55", "#af52de"];
const aiTitleMap: Record<string, string> = {
  article_title: TEXT.aiTitle,
  article_summary: TEXT.aiSummary,
  article_tags: TEXT.aiTags,
  article_polish: TEXT.aiPolish,
  article_outline: TEXT.aiOutline,
  article_expand: TEXT.aiExpand,
  article_continue: TEXT.aiContinue,
  article_seo: TEXT.aiSeo
};

// markdownTools 定义编辑器工具栏按钮，点击后会向正文插入对应 Markdown 语法。
const markdownTools = [
  { label: "H2", before: "\n## ", after: "" },
  { label: "\u52a0\u7c97", before: "**", after: "**" },
  { label: "\u5f15\u7528", before: "\n> ", after: "" },
  { label: "\u5217\u8868", before: "\n- ", after: "" },
  { label: "\u4ee3\u7801", before: "\n```\n", after: "\n```" },
  { label: "\u94fe\u63a5", before: "[", after: "](https://)" }
];

const wordCount = computed(() => form.content.trim().length);
const mediaHasMore = computed(() => mediaPage.value < mediaTotalPages.value);
const previewTitle = computed(() => form.title.trim() || TEXT.previewTitle);
const previewSummary = computed(() => form.summary.trim() || TEXT.previewSummary);
const isLocalFileUrl = computed(() => form.coverImage.trim().toLowerCase().startsWith("file://"));
// matchedAiTags 从 AI 返回的标签中找出系统里已经存在的标签。
const matchedAiTags = computed(() => {
  const names = new Set([...(aiResult.value?.existingTags ?? []), ...(aiResult.value?.newTags ?? [])].map((item) => item.toLowerCase()));
  return tags.value.filter((tag) => names.has(tag.name.toLowerCase()));
});
// unmatchedNewTags 提取 AI 推荐但系统里还不存在的新标签。
const unmatchedNewTags = computed(() => {
  const existingNames = new Set(tags.value.map((tag) => tag.name.trim().toLowerCase()));
  const seen = new Set<string>();
  return (aiResult.value?.newTags ?? [])
    .map((name) => name.trim())
    .filter((name) => {
      const key = name.toLowerCase();
      if (!name || existingNames.has(key) || seen.has(key)) return false;
      seen.add(key);
      return true;
    });
});

// cleanAiText 清理 AI 输出中的代码块、JSON 外壳和多余提示，保证写回文章时格式干净。
const cleanAiText = (value?: string) => {
  let text = (value ?? "").replace(/\r\n/g, "\n").trim();
  text = text.replace(/^\s*markdown\s+draft\s*[:\uFF1A]?\s*/i, "").trim();
  const fenced = /^```(?:markdown|md|text|json)?\s*\n([\s\S]*?)\n?```$/i.exec(text);
  if (fenced) {
    text = fenced[1].trim();
  }
  text = text
    .replace(/^```(?:markdown|md|text|json)?\s*\n?/i, "")
    .replace(/\n?```\s*$/i, "")
    .replace(/^\s*markdown\s+draft\s*[:\uFF1A]?\s*/i, "")
    .trim();

  const jsonStart = text.indexOf("{");
  const jsonEnd = text.lastIndexOf("}");
  if (jsonStart >= 0 && jsonEnd > jsonStart) {
    try {
      const parsed = JSON.parse(text.slice(jsonStart, jsonEnd + 1)) as Record<string, unknown>;
      const picked = parsed.content ?? parsed.summary ?? parsed.title ?? parsed.seoTitle ?? parsed.seoDescription;
      if (typeof picked === "string") {
        return cleanAiText(picked);
      }
    } catch {
      // Keep markdown text as-is when the model did not return valid JSON.
    }
  }

  return text;
};

// normalizeAiResult 对 AI 返回结果做统一规范化，便于标题、摘要、正文和 SEO 共用。
const normalizeAiResult = (result: AiSkillRunResult): AiSkillRunResult => ({
  ...result,
  titles: result.titles.map((item) => cleanAiText(item)).filter(Boolean),
  summary: cleanAiText(result.summary),
  content: cleanAiText(result.content),
  seoTitle: cleanAiText(result.seoTitle),
  seoDescription: cleanAiText(result.seoDescription),
  seoKeywords: result.seoKeywords.map((item) => cleanAiText(item)).filter(Boolean)
});
// loadOptions 加载分类和标签，下拉框和标签选择器依赖这些数据。
const loadOptions = async () => {
  const [categoryData, tagData] = await Promise.all([getCategoryList(), getTagList()]);
  categories.value = categoryData;
  tags.value = tagData;
};

// loadDetail 在编辑模式下加载文章详情，并回填到表单。
const loadDetail = async () => {
  if (!isEdit.value) return;
  const detail = await getArticleDetail(articleId.value);
  form.title = detail.title;
  form.summary = detail.summary;
  form.seoTitle = detail.seoTitle || "";
  form.seoDescription = detail.seoDescription || "";
  form.seoKeywords = detail.seoKeywords || "";
  form.content = detail.content;
  form.coverImage = detail.coverImage ?? "";
  form.categoryId = detail.categoryId;
  form.tagIds = detail.tags.map((tag) => tag.id);
  form.status = detail.status;
  form.isPinned = detail.isPinned ?? 0;
};

// isImageMedia 判断媒体资源是否是图片，媒体选择器只展示图片类型。
const isImageMedia = (item: MediaAsset) => {
  const contentType = item.contentType?.toLowerCase() ?? "";
  const extension = item.extension?.toLowerCase() ?? "";
  return contentType.startsWith("image/") || imageExtensions.has(extension);
};

// loadMediaCategories 加载媒体分类，供媒体选择弹窗筛选图片。
const loadMediaCategories = async () => {
  mediaCategories.value = await getMediaCategories();
};

// loadMedia 分页加载媒体库图片，避免图片很多时一次性加载过慢。
const loadMedia = async (reset = true) => {
  if (mediaLoading.value) return;
  mediaLoading.value = true;
  try {
    const nextPage = reset ? 1 : mediaPage.value + 1;
    const data = await getMediaPage({ keyword: mediaKeyword.value, category: mediaCategory.value || undefined, page: nextPage, size: mediaPageSize });
    const records = data.records.filter(isImageMedia);
    mediaList.value = reset ? records : [...mediaList.value, ...records];
    mediaPage.value = data.page;
    mediaTotal.value = data.total;
    mediaTotalPages.value = data.totalPages;
  } finally {
    mediaLoading.value = false;
  }
};

const searchMedia = async () => {
  await loadMedia(true);
};

const loadMoreMedia = async () => {
  if (!mediaHasMore.value) return;
  await loadMedia(false);
};
const loadPage = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    await loadOptions();
    await loadDetail();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.loadFail;
  } finally {
    loading.value = false;
  }
};

const toggleTag = (id: number) => {
  form.tagIds = form.tagIds.includes(id) ? form.tagIds.filter((tagId) => tagId !== id) : [...form.tagIds, id];
};

const insertText = async (before: string, after = "") => {
  const textarea = textareaRef.value;
  const start = textarea?.selectionStart ?? form.content.length;
  const end = textarea?.selectionEnd ?? form.content.length;
  const selected = form.content.slice(start, end);
  form.content = form.content.slice(0, start) + before + selected + after + form.content.slice(end);
  await nextTick();
  textareaRef.value?.focus();
  const cursor = start + before.length + selected.length;
  textareaRef.value?.setSelectionRange(cursor, cursor);
};

const insertImage = (url: string, alt = "image") => {
  void insertText("\n![" + alt.replace(/[\[\]]/g, "") + "](" + url + ")\n");
  mediaPickerOpen.value = false;
};

const openMediaPicker = async () => {
  mediaPickerOpen.value = true;
  if (mediaCategories.value.length === 0) {
    await loadMediaCategories();
  }
  await loadMedia(true);
};

const onCoverSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (file.size > maxCoverSize) {
    errorText.value = TEXT.sizeLimit;
    input.value = "";
    return;
  }
  uploadText.value = "";
  errorText.value = "";
  uploading.value = true;
  try {
    form.coverImage = await uploadArticleCover(file);
    uploadText.value = TEXT.uploadCoverOk;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.uploadFail;
  } finally {
    uploading.value = false;
    input.value = "";
  }
};

const onContentImageSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  mediaUploading.value = true;
  errorText.value = "";
  try {
    const media = await uploadMedia(file, "content");
    insertImage(media.url, media.originalName || "image");
    uploadText.value = TEXT.uploadContentOk;
    await loadMedia(true);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.uploadFail;
  } finally {
    mediaUploading.value = false;
    input.value = "";
  }
};

const runArticleAi = async (code: string) => {
  if (!form.title.trim() && !form.content.trim()) {
    errorText.value = TEXT.aiNeedContent;
    return;
  }
  aiRunning.value = code;
  errorText.value = "";
  uploadText.value = "";
  try {
    const result = await runAiSkill(code, {
      title: form.title,
      summary: form.summary,
      content: form.content,
      existingTags: tags.value.map((tag) => tag.name)
    });
    aiResult.value = normalizeAiResult(result);
    aiResultType.value = code;
    aiResultTitle.value = aiTitleMap[code] || TEXT.aiAssistant;
    aiResultOpen.value = true;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "AI failed";
  } finally {
    aiRunning.value = "";
  }
};

const applyAiText = (value: string) => {
  const text = cleanAiText(value);
  if (aiResultType.value === "article_summary") form.summary = text;
  else form.content = text;
  aiResultOpen.value = false;
};

const appendAiText = (value: string) => {
  const text = cleanAiText(value);
  if (!text.trim()) return;
  form.content = form.content.trim() ? form.content.trimEnd() + "\n\n" + text : text;
  aiResultOpen.value = false;
};

const applyAiSeo = () => {
  if (!aiResult.value) return;
  form.seoTitle = aiResult.value.seoTitle || form.title;
  form.seoDescription = aiResult.value.seoDescription || form.summary;
  form.seoKeywords = aiResult.value.seoKeywords.join(", ");
  aiResultOpen.value = false;
};

const applyAiTitle = (value: string) => {
  form.title = cleanAiText(value);
  aiResultOpen.value = false;
};

const applyAiTags = () => {
  const ids = matchedAiTags.value.map((tag) => tag.id);
  form.tagIds = Array.from(new Set([...form.tagIds, ...ids]));
  aiResultOpen.value = false;
};

const createAndApplyAiTags = async () => {
  if (!unmatchedNewTags.value.length || creatingAiTags.value) return;
  creatingAiTags.value = true;
  errorText.value = "";
  try {
    const createdNames = new Set<string>();
    for (const [index, name] of unmatchedNewTags.value.entries()) {
      const trimmed = name.trim();
      if (!trimmed) continue;
      const existed = tags.value.find((tag) => tag.name.trim().toLowerCase() === trimmed.toLowerCase());
      if (existed) {
        createdNames.add(existed.name.trim().toLowerCase());
        continue;
      }
      const created = await createTag({ name: trimmed, color: tagPalette[index % tagPalette.length] });
      createdNames.add(created.name.trim().toLowerCase());
    }
    tags.value = await getTagList();
    const newIds = tags.value.filter((tag) => createdNames.has(tag.name.trim().toLowerCase())).map((tag) => tag.id);
    const existingIds = matchedAiTags.value.map((tag) => tag.id);
    form.tagIds = Array.from(new Set([...form.tagIds, ...existingIds, ...newIds]));
    uploadText.value = TEXT.createTagsOk;
    aiResultOpen.value = false;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Create tags failed";
  } finally {
    creatingAiTags.value = false;
  }
};

const copyAiResult = async () => {
  const text = aiResult.value?.content || aiResult.value?.summary || aiResult.value?.titles.join("\n") || [aiResult.value?.seoTitle, aiResult.value?.seoDescription, aiResult.value?.seoKeywords.join(", ")].filter(Boolean).join("\n") || aiResult.value?.rawText || "";
  await navigator.clipboard?.writeText(cleanAiText(text));
};

const save = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    errorText.value = TEXT.titleRequired;
    return;
  }
  if (isLocalFileUrl.value) {
    errorText.value = TEXT.fileUrlError;
    return;
  }
  saving.value = true;
  errorText.value = "";
  try {
    if (isEdit.value) await updateArticle(articleId.value, form);
    else await createArticle(form);
    await router.push("/admin/articles");
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.saveFail;
  } finally {
    saving.value = false;
  }
};

onMounted(loadPage);
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>{{ isEdit ? TEXT.editArticle : TEXT.newArticle }}</h2>
        <p class="muted">{{ TEXT.desc }}</p>
      </div>
      <div class="editor-header-actions">
        <button class="ghost-btn" @click="router.push('/admin/media')">{{ TEXT.openMedia }}</button>
        <button class="ghost-btn" @click="router.push('/admin/articles')">{{ TEXT.backList }}</button>
      </div>
    </header>

    <p v-if="loading" class="muted">{{ TEXT.loading }}</p>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="uploadText" class="success-text">{{ uploadText }}</p>

    <form v-if="!loading" class="editor-layout" @submit.prevent="save">
      <div class="editor-form">
        <label>{{ TEXT.title }}<input v-model="form.title" type="text" :placeholder="TEXT.title" /></label>
        <label>{{ TEXT.summary }}<input v-model="form.summary" type="text" :placeholder="TEXT.summary" /></label>

        <div class="field-block seo-field-block">
          <span class="field-label">{{ TEXT.seoBlock }}</span>
          <div class="editor-meta-grid">
            <label>{{ TEXT.seoTitle }}<input v-model="form.seoTitle" type="text" :placeholder="form.title || TEXT.seoTitle" /></label>
            <label>{{ TEXT.seoKeywords }}<input v-model="form.seoKeywords" type="text" placeholder="Vue3, Spring Boot" /></label>
          </div>
          <label>{{ TEXT.seoDescription }}<input v-model="form.seoDescription" type="text" :placeholder="form.summary || TEXT.seoDescription" /></label>
        </div>

        <div class="field-block">
          <span class="field-label">{{ TEXT.cover }}</span>
          <div class="cover-upload-row">
            <input v-model="form.coverImage" type="url" placeholder="https://example.com/cover.png" />
            <label class="upload-btn">{{ uploading ? TEXT.uploading : TEXT.uploadLocal }}<input type="file" accept=".jpg,.jpeg,.png,.gif,.webp,image/jpeg,image/png,image/gif,image/webp" :disabled="uploading" @change="onCoverSelected" /></label>
          </div>
          <small class="muted">{{ TEXT.imageTip }}</small>
          <small v-if="isLocalFileUrl" class="error-text">{{ TEXT.localFileWarn }}</small>
        </div>

        <div class="editor-meta-grid">
          <label>{{ TEXT.category }}<select v-model.number="form.categoryId" class="select-input"><option :value="null">{{ TEXT.uncategorized }}</option><option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
          <label>{{ TEXT.status }}<select v-model.number="form.status" class="select-input"><option :value="0">{{ TEXT.draft }}</option><option :value="1">{{ TEXT.published }}</option><option :value="2">{{ TEXT.archived }}</option></select></label>
        </div>

        <label class="switch-row"><input v-model.number="form.isPinned" type="checkbox" :true-value="1" :false-value="0" /><span>{{ TEXT.pinned }}</span></label>

        <div class="field-block">
          <span class="field-label">{{ TEXT.tags }}</span>
          <div class="tag-check-list">
            <button v-for="item in tags" :key="item.id" class="tag-check" :class="{ selected: form.tagIds.includes(item.id) }" type="button" @click="toggleTag(item.id)"><span class="color-dot" :style="{ background: item.color }"></span>{{ item.name }}</button>
          </div>
        </div>

        <div class="field-block editor-body-block">
          <div class="editor-toolbar">
            <button v-for="tool in markdownTools" :key="tool.label" class="ghost-btn" type="button" @click="insertText(tool.before, tool.after)">{{ tool.label }}</button>
            <label class="ghost-btn toolbar-upload-btn">{{ mediaUploading ? TEXT.uploading : TEXT.uploadContent }}<input type="file" accept=".jpg,.jpeg,.png,.gif,.webp,image/jpeg,image/png,image/gif,image/webp" :disabled="mediaUploading" @change="onContentImageSelected" /></label>
            <button class="ghost-btn" type="button" @click="openMediaPicker">{{ TEXT.pickMedia }}</button>
          </div>
          <div class="editor-toolbar ai-toolbar">
            <span class="muted">{{ TEXT.aiAssistant }}</span>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_title')">{{ aiRunning === 'article_title' ? TEXT.aiRunning : TEXT.aiTitle }}</button>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_summary')">{{ aiRunning === 'article_summary' ? TEXT.aiRunning : TEXT.aiSummary }}</button>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_tags')">{{ aiRunning === 'article_tags' ? TEXT.aiRunning : TEXT.aiTags }}</button>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_polish')">{{ aiRunning === 'article_polish' ? TEXT.aiRunning : TEXT.aiPolish }}</button>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_outline')">{{ aiRunning === 'article_outline' ? TEXT.aiRunning : TEXT.aiOutline }}</button>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_expand')">{{ aiRunning === 'article_expand' ? TEXT.aiRunning : TEXT.aiExpand }}</button>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_continue')">{{ aiRunning === 'article_continue' ? TEXT.aiRunning : TEXT.aiContinue }}</button>
            <button class="ghost-btn" type="button" :disabled="!!aiRunning" @click="runArticleAi('article_seo')">{{ aiRunning === 'article_seo' ? TEXT.aiRunning : TEXT.aiSeo }}</button>
          </div>
          <label>{{ TEXT.body }}<textarea ref="textareaRef" v-model="form.content" class="editor-textarea" :placeholder="TEXT.bodyPlaceholder"></textarea></label>
        </div>

        <div class="editor-submit-row"><span class="muted">{{ wordCount }} chars</span><button class="primary-btn" type="submit" :disabled="saving || uploading || mediaUploading || !!aiRunning">{{ saving ? TEXT.saving : TEXT.saveArticle }}</button></div>
      </div>

      <aside class="editor-preview">
        <p class="front-category">{{ form.isPinned === 1 ? TEXT.pinnedArticle : TEXT.articlePreview }}</p>
        <div v-if="form.coverImage && !isLocalFileUrl" class="preview-cover" :style="{ backgroundImage: 'url(' + form.coverImage + ')' }"></div>
        <div v-else class="preview-cover empty-cover">{{ TEXT.coverPreview }}</div>
        <h3>{{ previewTitle }}</h3>
        <p>{{ previewSummary }}</p>
        <div class="mini-tag-row"><span v-for="item in tags.filter((tag) => form.tagIds.includes(tag.id))" :key="item.id" class="mini-tag" :style="{ borderColor: item.color }">{{ item.name }}</span></div>
      </aside>
    </form>

    <div v-if="mediaPickerOpen" class="media-picker-mask" @click.self="mediaPickerOpen = false">
      <section class="media-picker-panel glass">
        <header><div><h3>{{ TEXT.mediaTitle }}</h3><p class="muted">{{ TEXT.mediaDesc }}</p></div><button class="ghost-btn" @click="mediaPickerOpen = false">{{ TEXT.close }}</button></header>
        <div class="media-picker-tools">
          <input v-model="mediaKeyword" type="search" :placeholder="TEXT.mediaSearchPlaceholder" @keyup.enter="searchMedia" />
          <select v-model="mediaCategory" class="select-input" @change="searchMedia">
            <option value="">{{ TEXT.allMedia }}</option>
            <option v-for="item in mediaCategories" :key="item.code" :value="item.code">{{ item.name }}</option>
          </select>
          <button class="ghost-btn" type="button" :disabled="mediaLoading" @click="searchMedia">{{ TEXT.mediaSearch }}</button>
        </div>
        <p v-if="mediaLoading && mediaList.length === 0" class="muted">{{ TEXT.mediaLoading }}</p>
        <div v-else class="media-picker-grid">
          <button v-for="item in mediaList" :key="item.id" type="button" @click="insertImage(item.url, item.originalName || 'image')"><img :src="item.url" :alt="item.originalName" /><span>{{ item.originalName || item.fileName }}</span></button>
          <p v-if="mediaList.length === 0" class="muted">{{ TEXT.noMedia }}</p>
        </div>
        <div v-if="mediaList.length > 0" class="media-picker-footer">
          <span class="muted">{{ TEXT.mediaLoaded }} {{ mediaList.length }} / {{ mediaTotal }}</span>
          <button v-if="mediaHasMore" class="ghost-btn" type="button" :disabled="mediaLoading" @click="loadMoreMedia">{{ mediaLoading ? TEXT.mediaLoading : TEXT.loadMore }}</button>
        </div>      </section>
    </div>

    <div v-if="aiResultOpen" class="media-picker-mask" @click.self="aiResultOpen = false">
      <section class="media-picker-panel glass">
        <header><div><h3>{{ TEXT.aiResult }} · {{ aiResultTitle }}</h3><p class="muted">{{ aiResult?.elapsedMs }}ms</p></div><button class="ghost-btn" @click="aiResultOpen = false">{{ TEXT.close }}</button></header>
        <div v-if="aiResultType === 'article_title'" class="ai-result-list">
          <button v-for="item in aiResult?.titles" :key="item" class="ai-result-option" type="button" @click="applyAiTitle(item)"><span>{{ item }}</span><strong>{{ TEXT.apply }}</strong></button>
        </div>
        <div v-else-if="aiResultType === 'article_tags'" class="ai-result-list">
          <div v-if="matchedAiTags.length" class="ai-tag-result"><span v-for="item in matchedAiTags" :key="item.id">{{ item.name }}</span></div>
          <p v-else class="muted">{{ TEXT.noExistingTags }}</p>
          <p v-if="unmatchedNewTags.length" class="muted">{{ TEXT.newTags }}{{ TEXT.colon }}{{ unmatchedNewTags.join(' / ') }}</p>
          <div class="editor-header-actions">
            <button class="primary-btn" type="button" :disabled="matchedAiTags.length === 0" @click="applyAiTags">{{ TEXT.applyExistingTags }}</button>
            <button class="ghost-btn" type="button" :disabled="unmatchedNewTags.length === 0 || creatingAiTags" @click="createAndApplyAiTags">{{ creatingAiTags ? TEXT.creatingTags : TEXT.createNewTags }}</button>
          </div>
        </div>
        <div v-else-if="aiResultType === 'article_seo'" class="ai-result-list">
          <pre class="ai-result-preview">{{ aiResult?.seoTitle }}\n{{ aiResult?.seoDescription }}\n{{ aiResult?.seoKeywords.join(', ') }}</pre>
          <div class="editor-header-actions"><button class="primary-btn" type="button" @click="applyAiSeo">{{ TEXT.applySeo }}</button><button class="ghost-btn" type="button" @click="copyAiResult">{{ TEXT.copy }}</button></div>
        </div>
        <div v-else>
          <pre class="ai-result-preview">{{ aiResultType === 'article_summary' ? aiResult?.summary : aiResult?.content }}</pre>
          <div class="editor-header-actions"><button class="primary-btn" type="button" @click="applyAiText(aiResultType === 'article_summary' ? aiResult?.summary || '' : aiResult?.content || '')">{{ TEXT.apply }}</button><button v-if="aiResultType !== 'article_summary'" class="ghost-btn" type="button" @click="appendAiText(aiResult?.content || '')">{{ TEXT.append }}</button><button class="ghost-btn" type="button" @click="copyAiResult">{{ TEXT.copy }}</button></div>
        </div>
        <details><summary>{{ TEXT.raw }}</summary><pre class="ai-result-preview">{{ aiResult?.rawText }}</pre></details>
      </section>
    </div>
  </section>
</template>
