<script setup lang="ts">
// About.vue 是前台关于页：展示站点介绍、内容统计、最近文章和后台配置的路线图。
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { getFrontArticles, getFrontCategories, getFrontTags } from "../../api/front";
import { defaultSiteSetting, getFrontSetting } from "../../api/setting";
import type { Article } from "../../types/article";
import type { Category, Tag } from "../../types/taxonomy";
import type { SiteSetting } from "../../types/setting";

// RoadmapItem 对应后台站点设置中的 roadmapJson 结构。
interface RoadmapItem {
  title: string;
  description: string;
  active?: boolean;
}

// router 用于跳转专题页和最新文章详情页。
const router = useRouter();
// loading 和 errorText 控制关于页加载状态与错误反馈。
const loading = ref(false);
const errorText = ref("");
// articles、categories、tags 用于统计卡片和最近文章展示。
const articles = ref<Article[]>([]);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);
// setting 保存后台可配置的关于页与首页展示信息，默认先使用本地兜底配置。
const setting = ref<SiteSetting>(defaultSiteSetting);

// 这几个统计值把文章列表里的阅读、点赞、评论聚合成关于页概览数据。
const totalViews = computed(() => articles.value.reduce((sum, item) => sum + (item.viewCount ?? 0), 0));
const totalLikes = computed(() => articles.value.reduce((sum, item) => sum + (item.likeCount ?? 0), 0));
const totalComments = computed(() => articles.value.reduce((sum, item) => sum + (item.commentCount ?? 0), 0));
// latestArticle 默认取文章列表第一篇，依赖前台文章接口返回顺序。
const latestArticle = computed(() => articles.value[0]);

// roadmapItems 负责解析后台配置的路线图 JSON；如果结构异常则回退到默认配置。
const roadmapItems = computed<RoadmapItem[]>(() => {
  try {
    const parsed = JSON.parse(setting.value.roadmapJson || defaultSiteSetting.roadmapJson) as RoadmapItem[];
    if (!Array.isArray(parsed)) return JSON.parse(defaultSiteSetting.roadmapJson) as RoadmapItem[];
    return parsed
      .filter((item) => item && item.title && item.description)
      .map((item) => ({ title: item.title, description: item.description, active: item.active === true }));
  } catch {
    return JSON.parse(defaultSiteSetting.roadmapJson) as RoadmapItem[];
  }
});

// loadPage 并行加载关于页所需的文章、分类、标签和站点设置数据。
const loadPage = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [articleData, categoryData, tagData, settingData] = await Promise.all([
      getFrontArticles(),
      getFrontCategories(),
      getFrontTags(),
      getFrontSetting()
    ]);
    articles.value = articleData;
    categories.value = categoryData;
    tags.value = tagData;
    setting.value = settingData;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载关于页数据失败";
  } finally {
    loading.value = false;
  }
};

onMounted(loadPage);
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="about-hero">
      <div>
        <p class="hero-kicker">About This Blog</p>
        <h1>{{ setting.heroTitle }}</h1>
        <p>{{ setting.heroSubtitle }}</p>
      </div>
      <article class="profile-card">
        <span class="profile-avatar">
          <img v-if="setting.avatar" :src="setting.avatar" alt="站点头像" />
          <template v-else>{{ (setting.authorName || setting.siteName || "B").slice(0, 1).toUpperCase() }}</template>
        </span>
        <h2>{{ setting.siteName || "Blog CMS" }}</h2>
        <p>{{ setting.authorBio || "Vue 3 + Spring Boot + MySQL 的个人博客系统" }}</p>
        <button class="primary-btn" @click="router.push('/topics')">查看专题</button>
      </article>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading" class="muted">加载中...</p>

    <section v-else class="about-grid">
      <article class="about-card metric-card">
        <p>文章</p>
        <strong>{{ articles.length }}</strong>
        <span>已发布内容</span>
      </article>
      <article class="about-card metric-card">
        <p>分类</p>
        <strong>{{ categories.length }}</strong>
        <span>内容栏目</span>
      </article>
      <article class="about-card metric-card">
        <p>标签</p>
        <strong>{{ tags.length }}</strong>
        <span>主题线索</span>
      </article>
      <article class="about-card metric-card">
        <p>互动</p>
        <strong>{{ totalLikes + totalComments }}</strong>
        <span>{{ totalViews }} 次阅读</span>
      </article>

      <article class="about-card wide-card">
        <p class="hero-kicker">Roadmap</p>
        <h2>当前阶段</h2>
        <div class="timeline-strip">
          <div v-for="item in roadmapItems" :key="item.title" class="timeline-item" :class="{ active: item.active }">
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
          </div>
        </div>
      </article>

      <article class="about-card latest-about-card">
        <p class="hero-kicker">Latest</p>
        <h2>最近文章</h2>
        <template v-if="latestArticle">
          <h3>{{ latestArticle.title }}</h3>
          <p>{{ latestArticle.summary || "这篇文章还没有摘要。" }}</p>
          <button class="ghost-btn" @click="router.push(`/articles/${latestArticle.id}`)">继续阅读</button>
        </template>
        <p v-else class="muted">还没有发布文章。</p>
      </article>
    </section>
  </main>
</template>
