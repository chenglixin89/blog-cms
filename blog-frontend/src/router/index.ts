// router/index.ts 负责声明前台、后台所有页面路由，并通过导航守卫控制登录权限。
import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";

// routes 是路由表：每一项代表一个页面地址和它对应加载的 Vue 组件。
const routes: RouteRecordRaw[] = [
  // 前台首页，展示博客文章流、分类、标签、每日句子等内容。
  { path: "/", component: () => import("../pages/front/Home.vue") },
  // 前台搜索页，支持按关键词检索文章。
  { path: "/search", component: () => import("../pages/front/Search.vue") },
  // 前台文章详情页，:id 是动态参数，用来加载指定文章。
  { path: "/articles/:id", component: () => import("../pages/front/ArticleDetail.vue") },
  // 前台归档页，按时间线查看历史文章。
  { path: "/archive", component: () => import("../pages/front/Archive.vue") },
  // 前台专题页，按分类和标签组织内容。
  { path: "/topics", component: () => import("../pages/front/Topics.vue") },
  // 前台友链页，展示友情链接并支持申请。
  { path: "/links", component: () => import("../pages/front/Links.vue") },
  // 关于页面，展示站点介绍、技术栈和项目信息。
  { path: "/about", component: () => import("../pages/front/About.vue") },
  // 留言板页面，前台用户或访客可以提交留言。
  { path: "/guestbook", component: () => import("../pages/front/Guestbook.vue") },
  // 收藏页面，展示当前用户收藏或本地收藏的文章。
  { path: "/favorites", component: () => import("../pages/front/Favorites.vue") },
  // 前台普通用户登录 / 注册页面。
  { path: "/user/login", component: () => import("../pages/front/FrontLogin.vue") },
  // 前台用户中心，需要普通用户登录后访问。
  { path: "/user/center", component: () => import("../pages/front/UserCenter.vue") },
  // 分类文章列表，meta.filterType 用来告诉页面当前是分类筛选。
  {
    path: "/categories/:id",
    component: () => import("../pages/front/FilteredArticles.vue"),
    meta: { filterType: "category" }
  },
  // 标签文章列表，meta.filterType 用来告诉页面当前是标签筛选。
  {
    path: "/tags/:id",
    component: () => import("../pages/front/FilteredArticles.vue"),
    meta: { filterType: "tag" }
  },
  // 后台管理员登录页。
  { path: "/login", component: () => import("../pages/admin/Login.vue") },
  // 后台管理系统父路由，统一使用 AdminLayout 作为后台框架布局。
  {
    path: "/admin",
    component: () => import("../layouts/AdminLayout.vue"),
    children: [
      // 仪表盘页面，展示统计卡片和图表。
      { path: "dashboard", component: () => import("../pages/admin/Dashboard.vue"), meta: { title: "\u4eea\u8868\u76d8" } },
      // 文章管理列表页。
      { path: "articles", component: () => import("../pages/admin/ArticleList.vue"), meta: { title: "\u6587\u7ae0\u7ba1\u7406" } },
      // 新建文章页面。
      { path: "articles/new", component: () => import("../pages/admin/ArticleEdit.vue"), meta: { title: "\u65b0\u5efa\u6587\u7ae0" } },
      // 编辑文章页面，通过 id 找到文章。
      { path: "articles/:id/edit", component: () => import("../pages/admin/ArticleEdit.vue"), meta: { title: "\u7f16\u8f91\u6587\u7ae0" } },
      // 后台文章预览，复用前台详情组件，但通过 meta 标记这是后台预览。
      { path: "articles/:id/preview", component: () => import("../pages/front/ArticleDetail.vue"), meta: { title: "\u6587\u7ae0\u9884\u89c8", adminPreview: true } },
      // 分类管理。
      { path: "categories", component: () => import("../pages/admin/CategoryManager.vue"), meta: { title: "\u5206\u7c7b\u7ba1\u7406" } },
      // 标签管理。
      { path: "tags", component: () => import("../pages/admin/TagManager.vue"), meta: { title: "\u6807\u7b7e\u7ba1\u7406" } },
      // 评论管理。
      { path: "comments", component: () => import("../pages/admin/CommentManager.vue"), meta: { title: "\u8bc4\u8bba\u7ba1\u7406" } },
      // 用户管理。
      { path: "users", component: () => import("../pages/admin/UserManager.vue"), meta: { title: "\u7528\u6237\u7ba1\u7406" } },
      // 媒体库。
      { path: "media", component: () => import("../pages/admin/MediaLibrary.vue"), meta: { title: "\u5a92\u4f53\u5e93" } },
      // Skills 管理。
      { path: "ai", component: () => import("../pages/admin/AiManager.vue"), meta: { title: "Skills 管理" } },
      // 留言管理。
      { path: "messages", component: () => import("../pages/admin/MessageManager.vue"), meta: { title: "\u7559\u8a00\u7ba1\u7406" } },
      // 友链管理。
      { path: "links", component: () => import("../pages/admin/LinkManager.vue"), meta: { title: "\u53cb\u94fe\u7ba1\u7406" } },
      // 站点设置。
      { path: "settings", component: () => import("../pages/admin/SiteSettingManager.vue"), meta: { title: "\u7ad9\u70b9\u8bbe\u7f6e" } },
      // 操作日志。
      { path: "audit-logs", component: () => import("../pages/admin/AuditLogManager.vue"), meta: { title: "\u64cd\u4f5c\u65e5\u5fd7" } }
    ]
  }
];

// 创建路由实例，createWebHistory 表示使用普通 URL 模式，而不是 hash 模式。
const router = createRouter({
  history: createWebHistory(),
  routes
});

// 全局前置路由守卫：每次页面跳转前都会执行，用来判断是否需要登录。
router.beforeEach((to) => {
  // 后台管理员 token，登录成功后存入 localStorage。
  const adminToken = localStorage.getItem("blog_admin_token");
  // 前台普通用户 token，前台登录成功后存入 localStorage。
  const frontToken = localStorage.getItem("blog_front_token");
  // 访问后台页面时，如果没有管理员 token，就跳转到后台登录页。
  if (to.path.startsWith("/admin") && !adminToken) {
    return "/login";
  }
  // 已经登录管理员时，再访问登录页就直接进入仪表盘。
  if (to.path === "/login" && adminToken) {
    return "/admin/dashboard";
  }
  // 用户中心需要普通用户登录，没有 token 时跳转到前台登录页。
  if (to.path === "/user/center" && !frontToken) {
    return "/user/login?redirect=/user/center";
  }
  // 返回 true 表示允许本次路由跳转。
  return true;
});

// 导出路由实例，main.ts 会把它挂载到 Vue 应用上。
export default router;
