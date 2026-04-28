// app.ts 保存应用级 UI 状态，目前主要用于后台侧边栏展开 / 收起。
import { defineStore } from "pinia";

// 创建全局应用状态仓库。
export const useAppStore = defineStore("app", {
  // sidebarCollapsed 表示后台侧边栏是否处于折叠状态。
  state: () => ({
    sidebarCollapsed: false
  }),
  // actions 封装 UI 状态变化。
  actions: {
    // 切换后台侧边栏展开 / 收起。
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    }
  }
});