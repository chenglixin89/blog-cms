<script setup lang="ts">
// UserCenter.vue 是前台用户中心：展示个人资料、收藏、点赞、评论，并支持修改资料和密码。
import { onMounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import FrontHeader from "../../components/FrontHeader.vue";
import { getUserCenter, updateFrontPassword, updateFrontProfile } from "../../api/front";
import { useFrontUserStore } from "../../stores/frontUser";
import type { UserCenterData } from "../../types/frontUser";
import { formatArticleDate } from "../../utils/articleDate";

const router = useRouter();
const frontUserStore = useFrontUserStore();
const loading = ref(false);
const savingProfile = ref(false);
const savingPassword = ref(false);
const errorText = ref("");
const successText = ref("");
const center = ref<UserCenterData | null>(null);
// profileForm 保存个人资料表单。
const profileForm = reactive({ nickname: "", email: "", avatar: "", bio: "" });
// passwordForm 保存修改密码表单。
const passwordForm = reactive({ oldPassword: "", newPassword: "" });

// fillProfile 把后端用户资料回填到表单中。
const fillProfile = (data: UserCenterData) => {
  profileForm.nickname = data.profile.nickname ?? "";
  profileForm.email = data.profile.email ?? "";
  profileForm.avatar = data.profile.avatar ?? "";
  profileForm.bio = data.profile.bio ?? "";
};

// loadPage 加载用户中心数据，没有登录则跳转到前台登录页。
const loadPage = async () => {
  if (!frontUserStore.isLoggedIn) {
    await router.push("/user/login?redirect=/user/center");
    return;
  }
  loading.value = true;
  errorText.value = "";
  try {
    const data = await getUserCenter();
    center.value = data;
    fillProfile(data);
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载用户中心失败";
  } finally {
    loading.value = false;
  }
};

// saveProfile 保存个人资料，并同步更新 Pinia 中的昵称。
const saveProfile = async () => {
  if (!profileForm.nickname.trim()) {
    errorText.value = "昵称不能为空。";
    return;
  }
  savingProfile.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const profile = await updateFrontProfile(profileForm);
    frontUserStore.setAuth(frontUserStore.token, profile.nickname, profile.username);
    if (center.value) {
      center.value.profile = profile;
      fillProfile(center.value);
    }
    successText.value = "资料已保存。";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "保存资料失败";
  } finally {
    savingProfile.value = false;
  }
};

// savePassword 修改当前用户密码。
const savePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    errorText.value = "请填写旧密码和新密码。";
    return;
  }
  savingPassword.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await updateFrontPassword(passwordForm);
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    successText.value = "密码已更新，下次登录请使用新密码。";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "修改密码失败";
  } finally {
    savingPassword.value = false;
  }
};

// commentStatusText 把评论状态码转换为页面可读文本。
const commentStatusText = (status: number) => {
  if (status === 1) return "已通过";
  if (status === 2) return "已拒绝";
  return "待审核";
};
const commentStatusClass = (status: number) => (status === 1 ? "published" : status === 2 ? "archived" : "draft");

onMounted(loadPage);
</script>

<template>
  <main class="front-page">
    <FrontHeader />
    <section class="archive-hero user-center-hero"><p class="hero-kicker">User Center</p><h1>用户中心</h1><p>管理你的个人资料、云端收藏、点赞记录和评论动态。</p></section>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>
    <p v-if="loading" class="muted">加载中...</p>
    <section v-else-if="center" class="user-center-layout">
      <aside class="user-center-card profile-card"><div class="profile-avatar"><img v-if="center.profile.avatar" :src="center.profile.avatar" :alt="center.profile.nickname" /><span v-else>{{ center.profile.nickname.slice(0, 1).toUpperCase() }}</span></div><h2>{{ center.profile.nickname }}</h2><p>{{ center.profile.bio || "这个用户还没有填写简介。" }}</p><small>{{ center.profile.username }} · {{ center.profile.email || "未填写邮箱" }}</small></aside>
      <section class="user-center-main">
        <div class="user-stats-grid"><div><strong>{{ center.stats.favoriteCount }}</strong><span>收藏</span></div><div><strong>{{ center.stats.likeCount }}</strong><span>点赞</span></div><div><strong>{{ center.stats.commentCount }}</strong><span>评论</span></div><div><strong>{{ center.stats.pendingCommentCount }}</strong><span>待审核</span></div></div>
        <div class="user-center-panel"><h2>编辑资料</h2><div class="user-form-grid"><label>昵称<input v-model="profileForm.nickname" type="text" /></label><label>邮箱<input v-model="profileForm.email" type="email" /></label><label class="full-row">头像地址<input v-model="profileForm.avatar" type="url" placeholder="https://example.com/avatar.png" /></label><label class="full-row">个人简介<textarea v-model="profileForm.bio" rows="4"></textarea></label></div><button class="primary-btn" :disabled="savingProfile" @click="saveProfile">{{ savingProfile ? "保存中..." : "保存资料" }}</button></div>
        <div class="user-center-panel"><h2>修改密码</h2><div class="user-form-grid"><label>旧密码<input v-model="passwordForm.oldPassword" type="password" /></label><label>新密码<input v-model="passwordForm.newPassword" type="password" /></label></div><button class="ghost-btn" :disabled="savingPassword" @click="savePassword">{{ savingPassword ? "更新中..." : "更新密码" }}</button></div>
        <div class="user-center-panel"><h2>最近收藏</h2><div class="compact-article-list"><RouterLink v-for="item in center.favorites.slice(0, 5)" :key="item.id" :to="'/articles/' + item.id"><strong>{{ item.title }}</strong><small>{{ formatArticleDate(item) }} · {{ item.viewCount }} 阅读</small></RouterLink><p v-if="center.favorites.length === 0" class="muted">还没有收藏文章。</p></div></div>
        <div class="user-center-panel"><h2>最近点赞</h2><div class="compact-article-list"><RouterLink v-for="item in center.likes.slice(0, 5)" :key="item.id" :to="'/articles/' + item.id"><strong>{{ item.title }}</strong><small>{{ formatArticleDate(item) }} · {{ item.likeCount }} 点赞</small></RouterLink><p v-if="center.likes.length === 0" class="muted">还没有点赞文章。</p></div></div>
        <div class="user-center-panel full-panel"><h2>评论动态</h2><article v-for="item in center.comments" :key="item.id" class="user-comment-card"><div><strong>{{ item.articleTitle || "未知文章" }}</strong><span class="status-pill" :class="commentStatusClass(item.status)">{{ commentStatusText(item.status) }}</span></div><p>{{ item.content }}</p><small>{{ new Date(item.createdAt).toLocaleString() }}</small></article><p v-if="center.comments.length === 0" class="muted">还没有评论记录。</p></div>
      </section>
    </section>
  </main>
</template>
