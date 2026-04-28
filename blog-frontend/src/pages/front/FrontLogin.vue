<script setup lang="ts">
// FrontLogin.vue 是前台用户登录注册页：负责普通用户登录、注册和本地数据迁移。
import { computed, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { frontLogin, frontRegister, migrateCloudFavorites, migrateCloudLikes } from "../../api/front";
import { useFrontUserStore } from "../../stores/frontUser";

const router = useRouter();
const route = useRoute();
const frontUserStore = useFrontUserStore();
const mode = ref<"login" | "register">("login");
const submitting = ref(false);
const errorText = ref("");
const migrateText = ref("");
const bookmarkIdsKey = "bookmarked_article_ids";

// form 保存前台登录/注册共用表单。
const form = reactive({
  username: "",
  password: "",
  nickname: "",
  email: ""
});

const isRegister = computed(() => mode.value === "register");

// localFavoriteIds 读取未登录时保存在本地的收藏文章 id。
const localFavoriteIds = () => {
  try {
    const raw = localStorage.getItem(bookmarkIdsKey);
    const parsed = raw ? (JSON.parse(raw) as number[]) : [];
    return parsed.filter((value) => Number.isFinite(value));
  } catch {
    return [];
  }
};

const localLikeIds = () => {
  return Object.keys(localStorage)
    .filter((key) => key.startsWith("liked_article_") && localStorage.getItem(key) === "1")
    .map((key) => Number(key.replace("liked_article_", "")))
    .filter((value) => Number.isFinite(value));
};

// switchMode 在登录和注册模式之间切换，并清空提示信息。
const switchMode = (nextMode: "login" | "register") => {
  mode.value = nextMode;
  errorText.value = "";
  migrateText.value = "";
};

// submit 根据当前模式调用登录或注册接口，成功后保存用户 token。
const submit = async () => {
  errorText.value = "";
  migrateText.value = "";
  if (!form.username.trim() || !form.password.trim() || (isRegister.value && !form.nickname.trim())) {
    errorText.value = "请把必填信息填写完整";
    return;
  }

  submitting.value = true;
  try {
    const result = isRegister.value
      ? await frontRegister({
          username: form.username.trim(),
          password: form.password,
          nickname: form.nickname.trim(),
          email: form.email.trim()
        })
      : await frontLogin({ username: form.username.trim(), password: form.password });

    frontUserStore.setAuth(result.token, result.nickname, form.username.trim());

    const favoriteIds = localFavoriteIds();
    const likeIds = localLikeIds();
    if (favoriteIds.length > 0) {
      await migrateCloudFavorites(favoriteIds);
    }
    if (likeIds.length > 0) {
      await migrateCloudLikes(likeIds);
    }
    if (favoriteIds.length > 0 || likeIds.length > 0) {
      migrateText.value = `已同步 ${favoriteIds.length} 篇收藏、${likeIds.length} 个点赞到云端`;
    }

    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/favorites";
    router.push(redirect);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "登录失败，请稍后再试";
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="front-auth-shell">
      <div class="front-auth-copy">
        <p class="hero-kicker">Reader Account</p>
        <h1>把收藏放进你的云端书架</h1>
        <p>登录后收藏会写入数据库；没登录时依然保留本地收藏，登录成功会自动同步。</p>
        <div class="auth-feature-grid">
          <span>云端收藏</span>
          <span>多设备同步</span>
          <span>本地兜底</span>
        </div>
      </div>

      <form class="front-auth-card glass" @submit.prevent="submit">
        <div class="auth-switcher">
          <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
        </div>

        <label>
          <span>用户名</span>
          <input v-model="form.username" type="text" autocomplete="username" placeholder="例如 reader01" />
        </label>
        <label v-if="isRegister">
          <span>昵称</span>
          <input v-model="form.nickname" type="text" autocomplete="nickname" placeholder="前台展示昵称" />
        </label>
        <label v-if="isRegister">
          <span>邮箱</span>
          <input v-model="form.email" type="email" autocomplete="email" placeholder="可选，用于留言联系" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="form.password" type="password" :autocomplete="isRegister ? 'new-password' : 'current-password'" placeholder="至少 6 位" />
        </label>

        <p v-if="errorText" class="error-text">{{ errorText }}</p>
        <p v-if="migrateText" class="success-text">{{ migrateText }}</p>
        <button class="primary-btn" type="submit" :disabled="submitting">
          {{ submitting ? "处理中..." : isRegister ? "注册并同步收藏" : "登录并同步收藏" }}
        </button>
      </form>
    </section>
  </main>
</template>