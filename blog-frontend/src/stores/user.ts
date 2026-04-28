// user.ts 管理后台管理员登录状态，后台页面鉴权和顶部昵称展示会读取这里。
import { defineStore } from "pinia";

// 定义管理员状态的数据结构。
interface UserState {
  // 管理员昵称，默认显示“管理员”。
  nickname: string;
  // 管理员 JWT token，用于访问后台接口。
  token: string;
}

// 创建后台管理员 Pinia 仓库。
export const useUserStore = defineStore("user", {
  // 初始化时从 localStorage 恢复管理员登录状态。
  state: (): UserState => ({
    nickname: localStorage.getItem("blog_admin_nickname") ?? "管理员",
    token: localStorage.getItem("blog_admin_token") ?? ""
  }),
  // actions 封装状态修改逻辑，避免页面直接操作 localStorage。
  actions: {
    // 单独保存 token。
    setToken(token: string) {
      this.token = token;
      localStorage.setItem("blog_admin_token", token);
    },
    // 单独保存昵称。
    setNickname(nickname: string) {
      this.nickname = nickname;
      localStorage.setItem("blog_admin_nickname", nickname);
    },
    // 登录成功后一次性保存 token 和昵称。
    setAuth(token: string, nickname?: string) {
      this.setToken(token);
      if (nickname) {
        this.setNickname(nickname);
      }
    },
    // 退出登录时清空管理员 token 和昵称。
    clearToken() {
      this.token = "";
      localStorage.removeItem("blog_admin_token");
      localStorage.removeItem("blog_admin_nickname");
    }
  }
});