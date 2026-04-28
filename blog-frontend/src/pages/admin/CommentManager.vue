<script setup lang="ts">
// CommentManager.vue 是后台评论管理页：负责评论查询、审核、拒绝、回复和删除。
import { computed, onMounted, reactive, ref } from "vue";
import { deleteComment, getCommentPage, replyComment, updateCommentStatus } from "../../api/comment";
import type { CommentItem } from "../../types/comment";

// loading 表示评论列表是否正在加载。
const loading = ref(false);
// replying 表示管理员回复评论时是否正在提交。
const replying = ref(false);
const errorText = ref("");
const successText = ref("");
// keyword 支持按昵称、评论内容、文章标题和邮箱搜索评论。
const keyword = ref("");
// statusFilter 控制待审核、已通过、已拒绝等评论筛选。
const statusFilter = ref<number | "all">("all");
// activeReplyId 记录当前展开回复框的评论 id。
const activeReplyId = ref<number | null>(null);
// replyForms 按评论 id 保存管理员回复内容，支持多个评论临时输入。
const replyForms = reactive<Record<number, string>>({});
const list = ref<CommentItem[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);

// 以下 computed 负责分页按钮和本页评论状态统计。
const hasPrevious = computed(() => currentPage.value > 1);
const hasNext = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value);
const pendingCount = computed(() => list.value.filter((item) => item.status === 0).length);
const approvedCount = computed(() => list.value.filter((item) => item.status === 1).length);
const rejectedCount = computed(() => list.value.filter((item) => item.status === 2).length);
const replyCount = computed(() => list.value.filter((item) => item.parentId).length);

// statusParam 把页面筛选值转换成后端可识别的状态参数。
const statusParam = () => (statusFilter.value === "all" ? undefined : Number(statusFilter.value));

// loadList 按关键词、审核状态和分页参数加载评论。
const loadList = async (page = 1) => {
  loading.value = true;
  errorText.value = "";
  try {
    const data = await getCommentPage({
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
    errorText.value = error instanceof Error ? error.message : "加载评论失败";
  } finally {
    loading.value = false;
  }
};

// statusText 和 statusClass 把评论审核状态码转换为页面展示。
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

const search = () => {
  void loadList(1);
};

const resetFilters = () => {
  keyword.value = "";
  statusFilter.value = "all";
  pageSize.value = 10;
  void loadList(1);
};

const previousPage = () => {
  if (hasPrevious.value) {
    void loadList(currentPage.value - 1);
  }
};

const nextPage = () => {
  if (hasNext.value) {
    void loadList(currentPage.value + 1);
  }
};

// changeStatus 调用后端接口修改评论审核状态。
const changeStatus = async (id: number, status: number) => {
  errorText.value = "";
  successText.value = "";
  try {
    await updateCommentStatus(id, status);
    successText.value = status === 1 ? "评论已通过。" : status === 2 ? "评论已拒绝。" : "评论已改为待审核。";
    await loadList(currentPage.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "更新评论状态失败";
  }
};

// openReply 展开或收起某条评论的回复输入框。
const openReply = (id: number) => {
  activeReplyId.value = activeReplyId.value === id ? null : id;
  replyForms[id] = replyForms[id] ?? "";
};

// submitReply 发布管理员回复，回复会在前台评论区展示。
const submitReply = async (id: number) => {
  const content = replyForms[id]?.trim();
  if (!content) {
    errorText.value = "回复内容不能为空";
    return;
  }

  replying.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await replyComment(id, content);
    replyForms[id] = "";
    activeReplyId.value = null;
    successText.value = "管理员回复已发布到前台。";
    await loadList(currentPage.value);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "回复失败";
  } finally {
    replying.value = false;
  }
};

// remove 删除评论，删除后会根据当前页数量自动调整分页。
const remove = async (id: number) => {
  if (!window.confirm("确认删除这条评论吗？")) {
    return;
  }
  try {
    await deleteComment(id);
    const nextPageNumber = list.value.length === 1 && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value;
    await loadList(nextPageNumber);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "删除评论失败";
  }
};

onMounted(() => loadList(1));
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>评论管理</h2>
        <p class="muted">审核读者留言，也可以直接回复并展示到前台。</p>
      </div>
      <button class="ghost-btn" @click="loadList(currentPage)">刷新</button>
    </header>

    <section class="comment-stats">
      <div>
        <strong>{{ total }}</strong>
        <span>筛选总数</span>
      </div>
      <div>
        <strong>{{ pendingCount }}</strong>
        <span>本页待审核</span>
      </div>
      <div>
        <strong>{{ approvedCount }}</strong>
        <span>本页已通过</span>
      </div>
      <div>
        <strong>{{ rejectedCount + replyCount }}</strong>
        <span>拒绝/回复</span>
      </div>
    </section>

    <section class="admin-toolbar">
      <input v-model="keyword" type="search" placeholder="搜索昵称、内容、文章或邮箱" @keyup.enter="search" />
      <select v-model="statusFilter" @change="search">
        <option value="all">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="1">已通过</option>
        <option :value="2">已拒绝</option>
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

    <div v-else class="comment-list">
      <article v-for="item in list" :key="item.id" class="comment-card enhanced-comment-card">
        <div class="comment-body">
          <div class="comment-meta">
            <strong>{{ item.nickname }}</strong>
            <span class="status-pill" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
            <span>{{ item.articleTitle || "未知文章" }}</span>
            <span>{{ new Date(item.createdAt).toLocaleString() }}</span>
          </div>
          <p>{{ item.content }}</p>
          <small>
            {{ item.email || "未填写邮箱" }}
            <span v-if="item.parentId"> · 回复 #{{ item.parentId }}</span>
          </small>

          <div v-if="activeReplyId === item.id" class="admin-reply-box">
            <textarea v-model="replyForms[item.id]" placeholder="输入管理员回复，提交后会直接展示在前台"></textarea>
            <div>
              <button class="primary-btn" :disabled="replying" @click="submitReply(item.id)">
                {{ replying ? "回复中..." : "提交回复" }}
              </button>
              <button class="ghost-btn" @click="activeReplyId = null">取消</button>
            </div>
          </div>
        </div>
        <div class="action-cell comment-actions">
          <button class="ghost-btn table-btn" @click="changeStatus(item.id, 1)">通过</button>
          <button class="ghost-btn table-btn" @click="changeStatus(item.id, 2)">拒绝</button>
          <button class="ghost-btn table-btn" @click="openReply(item.id)">回复</button>
          <button class="ghost-btn table-btn danger-btn" @click="remove(item.id)">删除</button>
        </div>
      </article>

      <div v-if="list.length === 0" class="empty-state">
        <h3>没有匹配的评论</h3>
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