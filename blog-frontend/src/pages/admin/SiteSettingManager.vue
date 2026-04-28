<script setup lang="ts">
// SiteSettingManager.vue 是后台站点设置页：负责维护前台站点信息、首页文案、每日一句和关于页路线图。
import { onMounted, reactive, ref } from "vue";
import { uploadArticleCover } from "../../api/article";
import { defaultSiteSetting, getAdminSetting, updateSiteSetting } from "../../api/setting";
import type { SiteSettingPayload } from "../../types/setting";

// loading、saving 负责控制页面初始化和保存按钮状态。
const loading = ref(false);
const saving = ref(false);
// uploadingLogo、uploadingAvatar 分别记录两种图片上传过程，避免按钮状态互相影响。
const uploadingLogo = ref(false);
const uploadingAvatar = ref(false);
// errorText 和 successText 用于集中展示设置页反馈信息。
const errorText = ref("");
const successText = ref("");

// form 保存站点设置表单，默认值直接复用 setting.ts 中的默认站点配置。
const form = reactive<SiteSettingPayload>({
  siteName: defaultSiteSetting.siteName,
  siteSubtitle: defaultSiteSetting.siteSubtitle,
  logo: defaultSiteSetting.logo,
  avatar: defaultSiteSetting.avatar,
  heroTitle: defaultSiteSetting.heroTitle,
  heroSubtitle: defaultSiteSetting.heroSubtitle,
  authorName: defaultSiteSetting.authorName,
  authorBio: defaultSiteSetting.authorBio,
  footerText: defaultSiteSetting.footerText,
  dailyQuoteEnabled: defaultSiteSetting.dailyQuoteEnabled,
  dailyQuoteApiUrl: defaultSiteSetting.dailyQuoteApiUrl,
  roadmapJson: defaultSiteSetting.roadmapJson
});

// fillForm 把后端返回的配置回填到表单，同时为可选字段补上默认值。
const fillForm = (payload: SiteSettingPayload) => {
  form.siteName = payload.siteName;
  form.siteSubtitle = payload.siteSubtitle;
  form.logo = payload.logo;
  form.avatar = payload.avatar;
  form.heroTitle = payload.heroTitle;
  form.heroSubtitle = payload.heroSubtitle;
  form.authorName = payload.authorName;
  form.authorBio = payload.authorBio;
  form.footerText = payload.footerText;
  form.dailyQuoteEnabled = payload.dailyQuoteEnabled ?? defaultSiteSetting.dailyQuoteEnabled;
  form.dailyQuoteApiUrl = payload.dailyQuoteApiUrl || defaultSiteSetting.dailyQuoteApiUrl;
  form.roadmapJson = payload.roadmapJson || defaultSiteSetting.roadmapJson;
};

// loadSetting 初始化页面时加载后台当前站点配置。
const loadSetting = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    fillForm(await getAdminSetting());
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载站点设置失败";
  } finally {
    loading.value = false;
  }
};

// validateRoadmapJson 校验路线图 JSON 是否可解析且为数组，避免保存后前台渲染失败。
const validateRoadmapJson = () => {
  try {
    const parsed = JSON.parse(form.roadmapJson || "[]");
    if (!Array.isArray(parsed)) {
      errorText.value = "路线图配置必须是数组 JSON";
      return false;
    }
    return true;
  } catch {
    errorText.value = "路线图 JSON 格式不正确，请检查逗号、引号和中括号";
    return false;
  }
};

// save 先做基础表单校验，再提交完整站点配置到后端。
const save = async () => {
  if (!form.siteName.trim() || !form.heroTitle.trim()) {
    errorText.value = "博客名称和首页主标题不能为空";
    return;
  }
  if (!validateRoadmapJson()) return;

  saving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    fillForm(await updateSiteSetting(form));
    successText.value = "站点设置已保存，刷新前台即可看到最新效果。";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "保存站点设置失败";
  } finally {
    saving.value = false;
  }
};

// uploadImage 复用文章封面上传接口，为 Logo 和头像字段生成可直接保存的图片地址。
const uploadImage = async (event: Event, field: "logo" | "avatar") => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;

  if (field === "logo") uploadingLogo.value = true;
  else uploadingAvatar.value = true;
  errorText.value = "";

  try {
    form[field] = await uploadArticleCover(file);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "上传图片失败";
  } finally {
    uploadingLogo.value = false;
    uploadingAvatar.value = false;
    (event.target as HTMLInputElement).value = "";
  }
};

// resetDefault 用默认配置回填表单，方便重新编辑站点展示内容。
const resetDefault = () => {
  fillForm(defaultSiteSetting);
  successText.value = "";
  errorText.value = "";
};

onMounted(loadSetting);
</script>

<template>
  <section class="article-page glass">
    <header class="article-page-header">
      <div>
        <h2>站点设置</h2>
        <p class="muted">配置前台展示的博客名称、Logo、头像、首页文案、每日句子和关于页路线图。</p>
      </div>
      <button class="ghost-btn" @click="resetDefault">恢复默认文案</button>
    </header>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>
    <p v-if="loading" class="muted">加载中...</p>

    <form v-else class="setting-layout" @submit.prevent="save">
      <section class="setting-form-panel">
        <label>
          博客名称
          <input v-model="form.siteName" type="text" placeholder="例如：小新的个人博客" />
        </label>
        <label>
          博客副标题
          <input v-model="form.siteSubtitle" type="text" placeholder="例如：记录生活、技术和项目" />
        </label>
        <label>
          首页主标题
          <input v-model="form.heroTitle" type="text" placeholder="首页最醒目的标题" />
        </label>
        <label>
          首页简介
          <textarea v-model="form.heroSubtitle" placeholder="首页主标题下面的介绍文案"></textarea>
        </label>
        <label>
          作者名称
          <input v-model="form.authorName" type="text" placeholder="例如：Admin" />
        </label>
        <label>
          作者简介
          <textarea v-model="form.authorBio" placeholder="简单介绍自己或这个博客"></textarea>
        </label>
        <label>
          页脚文案
          <input v-model="form.footerText" type="text" placeholder="例如：Powered by Blog CMS" />
        </label>

        <div class="setting-switch-card">
          <label class="switch-line">
            <input v-model="form.dailyQuoteEnabled" type="checkbox" :true-value="1" :false-value="0" />
            <span>开启首页每日句子</span>
          </label>
          <p>使用第三方一言 API，后端会缓存 10 分钟，失败时自动展示本地兜底句子。</p>
        </div>

        <label>
          每日句子 API 地址
          <input v-model="form.dailyQuoteApiUrl" type="url" placeholder="https://v1.hitokoto.cn/?encode=json&max_length=60" />
        </label>

        <label>
          关于页路线图 JSON
          <textarea v-model="form.roadmapJson" class="roadmap-json-input" rows="12" placeholder='[{"title":"一期","description":"基础功能","active":true}]'></textarea>
        </label>
        <p class="muted">格式：数组 JSON。每一项支持 title、description、active，active 为 true 时前台显示为已完成/当前阶段。</p>
      </section>

      <aside class="setting-preview-panel">
        <div class="site-preview-card">
          <div class="site-preview-brand">
            <img v-if="form.logo" :src="form.logo" alt="Logo 预览" />
            <span v-else>CMS</span>
            <strong>{{ form.siteName || "个人博客" }}</strong>
          </div>
          <div class="site-preview-avatar">
            <img v-if="form.avatar" :src="form.avatar" alt="头像预览" />
            <span v-else>{{ (form.authorName || form.siteName || "A").slice(0, 1).toUpperCase() }}</span>
          </div>
          <p class="hero-kicker">{{ form.siteSubtitle || "Personal Blog CMS" }}</p>
          <h3>{{ form.heroTitle || "首页主标题" }}</h3>
          <p>{{ form.heroSubtitle || "这里会展示首页简介。" }}</p>
          <small>{{ form.authorName || "Admin" }} · {{ form.footerText || "Powered by Blog CMS" }}</small>
          <div v-if="form.dailyQuoteEnabled" class="site-preview-quote">“把细小的灵感认真保存，日子就会慢慢长出自己的光。”</div>
        </div>

        <label>
          Logo 地址
          <input v-model="form.logo" type="url" placeholder="可粘贴图片地址，也可上传" />
        </label>
        <label class="upload-btn">
          {{ uploadingLogo ? "Logo 上传中..." : "上传 Logo" }}
          <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="uploadImage($event, 'logo')" />
        </label>

        <label>
          头像地址
          <input v-model="form.avatar" type="url" placeholder="可粘贴图片地址，也可上传" />
        </label>
        <label class="upload-btn">
          {{ uploadingAvatar ? "头像上传中..." : "上传头像" }}
          <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="uploadImage($event, 'avatar')" />
        </label>

        <button class="primary-btn" type="submit" :disabled="saving">
          {{ saving ? "保存中..." : "保存站点设置" }}
        </button>
      </aside>
    </form>
  </section>
</template>
