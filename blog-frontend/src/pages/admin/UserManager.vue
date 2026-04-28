<script setup lang="ts">
// UserManager.vue 是后台用户管理页：负责查看前台用户、启用禁用账号和查看用户行为数据。
import { computed, onMounted, ref } from "vue";
import { getAdminUserPage, resetAdminUserPassword, updateAdminUserStatus } from "../../api/user";
import type { AdminUser } from "../../types/adminUser";

// loading 控制用户列表加载状态。
const loading = ref(false);
// errorText 和 successText 用于展示用户操作反馈。
const errorText = ref("");
const successText = ref("");
// keyword 支持按用户名、昵称和邮箱搜索前台用户。
const keyword = ref("");
// statusFilter 控制用户状态筛选：正常、禁用或全部。
const statusFilter = ref<number | "all">("all");
// list、currentPage、pageSize、total、totalPages 一起维护用户列表与分页数据。
const list = ref<AdminUser[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);

// 这些 computed 用于分页按钮和页面顶部统计卡片。
const hasPrevious = computed(() => currentPage.value > 1);
const hasNext = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value);
const activeCount = computed(() => list.value.filter((item) => item.status === 1).length);
const disabledCount = computed(() => list.value.filter((item) => item.status === 0).length);
const activityCount = computed(() => list.value.reduce((sum, item) => sum + item.favoriteCount + item.likeCount + item.commentCount, 0));

// statusParam 把页面筛选值转换成后端可识别参数。
const statusParam = () => (statusFilter.value === "all" ? undefined : Number(statusFilter.value));

// loadList 按关键词和分页条件加载前台用户。
const loadList = async (page = 1) => {
  loading.value = true;
  errorText.value = "";
  try {
    const data = await getAdminUserPage({ keyword: keyword.value.trim(), status: statusParam(), page, size: pageSize.value });
    list.value = data.records;
    total.value = data.total;
    totalPages.value = data.totalPages;
    currentPage.value = data.page;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载用户失败";
  } finally {
    loading.value = false;
  }
};

// search 从第一页开始重新查询用户。
const search = () => void loadList(1);
// resetFilters 清空用户筛选条件。
const resetFilters = () => {
  keyword.value = "";
  statusFilter.value = "all";
  pageSize.value = 10;
  void loadList(1);
};
// previousPage 跳转到上一页用户列表。
const previousPage = () => hasPrevious.value && void loadList(currentPage.value - 1);
// nextPage 跳转到下一页用户列表。
const nextPage = () => hasNext.value && void loadList(currentPage.value + 1);

// setStatus 启用或禁用前台用户账号。
const setStatus = async (item: AdminUser, status: number) => {
  errorText.value = "";
  successText.value = "";
  try {
    await updateAdminUserStatus(item.id, status);
    successText.value = status === 1 ? "用户已启用。" : "用户已禁用。";
    await loadList(currentPage.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "更新用户状态失败";
  }
};

// resetPassword 允许管理员直接重置某个前台用户的密码。
const resetPassword = async (item: AdminUser) => {
  const password = window.prompt("请输入新密码（至少 6 位）", "123456");
  if (!password) return;
  if (password.length < 6) {
    errorText.value = "密码至少 6 位。";
    return;
  }
  try {
    await resetAdminUserPassword(item.id, password);
    successText.value = "密码已重置。";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "重置密码失败";
  }
};

// formatTime 统一格式化用户注册时间和最后登录时间。
const formatTime = (value: string | null) => (value ? new Date(value).toLocaleString() : "-");
// statusText 和 statusClass 用于把状态码映射成页面文案和样式。
const statusText = (status: number) => (status === 1 ? "正常" : "禁用");
const statusClass = (status: number) => (status === 1 ? "published" : "archived");

onMounted(() => loadList(1));
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>用户管理</h2>
        <p class="muted">管理前台注册用户，查看收藏、点赞、评论行为，也可以禁用账号或重置密码。</p>
      </div>
      <button class="ghost-btn" @click="loadList(currentPage)">刷新</button>
    </header>

    <section class="comment-stats user-stats">
      <div><strong>{{ total }}</strong><span>用户总数</span></div>
      <div><strong>{{ activeCount }}</strong><span>本页正常</span></div>
      <div><strong>{{ disabledCount }}</strong><span>本页禁用</span></div>
      <div><strong>{{ activityCount }}</strong><span>本页行为数</span></div>
    </section>

    <section class="admin-toolbar user-toolbar">
      <input v-model="keyword" type="search" placeholder="搜索用户名、昵称或邮箱" @keyup.enter="search" />
      <select v-model="statusFilter" @change="search">
        <option value="all">全部状态</option>
        <option :value="1">正常</option>
        <option :value="0">禁用</option>
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
    <p v-if="successText" class="success-text">{{ successText }}</p>
    <p v-if="loading" class="muted">加载中...</p>

    <div v-else class="article-table-wrap">
      <table class="article-table user-table">
        <thead><tr><th>用户</th><th>状态</th><th>行为</th><th>最后登录</th><th>注册时间</th><th class="actions-header">操作</th></tr></thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td><div class="user-profile-cell"><img v-if="item.avatar" :src="item.avatar" :alt="item.nickname" /><span v-else>{{ item.nickname.slice(0, 1).toUpperCase() }}</span><div><strong>{{ item.nickname }}</strong><small>{{ item.username }} · {{ item.email || "未填写邮箱" }}</small></div></div></td>
            <td><span class="status-pill" :class="statusClass(item.status)">{{ statusText(item.status) }}</span></td>
            <td>{{ item.favoriteCount }} 收藏 · {{ item.likeCount }} 点赞 · {{ item.commentCount }} 评论</td>
            <td>{{ formatTime(item.lastLoginAt) }}</td>
            <td>{{ formatTime(item.createdAt) }}</td>
            <td><div class="action-cell"><button class="ghost-btn table-btn" @click="setStatus(item, item.status === 1 ? 0 : 1)">{{ item.status === 1 ? "禁用" : "启用" }}</button><button class="ghost-btn table-btn" @click="resetPassword(item)">重置密码</button></div></td>
          </tr>
        </tbody>
      </table>
      <div v-if="list.length === 0" class="empty-state"><h3>还没有匹配用户</h3><p>可以调整关键词或状态筛选。</p></div>
      <div v-if="total > 0" class="admin-pagination"><span>已显示 {{ list.length }} 条，本页 {{ currentPage }} / {{ totalPages }}</span><div><button class="ghost-btn" :disabled="!hasPrevious || loading" @click="previousPage">上一页</button><button class="ghost-btn" :disabled="!hasNext || loading" @click="nextPage">下一页</button></div></div>
    </div>
  </section>
</template>
