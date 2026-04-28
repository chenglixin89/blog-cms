<script setup lang="ts">
// Login.vue 是后台管理员登录页：负责提交账号密码并保存管理员 token。
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { loginByPassword } from "../../api/auth";
import { useUserStore } from "../../stores/user";

// router 负责登录成功后的页面跳转，userStore 负责持久化管理员登录态。
const router = useRouter();
const userStore = useUserStore();

// form 保存管理员登录表单。
const form = reactive({
  username: "admin",
  password: "123456"
});

// loading 控制提交按钮状态，errorText 用于展示登录失败或校验提示。
const loading = ref(false);
const errorText = ref("");

// submit 调用后台登录接口，成功后保存管理员 token 并进入仪表盘。
const submit = async () => {
  errorText.value = "";
  if (!form.username || !form.password) {
    errorText.value = "请输入用户名和密码";
    return;
  }

  loading.value = true;
  try {
    const result = await loginByPassword({
      username: form.username,
      password: form.password
    });
    userStore.setAuth(result.token, result.nickname);
    await router.push("/admin/dashboard");
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "登录失败，请稍后重试";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <section class="login-page">
    <div class="aurora aurora-a"></div>
    <div class="aurora aurora-b"></div>
    <form class="login-card glass" @submit.prevent="submit">
      <p class="eyebrow">Phase 2</p>
      <h1>个人博客 CMS</h1>
      <p class="muted">苹果风后台登录（真实接口模式）</p>

      <label>
        用户名
        <input v-model="form.username" type="text" placeholder="请输入管理员账号" />
      </label>
      <label>
        密码
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </label>

      <p v-if="errorText" class="error-text">{{ errorText }}</p>
      <button class="primary-btn" type="submit" :disabled="loading">
        {{ loading ? "登录中..." : "进入后台" }}
      </button>
    </form>
  </section>
</template>
