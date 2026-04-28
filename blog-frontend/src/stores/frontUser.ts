// frontUser.ts 管理前台普通用户登录状态，例如用户中心、收藏、点赞等功能都会读取这里。
import { defineStore } from "pinia";

// 定义前台用户状态的数据结构，方便 TypeScript 做类型检查。
interface FrontUserState {
  // 用户昵称，用于页面展示。
  nickname: string;
  // 用户账号名，用于标识当前登录用户。
  username: string;
  // 后端登录接口返回的 JWT token，用于后续请求鉴权。
  token: string;
}

// defineStore 创建 Pinia 仓库，名称 frontUser 会显示在 Vue DevTools 中。
export const useFrontUserStore = defineStore("frontUser", {
  // state 初始化时先从 localStorage 读取，刷新页面后登录状态不会丢失。
  state: (): FrontUserState => ({
    nickname: localStorage.getItem("blog_front_nickname") ?? "",
    username: localStorage.getItem("blog_front_username") ?? "",
    token: localStorage.getItem("blog_front_token") ?? ""
  }),
  // getters 相当于计算属性，用来从 state 派生出更好用的状态。
  getters: {
    // token 存在就认为前台用户已经登录。
    isLoggedIn: (state) => Boolean(state.token)
  },
  // actions 用来修改状态，也负责同步 localStorage。
  actions: {
    // 登录成功后保存 token、昵称和账号名。
    setAuth(token: string, nickname: string, username: string) {
      this.token = token;
      this.nickname = nickname;
      this.username = username;
      localStorage.setItem("blog_front_token", token);
      localStorage.setItem("blog_front_nickname", nickname);
      localStorage.setItem("blog_front_username", username);
    },
    // 退出登录或 token 失效时清空状态和本地缓存。
    clearAuth() {
      this.token = "";
      this.nickname = "";
      this.username = "";
      localStorage.removeItem("blog_front_token");
      localStorage.removeItem("blog_front_nickname");
      localStorage.removeItem("blog_front_username");
    }
  }
});