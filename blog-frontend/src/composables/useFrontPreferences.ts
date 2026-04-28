// useFrontPreferences.ts 封装前台的语言切换、日夜主题切换，以及固定中文文案自动翻译能力。
import { computed, ref, watch } from "vue";

type FrontLanguage = "zh" | "en";
type FrontTheme = "light" | "dark";
type I18nKey =
  | "siteFallback"
  | "home"
  | "search"
  | "favorites"
  | "topics"
  | "archive"
  | "links"
  | "guestbook"
  | "about"
  | "frontLogin"
  | "userCenter"
  | "logout"
  | "adminLogin"
  | "language"
  | "themeLight"
  | "themeDark"
  | "switchEnglish"
  | "switchChinese"
  | "switchDark"
  | "switchLight";

type TranslatableAttr = "placeholder" | "title" | "aria-label";

// language 保存当前前台语言，默认从 localStorage 恢复。
const language = ref<FrontLanguage>((localStorage.getItem("blog_front_language") as FrontLanguage) || "zh");
// theme 保存当前前台主题，支持 light / dark。
const theme = ref<FrontTheme>((localStorage.getItem("blog_front_theme") as FrontTheme) || "light");

// messages 是显式翻译表，主要给 Vue 模板里通过 t(key) 调用的导航和按钮使用。
const messages: Record<FrontLanguage, Record<I18nKey, string>> = {
  zh: {
    siteFallback: "\u4e2a\u4eba\u535a\u5ba2",
    home: "\u9996\u9875",
    search: "\u641c\u7d22",
    favorites: "\u6536\u85cf",
    topics: "\u4e13\u9898",
    archive: "\u5f52\u6863",
    links: "\u53cb\u94fe",
    guestbook: "\u7559\u8a00\u677f",
    about: "\u5173\u4e8e",
    frontLogin: "\u524d\u53f0\u767b\u5f55",
    userCenter: "\u7528\u6237\u4e2d\u5fc3",
    logout: "\u9000\u51fa",
    adminLogin: "\u540e\u53f0\u767b\u5f55",
    language: "\u4e2d\u6587",
    themeLight: "\u65e5\u95f4",
    themeDark: "\u591c\u95f4",
    switchEnglish: "EN",
    switchChinese: "\u4e2d",
    switchDark: "\u591c\u95f4",
    switchLight: "\u65e5\u95f4"
  },
  en: {
    siteFallback: "Personal Blog",
    home: "Home",
    search: "Search",
    favorites: "Favorites",
    topics: "Topics",
    archive: "Archive",
    links: "Links",
    guestbook: "Guestbook",
    about: "About",
    frontLogin: "Sign in",
    userCenter: "Profile",
    logout: "Logout",
    adminLogin: "Admin",
    language: "English",
    themeLight: "Light",
    themeDark: "Dark",
    switchEnglish: "EN",
    switchChinese: "\u4e2d",
    switchDark: "Dark",
    switchLight: "Light"
  }
};

// uiTextPairs 是前台页面固定中文文案的翻译表，用于补齐没有显式接入 t(key) 的静态文本。
const uiTextPairs = [
  ["\u9996\u9875", "Home"],
  ["\u641c\u7d22", "Search"],
  ["\u6536\u85cf", "Favorites"],
  ["\u4e13\u9898", "Topics"],
  ["\u5f52\u6863", "Archive"],
  ["\u53cb\u94fe", "Links"],
  ["\u7559\u8a00\u677f", "Guestbook"],
  ["\u5173\u4e8e", "About"],
  ["\u524d\u53f0\u767b\u5f55", "Sign in"],
  ["\u7528\u6237\u4e2d\u5fc3", "Profile"],
  ["\u540e\u53f0\u767b\u5f55", "Admin"],
  ["\u9000\u51fa", "Logout"],
  ["\u67e5\u770b\u5f52\u6863", "View Archive"],
  ["\u6d4f\u89c8\u4e13\u9898", "Browse Topics"],
  ["\u5168\u7ad9\u641c\u7d22", "Site Search"],
  ["\u91cd\u7f6e\u7b5b\u9009", "Reset Filters"],
  ["\u6587\u7ae0\u6d41", "Article Feed"],
  ["\u5206\u7c7b", "Categories"],
  ["\u6807\u7b7e", "Tags"],
  ["\u5168\u90e8", "All"],
  ["\u5168\u90e8\u5206\u7c7b", "All Categories"],
  ["\u5168\u90e8\u6807\u7b7e", "All Tags"],
  ["\u672c\u9875\u70ed\u95e8", "Trending"],
  ["\u7f6e\u9876\u6587\u7ae0", "Pinned Posts"],
  ["\u6700\u65b0\u6587\u7ae0", "Latest Articles"],
  ["\u7ee7\u7eed\u9605\u8bfb", "Read More"],
  ["\u52a0\u8f7d\u4e2d...", "Loading..."],
  ["\u52a0\u8f7d\u66f4\u591a", "Load More"],
  ["\u5df2\u7ecf\u5230\u5e95\u4e86", "No More"],
  ["\u6ca1\u6709\u5339\u914d\u7684\u6587\u7ae0", "No Matching Articles"],
  ["\u6362\u4e2a\u5173\u952e\u8bcd\uff0c\u6216\u8005\u6e05\u7a7a\u7b5b\u9009\u6761\u4ef6\u518d\u8bd5\u8bd5\u3002", "Try another keyword or clear the filters."],
  ["\u6bcf\u65e5\u53e5\u5b50", "Daily Quote"],
  ["\u67e5\u770b\u6765\u6e90", "View Source"],
  ["\u6700\u65b0\u6587\u7ae0\u63a8\u8350", "Latest Articles"],
  ["\u7f6e\u9876\u63a8\u8350", "Pinned"],
  ["\u641c\u7d22\u7ed3\u679c", "Search Results"],
  ["\u641c\u7d22\u5efa\u8bae", "Search Tips"],
  ["\u641c\u7d22\u5386\u53f2", "Search History"],
  ["\u70ed\u95e8\u641c\u7d22", "Popular Searches"],
  ["\u641c\u7d22\u5173\u952e\u8bcd", "Search Keyword"],
  ["\u8f93\u5165\u5173\u952e\u8bcd", "Enter keywords"],
  ["\u641c\u7d22\u6807\u9898\u3001\u6458\u8981\u6216\u6b63\u6587", "Search title, summary or content"],
  ["\u641c\u7d22\u539f\u6587\u4ef6\u540d\u6216\u5b58\u50a8\u6587\u4ef6\u540d", "Search original or stored filename"],
  ["\u91cd\u7f6e", "Reset"],
  ["\u7b5b\u9009", "Filter"],
  ["\u6392\u5e8f", "Sort"],
  ["\u6700\u65b0", "Latest"],
  ["\u9605\u8bfb", "Views"],
  ["\u70b9\u8d5e", "Likes"],
  ["\u8bc4\u8bba", "Comments"],
  ["\u4e13\u9898\u805a\u5408", "Topic Collections"],
  ["\u4ee3\u8868\u6587\u7ae0", "Featured Articles"],
  ["\u9605\u8bfb\u6307\u5357", "Reading Guide"],
  ["\u6309\u5206\u7c7b\u548c\u6807\u7b7e\u63a2\u7d22\u5185\u5bb9\u5730\u56fe", "Explore the content map by categories and tags"],
  ["\u65f6\u95f4\u5f52\u6863", "Timeline Archive"],
  ["\u6309\u65f6\u95f4\u7ebf\u56de\u770b\u6240\u6709\u6587\u7ae0", "Review all articles by timeline"],
  ["\u53cb\u94fe\u7533\u8bf7", "Link Application"],
  ["\u7533\u8bf7\u53cb\u94fe", "Apply for Link"],
  ["\u7f51\u7ad9\u540d\u79f0", "Site Name"],
  ["\u7f51\u7ad9\u5730\u5740", "Site URL"],
  ["\u7f51\u7ad9\u63cf\u8ff0", "Site Description"],
  ["Logo \u5730\u5740", "Logo URL"],
  ["\u63d0\u4ea4\u7533\u8bf7", "Submit"],
  ["\u5199\u7559\u8a00", "Write a Message"],
  ["\u6635\u79f0", "Nickname"],
  ["\u90ae\u7bb1", "Email"],
  ["\u7559\u8a00\u5185\u5bb9", "Message"],
  ["\u63d0\u4ea4\u7559\u8a00", "Submit Message"],
  ["\u7b49\u5f85\u7ba1\u7406\u5458\u5ba1\u6838\u540e\u5c55\u793a\u3002", "It will appear after admin approval."],
  ["\u4e2a\u4eba\u8d44\u6599", "Profile"],
  ["\u6211\u7684\u6536\u85cf", "My Favorites"],
  ["\u6211\u7684\u70b9\u8d5e", "My Likes"],
  ["\u6211\u7684\u8bc4\u8bba", "My Comments"],
  ["\u4fee\u6539\u8d44\u6599", "Edit Profile"],
  ["\u4fee\u6539\u5bc6\u7801", "Change Password"],
  ["\u4fdd\u5b58\u8d44\u6599", "Save Profile"],
  ["\u4fdd\u5b58\u5bc6\u7801", "Save Password"],
  ["\u67e5\u770b\u4e13\u9898", "View Topics"],
  ["\u5173\u4e8e\u672c\u7ad9", "About This Site"],
  ["\u6280\u672f\u6808", "Tech Stack"],
  ["\u9879\u76ee\u4eae\u70b9", "Highlights"],
  ["\u8054\u7cfb\u65b9\u5f0f", "Contact"],
  ["\u4e0a\u4e00\u7bc7", "Previous"],
  ["\u4e0b\u4e00\u7bc7", "Next"],
  ["\u76f8\u5173\u6587\u7ae0", "Related Articles"],
  ["\u6682\u65e0", "None"],
  ["\u672a\u5206\u7c7b", "Uncategorized"],
  ["\u7f6e\u9876", "Pinned"],
  ["\u53d1\u5e03\u4e8e", "Published"],
  ["\u9884\u8ba1\u9605\u8bfb", "Reading Time"],
  ["\u590d\u5236\u5730\u5740", "Copy URL"],
  ["\u53d1\u9001", "Send"],
  ["\u53d6\u6d88", "Cancel"],
  ["\u4fdd\u5b58", "Save"],
  ["\u7f16\u8f91", "Edit"],
  ["\u5220\u9664", "Delete"],
  ["\u767b\u5f55", "Sign in"],
  ["\u6ce8\u518c", "Register"],
  ["\u8d26\u53f7", "Username"],
  ["\u5bc6\u7801", "Password"],
  ["\u786e\u8ba4\u5bc6\u7801", "Confirm Password"],
  ["\u7acb\u5373\u767b\u5f55", "Sign in"],
  ["\u7acb\u5373\u6ce8\u518c", "Create Account"],
  ["\u8fd4\u56de\u9996\u9875", "Back Home"]
] as const;

const uiTextMap = new Map<string, string>(uiTextPairs);
const translatableAttrs: TranslatableAttr[] = ["placeholder", "title", "aria-label"];
// originalTextMap 保存文本节点的原始中文，切回中文时可以恢复，不会丢失原文。
const originalTextMap = new WeakMap<Text, string>();
const originalAttrMap = new WeakMap<Element, Partial<Record<TranslatableAttr, string>>>();
let observer: MutationObserver | null = null;
let applyTimer: number | undefined;
let applying = false;

// applyTheme 把主题状态写到 body 和 html 上，CSS 通过 data-front-theme 控制夜间样式。
const applyTheme = () => {
  document.body.dataset.frontTheme = theme.value;
  document.documentElement.dataset.frontTheme = theme.value;
};

// t 是模板中使用的翻译函数，根据当前 language 返回中文或英文。
const t = (key: I18nKey) => messages[language.value][key] || messages.zh[key] || key;

// toggleLanguage 在中文和英文之间切换。
const toggleLanguage = () => {
  language.value = language.value === "zh" ? "en" : "zh";
};

// toggleTheme 在日间和夜间主题之间切换。
const toggleTheme = () => {
  theme.value = theme.value === "light" ? "dark" : "light";
};

// shouldSkipElement 判断哪些元素不应该被自动翻译，例如代码块、输入框和 Markdown 正文。
const shouldSkipElement = (element: Element) => {
  const tag = element.tagName.toLowerCase();
  if (["script", "style", "textarea", "input", "code", "pre"].includes(tag)) return true;
  if (element.closest(".markdown-body, .front-header, .front-floating-prefs")) return true;
  return false;
};

const shouldSkipTextNode = (node: Text) => {
  const parent = node.parentElement;
  if (!parent) return true;
  return shouldSkipElement(parent);
};

// translateDynamicText 处理带数字的动态短句，例如“3 篇文章”“12 阅读”。
const translateDynamicText = (value: string) => {
  let next = value;
  next = next.replace(/\u5171\s*(\d+)\s*\u7bc7/g, "$1 posts");
  next = next.replace(/\u5df2\u52a0\u8f7d\s*(\d+)\s*\/\s*(\d+)\s*\u7bc7/g, "Loaded $1 / $2 posts");
  next = next.replace(/(\d+)\s*\u7bc7\u6587\u7ae0/g, "$1 posts");
  next = next.replace(/(\d+)\s*\u7bc7/g, "$1 posts");
  next = next.replace(/(\d+)\s*\u9605\u8bfb/g, "$1 views");
  next = next.replace(/(\d+)\s*\u70b9\u8d5e/g, "$1 likes");
  next = next.replace(/(\d+)\s*\u8bc4\u8bba/g, "$1 comments");
  next = next.replace(/(\d+)\s*\u5206\u949f/g, "$1 min");
  next = next.replace(/\u9605\u8bfb\s*(\d+)\s*\u6b21/g, "$1 views");
  next = next.replace(/\u70b9\u8d5e\s*(\d+)\s*\u6b21/g, "$1 likes");
  next = next.replace(/\u8bc4\u8bba\s*(\d+)\s*\u6761/g, "$1 comments");
  next = next.replace(/\u9884\u8ba1\u9605\u8bfb\s*(\d+)\s*\u5206\u949f/g, "Reading Time $1 min");
  next = next.replace(/(\d+)\s*\u4e2a\u5206\u7c7b/g, "$1 categories");
  next = next.replace(/(\d+)\s*\u4e2a\u6807\u7b7e/g, "$1 tags");
  return next;
};

// translateStaticText 先查固定翻译表，再尝试动态规则，最后保留无法识别的原文。
const translateStaticText = (value: string) => {
  const leading = value.match(/^\s*/)?.[0] ?? "";
  const trailing = value.match(/\s*$/)?.[0] ?? "";
  const trimmed = value.trim();
  if (!trimmed) return value;
  const mapped = uiTextMap.get(trimmed);
  if (mapped) return `${leading}${mapped}${trailing}`;
  const dynamic = translateDynamicText(trimmed);
  return dynamic === trimmed ? value : `${leading}${dynamic}${trailing}`;
};

// applyTextNodes 遍历前台页面中的文本节点，并根据当前语言替换展示文案。
const applyTextNodes = (root: Element) => {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  let node = walker.nextNode() as Text | null;
  while (node) {
    if (!shouldSkipTextNode(node)) {
      const original = originalTextMap.get(node) ?? node.nodeValue ?? "";
      if (!originalTextMap.has(node)) originalTextMap.set(node, original);
      node.nodeValue = language.value === "en" ? translateStaticText(original) : original;
    }
    node = walker.nextNode() as Text | null;
  }
};

// applyElementAttributes 翻译 placeholder、title、aria-label 等属性文案。
const applyElementAttributes = (root: Element) => {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT);
  let node = walker.nextNode() as Element | null;
  while (node) {
    if (!shouldSkipElement(node)) {
      const element = node;
      const originals = originalAttrMap.get(element) ?? {};
      translatableAttrs.forEach((attr) => {
        const current = element.getAttribute(attr);
        if (current === null || current === undefined) return;
        if (originals[attr] === undefined) originals[attr] = current;
        element.setAttribute(attr, language.value === "en" ? translateStaticText(originals[attr] ?? current) : originals[attr] ?? current);
      });
      if (Object.keys(originals).length > 0) originalAttrMap.set(element, originals);
    }
    node = walker.nextNode() as Element | null;
  }
};

// applyFrontLanguage 对整个前台页面执行一次语言刷新。
const applyFrontLanguage = () => {
  if (typeof document === "undefined") return;
  const root = document.querySelector(".front-page");
  if (!root) return;
  applying = true;
  applyTextNodes(root);
  applyElementAttributes(root);
  applying = false;
};

const scheduleFrontLanguageApply = () => {
  if (typeof window === "undefined") return;
  window.clearTimeout(applyTimer);
  applyTimer = window.setTimeout(applyFrontLanguage, 30);
};

// startFrontTranslator 启动 MutationObserver，页面异步加载新内容后也会自动补翻译。
const startFrontTranslator = () => {
  if (typeof document === "undefined") return;
  if (!observer) {
    observer = new MutationObserver(() => {
      if (!applying && language.value === "en") scheduleFrontLanguageApply();
    });
    observer.observe(document.body, { childList: true, subtree: true, characterData: true, attributes: true });
  }
  scheduleFrontLanguageApply();
};

// 监听语言变化：保存用户选择，并重新应用页面翻译。
watch(language, (value) => {
  localStorage.setItem("blog_front_language", value);
  scheduleFrontLanguageApply();
}, { immediate: true });
// 监听主题变化：保存用户选择，并重新应用主题样式。
watch(theme, (value) => {
  localStorage.setItem("blog_front_theme", value);
  applyTheme();
}, { immediate: true });

// 导出组合式函数，页面组件可以复用语言、主题和切换方法。
export const useFrontPreferences = () => ({
  language,
  theme,
  isEnglish: computed(() => language.value === "en"),
  isDark: computed(() => theme.value === "dark"),
  t,
  toggleLanguage,
  toggleTheme,
  applyTheme,
  applyFrontLanguage,
  startFrontTranslator
});