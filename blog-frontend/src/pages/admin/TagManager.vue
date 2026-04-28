<script setup lang="ts">
// TagManager.vue 是后台标签管理页：负责标签新增、编辑、删除、颜色和排序维护。
import { onMounted, reactive, ref } from "vue";
import { getArticleList } from "../../api/article";
import { createTag, deleteTag, getTagList, updateTag } from "../../api/taxonomy";
import type { Article } from "../../types/article";
import type { Tag, TagPayload } from "../../types/taxonomy";

// loading 控制标签列表加载状态。
const loading = ref(false);
// saving 表示标签表单是否正在提交。
const saving = ref(false);
const errorText = ref("");
const list = ref<Tag[]>([]);
// articles 用于实时计算每个标签关联的文章数量。
const articles = ref<Article[]>([]);
// articleStatsReady 表示标签统计是否已加载文章数据。
const articleStatsReady = ref(false);
const editingId = ref<number | null>(null);

// form 保存标签表单数据，包括名称、颜色和排序。
const form = reactive<TagPayload>({
  name: "",
  color: "#0a84ff"
});

// resetForm 清空标签表单并退出编辑状态。
const resetForm = () => {
  editingId.value = null;
  form.name = "";
  form.color = "#0a84ff";
};

// loadList 从后端加载标签列表。
const loadList = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [tagResult, articleResult] = await Promise.allSettled([getTagList(), getArticleList()]);

    if (tagResult.status === "fulfilled") {
      list.value = tagResult.value;
    } else {
      throw tagResult.reason;
    }

    if (articleResult.status === "fulfilled") {
      articles.value = articleResult.value;
      articleStatsReady.value = true;
    } else {
      articles.value = [];
      articleStatsReady.value = false;
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载标签失败";
  } finally {
    loading.value = false;
  }
};

// publishedArticleCount 统计该标签下已发布文章数量。
const publishedArticleCount = (item: Tag) => {
  if (!articleStatsReady.value) {
    return item.articleCount ?? 0;
  }

  return articles.value.filter((article) => {
    return article.status === 1 && (article.tags ?? []).some((tag) => tag.id === item.id);
  }).length;
};

// totalArticleCount 统计该标签下全部文章数量。
const totalArticleCount = (item: Tag) => {
  if (!articleStatsReady.value) {
    return item.articleCount ?? 0;
  }

  return articles.value.filter((article) => (article.tags ?? []).some((tag) => tag.id === item.id)).length;
};

// edit 将选中的标签数据回填到表单。
const edit = (item: Tag) => {
  editingId.value = item.id;
  form.name = item.name;
  form.color = item.color;
};

// save 根据编辑状态执行新增或更新标签。
const save = async () => {
  if (!form.name.trim()) {
    errorText.value = "标签名称不能为空";
    return;
  }

  saving.value = true;
  errorText.value = "";
  try {
    if (editingId.value) {
      await updateTag(editingId.value, form);
    } else {
      await createTag(form);
    }
    resetForm();
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "保存标签失败";
  } finally {
    saving.value = false;
  }
};

// remove 删除标签，删除前会进行确认。
const remove = async (id: number) => {
  if (!window.confirm("确认删除这个标签吗？已关联文章建议先移除标签。")) {
    return;
  }

  try {
    await deleteTag(id);
    await loadList();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "删除标签失败";
  }
};

onMounted(loadList);
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <p class="eyebrow">Tags</p>
        <h2>标签管理</h2>
      </div>
      <button class="ghost-btn" @click="resetForm">清空表单</button>
    </header>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="!articleStatsReady && !loading" class="muted">文章统计暂时使用后端返回值，重启后端后会同步为实时统计。</p>

    <form class="taxonomy-form tag-form" @submit.prevent="save">
      <input v-model="form.name" type="text" placeholder="标签名称" />
      <input v-model="form.color" type="color" aria-label="标签颜色" />
      <button class="primary-btn" type="submit" :disabled="saving">
        {{ saving ? "保存中..." : editingId ? "更新标签" : "新增标签" }}
      </button>
    </form>

    <p v-if="loading" class="muted">加载中...</p>

    <div v-else class="tag-cloud">
      <article v-for="item in list" :key="item.id" class="tag-card">
        <div class="tag-preview">
          <span class="color-dot" :style="{ background: item.color }"></span>
          <strong>{{ item.name }}</strong>
          <span class="count-pill">已发布 {{ publishedArticleCount(item) }} 篇</span>
          <span v-if="articleStatsReady" class="count-pill secondary">全部 {{ totalArticleCount(item) }} 篇</span>
        </div>
        <div class="action-cell">
          <button class="ghost-btn table-btn" @click="edit(item)">编辑</button>
          <button class="ghost-btn table-btn danger-btn" @click="remove(item.id)">删除</button>
        </div>
      </article>

      <p v-if="!list.length" class="muted">还没有标签，给文章准备几个主题标签吧。</p>
    </div>
  </section>
</template>
