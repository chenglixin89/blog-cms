<script setup lang="ts">
// CategoryManager.vue 是后台分类管理页：负责文章分类的新增、编辑、删除和排序。
import { computed, onMounted, reactive, ref } from "vue";
import { getArticleList } from "../../api/article";
import { createCategory, deleteCategory, getCategoryList, updateCategory } from "../../api/taxonomy";
import type { Article } from "../../types/article";
import type { Category, CategoryPayload } from "../../types/taxonomy";

// loading 表示分类列表是否正在加载。
const loading = ref(false);
// saving 表示分类表单是否正在提交。
const saving = ref(false);
// movingId 记录当前正在调整排序的分类 id，避免重复点击。
const movingId = ref<number | null>(null);
const errorText = ref("");
const list = ref<Category[]>([]);
// articles 用于在前端实时统计每个分类下关联的文章数量。
const articles = ref<Article[]>([]);
// articleStatsReady 表示分类统计是否使用到了实时文章数据。
const articleStatsReady = ref(false);
const editingId = ref<number | null>(null);

// form 保存分类表单数据，新增和编辑共用这一份表单。
const form = reactive<CategoryPayload>({
  name: "",
  slug: "",
  description: "",
  sortOrder: 0
});

// resetForm 清空分类表单，并退出编辑状态。
const resetForm = () => {
  editingId.value = null;
  form.name = "";
  form.slug = "";
  form.description = "";
  form.sortOrder = 0;
};

// categoryPayload 用于在调整排序时复用已有分类数据，只改 sortOrder。
const categoryPayload = (item: Category, sortOrder = item.sortOrder): CategoryPayload => ({
  name: item.name,
  slug: item.slug,
  description: item.description,
  sortOrder
});

// displayedCategories 按 sortOrder 和 id 排序，确保页面展示顺序稳定。
const displayedCategories = computed(() => {
  return [...list.value].sort((a, b) => {
    const sortDiff = (b.sortOrder ?? 0) - (a.sortOrder ?? 0);
    return sortDiff === 0 ? b.id - a.id : sortDiff;
  });
});

// loadList 从后端加载所有分类，并刷新文章数量统计。
const loadList = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [categoryResult, articleResult] = await Promise.allSettled([getCategoryList(), getArticleList()]);

    if (categoryResult.status === "fulfilled") {
      list.value = categoryResult.value;
    } else {
      throw categoryResult.reason;
    }

    if (articleResult.status === "fulfilled") {
      articles.value = articleResult.value;
      articleStatsReady.value = true;
    } else {
      articles.value = [];
      articleStatsReady.value = false;
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载分类失败";
  } finally {
    loading.value = false;
  }
};

// publishedArticleCount 统计该分类下已发布文章数量。
const publishedArticleCount = (item: Category) => {
  if (!articleStatsReady.value) {
    return item.articleCount ?? 0;
  }

  return articles.value.filter((article) => article.status === 1 && article.categoryId === item.id).length;
};

// totalArticleCount 统计该分类下全部文章数量。
const totalArticleCount = (item: Category) => {
  if (!articleStatsReady.value) {
    return item.articleCount ?? 0;
  }

  return articles.value.filter((article) => article.categoryId === item.id).length;
};

// edit 把当前分类数据回填到表单，进入编辑状态。
const edit = (item: Category) => {
  editingId.value = item.id;
  form.name = item.name;
  form.slug = item.slug;
  form.description = item.description;
  form.sortOrder = item.sortOrder;
};

// save 根据是否存在编辑 id 决定新增分类或更新分类。
const save = async () => {
  if (!form.name.trim()) {
    errorText.value = "分类名称不能为空";
    return;
  }

  saving.value = true;
  errorText.value = "";
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, form);
    } else {
      await createCategory(form);
    }
    resetForm();
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "保存分类失败";
  } finally {
    saving.value = false;
  }
};

// moveCategory 通过交换 sortOrder 实现分类上移或下移。
const moveCategory = async (index: number, direction: "up" | "down") => {
  const ordered = displayedCategories.value;
  const target = ordered[index];
  const neighbor = ordered[direction === "up" ? index - 1 : index + 1];

  if (!target || !neighbor) {
    return;
  }

  movingId.value = target.id;
  errorText.value = "";
  try {
    const targetSort = target.sortOrder ?? 0;
    const neighborSort = neighbor.sortOrder ?? 0;

    if (targetSort === neighborSort) {
      const nextSort = direction === "up" ? neighborSort + 1 : neighborSort - 1;
      await updateCategory(target.id, categoryPayload(target, nextSort));
    } else {
      await Promise.all([
        updateCategory(target.id, categoryPayload(target, neighborSort)),
        updateCategory(neighbor.id, categoryPayload(neighbor, targetSort))
      ]);
    }

    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "调整分类排序失败";
  } finally {
    movingId.value = null;
  }
};

// remove 删除分类，通常需要后端校验该分类下是否还有文章。
const remove = async (id: number) => {
  if (!window.confirm("确认删除这个分类吗？已关联文章建议先迁移分类。")) {
    return;
  }

  try {
    await deleteCategory(id);
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "删除分类失败";
  }
};

onMounted(loadList);
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <p class="eyebrow">Taxonomy</p>
        <h2>分类管理</h2>
      </div>
      <button class="ghost-btn" @click="resetForm">清空表单</button>
    </header>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="!articleStatsReady && !loading" class="muted">文章统计暂时使用后端返回值，重启后端后会同步为实时统计。</p>

    <form class="taxonomy-form" @submit.prevent="save">
      <input v-model="form.name" type="text" placeholder="分类名称" />
      <input v-model="form.slug" type="text" placeholder="URL 标识，可留空自动生成" />
      <input v-model="form.description" type="text" placeholder="分类描述" />
      <input v-model.number="form.sortOrder" type="number" placeholder="排序，数字越大越靠前" />
      <button class="primary-btn" type="submit" :disabled="saving">
        {{ saving ? "保存中..." : editingId ? "更新分类" : "新增分类" }}
      </button>
    </form>

    <p v-if="loading" class="muted">加载中...</p>

    <div v-else class="taxonomy-grid">
      <article v-for="(item, index) in displayedCategories" :key="item.id" class="taxonomy-card">
        <div>
          <h3>{{ item.name }}</h3>
          <p>{{ item.description || "暂无描述" }}</p>
          <div class="taxonomy-meta">
            <span>{{ item.slug || "未设置 URL 标识" }}</span>
            <span class="sort-pill">排序值 {{ item.sortOrder }}</span>
            <span class="count-pill">已发布 {{ publishedArticleCount(item) }} 篇</span>
            <span v-if="articleStatsReady" class="count-pill secondary">全部 {{ totalArticleCount(item) }} 篇</span>
          </div>
        </div>
        <div class="action-cell">
          <button
            class="ghost-btn table-btn"
            :disabled="index === 0 || movingId === item.id"
            @click="moveCategory(index, 'up')"
          >
            上移
          </button>
          <button
            class="ghost-btn table-btn"
            :disabled="index === displayedCategories.length - 1 || movingId === item.id"
            @click="moveCategory(index, 'down')"
          >
            下移
          </button>
          <button class="ghost-btn table-btn" @click="edit(item)">编辑</button>
          <button class="ghost-btn table-btn danger-btn" @click="remove(item.id)">删除</button>
        </div>
      </article>

      <p v-if="!list.length" class="muted">还没有分类，先新增一个文章分类吧。</p>
    </div>
  </section>
</template>
