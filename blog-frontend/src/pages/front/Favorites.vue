<script setup lang="ts">
// Favorites.vue 是前台收藏页：登录用户读取云端收藏，未登录用户读取本地收藏。
import { onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { getCloudFavorites, getFrontArticles, removeCloudFavorite } from "../../api/front";
import { useFrontUserStore } from "../../stores/frontUser";
import type { Article } from "../../types/article";
import { formatArticleDate } from "../../utils/articleDate";

const router = useRouter();
const frontUserStore = useFrontUserStore();
// loading 控制收藏列表加载状态。
const loading = ref(false);
const removingId = ref<number | null>(null);
const errorText = ref("");
const articles = ref<Article[]>([]);
const bookmarkIdsKey = "bookmarked_article_ids";

// loadBookmarkedIds 从 localStorage 读取未登录时保存的收藏文章 id。
const loadBookmarkedIds = () => {
  try {
    const raw = localStorage.getItem(bookmarkIdsKey);
    const parsed = raw ? (JSON.parse(raw) as number[]) : [];
    return parsed.filter((value) => Number.isFinite(value));
  } catch {
    return [];
  }
};


// removeLocalFavorite 从本地收藏列表中移除指定文章。
const removeLocalFavorite = (id: number) => {
  const next = loadBookmarkedIds().filter((item) => item !== id);
  localStorage.setItem(bookmarkIdsKey, JSON.stringify(next));
  articles.value = articles.value.filter((item) => item.id !== id);
};

// removeFavorite 取消收藏，并同步更新页面列表。
const removeFavorite = async (id: number) => {
  removingId.value = id;
  errorText.value = "";
  try {
    if (frontUserStore.isLoggedIn) {
      await removeCloudFavorite(id);
      articles.value = articles.value.filter((item) => item.id !== id);
    } else {
      removeLocalFavorite(id);
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "取消收藏失败";
  } finally {
    removingId.value = null;
  }
};

// loadPage 根据登录状态加载云端收藏或本地收藏。
const loadPage = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    if (frontUserStore.isLoggedIn) {
      articles.value = await getCloudFavorites();
      return;
    }

    const ids = loadBookmarkedIds();
    const indexMap = new Map<number, number>();
    ids.forEach((id, index) => indexMap.set(id, index));
    const allArticles = await getFrontArticles();
    articles.value = allArticles
      .filter((item) => indexMap.has(item.id))
      .sort((a, b) => (indexMap.get(a.id) ?? 9999) - (indexMap.get(b.id) ?? 9999));
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载收藏文章失败";
  } finally {
    loading.value = false;
  }
};

onMounted(loadPage);
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="archive-hero favorites-hero">
      <p class="hero-kicker">Favorites</p>
      <h1>我的收藏</h1>
      <p>{{ frontUserStore.isLoggedIn ? "这些文章已保存到你的云端收藏夹。" : "当前是本地收藏，登录后会自动同步到数据库。" }}</p>
      <RouterLink v-if="!frontUserStore.isLoggedIn" class="primary-btn" to="/user/login?redirect=/favorites">登录同步收藏</RouterLink>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading" class="muted">加载中...</p>

    <section v-else class="favorites-list">
      <article v-for="item in articles" :key="item.id" class="front-article-card favorites-card">
        <div v-if="item.coverImage" class="front-card-cover" :style="{ backgroundImage: `url(${item.coverImage})` }"></div>
        <div>
          <p class="front-category">{{ item.categoryName || "未分类" }}</p>
          <h2 @click="router.push(`/articles/${item.id}`)">{{ item.title }}</h2>
          <p>{{ item.summary || "这篇文章还没有摘要。" }}</p>
        </div>
        <div class="front-card-footer">
          <span>{{ formatArticleDate(item) }}</span>
          <span>{{ item.viewCount }} 阅读 · {{ item.likeCount }} 点赞 · {{ item.commentCount }} 评论</span>
          <div class="favorites-actions">
            <RouterLink class="ghost-btn" :to="`/articles/${item.id}`">阅读</RouterLink>
            <button class="ghost-btn" type="button" :disabled="removingId === item.id" @click="removeFavorite(item.id)">
              {{ removingId === item.id ? "处理中..." : "取消收藏" }}
            </button>
          </div>
        </div>
      </article>

      <div v-if="articles.length === 0" class="empty-state">
        <h3>还没有收藏文章</h3>
        <p>在文章详情页点击收藏后，会出现在这里。</p>
      </div>
    </section>
  </main>
</template>
