<script setup lang="ts">
// Topics.vue 是前台专题页：把分类、标签和代表文章组织成内容地图。
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { getFrontArticles, getFrontCategories, getFrontTags } from "../../api/front";
import type { Article } from "../../types/article";
import { formatArticleDate } from "../../utils/articleDate";
import type { Category, Tag } from "../../types/taxonomy";

const router = useRouter();
// loading 控制专题页数据加载状态。
const loading = ref(false);
const errorText = ref("");
const articles = ref<Article[]>([]);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);

const sortedCategories = computed(() => {
  return [...categories.value].sort((a, b) => {
    const sortDiff = (b.sortOrder ?? 0) - (a.sortOrder ?? 0);
    return sortDiff === 0 ? b.id - a.id : sortDiff;
  });
});

// featuredTopics 筛选出有文章的分类，作为专题卡片展示。
const featuredTopics = computed(() => sortedCategories.value.filter((item) => categoryArticleCount(item.id) > 0));
const topTags = computed(() => [...tags.value].sort((a, b) => tagArticleCount(b.id) - tagArticleCount(a.id)).slice(0, 16));
const uncategorizedCount = computed(() => articles.value.filter((item) => !item.categoryId).length);

const categoryArticleCount = (categoryId: number) =>
  articles.value.filter((item) => item.categoryId === categoryId).length;

const tagArticleCount = (tagId: number) =>
  articles.value.filter((item) => item.tags.some((tag) => tag.id === tagId)).length;

const representativeArticle = (categoryId: number) => {
  return articles.value.find((item) => item.categoryId === categoryId);
};


// loadPage 并行加载文章、分类和标签，生成专题页数据。
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
    errorText.value = error instanceof Error ? error.message : "加载专题数据失败";
  } finally {
    loading.value = false;
  }
};

onMounted(loadPage);
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="archive-hero topic-hero">
      <p class="hero-kicker">Topics</p>
      <h1>把文章按主题组织起来，读起来更像一张地图。</h1>
      <p>
        专题页会根据后台分类排序展示内容，也会把标签热度和代表文章拉出来，方便访客从兴趣点进入，而不是只能顺着首页列表往下翻。
      </p>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading" class="muted">加载中...</p>

    <section v-else class="topic-layout">
      <div class="topic-grid">
        <article v-for="item in featuredTopics" :key="item.id" class="topic-card" @click="router.push(`/categories/${item.id}`)">
          <div class="topic-card-top">
            <span>分类</span>
            <strong>{{ categoryArticleCount(item.id) }} 篇</strong>
          </div>
          <h2>{{ item.name }}</h2>
          <p>{{ item.description || "这个分类还没有填写描述，可以在后台分类管理里补充。" }}</p>

          <div v-if="representativeArticle(item.id)" class="topic-feature">
            <small>代表文章</small>
            <h3>{{ representativeArticle(item.id)?.title }}</h3>
            <span>{{ formatArticleDate(representativeArticle(item.id)) }}</span>
          </div>
        </article>

        <article v-if="uncategorizedCount" class="topic-card quiet-card">
          <div class="topic-card-top">
            <span>未分类</span>
            <strong>{{ uncategorizedCount }} 篇</strong>
          </div>
          <h2>等待整理</h2>
          <p>这些文章还没有分配分类，可以回到后台编辑文章，让前台结构更清楚。</p>
        </article>

        <div v-if="!featuredTopics.length && !uncategorizedCount" class="empty-state">
          <h3>还没有可展示的专题</h3>
          <p>发布文章并选择分类后，这里会自动生成专题入口。</p>
        </div>
      </div>

      <aside class="topic-aside">
        <section class="front-side-block">
          <h3>标签热度</h3>
          <RouterLink
            v-for="item in topTags"
            :key="item.id"
            class="filter-chip"
            :to="`/tags/${item.id}`"
          >
            <span class="color-dot" :style="{ background: item.color }"></span>
            {{ item.name }}
            <small>{{ tagArticleCount(item.id) }}</small>
          </RouterLink>
        </section>

        <section class="topic-guide-card">
          <p class="hero-kicker">Reading Guide</p>
          <h3>建议的阅读路径</h3>
          <ol>
            <li>先从专题页选一个分类。</li>
            <li>进入分类页看同主题文章。</li>
            <li>再用标签发现交叉主题。</li>
          </ol>
        </section>
      </aside>
    </section>
  </main>
</template>
