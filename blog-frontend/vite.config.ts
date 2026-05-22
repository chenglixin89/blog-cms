import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { VitePWA } from "vite-plugin-pwa";

const base = process.env.VITE_BASE || "/";

export default defineConfig({
  base,
  plugins: [
    vue(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["icon.svg"],
      manifest: {
        name: "个人博客 CMS",
        short_name: "博客CMS",
        description: "一个支持文章、评论、专题和搜索的个人博客系统",
        theme_color: "#0a84ff",
        background_color: "#f5f7fb",
        display: "standalone",
        start_url: base,
        icons: [
          {
            src: `${base}icon.svg`,
            sizes: "any",
            type: "image/svg+xml",
            purpose: "any"
          }
        ]
      },
      workbox: {
        navigateFallback: `${base}index.html`,
        cleanupOutdatedCaches: true,
        runtimeCaching: [
          {
            // The previous version cached every /api/* response for an hour
            // with NetworkFirst. Two problems:
            //   1. Admin endpoints (which are JWT-authenticated and carry
            //      privileged content) ended up in the SW cache.
            //   2. Even on the public surface, /api/front/favorites and
            //      /api/front/likes return per-user data that other users on
            //      the same browser must never see.
            // Default-deny: nothing under /api is ever cached. Public read
            // endpoints can opt back in selectively if perf becomes an issue.
            urlPattern: ({ url }) => url.pathname.startsWith("/api"),
            handler: "NetworkOnly"
          },
          {
            urlPattern: ({ request }) => request.destination === "image",
            handler: "StaleWhileRevalidate",
            options: {
              cacheName: "image-cache",
              cacheableResponse: {
                statuses: [0, 200]
              },
              expiration: {
                maxEntries: 120,
                maxAgeSeconds: 60 * 60 * 24 * 14
              }
            }
          }
        ]
      }
    })
  ],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
        timeout: 120000,
        proxyTimeout: 120000
      }
    }
  }
});
