# GitHub Pages 部署说明

本项目的前端是 Vue 3 + Vite，可以直接部署到 GitHub Pages。后端服务不能运行在 GitHub Pages 上，需要另外部署到服务器、云平台或本机内网穿透地址。

## 1. 创建 GitHub 仓库

在 GitHub 新建一个仓库，例如：

- 普通项目仓库：`blog-cms`
- 个人主页仓库：`你的用户名.github.io`

普通项目仓库部署后的访问地址通常是：

```text
https://你的用户名.github.io/blog-cms/
```

个人主页仓库部署后的访问地址通常是：

```text
https://你的用户名.github.io/
```

## 2. 提交代码到 GitHub

在项目根目录执行：

```powershell
git init
git add .
git commit -m "init blog cms"
git branch -M main
git remote add origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

如果仓库已经存在 Git 记录，只需要确认远程地址并推送：

```powershell
git remote -v
git push -u origin main
```

## 3. 开启 GitHub Pages

进入 GitHub 仓库页面：

1. 打开 `Settings`
2. 点击 `Pages`
3. 在 `Build and deployment` 中把 `Source` 设置为 `GitHub Actions`
4. 回到 `Actions` 页面，等待 `Deploy Blog Frontend` 工作流执行完成

工作流文件位于：

```text
.github/workflows/deploy-frontend.yml
```

它会自动进入 `blog-frontend` 目录安装依赖、执行构建，并把 `dist` 发布到 GitHub Pages。

## 4. 配置后端接口地址

本地开发时，前端仍然使用：

```text
/api
```

线上部署时，需要在 GitHub 仓库中添加变量：

1. 打开 `Settings`
2. 点击 `Secrets and variables`
3. 点击 `Actions`
4. 切换到 `Variables`
5. 新增变量 `VITE_API_BASE`

示例：

```text
https://你的后端域名.com/api
```

注意：如果后端没有部署到公网，GitHub Pages 页面无法访问本机的 `127.0.0.1:8080`。

## 5. 本地构建检查

推送前可以先执行：

```powershell
cd blog-frontend
npm run build
```

如果构建成功，再推送到 GitHub。

## 6. 常见问题

### 页面刷新后 404

已经在工作流中把 `dist/index.html` 复制为 `dist/404.html`，用于支持 Vue Router 前端路由刷新。

### 图片、样式路径错误

项目已通过 `VITE_BASE` 自动适配 GitHub Pages 的子路径，例如 `/blog-cms/`。

### 接口请求失败

检查 `VITE_API_BASE` 是否配置为公网可访问的后端地址，并确认后端已允许跨域访问 GitHub Pages 域名。
