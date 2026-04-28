<script setup lang="ts">
// AuditLogManager.vue 是后台操作日志页：负责查询、筛选和查看管理员操作审计详情。
import { computed, onMounted, ref } from "vue";
import { getAuditLogPage } from "../../api/audit";
import type { AuditLog } from "../../types/audit";
// moduleOptions 和 actionOptions 用于生成筛选下拉框，覆盖后台常见日志模块与动作类型。

const moduleOptions = ["系统", "文章", "分类", "标签", "评论", "留言", "友链", "站点"];
const actionOptions = ["登录", "新增", "编辑", "删除", "审核", "回复", "更新状态", "更新设置"];

// loading 表示日志列表是否正在加载。
const loading = ref(false);
const errorText = ref("");
// keyword 支持按操作人、资源名称和详情摘要搜索日志。
// keyword 支持按操作人、资源名称和详情摘要搜索日志。
const keyword = ref("");
const moduleFilter = ref("");
// moduleFilter、actionFilter 分别对应模块和动作筛选条件。
const actionFilter = ref("");
const list = ref<AuditLog[]>([]);
// list、currentPage、pageSize、total、totalPages 一起维护当前日志分页结果。
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);

// hasPrevious 和 hasNext 控制审计日志分页按钮状态。
const hasPrevious = computed(() => currentPage.value > 1);
const hasNext = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value);
const uniqueOperators = computed(() => new Set(list.value.map((item) => item.operator)).size);
// 这些统计值用于顶部卡片，帮助管理员快速了解当前筛选结果的范围和分布。
const moduleCount = computed(() => new Set(list.value.map((item) => item.module)).size);
const writeActionCount = computed(() =>
  list.value.filter((item) => ["新增", "编辑", "删除", "更新设置", "更新状态"].includes(item.action)).length
);

// loadList 按模块、操作、关键词和分页参数加载操作日志。
const loadList = async (page = 1) => {
  loading.value = true;
  errorText.value = "";
  try {
    const data = await getAuditLogPage({
      keyword: keyword.value.trim(),
      module: moduleFilter.value,
      action: actionFilter.value,
      page,
      size: pageSize.value
    });
    list.value = data.records;
    total.value = data.total;
    totalPages.value = data.totalPages;
    currentPage.value = data.page;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载操作日志失败";
  } finally {
    loading.value = false;
  }
};

// search 从第一页重新查询操作日志。
const search = () => {
  void loadList(1);
};

// resetFilters 清空操作日志筛选条件。
const resetFilters = () => {
  keyword.value = "";
  moduleFilter.value = "";
  actionFilter.value = "";
  pageSize.value = 10;
  void loadList(1);
};

// previousPage 跳转到上一页日志列表。
const previousPage = () => {
  if (hasPrevious.value) {
    void loadList(currentPage.value - 1);
  }
};

// nextPage 跳转到下一页日志列表。
const nextPage = () => {
  if (hasNext.value) {
    void loadList(currentPage.value + 1);
  }
};

const formatTime = (value: string) => {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString();
};

const actionClass = (action: string) => {
  if (["删除", "已拒绝"].includes(action)) {
    return "archived";
  }
  if (["新增", "登录", "更新设置"].includes(action)) {
    return "published";
  }
  return "draft";
};

const targetText = (item: AuditLog) => {
  if (!item.targetType && !item.targetId) {
    return "-";
  }
  return item.targetId ? `${item.targetType || "TARGET"} #${item.targetId}` : item.targetType;
};

onMounted(() => loadList(1));
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>操作日志</h2>
        <p class="muted">记录管理员登录、内容维护、审核、回复和站点设置等关键动作。</p>
      </div>
      <button class="ghost-btn" @click="loadList(currentPage)">刷新</button>
    </header>

    <section class="comment-stats audit-stats">
      <div>
        <strong>{{ total }}</strong>
        <span>筛选总数</span>
      </div>
      <div>
        <strong>{{ uniqueOperators }}</strong>
        <span>本页操作人</span>
      </div>
      <div>
        <strong>{{ moduleCount }}</strong>
        <span>本页模块</span>
      </div>
      <div>
        <strong>{{ writeActionCount }}</strong>
        <span>本页写操作</span>
      </div>
    </section>

    <section class="admin-toolbar audit-toolbar">
      <input v-model="keyword" type="search" placeholder="搜索操作人、详情、对象或 IP" @keyup.enter="search" />
      <select v-model="moduleFilter" @change="search">
        <option value="">全部模块</option>
        <option v-for="item in moduleOptions" :key="item" :value="item">{{ item }}</option>
      </select>
      <select v-model="actionFilter" @change="search">
        <option value="">全部动作</option>
        <option v-for="item in actionOptions" :key="item" :value="item">{{ item }}</option>
      </select>
      <select v-model.number="pageSize" @change="search">
        <option :value="10">10 条/页</option>
        <option :value="20">20 条/页</option>
        <option :value="50">50 条/页</option>
        <option :value="100">100 条/页</option>
      </select>
      <button class="ghost-btn" @click="search">搜索</button>
      <button class="ghost-btn" @click="resetFilters">重置</button>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="loading" class="muted">加载中...</p>

    <div v-else class="article-table-wrap audit-table-wrap">
      <table class="article-table audit-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>操作人</th>
            <th>模块</th>
            <th>动作</th>
            <th>对象</th>
            <th>详情</th>
            <th>IP</th>
            <th>设备</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>{{ formatTime(item.createdAt) }}</td>
            <td>
              <strong>{{ item.operator || "admin" }}</strong>
            </td>
            <td>{{ item.module }}</td>
            <td>
              <span class="status-pill" :class="actionClass(item.action)">{{ item.action }}</span>
            </td>
            <td>{{ targetText(item) }}</td>
            <td class="audit-detail-cell">{{ item.detail || "-" }}</td>
            <td>{{ item.ip || "-" }}</td>
            <td class="audit-agent-cell">{{ item.userAgent || "-" }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="list.length === 0" class="empty-state">
        <h3>还没有匹配的操作记录</h3>
        <p>进行登录、发布文章、审核评论等后台动作后，这里会自动留下记录。</p>
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
