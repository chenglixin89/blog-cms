<script setup lang="ts">
// Archive.vue 是前台归档页：按年份和月份组织已发布文章，方便按时间线浏览。
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { getFrontArticles, getFrontCategories, getFrontTags } from "../../api/front";
import type { Article } from "../../types/article";
import { formatArticleShortDate, getArticleDateValue } from "../../utils/articleDate";
import type { Category, Tag } from "../../types/taxonomy";

interface ArchiveGroup {
  key: string;
  label: string;
  articles: Article[];
}

const router = useRouter();
// loading 表示归档数据是否正在加载。
const loading = ref(false);
const errorText = ref("");
const articles = ref<Article[]>([]);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);

// archiveGroups 将文章按年份月份分组，生成时间线结构。
const archiveGroups = computed<ArchiveGroup[]>(() => {
  const groups = new Map<string, ArchiveGroup>();
  articles.value.forEach((item) => {
    const date = new Date(getArticleDateValue(item));
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
    const label = `${date.getFullYear()} 年 ${date.getMonth() + 1} 月`;
    if (!groups.has(key)) {
      groups.set(key, { key, label, articles: [] });
    }
    groups.get(key)?.articles.push(item);
  });
  return Array.from(groups.values());
});


// loadPage 从前台文章接口加载归档所需文章数据。
const loadPage = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [articleData, categoryData, tagData] = await Promise.all([
      getFrontArticles(),
      getFrontCategories(),
      getFrontTags()
    ]);
    articles.value = articleData;
    categories.value = categoryData;
    tags.value = tagData;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载归档失败";
  } finally {
    loading.value = false;
  }
};

onMounted(loadPage);
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="archive-hero">
      <p class="hero-kicker">Archive</p>
      <h1>按时间回看所有文章</h1>
      <p>把内容沉淀成时间线，后续文章多起来后，这里会成为最清晰的导航入口。</p>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading" class="muted">加载中...</p>

    <section v-else class="front-content-grid">
      <aside class="front-sidebar">
        <div class="front-stats">
          <strong>{{ articles.length }}</strong>
          <span>篇归档</span>
          <small>已按更新时间自动分组</small>
        </div>

        <div class="front-side-block">
          <h3>分类入口</h3>
          <button v-for="item in categories" :key="item.id" class="filter-chip" @click="router.push(`/categories/${item.id}`)">
            {{ item.name }}
          </button>
        </div>

        <div class="front-side-block">
          <h3>标签入口</h3>
          <button v-for="item in tags" :key="item.id" class="filter-chip" @click="router.push(`/tags/${item.id}`)">
            <span class="color-dot" :style="{ background: item.color }"></span>
            {{ item.name }}
          </button>
        </div>
      </aside>

      <div class="archive-list">
        <section v-for="group in archiveGroups" :key="group.key" class="archive-group">
          <h2>{{ group.label }}</h2>
          <article v-for="item in group.articles" :key="item.id" class="archive-item" @click="router.push(`/articles/${item.id}`)">
            <time>{{ formatArticleShortDate(item) }}</time>
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary || "这篇文章暂时没有摘要。" }}</p>
            </div>
            <span>{{ item.viewCount }} 阅读</span>
          </article>
        </section>

        <div v-if="archiveGroups.length === 0" class="empty-state">
          <h3>还没有文章</h3>
          <p>在后台发布第一篇文章后，归档会自动出现。</p>
        </div>
      </div>
    </section>
  </main>
</template>
