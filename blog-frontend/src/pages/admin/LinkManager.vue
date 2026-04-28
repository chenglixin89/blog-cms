<script setup lang="ts">
// LinkManager.vue 是后台友链管理页：负责友链申请审核、新增、编辑、删除和排序。
import { computed, onMounted, reactive, ref } from "vue";
import {
  createFriendLink,
  deleteFriendLink,
  getAdminLinks,
  updateFriendLink,
  updateFriendLinkStatus
} from "../../api/link";
import type { FriendLink, FriendLinkPayload } from "../../types/link";

// loading 表示友链列表是否正在加载。
const loading = ref(false);
// saving 表示友链表单是否正在提交。
const saving = ref(false);
const errorText = ref("");
const successText = ref("");
// keyword 支持按友链名称、地址和简介搜索。
const keyword = ref("");
// statusFilter 控制待审核、已通过、已拒绝等友链状态筛选。
const statusFilter = ref<number | "all">("all");
const editingId = ref<number | null>(null);
const list = ref<FriendLink[]>([]);

// form 保存友链表单数据，新增和编辑共用。
const form = reactive<FriendLinkPayload>({
  name: "",
  url: "",
  description: "",
  logo: "",
  status: 1,
  sortOrder: 0
});

// filteredList 在前端根据关键词和状态对友链列表做二次过滤。
const filteredList = computed(() => {
  const key = keyword.value.trim().toLowerCase();
  return list.value.filter((item) => {
    const statusMatched = statusFilter.value === "all" || item.status === statusFilter.value;
    const keywordMatched =
      !key ||
      item.name.toLowerCase().includes(key) ||
      item.url.toLowerCase().includes(key) ||
      (item.description ?? "").toLowerCase().includes(key);
    return statusMatched && keywordMatched;
  });
});

// 这些统计值用于页面顶部展示不同状态的友链数量。
const pendingCount = computed(() => list.value.filter((item) => item.status === 0).length);
const approvedCount = computed(() => list.value.filter((item) => item.status === 1).length);
const rejectedCount = computed(() => list.value.filter((item) => item.status === 2).length);

// resetForm 清空友链表单并退出编辑状态。
const resetForm = () => {
  editingId.value = null;
  form.name = "";
  form.url = "";
  form.description = "";
  form.logo = "";
  form.status = 1;
  form.sortOrder = 0;
};

// loadList 按筛选条件加载友链列表。
const loadList = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    list.value = await getAdminLinks();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载友情链接失败";
  } finally {
    loading.value = false;
  }
};

// edit 把选中的友链数据回填到表单。
const edit = (item: FriendLink) => {
  editingId.value = item.id;
  form.name = item.name;
  form.url = item.url;
  form.description = item.description;
  form.logo = item.logo;
  form.status = item.status;
  form.sortOrder = item.sortOrder;
};

// save 根据编辑状态新增或更新友链。
const save = async () => {
  if (!form.name.trim() || !form.url.trim()) {
    errorText.value = "网站名称和链接地址不能为空";
    return;
  }

  saving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    if (editingId.value) {
      await updateFriendLink(editingId.value, form);
      successText.value = "友情链接已更新。";
    } else {
      await createFriendLink(form);
      successText.value = "友情链接已新增。";
    }
    resetForm();
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "保存友情链接失败";
  } finally {
    saving.value = false;
  }
};

// statusText 和 statusClass 把审核状态码转换成展示文本和样式。
const statusText = (status: number) => {
  if (status === 1) {
    return "已通过";
  }
  if (status === 2) {
    return "已拒绝";
  }
  return "待审核";
};

const statusClass = (status: number) => {
  if (status === 1) {
    return "published";
  }
  if (status === 2) {
    return "archived";
  }
  return "draft";
};

// changeStatus 修改友链审核状态。
const changeStatus = async (id: number, status: number) => {
  errorText.value = "";
  successText.value = "";
  try {
    await updateFriendLinkStatus(id, status);
    successText.value = status === 1 ? "友链已通过。" : status === 2 ? "友链已拒绝。" : "友链已改为待审核。";
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "更新友链状态失败";
  }
};

// remove 删除友链记录。
const remove = async (id: number) => {
  if (!window.confirm("确认删除这个友情链接吗？")) {
    return;
  }

  try {
    await deleteFriendLink(id);
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "删除友情链接失败";
  }
};

onMounted(loadList);
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>友链管理</h2>
        <p class="muted">管理前台友情链接，也可以审核访客提交的友链申请。</p>
      </div>
      <button class="ghost-btn" @click="resetForm">清空表单</button>
    </header>

    <section class="comment-stats">
      <div>
        <strong>{{ pendingCount }}</strong>
        <span>待审核</span>
      </div>
      <div>
        <strong>{{ approvedCount }}</strong>
        <span>已通过</span>
      </div>
      <div>
        <strong>{{ rejectedCount }}</strong>
        <span>已拒绝</span>
      </div>
      <div>
        <strong>{{ list.length }}</strong>
        <span>全部友链</span>
      </div>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>

    <form class="link-form" @submit.prevent="save">
      <input v-model="form.name" type="text" placeholder="网站名称" />
      <input v-model="form.url" type="url" placeholder="网站地址" />
      <input v-model="form.logo" type="url" placeholder="Logo 地址" />
      <input v-model="form.description" type="text" placeholder="网站简介" />
      <input v-model.number="form.sortOrder" type="number" placeholder="排序，数字越大越靠前" />
      <select v-model.number="form.status">
        <option :value="0">待审核</option>
        <option :value="1">已通过</option>
        <option :value="2">已拒绝</option>
      </select>
      <button class="primary-btn" type="submit" :disabled="saving">
        {{ saving ? "保存中..." : editingId ? "更新友链" : "新增友链" }}
      </button>
    </form>

    <section class="admin-toolbar">
      <input v-model="keyword" type="search" placeholder="搜索名称、链接或简介" />
      <select v-model="statusFilter">
        <option value="all">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="1">已通过</option>
        <option :value="2">已拒绝</option>
      </select>
      <button class="ghost-btn" @click="keyword = ''; statusFilter = 'all'">重置</button>
    </section>

    <p v-if="loading" class="muted">加载中...</p>

    <div v-else class="comment-list">
      <article v-for="item in filteredList" :key="item.id" class="link-admin-card">
        <div class="link-logo">
          <img v-if="item.logo" :src="item.logo" :alt="item.name" />
          <span v-else>{{ item.name.slice(0, 1).toUpperCase() }}</span>
        </div>
        <div class="comment-body">
          <div class="comment-meta">
            <strong>{{ item.name }}</strong>
            <span class="status-pill" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
            <span>排序值 {{ item.sortOrder }}</span>
          </div>
          <p>{{ item.description || "暂无简介" }}</p>
          <small>{{ item.url }}</small>
        </div>
        <div class="action-cell comment-actions">
          <button class="ghost-btn table-btn" @click="edit(item)">编辑</button>
          <button class="ghost-btn table-btn" @click="changeStatus(item.id, 1)">通过</button>
          <button class="ghost-btn table-btn" @click="changeStatus(item.id, 2)">拒绝</button>
          <button class="ghost-btn table-btn danger-btn" @click="remove(item.id)">删除</button>
        </div>
      </article>

      <div v-if="filteredList.length === 0" class="empty-state">
        <h3>没有匹配的友情链接</h3>
        <p>可以调整关键词或状态筛选。</p>
      </div>
    </div>
  </section>
</template>
