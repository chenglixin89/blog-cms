<script setup lang="ts">
// Guestbook.vue 是前台留言板：展示已审核留言，并支持用户提交新留言。
import { computed, onMounted, reactive, ref } from "vue";
import FrontHeader from "../../components/FrontHeader.vue";
import { getFrontMe } from "../../api/front";
import { getFrontMessages, submitFrontMessage } from "../../api/message";
import { useFrontUserStore } from "../../stores/frontUser";
import type { GuestbookMessage, GuestbookMessagePayload } from "../../types/message";

interface ThreadMessage extends GuestbookMessage {
  depth: number;
}

const frontUserStore = useFrontUserStore();
// loading 表示留言列表是否正在加载。
const loading = ref(false);
const submitting = ref(false);
const profileLoading = ref(false);
const errorText = ref("");
const successText = ref("");
const messages = ref<GuestbookMessage[]>([]);
const replyTarget = ref<GuestbookMessage | null>(null);

// form 保存留言表单内容。
const form = reactive<GuestbookMessagePayload>({
  parentId: null,
  nickname: "",
  email: "",
  content: ""
});

const isLoggedIn = computed(() => frontUserStore.isLoggedIn);
const identityTip = computed(() => {
  if (!isLoggedIn.value) {
    return "游客留言需要填写昵称；邮箱不会在前台公开。";
  }
  return profileLoading.value ? "正在读取登录用户资料..." : "已登录，将使用你的账号昵称和邮箱提交留言。";
});
const rootMessages = computed(() => messages.value.filter((item) => !item.parentId));

const descendantsOf = (parentId: number, depth = 1): ThreadMessage[] => {
  return messages.value
    .filter((item) => item.parentId === parentId)
    .flatMap((item) => [{ ...item, depth }, ...descendantsOf(item.id, depth + 1)]);
};

const formatDateTime = (value: string) =>
  new Date(value).toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });

const syncLoggedInProfile = async () => {
  if (!frontUserStore.isLoggedIn) {
    return;
  }
  profileLoading.value = true;
  form.nickname = frontUserStore.nickname;
  try {
    const profile = await getFrontMe();
    form.nickname = profile.nickname;
    form.email = profile.email;
    frontUserStore.setAuth(frontUserStore.token, profile.nickname, profile.username);
  } catch {
    form.nickname = frontUserStore.nickname;
  } finally {
    profileLoading.value = false;
  }
};

// loadMessages 加载前台可见的留言和管理员回复。
const loadMessages = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    messages.value = await getFrontMessages();
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载留言失败";
  } finally {
    loading.value = false;
  }
};

const startReply = (message: GuestbookMessage) => {
  replyTarget.value = message;
  form.parentId = message.id;
  document.getElementById("guestbook-form")?.scrollIntoView({ behavior: "smooth", block: "center" });
};

const cancelReply = () => {
  replyTarget.value = null;
  form.parentId = null;
};

// resetFormAfterSubmit 留言成功后清空内容，登录用户保留昵称和邮箱。
const resetFormAfterSubmit = () => {
  form.parentId = null;
  form.content = "";
  replyTarget.value = null;
  if (!frontUserStore.isLoggedIn) {
    form.nickname = "";
    form.email = "";
  }
};

// submitMessage 提交留言，后端保存后等待管理员审核。
const submitMessage = async () => {
  successText.value = "";
  errorText.value = "";
  if (!frontUserStore.isLoggedIn && !form.nickname?.trim()) {
    errorText.value = "游客留言需要填写昵称";
    return;
  }
  if (!form.content.trim()) {
    errorText.value = "留言内容不能为空";
    return;
  }

  submitting.value = true;
  try {
    await submitFrontMessage({
      parentId: form.parentId,
      nickname: form.nickname?.trim() ?? "",
      email: form.email?.trim() ?? "",
      content: form.content.trim()
    });
    resetFormAfterSubmit();
    successText.value = "留言已提交，管理员审核后会展示出来。";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "提交留言失败";
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadMessages();
  syncLoggedInProfile();
});
</script>

<template>
  <main class="front-page">
    <FrontHeader />

    <section class="archive-hero guestbook-hero">
      <p class="hero-kicker">Guestbook</p>
      <h1>给这个博客留一句话。</h1>
      <p>这里适合放建议、问候、问题和灵感。留言会先进入后台审核，管理员也可以在后台直接回复。</p>
    </section>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>

    <section class="guestbook-layout">
      <div class="guestbook-list">
        <p v-if="loading" class="muted">加载中...</p>
        <article v-for="item in rootMessages" :key="item.id" class="guestbook-card">
          <div class="comment-line">
            <strong>{{ item.nickname }}</strong>
            <small v-if="item.loggedInUser" class="status-pill published">用户</small>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <p>{{ item.content }}</p>
          <button class="text-btn" @click="startReply(item)">回复</button>

          <div v-if="descendantsOf(item.id).length" class="comment-replies">
            <article
              v-for="child in descendantsOf(item.id)"
              :key="child.id"
              class="guestbook-card child-comment"
              :style="{ marginLeft: `${Math.min(child.depth - 1, 3) * 14}px` }"
            >
              <div class="comment-line">
                <strong>{{ child.nickname }}</strong>
                <small v-if="child.loggedInUser" class="status-pill published">用户</small>
                <span>{{ formatDateTime(child.createdAt) }}</span>
                <small>回复 #{{ child.parentId }}</small>
              </div>
              <p>{{ child.content }}</p>
              <button class="text-btn" @click="startReply(child)">回复</button>
            </article>
          </div>
        </article>

        <div v-if="!loading && rootMessages.length === 0" class="empty-state">
          <h3>还没有留言</h3>
          <p>可以留下第一条，审核后这里会展示出来。</p>
        </div>
      </div>

      <aside class="guestbook-form-card">
        <h2>写留言</h2>
        <p>{{ identityTip }}</p>
        <form id="guestbook-form" class="comment-form" @submit.prevent="submitMessage">
          <div v-if="replyTarget" class="reply-banner">
            正在回复 {{ replyTarget.nickname }}
            <button type="button" class="text-btn" @click="cancelReply">取消回复</button>
          </div>
          <input
            v-model="form.nickname"
            type="text"
            placeholder="昵称（游客必填）"
            :disabled="isLoggedIn"
          />
          <input
            v-model="form.email"
            type="email"
            placeholder="邮箱（可选）"
            :disabled="isLoggedIn"
          />
          <textarea v-model="form.content" maxlength="1000" placeholder="写下你的留言，审核后展示"></textarea>
          <button class="primary-btn" type="submit" :disabled="submitting || profileLoading">
            {{ submitting ? "提交中..." : "提交留言" }}
          </button>
        </form>
      </aside>
    </section>
  </main>
</template>