<script setup lang="ts">
// Links.vue 是前台友链页：展示已审核友链，并支持访客提交友链申请。
import { onMounted, reactive, ref } from "vue";
import FrontHeader from "../../components/FrontHeader.vue";
import { applyFriendLink, getFrontLinks } from "../../api/link";
import type { FriendLink, FriendLinkPayload } from "../../types/link";

// loading 表示友链列表是否正在加载。
const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const links = ref<FriendLink[]>([]);

// form 保存友链申请表单内容。
const form = reactive<FriendLinkPayload>({
  name: "",
  url: "",
  description: "",
  logo: "",
  sortOrder: 0
});

// loadLinks 加载前台展示的已审核友链。
const loadLinks = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    links.value = await getFrontLinks();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载友情链接失败";
  } finally {
    loading.value = false;
  }
};

// submitApply 提交友链申请，等待后台管理员审核。
const submitApply = async () => {
  successText.value = "";
  errorText.value = "";
  if (!form.name.trim() || !form.url.trim()) {
    errorText.value = "网站名称和链接地址不能为空";
    return;
  }

  submitting.value = true;
  try {
    await applyFriendLink(form);
    form.name = "";
    form.url = "";
    form.description = "";
    form.logo = "";
    form.sortOrder = 0;
    successText.value = "友链申请已提交，审核通过后会展示出来。";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "提交友链申请失败";
  } finally {
    submitting.value = false;
  }
};

const openLink = (url: string) => {
  window.open(url, "_blank", "noopener,noreferrer");
};

onMounted(loadLinks);
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="archive-hero links-hero">
      <p class="hero-kicker">Friends</p>
      <h1>和有趣的网站互相照亮。</h1>
      <p>这里展示已审核通过的友情链接。你也可以提交自己的网站，管理员在后台审核后会出现在列表中。</p>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>

    <section class="links-layout">
      <div class="link-grid">
        <p v-if="loading" class="muted">加载中...</p>
        <article v-for="item in links" :key="item.id" class="link-card" @click="openLink(item.url)">
          <div class="link-logo">
            <img v-if="item.logo" :src="item.logo" :alt="item.name" />
            <span v-else>{{ item.name.slice(0, 1).toUpperCase() }}</span>
          </div>
          <div>
            <h2>{{ item.name }}</h2>
            <p>{{ item.description || "这个网站还没有填写简介。" }}</p>
            <small>{{ item.url }}</small>
          </div>
        </article>

        <div v-if="!loading && links.length === 0" class="empty-state">
          <h3>还没有友情链接</h3>
          <p>可以从右侧提交申请，审核通过后会展示出来。</p>
        </div>
      </div>

      <aside class="guestbook-form-card">
        <h2>申请友链</h2>
        <p>提交后会进入后台待审核状态，建议填写简短清晰的网站介绍。</p>
        <form class="comment-form" @submit.prevent="submitApply">
          <input v-model="form.name" type="text" placeholder="网站名称（必填）" />
          <input v-model="form.url" type="url" placeholder="网站地址（必填）" />
          <input v-model="form.logo" type="url" placeholder="Logo 地址（可选）" />
          <textarea v-model="form.description" placeholder="网站简介"></textarea>
          <button class="primary-btn" type="submit" :disabled="submitting">
            {{ submitting ? "提交中..." : "提交申请" }}
          </button>
        </form>
      </aside>
    </section>
  </main>
</template>
