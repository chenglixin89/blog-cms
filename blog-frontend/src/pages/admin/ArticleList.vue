<script setup lang="ts">
// ArticleList.vue 是后台文章列表页：负责文章查询、筛选、预览、编辑和删除。
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { deleteArticle, getArticlePage } from "../../api/article";
import type { Article } from "../../types/article";
import { formatArticleDateTime } from "../../utils/articleDate";

const router = useRouter();
// loading 控制列表加载状态，避免用户重复操作。
const loading = ref(false);
// list 保存当前页文章数据，分页信息由 total、currentPage 和 totalPages 控制。
const list = ref<Article[]>([]);
// errorText 用来展示列表加载或删除失败时的错误提示。
const errorText = ref("");
// keyword 是文章检索关键词，可搜索标题、摘要、正文和分类。
const keyword = ref("");
// statusFilter 表示当前文章状态筛选条件，all 代表全部。
const statusFilter = ref<number | "all">("all");
// currentPage、pageSize、total、totalPages 共同控制文章列表分页。
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);

// hasPrevious 和 hasNext 用于控制分页按钮是否可点击。
const hasPrevious = computed(() => currentPage.value > 1);
const hasNext = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value);

// statusParam 把页面里的 all / 数字状态转换成后端接口参数。
const statusParam = () => (statusFilter.value === "all" ? undefined : Number(statusFilter.value));

// loadList 按关键词、状态和分页参数从后端加载文章列表。
const loadList = async (page = 1) => {
  loading.value = true;
  errorText.value = "";
  try {
    const data = await getArticlePage({
      keyword: keyword.value.trim(),
      status: statusParam(),
      page,
      size: pageSize.value
    });
    list.value = data.records;
    total.value = data.total;
    totalPages.value = data.totalPages;
    currentPage.value = data.page;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载文章列表失败";
  } finally {
    loading.value = false;
  }
};

// search 从第一页重新查询文章。
const search = () => {
  void loadList(1);
};

// resetFilters 清空筛选条件并重新加载列表。
const resetFilters = () => {
  keyword.value = "";
  statusFilter.value = "all";
  pageSize.value = 10;
  void loadList(1);
};

// previousPage 跳转到上一页文章列表。
const previousPage = () => {
  if (hasPrevious.value) {
    void loadList(currentPage.value - 1);
  }
};

// nextPage 跳转到下一页文章列表。
const nextPage = () => {
  if (hasNext.value) {
    void loadList(currentPage.value + 1);
  }
};

// onDelete 删除文章，删除前会先弹窗确认。
const onDelete = async (id: number) => {
  const confirmed = window.confirm("确认删除这篇文章吗？");
  if (!confirmed) {
    return;
  }
  await deleteArticle(id);
  const nextPageNumber = list.value.length === 1 && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value;
  await loadList(nextPageNumber);
};

// getStatusText 把状态码转换为页面展示文案。
const getStatusText = (status: number) => {
  if (status === 1) {
    return "已发布";
  }
  if (status === 2) {
    return "已归档";
  }
  return "草稿";
};

// getStatusClass 根据状态码返回不同样式类名。
const getStatusClass = (status: number) => {
  if (status === 1) {
    return "published";
  }
  if (status === 2) {
    return "archived";
  }
  return "draft";
};

onMounted(() => loadList(1));
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>文章列表</h2>
        <p class="muted">共 {{ total }} 篇，当前第 {{ totalPages === 0 ? 0 : currentPage }} / {{ totalPages }} 页。</p>
      </div>
      <button class="primary-btn" @click="router.push('/admin/articles/new')">新建文章</button>
    </header>

    <section class="admin-toolbar">
      <input v-model="keyword" type="search" placeholder="搜索标题、摘要、正文或分类" @keyup.enter="search" />
      <select v-model="statusFilter" @change="search">
        <option value="all">全部状态</option>
        <option :value="0">草稿</option>
        <option :value="1">已发布</option>
        <option :value="2">已归档</option>
      </select>
      <select v-model.number="pageSize" @change="search">
        <option :value="10">10 条/页</option>
        <option :value="20">20 条/页</option>
        <option :value="50">50 条/页</option>
      </select>
      <button class="ghost-btn" @click="search">搜索</button>
      <button class="ghost-btn" @click="resetFilters">重置</button>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-else-if="loading" class="muted">加载中...</p>

    <div v-else class="article-table-wrap">
      <table class="article-table">
        <colgroup>
          <col class="col-title" />
          <col class="col-category" />
          <col class="col-status" />
          <col class="col-date" />
          <col class="col-actions" />
        </colgroup>
        <thead>
          <tr>
            <th>标题</th>
            <th>分类/标签</th>
            <th>状态</th>
            <th>发布时间</th>
            <th class="actions-header">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>
              <div class="article-title-cell">
                <div v-if="item.coverImage" class="article-cover-thumb" :style="{ backgroundImage: `url(${item.coverImage})` }"></div>
                <div>
                  <p class="title-cell">
                    <span v-if="item.isPinned === 1" class="pin-badge">置顶</span>
                    {{ item.title }}
                  </p>
                  <p class="summary-cell">{{ item.summary || "暂无摘要" }}</p>
                </div>
              </div>
            </td>
            <td>
              <p class="category-cell">{{ item.categoryName || "未分类" }}</p>
              <div class="mini-tag-row">
                <span v-for="tag in item.tags" :key="tag.id" class="mini-tag" :style="{ borderColor: tag.color }">
                  {{ tag.name }}
                </span>
              </div>
            </td>
            <td>
              <span class="status-pill" :class="getStatusClass(item.status)">{{ getStatusText(item.status) }}</span>
            </td>
            <td>{{ formatArticleDateTime(item) }}</td>
            <td>
              <div class="action-cell">
                <button class="ghost-btn table-btn" @click="router.push(`/admin/articles/${item.id}/preview`)">预览</button>
                <button class="ghost-btn table-btn" @click="router.push(`/admin/articles/${item.id}/edit`)">编辑</button>
                <button class="ghost-btn table-btn danger-btn" @click="onDelete(item.id)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="list.length === 0" class="empty-state">
        <h3>没有匹配的文章</h3>
        <p>可以调整关键词、状态筛选或每页数量后再试。</p>
      </div>

      <div v-if="total > 0" class="admin-pagination">
        <span>已显示 {{ list.length }} 条，本页 {{ currentPage }} / {{ totalPages }}</span>
        <div>
          <button class="ghost-btn" :disabled="!hasPrevious || loading" @click="previousPage">上一页</button>
          <button class="ghost-btn" :disabled="!hasNext || loading" @click="nextPage">下一页</button>
        </div>
      </div>
    </div>
  </section>
</template>