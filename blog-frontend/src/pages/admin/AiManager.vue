<script setup lang="ts">
// AiManager.vue 是后台 Skills 管理：负责配置模型、管理技能库和查看调用日志。
import { computed, onMounted, reactive, ref } from "vue";
import { createAiSkill, deleteAiSkill, getAiLogDetail, getAiLogPage, getAiProvider, getAiSkills, testAiProvider, updateAiProvider, updateAiSkill } from "../../api/ai";
import type { AiCallLog, AiCallLogDetail, AiProvider, AiSkill, AiSkillPayload } from "../../types/ai";

// TEXT 集中管理 AI 页面文案，避免模板中到处写固定字符串。
const TEXT = {
  title: "Skills 管理",
  desc: "配置小米 MiMo 或其他 OpenAI 兼容模型，管理文章编辑 Skills 和调用日志。",
  provider: "\u6a21\u578b\u914d\u7f6e",
  skills: "\u0053\u006b\u0069\u006c\u006c\u0073 \u6280\u80fd\u5e93",
  logs: "\u8c03\u7528\u65e5\u5fd7",
  save: "\u4fdd\u5b58\u914d\u7f6e",
  saving: "\u4fdd\u5b58\u4e2d...",
  test: "\u6d4b\u8bd5\u8fde\u63a5",
  testing: "\u6d4b\u8bd5\u4e2d...",
  enabled: "\u542f\u7528",
  disabled: "\u7981\u7528",
  apiKeyTip: "\u0041\u0050\u0049 \u004b\u0065\u0079 \u53ea\u4fdd\u5b58\u5230\u540e\u7aef\uff0c\u67e5\u8be2\u65f6\u53ea\u663e\u793a\u8131\u654f\u7ed3\u679c\u3002\u7559\u7a7a\u8868\u793a\u4fdd\u6301\u539f\u004b\u0065\u0079\u3002",
  masked: "\u5df2\u4fdd\u5b58\u7684\u004b\u0065\u0079",
  editSkill: "\u7f16\u8f91\u6280\u80fd",
  newSkill: "\u65b0\u589e\u6280\u80fd",
  saveSkill: "\u4fdd\u5b58\u6280\u80fd",
  reset: "\u91cd\u7f6e",
  delete: "\u5220\u9664",
  edit: "\u7f16\u8f91",
  refresh: "\u5237\u65b0",
  all: "\u5168\u90e8",
  success: "\u6210\u529f",
  fail: "\u5931\u8d25",
  successSaved: "\u4fdd\u5b58\u6210\u529f",
  testOk: "\u8fde\u63a5\u6d4b\u8bd5\u6210\u529f\uff1a",
  emptyLogs: "\u6682\u65e0\u8c03\u7528\u65e5\u5fd7",
  emptySkills: "\u6682\u65e0\u6280\u80fd",
  loading: "\u52a0\u8f7d\u4e2d...",
  detail: "\u8be6\u60c5",
  fullInput: "\u5b8c\u6574\u8f93\u5165",
  fullOutput: "\u5b8c\u6574\u8f93\u51fa",
  close: "\u5173\u95ed",
  colon: "\uff1a"
};

// loading 表示 AI 页面基础数据是否正在加载。
const loading = ref(false);
// providerSaving、providerTesting、skillSaving 分别控制三类 AI 操作状态。
const providerSaving = ref(false);
const providerTesting = ref(false);
const skillSaving = ref(false);
const message = ref("");
const errorText = ref("");
const provider = ref<AiProvider | null>(null);
const skills = ref<AiSkill[]>([]);
const logs = ref<AiCallLog[]>([]);
// selectedLog 保存当前打开的 AI 调用日志详情。
const selectedLog = ref<AiCallLogDetail | null>(null);
const detailLoading = ref(false);
const total = ref(0);
// page、size、total、totalPages 用于 AI 调用日志分页。
const page = ref(1);
const size = 10;
const totalPages = ref(0);
// logKeyword 和 logSuccess 用于筛选 AI 调用日志。
const logKeyword = ref("");
const logSuccess = ref<number | null>(null);
const editingSkillId = ref<number | null>(null);

// providerForm 保存模型服务配置，例如 baseUrl、apiKey、model、temperature 和 maxTokens。
const providerForm = reactive({
  name: "Xiaomi MiMo Token Plan",
  baseUrl: "https://token-plan-cn.xiaomimimo.com/v1",
  apiKey: "",
  model: "mimo-v2.5-pro",
  temperature: 0.4,
  maxTokens: 1200,
  enabled: 1
});

// skillForm 保存 AI 技能表单，一个技能对应一种文章编辑能力。
const skillForm = reactive<AiSkillPayload>({
  name: "",
  code: "",
  description: "",
  scene: "article",
  promptTemplate: "",
  enabled: 1,
  sortOrder: 100
});

// successCount 和 failCount 用于统计当前页调用日志成功 / 失败数量。
const successCount = computed(() => logs.value.filter((item) => item.success === 1).length);
const failCount = computed(() => logs.value.filter((item) => item.success !== 1).length);

// fillProvider 把后端返回的模型配置回填到表单，apiKey 查询时保持脱敏。
const fillProvider = (item: AiProvider) => {
  provider.value = item;
  providerForm.name = item.name;
  providerForm.baseUrl = item.baseUrl;
  providerForm.apiKey = "";
  providerForm.model = item.model;
  providerForm.temperature = item.temperature;
  providerForm.maxTokens = item.maxTokens;
  providerForm.enabled = item.enabled;
};

// resetSkillForm 清空技能表单并退出编辑状态。
const resetSkillForm = () => {
  editingSkillId.value = null;
  skillForm.name = "";
  skillForm.code = "";
  skillForm.description = "";
  skillForm.scene = "article";
  skillForm.promptTemplate = "";
  skillForm.enabled = 1;
  skillForm.sortOrder = 100;
};

// editSkill 把选中的技能回填到表单，进入编辑模式。
const editSkill = (item: AiSkill) => {
  editingSkillId.value = item.id;
  skillForm.name = item.name;
  skillForm.code = item.code;
  skillForm.description = item.description;
  skillForm.scene = item.scene;
  skillForm.promptTemplate = item.promptTemplate;
  skillForm.enabled = item.enabled;
  skillForm.sortOrder = item.sortOrder;
};

// loadLogs 按关键词、成功状态和分页参数加载 AI 调用日志。
const loadLogs = async () => {
  const data = await getAiLogPage({ keyword: logKeyword.value, success: logSuccess.value, page: page.value, size });
  logs.value = data.records;
  total.value = data.total;
  totalPages.value = data.totalPages;
};

// loadPage 初始化 AI 页面，同时加载模型配置、技能列表和调用日志。
const loadPage = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [providerData, skillData] = await Promise.all([getAiProvider(), getAiSkills()]);
    fillProvider(providerData);
    skills.value = skillData;
    await loadLogs();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Load failed";
  } finally {
    loading.value = false;
  }
};

// saveProvider 保存模型配置，后端会负责安全保存 API Key。
const saveProvider = async () => {
  providerSaving.value = true;
  message.value = "";
  errorText.value = "";
  try {
    fillProvider(await updateAiProvider(providerForm));
    message.value = TEXT.successSaved;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Save failed";
  } finally {
    providerSaving.value = false;
  }
};

// runProviderTest 调用后端测试接口，验证当前模型配置是否可用。
const runProviderTest = async () => {
  providerTesting.value = true;
  message.value = "";
  errorText.value = "";
  try {
    const data = await testAiProvider();
    message.value = TEXT.testOk + " " + (data.content || data.rawText);
    page.value = 1;
    await loadLogs();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Test failed";
    await loadLogs();
  } finally {
    providerTesting.value = false;
  }
};

// saveSkill 新增或更新 AI 技能，文章编辑页会按 code 调用对应技能。
const saveSkill = async () => {
  if (!skillForm.name.trim() || !skillForm.code.trim() || !skillForm.promptTemplate.trim()) return;
  skillSaving.value = true;
  message.value = "";
  errorText.value = "";
  try {
    if (editingSkillId.value) {
      await updateAiSkill(editingSkillId.value, skillForm);
    } else {
      await createAiSkill(skillForm);
    }
    skills.value = await getAiSkills();
    resetSkillForm();
    message.value = TEXT.successSaved;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Save skill failed";
  } finally {
    skillSaving.value = false;
  }
};

// removeSkill 删除一个 AI 技能，删除前会弹窗确认。
// removeSkill 删除一个 AI 技能，通常用于清理废弃的文章生成能力。
const removeSkill = async (item: AiSkill) => {
  if (!window.confirm("Delete skill " + item.code + "?")) return;
  await deleteAiSkill(item.id);
  skills.value = await getAiSkills();
  if (editingSkillId.value === item.id) resetSkillForm();
};

const searchLogs = async () => {
  page.value = 1;
  await loadLogs();
};

// openLogDetail 打开 AI 调用日志详情，查看完整输入和输出。
// openLogDetail 加载单条 AI 日志详情，方便查看完整输入和输出。
const openLogDetail = async (item: AiCallLog) => {
  detailLoading.value = true;
  selectedLog.value = null;
  errorText.value = "";
  try {
    selectedLog.value = await getAiLogDetail(item.id);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "Load detail failed";
  } finally {
    detailLoading.value = false;
  }
};

onMounted(loadPage);
</script>

<template>
  <section class="article-page glass ai-page">
    <header class="article-page-header">
      <div>
        <h2>{{ TEXT.title }}</h2>
        <p class="muted">{{ TEXT.desc }}</p>
      </div>
      <button class="ghost-btn" @click="loadPage">{{ TEXT.refresh }}</button>
    </header>

    <p v-if="loading" class="muted">{{ TEXT.loading }}</p>
    <p v-if="message" class="success-text">{{ message }}</p>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>

    <div v-if="!loading" class="ai-layout">
      <section class="ai-card">
        <h3>{{ TEXT.provider }}</h3>
        <div class="ai-form-grid">
          <label>Provider<input v-model="providerForm.name" type="text" /></label>
          <label>Base URL<input v-model="providerForm.baseUrl" type="url" /></label>
          <label>Model<input v-model="providerForm.model" type="text" /></label>
          <label>Temperature<input v-model.number="providerForm.temperature" type="number" min="0" max="2" step="0.1" /></label>
          <label>Max Tokens<input v-model.number="providerForm.maxTokens" type="number" min="50" max="8000" /></label>
          <label>Status<select v-model.number="providerForm.enabled"><option :value="1">{{ TEXT.enabled }}</option><option :value="0">{{ TEXT.disabled }}</option></select></label>
          <label class="full-row">API Key<input v-model="providerForm.apiKey" type="password" placeholder="sk-..." /><small class="muted">{{ TEXT.apiKeyTip }}</small></label>
          <p class="muted full-row">{{ TEXT.masked }}{{ TEXT.colon }}{{ provider?.maskedApiKey || '-' }}</p>
        </div>
        <div class="editor-header-actions">
          <button class="primary-btn" type="button" :disabled="providerSaving" @click="saveProvider">{{ providerSaving ? TEXT.saving : TEXT.save }}</button>
          <button class="ghost-btn" type="button" :disabled="providerTesting" @click="runProviderTest">{{ providerTesting ? TEXT.testing : TEXT.test }}</button>
        </div>
      </section>

      <section class="ai-card">
        <h3>{{ editingSkillId ? TEXT.editSkill : TEXT.newSkill }}</h3>
        <div class="ai-form-grid">
          <label>Name<input v-model="skillForm.name" type="text" /></label>
          <label>Code<input v-model="skillForm.code" type="text" placeholder="article_summary" /></label>
          <label>Scene<input v-model="skillForm.scene" type="text" /></label>
          <label>Sort<input v-model.number="skillForm.sortOrder" type="number" /></label>
          <label>Status<select v-model.number="skillForm.enabled"><option :value="1">{{ TEXT.enabled }}</option><option :value="0">{{ TEXT.disabled }}</option></select></label>
          <label class="full-row">Description<input v-model="skillForm.description" type="text" /></label>
          <label class="full-row">Prompt Template<textarea v-model="skillForm.promptTemplate" rows="8" placeholder="Use variables: {{title}}, {{summary}}, {{content}}, {{existingTags}}"></textarea></label>
        </div>
        <div class="editor-header-actions">
          <button class="primary-btn" type="button" :disabled="skillSaving" @click="saveSkill">{{ skillSaving ? TEXT.saving : TEXT.saveSkill }}</button>
          <button class="ghost-btn" type="button" @click="resetSkillForm">{{ TEXT.reset }}</button>
        </div>
      </section>
    </div>

    <section v-if="!loading" class="ai-card ai-wide-card">
      <div class="section-title-row"><h3>{{ TEXT.skills }}</h3><span>{{ skills.length }}</span></div>
      <div v-if="skills.length" class="ai-skill-grid">
        <article v-for="item in skills" :key="item.id" class="ai-skill-card">
          <div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.scene }}</small></div>
          <p>{{ item.description }}</p>
          <span class="status-pill" :class="item.enabled === 1 ? 'published' : 'draft'">{{ item.enabled === 1 ? TEXT.enabled : TEXT.disabled }}</span>
          <div class="media-card-actions">
            <button class="ghost-btn" @click="editSkill(item)">{{ TEXT.edit }}</button>
            <button class="ghost-btn danger-btn" @click="removeSkill(item)">{{ TEXT.delete }}</button>
          </div>
        </article>
      </div>
      <p v-else class="muted">{{ TEXT.emptySkills }}</p>
    </section>

    <section v-if="!loading" class="ai-card ai-wide-card">
      <div class="section-title-row"><h3>{{ TEXT.logs }}</h3><span>{{ total }} · {{ TEXT.success }} {{ successCount }} / {{ TEXT.fail }} {{ failCount }}</span></div>
      <div class="admin-toolbar audit-toolbar">
        <input v-model="logKeyword" type="search" placeholder="keyword / skill / error" @keyup.enter="searchLogs" />
        <select v-model="logSuccess"><option :value="null">{{ TEXT.all }}</option><option :value="1">{{ TEXT.success }}</option><option :value="0">{{ TEXT.fail }}</option></select>
        <button class="ghost-btn" @click="searchLogs">{{ TEXT.refresh }}</button>
      </div>
      <div v-if="logs.length" class="article-table-wrap audit-table-wrap">
        <table class="article-table audit-table">
          <thead><tr><th>Skill</th><th>Model</th><th>Status</th><th>Preview</th><th>Time</th></tr></thead>
          <tbody>
            <tr v-for="item in logs" :key="item.id">
              <td><strong>{{ item.skillName }}</strong><br /><small>{{ item.skillCode }}</small></td>
              <td>{{ item.providerName }}<br /><small>{{ item.model }}</small></td>
              <td><span class="status-pill" :class="item.success === 1 ? 'published' : 'draft'">{{ item.success === 1 ? TEXT.success : TEXT.fail }}</span><br /><small>{{ item.elapsedMs }}ms / {{ item.totalTokens ?? '-' }} tokens</small></td>
              <td class="audit-detail-cell"><p>{{ item.success === 1 ? item.outputPreview : item.errorMessage }}</p><button class="ghost-btn" type="button" @click="openLogDetail(item)">{{ TEXT.detail }}</button></td>
              <td>{{ item.createdAt }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="muted">{{ TEXT.emptyLogs }}</p>
      <div v-if="total > size" class="admin-pagination">
        <span>{{ page }} / {{ totalPages }}</span>
        <div><button class="ghost-btn" :disabled="page <= 1" @click="page--; loadLogs()">Prev</button><button class="ghost-btn" :disabled="page >= totalPages" @click="page++; loadLogs()">Next</button></div>
      </div>
    </section>

    <div v-if="selectedLog || detailLoading" class="media-picker-mask" @click.self="selectedLog = null">
      <section class="media-picker-panel glass ai-log-detail-panel">
        <header>
          <div>
            <h3>{{ TEXT.logs }} / {{ TEXT.detail }}</h3>
            <p v-if="selectedLog" class="muted">{{ selectedLog.skillName }} / {{ selectedLog.model }} / {{ selectedLog.elapsedMs }}ms</p>
          </div>
          <button class="ghost-btn" type="button" @click="selectedLog = null">{{ TEXT.close }}</button>
        </header>
        <p v-if="detailLoading" class="muted">{{ TEXT.loading }}</p>
        <div v-else-if="selectedLog" class="ai-log-detail-grid">
          <article>
            <h4>{{ TEXT.fullInput }}</h4>
            <pre class="ai-result-preview">{{ selectedLog.inputText }}</pre>
          </article>
          <article>
            <h4>{{ TEXT.fullOutput }}</h4>
            <pre class="ai-result-preview">{{ selectedLog.success === 1 ? selectedLog.outputText : selectedLog.errorMessage }}</pre>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>
