<script setup lang="ts">
// AdminLayout.vue 是后台管理系统的全局布局组件：负责侧边栏导航、顶部栏、页面标题、前台入口、管理员退出和修改密码弹窗。
import { computed, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { changeAdminPassword } from "../api/auth";
import { useAppStore } from "../stores/app";
import { useUserStore } from "../stores/user";

// TEXT 集中管理后台布局文案，避免模板中散落固定字符串。
const TEXT = {
  defaultTitle: "\u540e\u53f0\u7ba1\u7406",
  dashboard: "\u4eea\u8868\u76d8",
  articles: "\u6587\u7ae0\u7ba1\u7406",
  categories: "\u5206\u7c7b\u7ba1\u7406",
  tags: "\u6807\u7b7e\u7ba1\u7406",
  comments: "\u8bc4\u8bba\u7ba1\u7406",
  users: "\u7528\u6237\u7ba1\u7406",
  media: "\u5a92\u4f53\u5e93",
  ai: "Skills 管理",
  messages: "\u7559\u8a00\u7ba1\u7406",
  links: "\u53cb\u94fe\u7ba1\u7406",
  settings: "\u7ad9\u70b9\u8bbe\u7f6e",
  auditLogs: "\u64cd\u4f5c\u65e5\u5fd7",
  expandNav: "\u5c55\u5f00\u5bfc\u822a",
  collapseNav: "\u6298\u53e0\u5bfc\u822a",
  viewFront: "\u67e5\u770b\u524d\u53f0",
  changePassword: "\u4fee\u6539\u5bc6\u7801",
  logout: "\u9000\u51fa",
  passwordTitle: "\u4fee\u6539\u7ba1\u7406\u5458\u5bc6\u7801",
  passwordDesc: "\u9700\u8981\u5148\u9a8c\u8bc1\u539f\u5bc6\u7801\uff0c\u4fee\u6539\u6210\u529f\u540e\u8bf7\u91cd\u65b0\u767b\u5f55\u3002",
  oldPassword: "\u539f\u5bc6\u7801",
  newPassword: "\u65b0\u5bc6\u7801",
  confirmPassword: "\u786e\u8ba4\u65b0\u5bc6\u7801",
  oldPasswordPlaceholder: "\u8f93\u5165\u5f53\u524d\u5bc6\u7801",
  newPasswordPlaceholder: "\u81f3\u5c11 6 \u4f4d",
  cancel: "\u53d6\u6d88",
  saving: "\u4fdd\u5b58\u4e2d...",
  save: "\u786e\u8ba4\u4fee\u6539",
  fillAll: "\u8bf7\u586b\u5199\u539f\u5bc6\u7801\u3001\u65b0\u5bc6\u7801\u548c\u786e\u8ba4\u5bc6\u7801",
  tooShort: "\u65b0\u5bc6\u7801\u81f3\u5c11 6 \u4f4d",
  mismatch: "\u4e24\u6b21\u8f93\u5165\u7684\u65b0\u5bc6\u7801\u4e0d\u4e00\u81f4",
  samePassword: "\u65b0\u5bc6\u7801\u4e0d\u80fd\u548c\u539f\u5bc6\u7801\u4e00\u6837",
  success: "\u5bc6\u7801\u4fee\u6539\u6210\u529f\uff0c\u5373\u5c06\u8df3\u8f6c\u5230\u767b\u5f55\u9875\u3002"
};

// appStore 保存后台 UI 状态，例如侧边栏折叠。
const appStore = useAppStore();
// userStore 保存管理员昵称和 token，用于顶部栏和退出登录。
const userStore = useUserStore();
// router 和 route 用于页面跳转和读取当前路由标题。
const router = useRouter();
const route = useRoute();

// sidebarClass 根据 Pinia 状态决定侧边栏是否折叠。
const sidebarClass = computed(() => (appStore.sidebarCollapsed ? "is-collapsed" : ""));
// pageTitle 读取当前路由 meta.title，作为顶部页面标题。
const pageTitle = computed(() => String(route.meta.title ?? TEXT.defaultTitle));
// passwordVisible 控制修改管理员密码弹窗是否显示。
const passwordVisible = ref(false);
const changingPassword = ref(false);
const passwordError = ref("");
const passwordSuccess = ref("");
// passwordForm 保存管理员修改密码表单。
const passwordForm = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" });

// resetPasswordForm 清空密码表单和提示信息。
const resetPasswordForm = () => {
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  passwordError.value = "";
  passwordSuccess.value = "";
};

// openPasswordDialog 打开管理员修改密码弹窗。
const openPasswordDialog = () => {
  resetPasswordForm();
  passwordVisible.value = true;
};

// closePasswordDialog 在非提交状态下关闭弹窗并重置表单。
const closePasswordDialog = () => {
  if (changingPassword.value) return;
  passwordVisible.value = false;
  resetPasswordForm();
};

// logout 清理管理员登录状态并跳回登录页。
const logout = () => {
  userStore.clearToken();
  router.push("/login");
};

// submitPassword 校验旧密码、新密码和确认密码后，调用后端接口完成修改。
const submitPassword = async () => {
  passwordError.value = "";
  passwordSuccess.value = "";
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    passwordError.value = TEXT.fillAll;
    return;
  }
  if (passwordForm.newPassword.length < 6) {
    passwordError.value = TEXT.tooShort;
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = TEXT.mismatch;
    return;
  }
  if (passwordForm.oldPassword === passwordForm.newPassword) {
    passwordError.value = TEXT.samePassword;
    return;
  }
  changingPassword.value = true;
  try {
    await changeAdminPassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword });
    passwordSuccess.value = TEXT.success;
    window.setTimeout(() => {
      userStore.clearToken();
      router.push("/login");
    }, 900);
  } catch (error) {
    passwordError.value = error instanceof Error ? error.message : "Change password failed";
  } finally {
    changingPassword.value = false;
  }
};
</script>

<template>
  <!-- 后台整体布局：左侧是导航，右侧是顶部栏和页面内容。 -->
  <div class="shell">
        <!-- 左侧边栏：展示后台模块导航，并支持折叠。 -->
    <aside class="sidebar glass" :class="sidebarClass">
      <div class="brand">个人博客后台管理</div>
      <nav class="nav">
        <RouterLink class="nav-item" to="/admin/dashboard" active-class="active">{{ TEXT.dashboard }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/articles" active-class="active">{{ TEXT.articles }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/categories" active-class="active">{{ TEXT.categories }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/tags" active-class="active">{{ TEXT.tags }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/comments" active-class="active">{{ TEXT.comments }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/users" active-class="active">{{ TEXT.users }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/media" active-class="active">{{ TEXT.media }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/ai" active-class="active">{{ TEXT.ai }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/messages" active-class="active">{{ TEXT.messages }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/links" active-class="active">{{ TEXT.links }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/settings" active-class="active">{{ TEXT.settings }}</RouterLink>
        <RouterLink class="nav-item" to="/admin/audit-logs" active-class="active">{{ TEXT.auditLogs }}</RouterLink>
      </nav>
      <button class="ghost-btn" @click="appStore.toggleSidebar">{{ appStore.sidebarCollapsed ? TEXT.expandNav : TEXT.collapseNav }}</button>
    </aside>

        <!-- 右侧主区域：顶部栏 + 当前路由页面内容。 -->
    <div class="main">
            <!-- 顶部栏：显示当前页面标题、查看前台入口、改密和退出。 -->
      <header class="topbar glass">
        <h1>{{ pageTitle }}</h1>
        <div class="topbar-right">
          <button class="ghost-btn" @click="router.push('/')">{{ TEXT.viewFront }}</button>
          <button class="ghost-btn" @click="openPasswordDialog">{{ TEXT.changePassword }}</button>
          <span class="admin-nickname">{{ userStore.nickname }}</span>
          <button class="ghost-btn" @click="logout">{{ TEXT.logout }}</button>
        </div>
      </header>
            <!-- router-view 是后台子页面挂载点。 -->
      <main class="content"><router-view /></main>
    </div>

        <!-- 修改密码弹窗：管理员可以在后台直接修改自己的登录密码。 -->
    <div v-if="passwordVisible" class="admin-modal-mask" @click.self="closePasswordDialog">
      <form class="admin-password-modal glass" @submit.prevent="submitPassword">
        <header>
          <div><h2>{{ TEXT.passwordTitle }}</h2><p>{{ TEXT.passwordDesc }}</p></div>
          <button class="ghost-btn" type="button" @click="closePasswordDialog">{{ TEXT.cancel }}</button>
        </header>
        <label><span>{{ TEXT.oldPassword }}</span><input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" :placeholder="TEXT.oldPasswordPlaceholder" /></label>
        <label><span>{{ TEXT.newPassword }}</span><input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" :placeholder="TEXT.newPasswordPlaceholder" /></label>
        <label><span>{{ TEXT.confirmPassword }}</span><input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" :placeholder="TEXT.confirmPassword" /></label>
        <p v-if="passwordError" class="error-text">{{ passwordError }}</p>
        <p v-if="passwordSuccess" class="success-text">{{ passwordSuccess }}</p>
        <div class="admin-modal-actions">
          <button class="ghost-btn" type="button" @click="closePasswordDialog">{{ TEXT.cancel }}</button>
          <button class="primary-btn" type="submit" :disabled="changingPassword">{{ changingPassword ? TEXT.saving : TEXT.save }}</button>
        </div>
      </form>
    </div>
  </div>
</template>
