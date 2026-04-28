// http.ts 对 axios 做统一封装：所有前后端接口请求都会经过这里。
import axios from "axios";

// 创建 axios 实例，baseURL=/api 会被 Vite 代理转发到 Spring Boot 后端。
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "/api",
  timeout: 10000
});

// 判断当前接口是否属于前台用户体系，用来区分普通用户 token 和管理员 token。
const isFrontUserRequest = (url = "") =>
  url.startsWith("/front/auth") ||
  url.startsWith("/front/favorites") ||
  url.startsWith("/front/likes") ||
  url.startsWith("/front/articles");

// 请求拦截器：请求发出前自动给接口添加 Authorization 请求头。
http.interceptors.request.use((config) => {
  // 读取本次请求地址，防止 config.url 为空时报错。
  const url = String(config.url ?? "");
  // 前台接口使用 blog_front_token，后台接口使用 blog_admin_token。
  const tokenKey = isFrontUserRequest(url) ? "blog_front_token" : "blog_admin_token";
  // 从浏览器本地存储读取 token。
  const token = localStorage.getItem(tokenKey);
  // 如果 token 存在，就按照 Bearer Token 格式放到请求头里。
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // 返回 config 后，axios 才会继续发送请求。
  return config;
});

// 响应拦截器：统一处理成功响应和失败响应，让页面不用重复写错误处理代码。
http.interceptors.response.use(
  // 后端统一响应结构通常是 { code, message, data }，这里直接返回 response.data 给业务页面使用。
  (response) => response.data,
  (error) => {
    // HTTP 状态码，例如 401、404、500。
    const status = error?.response?.status;
    // 当前失败接口的地址。
    const requestUrl = String(error?.config?.url ?? "");
    // 优先读取后端返回的错误信息。
    const backendMessage =
      error?.response?.data?.message ??
      error?.response?.data?.msg ??
      error?.response?.data?.error;

    // 401 表示登录失效或 token 无效，需要清理对应登录状态。
    if (status === 401) {
      if (isFrontUserRequest(requestUrl)) {
        // 前台用户失效时，只清理前台用户信息。
        localStorage.removeItem("blog_front_token");
        localStorage.removeItem("blog_front_nickname");
        localStorage.removeItem("blog_front_username");
      } else {
        // 后台管理员失效时，清理管理员信息并跳回后台登录页。
        localStorage.removeItem("blog_admin_token");
        localStorage.removeItem("blog_admin_nickname");
        location.href = "/login";
      }
    }

    // axios 超时一般发生在 AI 生成较慢时，给用户更明确的提示。
    if (error?.code === "ECONNABORTED") {
      return Promise.reject(new Error("\u8bf7\u6c42\u8d85\u65f6\uff1aAI \u751f\u6210\u8017\u65f6\u8f83\u957f\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u6216\u51cf\u5c11\u6b63\u6587\u957f\u5ea6"));
    }

    // 如果后端返回了 message，就把它包装成前端 Error 抛给页面。
    if (backendMessage) {
      return Promise.reject(new Error(`\u8bf7\u6c42\u5931\u8d25\uff08${status}\uff09\uff1a${backendMessage}`));
    }
    // 有状态码但没有 message 时，提示用户查看后端控制台日志。
    if (status) {
      return Promise.reject(new Error(`\u8bf7\u6c42\u5931\u8d25\uff08${status}\uff09\uff1a\u8bf7\u67e5\u770b\u540e\u7aef\u63a7\u5236\u53f0\u65e5\u5fd7`));
    }
    // 没有状态码通常表示后端服务没启动、网络断开或代理没有连上。
    return Promise.reject(new Error("\u8bf7\u6c42\u5931\u8d25\uff1a\u65e0\u6cd5\u8fde\u63a5\u5230\u540e\u7aef\u670d\u52a1"));
  }
);

// 导出封装好的 http 实例，其他 api 文件会复用它。
export default http;
