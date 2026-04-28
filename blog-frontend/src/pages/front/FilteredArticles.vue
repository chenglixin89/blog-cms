<script setup lang="ts">
// FilteredArticles.vue 是分类/标签文章列表页：根据路由参数展示某个分类或标签下的文章。
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { getFrontArticles, getFrontCategories, getFrontTags } from "../../api/front";
import type { Article } from "../../types/article";
import { formatArticleDate } from "../../utils/articleDate";
import type { Category, Tag } from "../../types/taxonomy";

const route = useRoute();
const router = useRouter();
// loading 表示筛选文章列表是否正在加载。
const loading = ref(false);
const errorText = ref("");
const articles = ref<Article[]>([]);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);

const filterId = computed(() => Number(route.params.id));
// filterType 根据路由 meta 判断当前是分类筛选还是标签筛选。
const filterType = computed(() => String(route.meta.filterType ?? "category"));

const targetName = computed(() => {
  if (filterType.value === "tag") {
    return tags.value.find((item) => item.id === filterId.value)?.name ?? "未知标签";
  }
  return categories.value.find((item) => item.id === filterId.value)?.name ?? "未知分类";
});

const pageTitle = computed(() => (filterType.value === "tag" ? `标签：${targetName.value}` : `分类：${targetName.value}`));

const filteredArticles = computed(() =>
  articles.value.filter((item) => {
    if (filterType.value === "tag") {
      return item.tags.some((tag) => tag.id === filterId.value);
    }
    return item.categoryId === filterId.value;
  })
);

const latestArticles = computed(() => articles.value.slice(0, 5));
const categoryArticleCount = (categoryId: number) =>
  articles.value.filter((item) => item.categoryId === categoryId).length;
const tagArticleCount = (tagId: number) =>
  articles.value.filter((item) => item.tags.some((tag) => tag.id === tagId)).length;


// loadPage 根据分类 id 或标签 id 加载对应文章。
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
    errorText.value = error instanceof Error ? error.message : "加载文章列表失败";
  } finally {
    loading.value = false;
  }
};

onMounted(loadPage);

watch(
  () => route.fullPath,
  () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
);
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="archive-hero">
      <p class="hero-kicker">{{ filterType === "tag" ? "Tag" : "Category" }}</p>
      <h1>{{ pageTitle }}</h1>
      <p>当前共筛选出 {{ filteredArticles.length }} 篇文章，你也可以从侧栏切换到其他分类或标签。</p>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading" class="muted">加载中...</p>

    <section v-else class="front-content-grid">
      <aside class="front-sidebar">
        <div class="front-side-block">
          <h3>全部分类</h3>
          <button
            v-for="item in categories"
            :key="item.id"
            class="filter-chip"
            :class="{ active: filterType === 'category' && filterId === item.id }"
            @click="router.push(`/categories/${item.id}`)"
          >
            {{ item.name }}
            <small>{{ categoryArticleCount(item.id) }}</small>
          </button>
        </div>

        <div class="front-side-block">
          <h3>全部标签</h3>
          <button
            v-for="item in tags"
            :key="item.id"
            class="filter-chip"
            :class="{ active: filterType === 'tag' && filterId === item.id }"
            @click="router.push(`/tags/${item.id}`)"
          >
            <span class="color-dot" :style="{ background: item.color }"></span>
            {{ item.name }}
            <small>{{ tagArticleCount(item.id) }}</small>
          </button>
        </div>

        <div class="front-side-block latest-list">
          <h3>最新文章</h3>
          <button v-for="item in latestArticles" :key="item.id" @click="router.push(`/articles/${item.id}`)">
            {{ item.title }}
          </button>
        </div>
      </aside>

      <div class="front-article-list">
        <article v-for="item in filteredArticles" :key="item.id" class="front-article-card">
          <div>
            <p class="front-category">{{ item.categoryName || "未分类" }}</p>
            <h2 @click="router.push(`/articles/${item.id}`)">{{ item.title }}</h2>
            <p>{{ item.summary || "这篇文章还没有摘要，点击继续阅读完整内容。" }}</p>
          </div>
          <div class="front-card-footer">
            <span>{{ formatArticleDate(item) }}</span>
            <span>{{ item.viewCount }} 阅读 · {{ item.likeCount }} 点赞 · {{ item.commentCount }} 评论</span>
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
          <button class="read-more-btn" @click="router.push(`/articles/${item.id}`)">继续阅读</button>
        </article>

        <div v-if="filteredArticles.length === 0" class="empty-state">
          <h3>这里暂时没有文章</h3>
          <p>可以先去后台发布文章，或切换到其他分类与标签。</p>
        </div>
      </div>
    </section>
  </main>
</template>
