<script setup lang="ts">
// Dashboard.vue 是后台仪表盘：把文章、评论、分类、标签等数据加工成统计卡片和图表。
import { computed, onMounted, ref } from "vue";
import { getArticleList } from "../../api/article";
import { getCommentList } from "../../api/comment";
import { getDashboardPublishTrend, getDashboardStats, type DashboardStats } from "../../api/dashboard";
import { getCategoryList, getTagList } from "../../api/taxonomy";
import type { Article } from "../../types/article";
import type { CommentItem } from "../../types/comment";
import type { Category, Tag } from "../../types/taxonomy";

// PieItem 表示饼图中的一个扇区数据。
interface PieItem {
  label: string;
  value: number;
  color: string;
}

// BarItem 用于柱状图和排行条形图，color 主要给标签排行复用。
interface BarItem {
  label: string;
  value: number;
  color?: string;
}

// TrendPoint 表示近 30 天发布趋势折线图中的一个日期点。
interface TrendPoint {
  key: string;
  label: string;
  value: number;
}

interface TrendCoord extends TrendPoint {
  x: number;
  y: number;
}

// loading 和 errorText 用于控制仪表盘整体加载状态与错误提示。
const loading = ref(false);
const errorText = ref("");
// stats 保存后端聚合好的基础统计卡片数据。
const stats = ref<DashboardStats>({
  articleCount: 0,
  publishedCount: 0,
  draftCount: 0,
  categoryCount: 0,
  tagCount: 0,
  pendingCommentCount: 0
});
// articles、categories、tags、comments 用于前端进一步派生图表和排行数据。
const articles = ref<Article[]>([]);
const categories = ref<Category[]>([]);
const tags = ref<Tag[]>([]);
const comments = ref<CommentItem[]>([]);

// 以下 computed 负责从接口数据中派生统计图表需要的数据。
const archivedCount = computed(() => articles.value.filter((item) => item.status === 2).length);
const rejectedCommentCount = computed(() => comments.value.filter((item) => item.status === 2).length);
const approvedCommentCount = computed(() => comments.value.filter((item) => item.status === 1).length);

// statusPieItems 统计文章发布状态，用于文章状态饼图。
const statusPieItems = computed<PieItem[]>(() => [
  { label: "已发布", value: stats.value.publishedCount, color: "#34c759" },
  { label: "草稿", value: stats.value.draftCount, color: "#0a84ff" },
  { label: "归档", value: archivedCount.value, color: "#ff9500" }
]);

const commentPieItems = computed<PieItem[]>(() => [
  { label: "待审核", value: stats.value.pendingCommentCount, color: "#0a84ff" },
  { label: "已通过", value: approvedCommentCount.value, color: "#34c759" },
  { label: "已拒绝", value: rejectedCommentCount.value, color: "#ff3b30" }
]);

// categoryBars 统计各分类下已发布文章数量，用于分类柱状图。
const categoryBars = computed<BarItem[]>(() => {
  return [...categories.value]
    .map((category) => ({
      label: category.name,
      value: articles.value.filter((article) => article.categoryId === category.id && article.status === 1).length
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);
});

// tagBars 统计已发布文章里各标签出现次数，并保留标签色彩用于图表条形填充。
const tagBars = computed<BarItem[]>(() => {
  return [...tags.value]
    .map((tag) => ({
      label: tag.name,
      value: articles.value.filter((article) => article.status === 1 && article.tags.some((item) => item.id === tag.id)).length,
      color: tag.color
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);
});

// engagementBars 根据阅读、点赞、评论计算文章热度排行。
const engagementBars = computed<BarItem[]>(() => {
  return [...articles.value]
    .map((article) => ({
      label: article.title,
      value: (article.viewCount ?? 0) + (article.likeCount ?? 0) * 3 + (article.commentCount ?? 0) * 5
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 6);
});

// 这几个最大值用于把柱状图宽度归一化，避免不同图之间的比例混乱。
const maxCategoryValue = computed(() => Math.max(...categoryBars.value.map((item) => item.value), 1));
const maxTagValue = computed(() => Math.max(...tagBars.value.map((item) => item.value), 1));
const maxEngagementValue = computed(() => Math.max(...engagementBars.value.map((item) => item.value), 1));

// trendPoints 保存最近 30 天发布趋势原始数据。
const trendPoints = ref<TrendPoint[]>([]);

// trendTotal、trendPeak 和 hover 状态一起驱动折线图摘要、默认高亮和 tooltip。
const maxTrendValue = computed(() => Math.max(...trendPoints.value.map((item) => item.value), 1));
const trendTotal = computed(() => trendPoints.value.reduce((sum, item) => sum + item.value, 0));
const trendPeak = computed(() => {
  return [...trendPoints.value].sort((a, b) => b.value - a.value)[0] ?? { key: "", label: "暂无", value: 0 };
});
const hoveredTrendIndex = ref<number | null>(null);

// 这些常量定义 SVG 折线图绘制区域，方便统一控制坐标换算。
const chartWidth = 680;
const chartHeight = 230;
const chartPlotLeft = 34;
const chartPlotRight = 660;
const chartTopY = 28;
const chartFloorY = 190;
const chartDrawWidth = chartPlotRight - chartPlotLeft;
const chartDrawHeight = chartFloorY - chartTopY;

// trendPointPosition 把日期发布数量转换成 SVG 折线图坐标。
const trendPointPosition = (item: TrendPoint, index: number) => {
  const ratioX = trendPoints.value.length <= 1 ? 0.5 : index / (trendPoints.value.length - 1);
  const ratioY = maxTrendValue.value <= 0 ? 0 : item.value / maxTrendValue.value;
  const x = chartPlotLeft + ratioX * chartDrawWidth;
  const y = chartFloorY - ratioY * chartDrawHeight;
  return { x, y };
};

// trendCoords 把趋势原始数据转换成可直接用于 SVG 渲染的坐标点集合。
const trendCoords = computed<TrendCoord[]>(() => {
  return trendPoints.value.map((item, index) => {
    const position = trendPointPosition(item, index);
    return {
      ...item,
      x: position.x,
      y: position.y
    };
  });
});

// buildLinePath 根据坐标点生成 SVG path 的 d 属性。
const buildLinePath = (points: TrendCoord[]) => {
  if (points.length === 0) {
    return "";
  }
  return points
    .map((point, index) => `${index === 0 ? "M" : "L"} ${point.x} ${point.y}`)
    .join(" ");
};

// trendLinePath、trendAreaPath、trendLabelPoints 一起驱动折线、面积和底部日期标签。
const trendLinePath = computed(() => buildLinePath(trendCoords.value));
const trendAreaPath = computed(() => {
  if (trendCoords.value.length === 0) {
    return "";
  }
  const first = trendCoords.value[0];
  const last = trendCoords.value[trendCoords.value.length - 1];
  return `${trendLinePath.value} L ${last.x} ${chartFloorY} L ${first.x} ${chartFloorY} Z`;
});
const trendLabelPoints = computed(() => trendCoords.value.filter((_, index) => index % 5 === 0 || index === trendCoords.value.length - 1));
const defaultTrendIndex = computed(() => trendCoords.value.findIndex((point) => point.key === trendPeak.value.key));
const activeTrendIndex = computed(() => {
  if (hoveredTrendIndex.value !== null) {
    return hoveredTrendIndex.value;
  }
  if (defaultTrendIndex.value >= 0) {
    return defaultTrendIndex.value;
  }
  return trendCoords.value.length > 0 ? trendCoords.value.length - 1 : -1;
});
const activeTrendPoint = computed(() => {
  if (activeTrendIndex.value < 0) {
    return null;
  }
  return trendCoords.value[activeTrendIndex.value] ?? null;
});
const trendTooltipStyle = computed(() => {
  if (!activeTrendPoint.value) {
    return { display: "none" };
  }
  return {
    left: `${(activeTrendPoint.value.x / chartWidth) * 100}%`,
    top: `${(activeTrendPoint.value.y / chartHeight) * 100}%`
  };
});

// hoveredStatusIndex 和 hoveredCommentIndex 让鼠标悬停图例时同步切换当前高亮扇区。
const hoveredStatusIndex = ref<number | null>(null);
const hoveredCommentIndex = ref<number | null>(null);

// activePieIndex 统一处理“优先取 hover 项，否则取占比最高项”的高亮策略。
const activePieIndex = (items: PieItem[], hoveredIndex: number | null) => {
  if (hoveredIndex !== null) {
    return hoveredIndex;
  }
  return items.findIndex((item) => item.value === topPieItem(items).value);
};

const activeStatusItem = computed(() => {
  const index = activePieIndex(statusPieItems.value, hoveredStatusIndex.value);
  return statusPieItems.value[index] ?? topPieItem(statusPieItems.value);
});

const activeCommentItem = computed(() => {
  const index = activePieIndex(commentPieItems.value, hoveredCommentIndex.value);
  return commentPieItems.value[index] ?? topPieItem(commentPieItems.value);
});

// 这组样式辅助函数根据当前高亮颜色生成卡片光晕、圆环外壳和阴影。
const pieCardTint = (color: string) => ({
  background: `radial-gradient(circle at 24% 14%, ${color}14, transparent 56%)`
});

const pieShellStyle = (color: string) => ({
  boxShadow: `0 20px 38px rgba(15, 23, 42, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 0 0 1px ${color}26`
});

const pieDonutStyle = (items: PieItem[], color: string) => {
  return {
    ...pieStyle(items),
    boxShadow: `0 18px 28px ${color}2e`
  };
};

const pieTotal = (items: PieItem[]) => items.reduce((sum, item) => sum + item.value, 0);

// pieStyle 使用 conic-gradient 生成苹果风格的环形饼图背景。
const pieStyle = (items: PieItem[]) => {
  const total = pieTotal(items);
  if (total <= 0) {
    return { background: "conic-gradient(rgba(148, 163, 184, 0.35) 0deg 360deg)" };
  }

  let start = 0;
  const segments = items.map((item) => {
    const degrees = (item.value / total) * 360;
    const end = start + degrees;
    const segment = `${item.color} ${start}deg ${end}deg`;
    start = end;
    return segment;
  });

  return { background: `conic-gradient(${segments.join(", ")})` };
};

// percent、barWidth、topPieItem 负责把原始数值转换成模板更好消费的展示数据。
const percent = (value: number, total: number) => (total > 0 ? Math.round((value / total) * 100) : 0);
const barWidth = (value: number, max: number) => `${Math.max(6, Math.round((value / max) * 88))}%`;
const topPieItem = (items: PieItem[]) => {
  return [...items].sort((a, b) => b.value - a.value)[0] ?? { label: "暂无", value: 0, color: "#94a3b8" };
};

// loadStats 并行加载统计卡片、趋势、文章、分类、标签和评论，再统一回填页面。
const loadStats = async () => {
  loading.value = true;
  errorText.value = "";
  try {
    const [statsData, trendData, articleData, categoryData, tagData, commentData] = await Promise.all([
      getDashboardStats(),
      getDashboardPublishTrend(),
      getArticleList(),
      getCategoryList(),
      getTagList(),
      getCommentList()
    ]);
    stats.value = statsData;
    trendPoints.value = trendData;
    articles.value = articleData;
    categories.value = categoryData;
    tags.value = tagData;
    comments.value = commentData;
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "加载统计数据失败";
  } finally {
    loading.value = false;
  }
};

onMounted(loadStats);
</script>

<template>
  <section class="dashboard-grid">
    <article class="glass stat-card">
      <p>总文章数</p>
      <h2>{{ stats.articleCount }}</h2>
      <span>{{ loading ? "同步中..." : "来自数据库" }}</span>
    </article>
    <article class="glass stat-card">
      <p>已发布</p>
      <h2>{{ stats.publishedCount }}</h2>
      <span>前台可展示</span>
    </article>
    <article class="glass stat-card">
      <p>草稿</p>
      <h2>{{ stats.draftCount }}</h2>
      <span>待完善内容</span>
    </article>

    <article class="glass stat-card">
      <p>分类数</p>
      <h2>{{ stats.categoryCount }}</h2>
      <span>内容结构</span>
    </article>
    <article class="glass stat-card">
      <p>标签数</p>
      <h2>{{ stats.tagCount }}</h2>
      <span>主题索引</span>
    </article>
    <article class="glass stat-card">
      <p>待审评论</p>
      <h2>{{ stats.pendingCommentCount }}</h2>
      <span>{{ errorText || "需要处理" }}</span>
    </article>

    <article class="glass chart-card apple-chart-card pie-card" :style="pieCardTint(activeStatusItem.color)">
      <header class="chart-header">
        <div>
          <p class="eyebrow">Article Status</p>
          <h3>文章状态分布</h3>
        </div>
        <strong>{{ activeStatusItem.label }}</strong>
      </header>
      <div class="apple-pie-layout">
        <div class="apple-ring-shell" :style="pieShellStyle(activeStatusItem.color)">
          <div class="donut-chart apple-donut" :style="pieDonutStyle(statusPieItems, activeStatusItem.color)">
            <div>
              <strong>{{ activeStatusItem.value }}</strong>
              <span>{{ percent(activeStatusItem.value, pieTotal(statusPieItems)) }}% · {{ activeStatusItem.label }}</span>
            </div>
          </div>
        </div>
        <div class="chart-legend apple-legend">
          <div
            v-for="(item, index) in statusPieItems"
            :key="item.label"
            class="legend-row"
            :class="{ active: index === activePieIndex(statusPieItems, hoveredStatusIndex) }"
            @mouseenter="hoveredStatusIndex = index"
            @mouseleave="hoveredStatusIndex = null"
          >
            <span class="legend-dot" :style="{ background: item.color }"></span>
            <strong>{{ item.label }}</strong>
            <em>{{ item.value }} 篇</em>
            <small>{{ percent(item.value, pieTotal(statusPieItems)) }}%</small>
          </div>
        </div>
      </div>
    </article>

    <article class="glass chart-card apple-chart-card pie-card" :style="pieCardTint(activeCommentItem.color)">
      <header class="chart-header">
        <div>
          <p class="eyebrow">Comments</p>
          <h3>评论审核分布</h3>
        </div>
        <strong>{{ activeCommentItem.label }}</strong>
      </header>
      <div class="apple-pie-layout">
        <div class="apple-ring-shell" :style="pieShellStyle(activeCommentItem.color)">
          <div class="donut-chart apple-donut" :style="pieDonutStyle(commentPieItems, activeCommentItem.color)">
            <div>
              <strong>{{ activeCommentItem.value }}</strong>
              <span>{{ percent(activeCommentItem.value, pieTotal(commentPieItems)) }}% · {{ activeCommentItem.label }}</span>
            </div>
          </div>
        </div>
        <div class="chart-legend apple-legend">
          <div
            v-for="(item, index) in commentPieItems"
            :key="item.label"
            class="legend-row"
            :class="{ active: index === activePieIndex(commentPieItems, hoveredCommentIndex) }"
            @mouseenter="hoveredCommentIndex = index"
            @mouseleave="hoveredCommentIndex = null"
          >
            <span class="legend-dot" :style="{ background: item.color }"></span>
            <strong>{{ item.label }}</strong>
            <em>{{ item.value }} 条</em>
            <small>{{ percent(item.value, pieTotal(commentPieItems)) }}%</small>
          </div>
        </div>
      </div>
    </article>

    <article class="glass chart-card wide-chart-card line-chart-card">
      <header class="chart-header">
        <div>
          <p class="eyebrow">Publishing Trend</p>
          <h3>近 30 天发布趋势</h3>
        </div>
        <div class="trend-summary">
          <strong>{{ trendTotal }} 篇</strong>
          <small>峰值 {{ trendPeak.label }}：{{ trendPeak.value }} 篇</small>
        </div>
      </header>
      <div class="line-chart-wrap">
        <div class="trend-panel-glow"></div>
        <svg
          class="line-chart"
          viewBox="0 0 680 230"
          role="img"
          aria-label="近 30 天每日发布文章数量折线图"
          @mouseleave="hoveredTrendIndex = null"
        >
          <defs>
            <linearGradient id="trendLineGradient" x1="0" x2="1" y1="0" y2="0">
              <stop offset="0%" stop-color="#0a84ff" />
              <stop offset="55%" stop-color="#5ac8fa" />
              <stop offset="100%" stop-color="#34c759" />
            </linearGradient>
            <linearGradient id="trendAreaGradient" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stop-color="#0a84ff" stop-opacity="0.28" />
              <stop offset="100%" stop-color="#0a84ff" stop-opacity="0" />
            </linearGradient>
          </defs>
          <g class="trend-grid">
            <line v-for="y in [40, 80, 120, 160, 190]" :key="y" x1="0" :y1="y" x2="680" :y2="y" />
          </g>
          <line
            v-if="activeTrendPoint"
            class="trend-focus-line"
            :x1="activeTrendPoint.x"
            y1="20"
            :x2="activeTrendPoint.x"
            y2="190"
          />
          <text class="trend-y-label" x="0" y="18">{{ maxTrendValue }} 篇</text>
          <text class="trend-y-label muted-label" x="0" y="198">0</text>
          <path class="trend-area" :d="trendAreaPath" />
          <path class="trend-line trend-line-shadow" :d="trendLinePath" />
          <path class="trend-line" :d="trendLinePath" />
          <circle
            v-if="activeTrendPoint"
            class="trend-active-halo"
            :cx="activeTrendPoint.x"
            :cy="activeTrendPoint.y"
            r="12"
          />
          <g>
            <circle
              v-for="(item, index) in trendCoords"
              :key="item.key"
              class="trend-dot"
              :class="{ active: index === activeTrendIndex }"
              :cx="item.x"
              :cy="item.y"
              r="4"
              @mouseenter="hoveredTrendIndex = index"
            >
              <title>{{ item.key }}：{{ item.value }} 篇</title>
            </circle>
          </g>
          <g class="trend-labels">
            <text
              v-for="item in trendLabelPoints"
              :key="item.key"
              :x="item.x"
              y="224"
            >
              {{ item.label }}
            </text>
          </g>
        </svg>
        <div v-if="activeTrendPoint" class="trend-tooltip" :style="trendTooltipStyle">
          <small>{{ activeTrendPoint.key }}</small>
          <strong>{{ activeTrendPoint.value }} 篇</strong>
        </div>
      </div>
    </article>

    <article class="glass chart-card apple-chart-card apple-bar-card">
      <header class="chart-header">
        <div>
          <p class="eyebrow">Categories</p>
          <h3>分类文章数</h3>
        </div>
      </header>
      <div class="bar-chart apple-bar-chart">
        <div v-for="item in categoryBars" :key="item.label" class="bar-row">
          <span>{{ item.label }}</span>
          <div class="bar-track">
            <i :style="{ width: barWidth(item.value, maxCategoryValue) }"></i>
          </div>
          <strong>{{ item.value }}</strong>
        </div>
        <p v-if="categoryBars.length === 0" class="muted">暂无分类数据</p>
      </div>
    </article>

    <article class="glass chart-card apple-chart-card apple-bar-card">
      <header class="chart-header">
        <div>
          <p class="eyebrow">Tags</p>
          <h3>标签文章数</h3>
        </div>
      </header>
      <div class="bar-chart apple-bar-chart">
        <div v-for="item in tagBars" :key="item.label" class="bar-row">
          <span>{{ item.label }}</span>
          <div class="bar-track">
            <i :style="{ width: barWidth(item.value, maxTagValue), background: item.color || undefined }"></i>
          </div>
          <strong>{{ item.value }}</strong>
        </div>
        <p v-if="tagBars.length === 0" class="muted">暂无标签数据</p>
      </div>
    </article>

    <article class="glass chart-card wide-chart-card apple-chart-card apple-bar-card">
      <header class="chart-header">
        <div>
          <p class="eyebrow">Engagement</p>
          <h3>热门文章互动指数</h3>
        </div>
        <small>阅读 + 点赞 x3 + 评论 x5</small>
      </header>
      <div class="bar-chart apple-bar-chart">
        <div v-for="item in engagementBars" :key="item.label" class="bar-row wide-bar-row">
          <span>{{ item.label }}</span>
          <div class="bar-track">
            <i :style="{ width: barWidth(item.value, maxEngagementValue) }"></i>
          </div>
          <strong>{{ item.value }}</strong>
        </div>
        <p v-if="engagementBars.length === 0" class="muted">暂无文章互动数据</p>
      </div>
    </article>
  </section>
</template>
