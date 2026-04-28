<script setup lang="ts">
// MediaLibrary.vue 是后台媒体库：统一管理上传图片、图片分类、复制图片地址和删除资源。
import { computed, onMounted, reactive, ref } from "vue";
import { createMediaCategory, deleteMedia, deleteMediaCategory, getMediaCategories, getMediaPage, updateMediaAssetCategory, updateMediaCategory, uploadMedia } from "../../api/media";
import type { MediaAsset, MediaCategory } from "../../types/media";

// TEXT 集中存放页面文案，方便维护和后续国际化。
const TEXT = {
  title: "\u5a92\u4f53\u5e93",
  desc: "\u7edf\u4e00\u7ba1\u7406\u6587\u7ae0\u5c01\u9762\u3001\u6b63\u6587\u63d2\u56fe\u548c\u81ea\u5b9a\u4e49\u7d20\u6750\u5206\u7c7b\uff0c\u652f\u6301 jpg / png / gif / webp\u3002",
  uploadTo: "\u4e0a\u4f20\u5230\uff1a",
  uploading: "\u4e0a\u4f20\u4e2d...",
  uploadImage: "\u4e0a\u4f20\u56fe\u7247",
  totalMedia: "\u5a92\u4f53\u603b\u6570",
  pageImages: "\u672c\u9875\u56fe\u7247",
  pageSize: "\u672c\u9875\u4f53\u79ef",
  currentCategory: "\u5f53\u524d\u5206\u7c7b",
  all: "\u5168\u90e8",
  uncategorized: "\u672a\u5206\u7c7b",
  addCategory: "\u65b0\u589e\u5a92\u4f53\u5206\u7c7b",
  editCategory: "\u7f16\u8f91\u5a92\u4f53\u5206\u7c7b",
  categoryNamePlaceholder: "\u5206\u7c7b\u540d\u79f0\uff0c\u4f8b\u5982\uff1a\u9879\u76ee\u622a\u56fe",
  categoryCodePlaceholder: "\u5206\u7c7b\u7f16\u7801\uff0c\u53ef\u7559\u7a7a\u81ea\u52a8\u751f\u6210",
  categoryDescPlaceholder: "\u5206\u7c7b\u8bf4\u660e",
  sortPlaceholder: "\u6392\u5e8f\u503c",
  saving: "\u4fdd\u5b58\u4e2d...",
  saveCategory: "\u4fdd\u5b58\u5206\u7c7b",
  cancel: "\u53d6\u6d88",
  system: "\u7cfb\u7edf",
  searchPlaceholder: "\u641c\u7d22\u539f\u6587\u4ef6\u540d\u6216\u5b58\u50a8\u6587\u4ef6\u540d",
  allCategories: "\u5168\u90e8\u5206\u7c7b",
  search: "\u641c\u7d22",
  reset: "\u91cd\u7f6e",
  loading: "\u52a0\u8f7d\u4e2d...",
  copyUrl: "\u590d\u5236\u5730\u5740",
  remove: "\u79fb\u9664",
  emptyTitle: "\u5a92\u4f53\u5e93\u8fd8\u662f\u7a7a\u7684",
  emptyDesc: "\u4e0a\u4f20\u5c01\u9762\u6216\u6b63\u6587\u56fe\u7247\u540e\uff0c\u4f1a\u81ea\u52a8\u51fa\u73b0\u5728\u8fd9\u91cc\u3002",
  categoryList: "\u5206\u7c7b\u5217\u8868",
  name: "\u540d\u79f0",
  code: "\u7f16\u7801",
  imageCount: "\u56fe\u7247\u6570",
  sort: "\u6392\u5e8f",
  action: "\u64cd\u4f5c",
  edit: "\u7f16\u8f91",
  delete: "\u5220\u9664",
  prev: "\u4e0a\u4e00\u9875",
  next: "\u4e0b\u4e00\u9875",
  categoryRequired: "\u5206\u7c7b\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a",
  loadFail: "\u52a0\u8f7d\u5a92\u4f53\u5e93\u5931\u8d25",
  uploadOk: "\u4e0a\u4f20\u5b8c\u6210\uff0c\u5a92\u4f53\u5e93\u5df2\u66f4\u65b0\u3002",
  uploadFail: "\u4e0a\u4f20\u5931\u8d25",
  copyOk: "\u56fe\u7247\u5730\u5740\u5df2\u590d\u5236\u3002",
  deleteConfirm: "\u786e\u8ba4\u4ece\u5a92\u4f53\u5e93\u79fb\u9664\u8fd9\u5f20\u56fe\u7247\u5417\uff1f\u6587\u7ae0\u4e2d\u5df2\u4f7f\u7528\u7684\u56fe\u7247\u4e0d\u4f1a\u81ea\u52a8\u66ff\u6362\u3002",
  deleteFail: "\u5220\u9664\u5931\u8d25",
  categoryUpdateOk: "\u56fe\u7247\u5206\u7c7b\u5df2\u66f4\u65b0\u3002",
  categoryUpdateFail: "\u4fee\u6539\u56fe\u7247\u5206\u7c7b\u5931\u8d25",
  saveCategoryOk: "\u5a92\u4f53\u5206\u7c7b\u5df2\u4fdd\u5b58\u3002",
  createCategoryOk: "\u5a92\u4f53\u5206\u7c7b\u5df2\u521b\u5efa\u3002",
  saveCategoryFail: "\u4fdd\u5b58\u5206\u7c7b\u5931\u8d25",
  deleteCategoryConfirm: "\u786e\u8ba4\u5220\u9664\u8fd9\u4e2a\u5a92\u4f53\u5206\u7c7b\u5417\uff1f\u5206\u7c7b\u4e0b\u8fd8\u6709\u56fe\u7247\u65f6\u4e0d\u80fd\u5220\u9664\u3002",
  deleteCategoryOk: "\u5a92\u4f53\u5206\u7c7b\u5df2\u5220\u9664\u3002",
  deleteCategoryFail: "\u5220\u9664\u5206\u7c7b\u5931\u8d25"
};

const loading = ref(false);
const uploading = ref(false);
const categorySaving = ref(false);
const errorText = ref("");
const successText = ref("");
const keyword = ref("");
const filterCategory = ref("");
const uploadCategory = ref("content");
const list = ref<MediaAsset[]>([]);
const categories = ref<MediaCategory[]>([]);
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);
const totalPages = ref(0);
const editingCategoryId = ref<number | null>(null);
const assetCategorySaving = ref<Record<number, boolean>>({});

// categoryForm 保存媒体分类表单，用于新增和编辑自定义分类。
const categoryForm = reactive({ name: "", code: "", description: "", sortOrder: 0 });

// 以下 computed 负责分页、统计和当前分类名称展示。
const hasPrevious = computed(() => currentPage.value > 1);
const hasNext = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value);
const totalSize = computed(() => list.value.reduce((sum, item) => sum + item.size, 0));
const currentCategoryName = computed(() => filterCategory.value ? categoryName(filterCategory.value) : TEXT.all);

// categoryName 根据分类 code 查找分类名称，找不到时显示兜底文本。
const categoryName = (code: string) => categories.value.find((item) => item.code === code)?.name || code || TEXT.uncategorized;

// loadCategories 加载媒体分类，同时保证上传分类始终有可用默认值。
const loadCategories = async () => {
  categories.value = await getMediaCategories();
  if (!categories.value.some((item) => item.code === uploadCategory.value)) {
    uploadCategory.value = categories.value.find((item) => item.code === "content")?.code || categories.value[0]?.code || "general";
  }
};

// loadList 按关键词、分类和页码加载媒体资源列表。
const loadList = async (page = 1) => {
  loading.value = true;
  errorText.value = "";
  try {
    const data = await getMediaPage({ keyword: keyword.value.trim(), category: filterCategory.value, page, size: pageSize.value });
    list.value = data.records;
    total.value = data.total;
    totalPages.value = data.totalPages;
    currentPage.value = data.page;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.loadFail;
  } finally {
    loading.value = false;
  }
};

// onUpload 处理多文件上传，上传成功后刷新分类统计和当前列表。
const onUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (files.length === 0) return;
  uploading.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    for (const file of files) {
      await uploadMedia(file, uploadCategory.value || "general");
    }
    successText.value = TEXT.uploadOk;
    filterCategory.value = uploadCategory.value;
    await Promise.all([loadCategories(), loadList(1)]);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.uploadFail;
  } finally {
    uploading.value = false;
    input.value = "";
  }
};

// copyUrl 把图片 HTTP 地址复制到剪贴板，方便粘贴到文章正文或封面。
const copyUrl = async (url: string) => {
  await navigator.clipboard.writeText(url);
  successText.value = TEXT.copyOk;
};

// remove 删除媒体库中的图片记录，删除前会弹窗确认。
const remove = async (item: MediaAsset) => {
  if (!window.confirm(TEXT.deleteConfirm)) return;
  try {
    await deleteMedia(item.id);
    await Promise.all([loadCategories(), loadList(currentPage.value)]);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.deleteFail;
  }
};

// changeAssetCategory 修改单张图片所属分类，并同步刷新分类统计。
const changeAssetCategory = async (item: MediaAsset, category: string) => {
  assetCategorySaving.value[item.id] = true;
  errorText.value = "";
  successText.value = "";
  try {
    const updated = await updateMediaAssetCategory(item.id, category);
    item.category = updated.category;
    successText.value = TEXT.categoryUpdateOk;
    await loadCategories();
    if (filterCategory.value && filterCategory.value !== updated.category) {
      await loadList(currentPage.value);
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.categoryUpdateFail;
    await loadList(currentPage.value);
  } finally {
    assetCategorySaving.value[item.id] = false;
  }
};

// resetCategoryForm 清空媒体分类表单，退出编辑状态。
const resetCategoryForm = () => {
  editingCategoryId.value = null;
  categoryForm.name = "";
  categoryForm.code = "";
  categoryForm.description = "";
  categoryForm.sortOrder = 0;
};

const editCategory = (item: MediaCategory) => {
  editingCategoryId.value = item.id;
  categoryForm.name = item.name;
  categoryForm.code = item.code;
  categoryForm.description = item.description || "";
  categoryForm.sortOrder = item.sortOrder || 0;
};

// saveCategory 根据是否存在 editingCategoryId 决定新增或更新媒体分类。
const saveCategory = async () => {
  if (!categoryForm.name.trim()) {
    errorText.value = TEXT.categoryRequired;
    return;
  }
  categorySaving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const payload = { name: categoryForm.name.trim(), code: categoryForm.code.trim(), description: categoryForm.description.trim(), sortOrder: categoryForm.sortOrder };
    if (editingCategoryId.value) {
      await updateMediaCategory(editingCategoryId.value, payload);
      successText.value = TEXT.saveCategoryOk;
    } else {
      const created = await createMediaCategory(payload);
      filterCategory.value = created.code;
      uploadCategory.value = created.code;
      successText.value = TEXT.createCategoryOk;
    }
    resetCategoryForm();
    await Promise.all([loadCategories(), loadList(1)]);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.saveCategoryFail;
  } finally {
    categorySaving.value = false;
  }
};

// removeCategory 删除自定义媒体分类，系统分类不允许删除。
const removeCategory = async (item: MediaCategory) => {
  if (item.isSystem === 1) return;
  if (!window.confirm(TEXT.deleteCategoryConfirm)) return;
  try {
    await deleteMediaCategory(item.id);
    if (filterCategory.value === item.code) filterCategory.value = "";
    if (uploadCategory.value === item.code) uploadCategory.value = "general";
    successText.value = TEXT.deleteCategoryOk;
    await Promise.all([loadCategories(), loadList(1)]);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : TEXT.deleteCategoryFail;
  }
};

const search = () => void loadList(1);
const resetFilters = () => { keyword.value = ""; filterCategory.value = ""; pageSize.value = 12; void loadList(1); };
const previousPage = () => hasPrevious.value && void loadList(currentPage.value - 1);
const nextPage = () => hasNext.value && void loadList(currentPage.value + 1);
const formatSize = (size: number) => size < 1024 * 1024 ? (size / 1024).toFixed(1) + " KB" : (size / 1024 / 1024).toFixed(2) + " MB";
const formatTime = (value: string) => (value ? new Date(value).toLocaleString() : "-");

onMounted(async () => {
  await loadCategories();
  await loadList(1);
});
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>{{ TEXT.title }}</h2>
        <p class="muted">{{ TEXT.desc }}</p>
      </div>
      <div class="media-upload-panel">
        <select v-model="uploadCategory" class="select-input">
          <option v-for="item in categories" :key="item.code" :value="item.code">{{ TEXT.uploadTo }}{{ item.name }}</option>
        </select>
        <label class="upload-btn media-upload-btn">
          {{ uploading ? TEXT.uploading : TEXT.uploadImage }}
          <input type="file" multiple accept=".jpg,.jpeg,.png,.gif,.webp,image/jpeg,image/png,image/gif,image/webp" :disabled="uploading" @change="onUpload" />
        </label>
      </div>
    </header>

    <section class="comment-stats media-stats">
      <div><strong>{{ total }}</strong><span>{{ TEXT.totalMedia }}</span></div>
      <div><strong>{{ list.length }}</strong><span>{{ TEXT.pageImages }}</span></div>
      <div><strong>{{ formatSize(totalSize) }}</strong><span>{{ TEXT.pageSize }}</span></div>
      <div><strong>{{ currentCategoryName }}</strong><span>{{ TEXT.currentCategory }}</span></div>
    </section>

    <section class="media-category-manager">
      <div class="media-category-form glass-lite">
        <h3>{{ editingCategoryId ? TEXT.editCategory : TEXT.addCategory }}</h3>
        <input v-model="categoryForm.name" type="text" :placeholder="TEXT.categoryNamePlaceholder" />
        <input v-model="categoryForm.code" type="text" :placeholder="TEXT.categoryCodePlaceholder" :disabled="!!editingCategoryId" />
        <input v-model="categoryForm.description" type="text" :placeholder="TEXT.categoryDescPlaceholder" />
        <input v-model.number="categoryForm.sortOrder" type="number" :placeholder="TEXT.sortPlaceholder" />
        <div class="media-category-actions">
          <button class="primary-btn" type="button" :disabled="categorySaving" @click="saveCategory">{{ categorySaving ? TEXT.saving : TEXT.saveCategory }}</button>
          <button class="ghost-btn" type="button" @click="resetCategoryForm">{{ TEXT.cancel }}</button>
        </div>
      </div>
      <div class="media-category-list">
        <button v-for="item in categories" :key="item.id" class="media-category-chip" :class="{ active: filterCategory === item.code }" type="button" @click="filterCategory = item.code; search()">
          <span><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.assetCount }}</small></span>
          <em v-if="item.isSystem === 1">{{ TEXT.system }}</em>
        </button>
      </div>
    </section>

    <section class="admin-toolbar media-toolbar">
      <input v-model="keyword" type="search" :placeholder="TEXT.searchPlaceholder" @keyup.enter="search" />
      <select v-model="filterCategory" @change="search">
        <option value="">{{ TEXT.allCategories }}</option>
        <option v-for="item in categories" :key="item.code" :value="item.code">{{ item.name }}</option>
      </select>
      <select v-model.number="pageSize" @change="search">
        <option :value="12">12 / page</option>
        <option :value="24">24 / page</option>
        <option :value="48">48 / page</option>
      </select>
      <button class="ghost-btn" @click="search">{{ TEXT.search }}</button>
      <button class="ghost-btn" @click="resetFilters">{{ TEXT.reset }}</button>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>
    <p v-if="loading" class="muted">{{ TEXT.loading }}</p>

    <section v-else class="media-grid">
      <article v-for="item in list" :key="item.id" class="media-card">
        <img :src="item.url" :alt="item.originalName" />
        <div>
          <strong>{{ item.originalName || item.fileName }}</strong>
          <small>{{ categoryName(item.category) }} · {{ item.extension }} · {{ formatSize(item.size) }}</small>
          <small>{{ formatTime(item.createdAt) }}</small>
        </div>
        <select class="media-card-category" :value="item.category" :disabled="assetCategorySaving[item.id]" @change="changeAssetCategory(item, ($event.target as HTMLSelectElement).value)">
          <option v-for="categoryItem in categories" :key="categoryItem.code" :value="categoryItem.code">{{ categoryItem.name }}</option>
        </select>
        <div class="media-card-actions">
          <button class="ghost-btn" @click="copyUrl(item.url)">{{ TEXT.copyUrl }}</button>
          <button class="ghost-btn danger-btn" @click="remove(item)">{{ TEXT.remove }}</button>
        </div>
      </article>
      <div v-if="list.length === 0" class="empty-state"><h3>{{ TEXT.emptyTitle }}</h3><p>{{ TEXT.emptyDesc }}</p></div>
    </section>

    <div class="media-category-table glass-lite">
      <h3>{{ TEXT.categoryList }}</h3>
      <table class="admin-table">
        <thead><tr><th>{{ TEXT.name }}</th><th>{{ TEXT.code }}</th><th>{{ TEXT.imageCount }}</th><th>{{ TEXT.sort }}</th><th>{{ TEXT.action }}</th></tr></thead>
        <tbody>
          <tr v-for="item in categories" :key="item.id">
            <td>{{ item.name }}</td>
            <td>{{ item.code }}</td>
            <td>{{ item.assetCount }}</td>
            <td>{{ item.sortOrder }}</td>
            <td><button class="ghost-btn" @click="editCategory(item)">{{ TEXT.edit }}</button><button class="ghost-btn danger-btn" :disabled="item.isSystem === 1 || item.assetCount > 0" @click="removeCategory(item)">{{ TEXT.delete }}</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > 0" class="admin-pagination"><span>{{ list.length }} / {{ total }} · {{ currentPage }} / {{ totalPages }}</span><div><button class="ghost-btn" :disabled="!hasPrevious || loading" @click="previousPage">{{ TEXT.prev }}</button><button class="ghost-btn" :disabled="!hasNext || loading" @click="nextPage">{{ TEXT.next }}</button></div></div>
  </section>
</template>