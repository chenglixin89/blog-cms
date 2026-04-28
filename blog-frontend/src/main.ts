// main.ts 是前端项目入口文件：负责创建 Vue 应用、挂载插件、注册 PWA，并把页面挂载到 index.html 的 #app 节点。
import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./style.css";

// 开发环境下清理浏览器里旧的 Service Worker 和缓存，避免 PWA 缓存导致页面更新后仍显示旧代码。
const cleanupDevServiceWorker = async () => {
  // 如果当前浏览器不支持 Service Worker，直接结束。
  if (!("serviceWorker" in navigator)) {
    return;
  }

  // 找到浏览器里已经注册的所有 Service Worker，并逐个注销。
  const registrations = await navigator.serviceWorker.getRegistrations();
  await Promise.all(registrations.map((registration) => registration.unregister()));

  // 同时清理 Cache Storage，防止旧静态资源继续被读取。
  if ("caches" in window) {
    const keys = await caches.keys();
    await Promise.all(keys.map((key) => caches.delete(key)));
  }
};

// 生产环境才注册 PWA，让打包后的项目支持离线缓存和类 App 体验。
if (import.meta.env.PROD) {
  void import("virtual:pwa-register").then(({ registerSW }) => {
    registerSW({ immediate: true });
  });
} else {
  // 开发环境主动清缓存，方便调试最新代码。
  void cleanupDevServiceWorker();
}

// 创建 Vue 应用实例，安装 Pinia 状态管理和 Vue Router 路由，最后挂载到页面根节点。
createApp(App).use(createPinia()).use(router).mount("#app");