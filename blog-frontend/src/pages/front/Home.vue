<script setup lang="ts">
// Home.vue 是博客前台首页：负责加载站点设置、每日句子、分类标签和文章列表。
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import {
  getDailyQuote,
  getFrontArticlePage,
  getFrontCategories,
  getFrontTags,
  type DailyQuote,
  type FrontArticlePageQuery
} from "../../api/front";
import { defaultSiteSetting, getFrontSetting } from "../../api/setting";
import type { Article } from "../../types/article";
import { formatArticleDate } from "../../utils/articleDate";
import type { SiteSetting } from "../../types/setting";
import type { Category, Tag } from "../../types/taxonomy";

// SortType 复用接口查询参数中的排序类型，保证前端排序值和后端接口保持一致。
type SortType = NonNullable<FrontArticlePageQuery["sort"]>;

const router = useRouter();
const loading = ref(false);
const articleLoading = ref(false);
const errorText = ref("");
const setting = ref<SiteSetting>(defaultSiteSetting);
const dailyQuote = ref<DailyQuote | null>(null);
const articles = ref<Article[]>([]);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);
const keyword = ref("");
const activeCategoryId = ref<number | null>(null);
const activeTagId = ref<number | null>(null);
const currentPage = ref(1);
const pageSize = 8;
const total = ref(0);
const totalPages = ref(0);
const activeSort = ref<SortType>("latest");

// 首页提供多种排序方式：最新、阅读、点赞、评论。
const sortOptions: Array<{ label: string; value: SortType; hint: string }> = [
  { label: "最新", value: "latest", hint: "按发布时间" },
  { label: "阅读", value: "views", hint: "最多阅读" },
  { label: "点赞", value: "likes", hint: "最多点赞" },
  { label: "评论", value: "comments", hint: "最多讨论" }
];

// 快捷入口用于把首页连接到搜索、专题、归档、留言板等前台功能页。
const quickEntries = [
  { index: "01", title: "全站搜索", desc: "按关键词查找标题、摘要和正文", path: "/search" },
  { index: "02", title: "专题聚合", desc: "按分类和标签探索内容地图", path: "/topics" },
  { index: "03", title: "时间归档", desc: "顺着发布时间回看所有文章", path: "/archive" },
  { index: "04", title: "留言交流", desc: "留下想法，等待管理员回复", path: "/guestbook" }
];

// 以下 computed 负责把文章数据加工成页面需要的展示状态。
const displayArticles = computed(() => articles.value);
const latestArticles = computed(() => articles.value.slice(0, 5));
const pinnedArticles = computed(() => articles.value.filter((item) => item.isPinned === 1));
const featuredArticle = computed(() => pinnedArticles.value[0] ?? articles.value[0]);
const popularArticles = computed(() =>
  [...articles.value]
    .sort((a, b) =>
      (b.viewCount ?? 0) + (b.likeCount ?? 0) * 3 + (b.commentCount ?? 0) * 5
      - ((a.viewCount ?? 0) + (a.likeCount ?? 0) * 3 + (a.commentCount ?? 0) * 5)
    )
    .slice(0, 4)
);
const totalViews = computed(() => articles.value.reduce((sum, item) => sum + (item.viewCount ?? 0), 0));
const totalLikes = computed(() => articles.value.reduce((sum, item) => sum + (item.likeCount ?? 0), 0));
const totalComments = computed(() => articles.value.reduce((sum, item) => sum + (item.commentCount ?? 0), 0));
const hasMore = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value);
const categoryArticleCount = (item: Category) => item.articleCount ?? 0;
const tagArticleCount = (item: Tag) => item.articleCount ?? 0;
// 根据摘要和正文粗略估算阅读时长，提升前台阅读体验。
const readingMinutes = (item: Article) => {
  const rawText = [item.summary, item.content]
    .filter(Boolean)
    .join(" ")
    .replace(new RegExp("[#>*_`\\-\\[\\]()]", "g"), "");
  return Math.max(1, Math.ceil(rawText.replace(/\s/g, "").length / 500));
};
const activeFilterSummary = computed(() => {
  const categoryName = categories.value.find((item) => item.id === activeCategoryId.value)?.name;
  const tagName = tags.value.find((item) => item.id === activeTagId.value)?.name;
  const parts = [categoryName ? "分类：" + categoryName : "全部分类", tagName ? "标签：" + tagName : "全部标签"];
  return parts.join(" · ");
});
const quoteMeta = computed(() => {
  if (!dailyQuote.value) {
    return "";
  }
  const parts = [dailyQuote.value.author, dailyQuote.value.source].filter(Boolean);
  return parts.length ? parts.join(" · ") : dailyQuote.value.provider;
});

// loadArticles 按当前筛选条件分页加载文章，reset=true 表示重新查询第一页。
const loadArticles = async (reset = true) => {
  if (articleLoading.value) {
    return;
  }
  articleLoading.value = true;
  if (reset) {
    currentPage.value = 1;
    articles.value = [];
  }
  try {
    const page = await getFrontArticlePage({
      page: currentPage.value,
      size: pageSize,
      categoryId: activeCategoryId.value,
      tagId: activeTagId.value,
      sort: activeSort.value
    });
    articles.value = reset ? page.records : [...articles.value, ...page.records];
    total.value = page.total;
    totalPages.value = page.totalPages;
    currentPage.value = page.page;
  } finally {
    articleLoading.value = false;
  }
};

// loadMore 用于首页“加载更多”，在当前页基础上继续请求下一页。
const loadMore = async () => {
  if (!hasMore.value || articleLoading.value) {
    return;
  }
  currentPage.value += 1;
  await loadArticles(false);
};

// loadPage 是首页初始化入口，并行加载站点设置、分类、标签和每日句子。
const loadPage = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [settingData, categoryData, tagData, quoteData] = await Promise.all([
      getFrontSetting(),
      getFrontCategories(),
      getFrontTags(),
      getDailyQuote()
    ]);
    setting.value = settingData;
    categories.value = categoryData;
    tags.value = tagData;
    dailyQuote.value = quoteData.enabled ? quoteData : null;
    await loadArticles(true);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载前台数据失败";
  } finally {
    loading.value = false;
  }
};

// 点击分类后更新筛选条件，并重新加载文章列表。
const selectCategory = (id: number | null) => {
  activeCategoryId.value = id;
  void loadArticles(true);
};

// 点击标签后更新筛选条件，并重新加载文章列表。
const selectTag = (id: number | null) => {
  activeTagId.value = id;
  void loadArticles(true);
};

// resetFilters 清空搜索、分类、标签和排序，恢复默认文章流。
const resetFilters = () => {
  keyword.value = "";
  activeCategoryId.value = null;
  activeTagId.value = null;
  activeSort.value = "latest";
  void loadArticles(true);
};

const changeSort = (sort: SortType) => {
  if (activeSort.value === sort) {
    return;
  }
  activeSort.value = sort;
  void loadArticles(true);
};

// openArticle 跳转到文章详情页。
const openArticle = (id: number) => {
  router.push(`/articles/${id}`);
};

const openSearch = () => {
  const key = keyword.value.trim();
  router.push(key ? `/search?q=${encodeURIComponent(key)}` : "/search");
};

// 组件挂载后执行首页初始化加载。
onMounted(loadPage);
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="front-hero">
      <div>
        <p class="hero-kicker">{{ setting.siteSubtitle || "Personal Blog CMS" }}</p>
        <div v-if="setting.avatar" class="home-author-row">
          <img :src="setting.avatar" :alt="setting.authorName || setting.siteName" />
          <span>{{ setting.authorName || setting.siteName }}</span>
        </div>
        <h1>{{ setting.heroTitle }}</h1>
        <p>{{ setting.heroSubtitle }}</p>
        <div v-if="dailyQuote" class="daily-quote-card">
          <span>每日句子 · {{ dailyQuote.provider }}</span>
          <p>“{{ dailyQuote.content }}”</p>
          <a v-if="dailyQuote.sourceUrl" :href="dailyQuote.sourceUrl" target="_blank" rel="noreferrer">
            {{ quoteMeta || "查看来源" }}
          </a>
          <small v-else>{{ quoteMeta }}</small>
        </div>
        <div class="hero-actions">
          <button class="primary-btn" @click="router.push('/archive')">查看归档</button>
          <button class="ghost-btn" @click="router.push('/topics')">浏览专题</button>
        </div>
      </div>
      <article
        v-if="featuredArticle"
        class="hero-feature-card"
        :style="featuredArticle.coverImage ? { backgroundImage: `linear-gradient(145deg, rgba(255,255,255,.86), rgba(255,255,255,.58)), url(${featuredArticle.coverImage})` } : undefined"
        @click="openArticle(featuredArticle.id)"
      >
        <span>{{ featuredArticle.isPinned === 1 ? "置顶推荐" : "最新文章" }}</span>
        <h2>{{ featuredArticle.title }}</h2>
        <p>{{ featuredArticle.summary || "这篇文章暂时没有摘要，点击继续阅读完整内容。" }}</p>
        <small>{{ formatArticleDate(featuredArticle) }}</small>
      </article>
    </section>

    <section class="home-quick-grid">
      <button v-for="entry in quickEntries" :key="entry.path" class="home-quick-card" type="button" @click="router.push(entry.path)">
        <span>{{ entry.index }}</span>
        <strong>{{ entry.title }}</strong>
        <small>{{ entry.desc }}</small>
      </button>
    </section>

    <section class="front-toolbar enhanced-front-toolbar">
      <input v-model="keyword" type="search" placeholder="搜索标题、摘要或正文" @keyup.enter="openSearch" />
      <button class="ghost-btn" @click="openSearch">全站搜索</button>
      <button class="ghost-btn" @click="resetFilters">重置筛选</button>
    </section>

    <section class="home-sort-strip">
      <div>
        <strong>文章流</strong>
        <span>{{ activeFilterSummary }} · 共 {{ total }} 篇</span>
      </div>
      <div class="home-sort-tabs">
        <button
          v-for="option in sortOptions"
          :key="option.value"
          class="sort-tab"
          :class="{ active: activeSort === option.value }"
          type="button"
          @click="changeSort(option.value)"
        >
          <strong>{{ option.label }}</strong>
          <small>{{ option.hint }}</small>
        </button>
      </div>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading || (articleLoading && displayArticles.length === 0)" class="muted">加载中...</p>

    <section v-else class="front-content-grid">
      <aside class="front-sidebar">
        <div class="front-stats enhanced-front-stats">
          <strong>{{ total }}</strong>
          <span>篇文章</span>
          <small>当前已加载阅读 {{ totalViews }} 次，点赞 {{ totalLikes }} 次，评论 {{ totalComments }} 条</small>
        </div>

        <div v-if="popularArticles.length" class="front-side-block top-rank-list">
          <h3>本页热门</h3>
          <button v-for="(item, index) in popularArticles" :key="item.id" @click="openArticle(item.id)">
            <strong>{{ index + 1 }}</strong>
            <span>{{ item.title }}</span>
            <small>{{ item.viewCount }} 阅读 · {{ item.likeCount }} 赞</small>
          </button>
        </div>

        <div v-if="pinnedArticles.length" class="front-side-block latest-list">
          <h3>置顶文章</h3>
          <button v-for="item in pinnedArticles" :key="item.id" @click="openArticle(item.id)">
            {{ item.title }}
          </button>
        </div>

        <div class="front-side-block">
          <h3>分类</h3>
          <button class="filter-chip" :class="{ active: activeCategoryId === null }" @click="selectCategory(null)">
            全部
          </button>
          <button
            v-for="item in categories"
            :key="item.id"
            class="filter-chip"
            :class="{ active: activeCategoryId === item.id }"
            @click="selectCategory(item.id)"
            @dblclick="router.push(`/categories/${item.id}`)"
          >
            {{ item.name }}
            <small>{{ categoryArticleCount(item) }}</small>
          </button>
        </div>

        <div class="front-side-block">
          <h3>标签</h3>
          <button class="filter-chip" :class="{ active: activeTagId === null }" @click="selectTag(null)">
            全部
          </button>
          <button
            v-for="item in tags"
            :key="item.id"
            class="filter-chip"
            :class="{ active: activeTagId === item.id }"
            @click="selectTag(item.id)"
            @dblclick="router.push(`/tags/${item.id}`)"
          >
            <span class="color-dot" :style="{ background: item.color }"></span>
            {{ item.name }}
            <small>{{ tagArticleCount(item) }}</small>
          </button>
        </div>

        <div class="front-side-block latest-list">
          <h3>最新文章</h3>
          <button v-for="item in latestArticles" :key="item.id" @click="openArticle(item.id)">
            {{ item.title }}
          </button>
        </div>
      </aside>

      <div class="front-article-list">
        <article v-for="item in displayArticles" :key="item.id" class="front-article-card">
          <div v-if="item.coverImage" class="front-card-cover" :style="{ backgroundImage: `url(${item.coverImage})` }"></div>
          <div>
            <p class="front-category">
              <span v-if="item.isPinned === 1" class="pin-badge">置顶</span>
              {{ item.categoryName || "未分类" }}
            </p>
            <h2 @click="openArticle(item.id)">{{ item.title }}</h2>
            <p>{{ item.summary || "这篇文章还没有摘要，点击继续阅读完整内容。" }}</p>
          </div>
          <div class="front-card-footer">
            <span>{{ formatArticleDate(item) }}</span>
            <span>{{ item.viewCount }} 阅读 · {{ item.likeCount }} 点赞 · {{ item.commentCount }} 评论 · {{ readingMinutes(item) }} 分钟</span>
            <div class="mini-tag-row">
              <RouterLink
                v-for="tag in item.tags"
                :key="tag.id"
                class="mini-tag"
                :style="{ borderColor: tag.color }"
                :to="`/tags/${tag.id}`"
              >
                {{ tag.name }}
              </RouterLink>
            </div>
          </div>
          <button class="read-more-btn" @click="openArticle(item.id)">继续阅读</button>
        </article>

        <div v-if="displayArticles.length === 0" class="empty-state">
          <h3>没有匹配的文章</h3>
          <p>换个关键词，或者清空筛选条件再试试。</p>
        </div>

        <div v-if="displayArticles.length > 0" class="front-load-more">
          <span>已加载 {{ displayArticles.length }} / {{ total }} 篇</span>
          <button class="ghost-btn" :disabled="!hasMore || articleLoading" @click="loadMore">
            {{ articleLoading ? "加载中..." : hasMore ? "加载更多" : "已经到底了" }}
          </button>
        </div>
      </div>
    </section>
  </main>
</template>