<script setup lang="ts">
// Search.vue 是前台搜索页：支持关键词搜索、分类标签筛选、排序、搜索历史和搜索建议。
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { getFrontArticlePage, getFrontCategories, getFrontTags } from "../../api/front";
import type { Article } from "../../types/article";
import { formatArticleDate } from "../../utils/articleDate";
import type { Category, Tag } from "../../types/taxonomy";
import { highlightKeyword } from "../../utils/sanitize";

// sortOptions 定义搜索结果支持的排序方式。
const sortOptions = ["relevance", "latest", "views", "likes", "comments"] as const;
type SortType = (typeof sortOptions)[number];

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const articleLoading = ref(false);
const errorText = ref("");
const articles = ref<Article[]>([]);
const hotArticles = ref<Article[]>([]);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);
const keyword = ref(String(route.query.q ?? ""));
const activeCategoryId = ref<number | "all">("all");
const activeTagId = ref<number | "all">("all");
const sortType = ref<SortType>("relevance");
const searchHistory = ref<string[]>([]);
const showSuggest = ref(false);
const currentPage = ref(1);
const pageSize = 8;
const total = ref(0);
const totalPages = ref(0);

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase());
const results = computed(() => articles.value);
const hasMore = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value);

// keywordSuggestions 根据历史记录、文章标题、分类和标签生成搜索建议。
const keywordSuggestions = computed(() => {
  const key = normalizedKeyword.value;
  if (!key) {
    return searchHistory.value.slice(0, 6);
  }

  const bag = new Set<string>();
  searchHistory.value.forEach((item) => {
    if (item.toLowerCase().includes(key)) {
      bag.add(item);
    }
  });
  [...articles.value, ...hotArticles.value].forEach((item) => {
    if (item.title.toLowerCase().includes(key)) {
      bag.add(item.title);
    }
  });
  categories.value.forEach((item) => {
    if (item.name.toLowerCase().includes(key)) {
      bag.add(item.name);
    }
  });
  tags.value.forEach((item) => {
    if (item.name.toLowerCase().includes(key)) {
      bag.add(item.name);
    }
  });
  return Array.from(bag).slice(0, 8);
});

const optionalNumber = (value: number | "all") => (value === "all" ? undefined : Number(value));

// highlighted 把搜索关键词用 mark 标签高亮展示，并先做 HTML 转义防止
// 文章标题或摘要里的原始 HTML 在 v-html 渲染时执行。
const highlighted = (text: string) => highlightKeyword(text, keyword.value);

// loadHistory 从 localStorage 读取本地搜索历史。
const loadHistory = () => {
  try {
    searchHistory.value = JSON.parse(localStorage.getItem("blog_search_history") ?? "[]");
  } catch {
    searchHistory.value = [];
  }
};

// saveKeyword 保存本次搜索词，并限制历史记录数量。
const saveKeyword = () => {
  const key = keyword.value.trim();
  if (!key) {
    return;
  }
  searchHistory.value = [key, ...searchHistory.value.filter((item) => item !== key)].slice(0, 8);
  localStorage.setItem("blog_search_history", JSON.stringify(searchHistory.value));
};

// loadResults 按关键词、分类、标签和排序方式分页加载搜索结果。
const loadResults = async (reset = true) => {
  if (articleLoading.value) {
    return;
  }
  articleLoading.value = true;
  errorText.value = "";
  if (reset) {
    currentPage.value = 1;
    articles.value = [];
  }
  try {
    const page = await getFrontArticlePage({
      keyword: keyword.value.trim(),
      categoryId: optionalNumber(activeCategoryId.value),
      tagId: optionalNumber(activeTagId.value),
      sort: sortType.value,
      page: currentPage.value,
      size: pageSize
    });
    articles.value = reset ? page.records : [...articles.value, ...page.records];
    total.value = page.total;
    totalPages.value = page.totalPages;
    currentPage.value = page.page;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载搜索数据失败";
  } finally {
    articleLoading.value = false;
  }
};

// loadMore 加载下一页搜索结果。
const loadMore = async () => {
  if (!hasMore.value || articleLoading.value) {
    return;
  }
  currentPage.value += 1;
  await loadResults(false);
};

// runSearch 保存搜索词、同步 URL 查询参数，并重新加载第一页结果。
const runSearch = () => {
  saveKeyword();
  const key = keyword.value.trim();
  void router.replace({ path: "/search", query: key ? { q: key } : {} }).then(() => loadResults(true));
};

// useHistory 点击历史记录或搜索建议后直接使用该关键词搜索。
const useHistory = (value: string) => {
  keyword.value = value;
  runSearch();
};

const pickSuggestion = (value: string) => {
  keyword.value = value;
  showSuggest.value = false;
  runSearch();
};

const hideSuggest = () => {
  window.setTimeout(() => {
    showSuggest.value = false;
  }, 120);
};

const clearFilters = () => {
  keyword.value = "";
  activeCategoryId.value = "all";
  activeTagId.value = "all";
  sortType.value = "relevance";
  void router.replace({ path: "/search" }).then(() => loadResults(true));
};

// loadPage 初始化搜索页基础数据和默认结果。
const loadPage = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [categoryData, tagData, hotData] = await Promise.all([
      getFrontCategories(),
      getFrontTags(),
      getFrontArticlePage({ sort: "views", page: 1, size: 5 })
    ]);
    categories.value = categoryData;
    tags.value = tagData;
    hotArticles.value = hotData.records;
    await loadResults(true);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载搜索数据失败";
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadHistory();
  loadPage();
});

watch(
  () => route.query.q,
  (value) => {
    keyword.value = String(value ?? "");
    void loadResults(true);
  }
);

watch([activeCategoryId, activeTagId, sortType], () => {
  void loadResults(true);
});
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="archive-hero search-hero">
      <p class="hero-kicker">Search</p>
      <h1>更快找到你想读的内容。</h1>
      <p>支持标题、摘要、正文检索，也可以叠加分类、标签和排序方式，把博客内容真正串起来。</p>
    </section>

    <section class="search-panel">
      <div class="search-input-row">
        <div class="search-input-box">
          <input
            v-model="keyword"
            type="search"
            placeholder="输入关键词，例如 Vue、Spring、项目复盘"
            @focus="showSuggest = true"
            @input="showSuggest = true"
            @blur="hideSuggest"
            @keyup.enter="runSearch"
          />
          <div v-if="showSuggest && keywordSuggestions.length" class="search-suggest-list">
            <button
              v-for="item in keywordSuggestions"
              :key="item"
              type="button"
              class="search-suggest-item"
              @mousedown.prevent
              @click="pickSuggestion(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>
        <button class="primary-btn" @click="runSearch">搜索</button>
        <button class="ghost-btn" @click="clearFilters">清空</button>
      </div>

      <div v-if="searchHistory.length" class="search-history">
        <span>最近搜索</span>
        <button v-for="item in searchHistory" :key="item" class="filter-chip" @click="useHistory(item)">
          {{ item }}
        </button>
      </div>

      <div class="search-filter-grid">
        <label>
          分类
          <select v-model="activeCategoryId">
            <option value="all">全部分类</option>
            <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <label>
          标签
          <select v-model="activeTagId">
            <option value="all">全部标签</option>
            <option v-for="item in tags" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <label>
          排序
          <select v-model="sortType">
            <option value="relevance">相关度优先</option>
            <option value="latest">最新发布</option>
            <option value="views">阅读最多</option>
            <option value="likes">点赞最多</option>
            <option value="comments">评论最多</option>
          </select>
        </label>
      </div>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading || (articleLoading && results.length === 0)" class="muted">加载中...</p>

    <section v-else class="search-layout">
      <div class="search-results">
        <div class="section-title-row">
          <h2>搜索结果</h2>
          <span>{{ total }} 篇</span>
        </div>

        <article v-for="item in results" :key="item.id" class="search-result-card">
          <div v-if="item.coverImage" class="front-card-cover" :style="{ backgroundImage: `url(${item.coverImage})` }"></div>
          <div>
            <p class="front-category">{{ item.categoryName || "未分类" }}</p>
            <h2 @click="router.push(`/articles/${item.id}`)" v-html="highlighted(item.title)"></h2>
            <p v-html="highlighted(item.summary || '这篇文章还没有摘要。')"></p>
            <div class="front-card-footer">
              <span>{{ formatArticleDate(item) }}</span>
              <span>{{ item.viewCount }} 阅读 · {{ item.likeCount }} 点赞 · {{ item.commentCount }} 评论</span>
            </div>
            <div class="mini-tag-row">
              <RouterLink v-for="tag in item.tags" :key="tag.id" class="mini-tag" :style="{ borderColor: tag.color }" :to="`/tags/${tag.id}`">
                {{ tag.name }}
              </RouterLink>
            </div>
          </div>
        </article>

        <div v-if="results.length === 0" class="empty-state">
          <h3>没有找到匹配文章</h3>
          <p>可以换个关键词，或者清空分类、标签筛选后再试。</p>
        </div>

        <div v-if="results.length > 0" class="front-load-more">
          <span>已加载 {{ results.length }} / {{ total }} 篇</span>
          <button class="ghost-btn" :disabled="!hasMore || articleLoading" @click="loadMore">
            {{ articleLoading ? "加载中..." : hasMore ? "加载更多" : "已经到底了" }}
          </button>
        </div>
      </div>

      <aside class="search-aside">
        <section class="front-side-block latest-list">
          <h3>热门文章</h3>
          <button v-for="item in hotArticles" :key="item.id" @click="router.push(`/articles/${item.id}`)">
            {{ item.title }}
          </button>
        </section>

        <section class="topic-guide-card">
          <p class="hero-kicker">Tip</p>
          <h3>搜索小技巧</h3>
          <ol>
            <li>先输入关键词，再叠加分类。</li>
            <li>用标签可以发现交叉主题。</li>
            <li>热门排序适合快速找重点文章。</li>
          </ol>
        </section>
      </aside>
    </section>
  </main>
</template>