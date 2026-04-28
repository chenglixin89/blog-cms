<script setup lang="ts">
// FrontHeader.vue 是前台全局头部组件：负责站点 Logo、导航菜单、登录入口、用户退出、语言切换和日夜主题切换。
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getFrontSetting, defaultSiteSetting } from "../api/setting";
import { useFrontUserStore } from "../stores/frontUser";
import { useFrontPreferences } from "../composables/useFrontPreferences";
import type { SiteSetting } from "../types/setting";

// setting 保存前台头部需要展示的站点配置，例如 Logo、站点名等。
const setting = ref<SiteSetting>(defaultSiteSetting);
// frontUserStore 读取前台用户登录状态，用于决定显示登录入口还是用户中心。
const frontUserStore = useFrontUserStore();
// menuOpen 控制移动端导航菜单是否展开。
const menuOpen = ref(false);
// useFrontPreferences 提供前台语言、主题和自动翻译能力。
const { language, theme, t, toggleLanguage, toggleTheme, applyTheme, startFrontTranslator } = useFrontPreferences();

// languageCurrent / languageNext 用于语言切换按钮当前态和下一态的显示。
const languageCurrent = computed(() => (language.value === "zh" ? "\u4e2d" : "EN"));
const languageNext = computed(() => (language.value === "zh" ? "EN" : "\u4e2d"));
// themeCurrent / themeNext 用于主题切换按钮当前态和下一态的显示。
const themeCurrent = computed(() => (theme.value === "light" ? "\u65e5" : "\u591c"));
const themeNext = computed(() => (theme.value === "light" ? t("switchDark") : t("switchLight")));

// closeMenu 在跳转路由或退出后收起移动端菜单。
const closeMenu = () => {
  menuOpen.value = false;
};

// logout 清理前台用户登录状态，并关闭菜单。
const logout = () => {
  frontUserStore.clearAuth();
  closeMenu();
};

// 组件挂载时应用主题、启动页面自动翻译，并加载站点设置。
onMounted(async () => {
  applyTheme();
  startFrontTranslator();
  setting.value = await getFrontSetting();
});
</script>

<template>
  <!-- 前台头部：包含站点 Logo 和主导航。 -->
  <header class="front-header">
        <!-- 品牌区：展示 Logo 和站点名称，点击返回首页。 -->
    <RouterLink class="front-logo" to="/" @click="closeMenu">
      <img v-if="setting.logo" :src="setting.logo" :alt="setting.siteName" />
      <span v-else>CMS</span>
      <strong>{{ setting.siteName || t("siteFallback") }}</strong>
    </RouterLink>

        <!-- 移动端菜单按钮：小屏设备下展开或收起导航。 -->
    <button class="front-menu-toggle" type="button" :aria-expanded="menuOpen" @click="menuOpen = !menuOpen">
      <span></span>
      <span></span>
      <span></span>
    </button>

        <!-- 主导航区：根据登录状态显示登录入口、用户中心和退出按钮。 -->
    <nav class="front-nav" :class="{ open: menuOpen }">
      <RouterLink to="/" @click="closeMenu">{{ t("home") }}</RouterLink>
      <RouterLink to="/search" @click="closeMenu">{{ t("search") }}</RouterLink>
      <RouterLink to="/favorites" @click="closeMenu">{{ t("favorites") }}</RouterLink>
      <RouterLink to="/topics" @click="closeMenu">{{ t("topics") }}</RouterLink>
      <RouterLink to="/archive" @click="closeMenu">{{ t("archive") }}</RouterLink>
      <RouterLink to="/links" @click="closeMenu">{{ t("links") }}</RouterLink>
      <RouterLink to="/guestbook" @click="closeMenu">{{ t("guestbook") }}</RouterLink>
      <RouterLink to="/about" @click="closeMenu">{{ t("about") }}</RouterLink>
      <RouterLink v-if="!frontUserStore.isLoggedIn" to="/user/login" @click="closeMenu">{{ t("frontLogin") }}</RouterLink>
      <RouterLink v-else to="/user/center" @click="closeMenu">{{ t("userCenter") }}</RouterLink>
      <button v-if="frontUserStore.isLoggedIn" class="front-user-pill" type="button" @click="logout">
        {{ frontUserStore.nickname }} / {{ t("logout") }}
      </button>
      <RouterLink to="/login" @click="closeMenu">{{ t("adminLogin") }}</RouterLink>
    </nav>
  </header>

  <div class="front-floating-prefs" aria-label="front preferences">
    <button class="front-pref-btn" type="button" :aria-label="language === 'zh' ? t('switchEnglish') : t('switchChinese')" @click="toggleLanguage">
      <span>{{ languageCurrent }}</span>
      <strong>{{ languageNext }}</strong>
    </button>
    <button class="front-pref-btn theme-pref-btn" type="button" :aria-label="theme === 'light' ? t('switchDark') : t('switchLight')" @click="toggleTheme">
      <span>{{ themeCurrent }}</span>
      <strong>{{ themeNext }}</strong>
    </button>
  </div>

    <!-- 右下角浮动偏好设置：语言切换和日夜主题切换。 -->
</template>
