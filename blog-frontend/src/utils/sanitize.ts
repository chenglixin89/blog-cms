// Tiny sanitisation helpers shared between the front-end render paths that
// still have to use `v-html` (article body Markdown, search-result keyword
// highlight). Keeping them in one place means we audit one function instead
// of every component.

/** HTML-escape every character that has special meaning in HTML or attribute context. */
export const escapeHtml = (value: string): string =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

/**
 * Whitelist of URL schemes that we allow inside Markdown links and images.
 *
 * We accept three forms only:
 *   - `http://...` / `https://...`     (absolute URLs)
 *   - `/path` and `/path?...`         (same-origin paths, **but not** `//host`)
 *   - `#anchor`                        (in-page anchors)
 *
 * Protocol-relative URLs (`//evil.com/...`) are rejected on purpose: they
 * silently resolve to a different host, which is a phishing / open-redirect
 * vector even if the rest of the renderer is safe. Anything that does not
 * match the allow-list returns an empty string so the caller can fall back to
 * displaying the raw text.
 */
export const safeUrl = (value: string): string => {
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (trimmed.startsWith("//")) return "";
  if (/^(https?:\/\/|\/|#)/i.test(trimmed)) {
    return escapeHtml(trimmed);
  }
  return "";
};

/**
 * Escapes the given text and then wraps every (case-insensitive) occurrence
 * of `keyword` in a `<mark>` tag. The keyword itself is HTML-escaped before
 * it is used in the regex so that titles containing HTML special characters
 * still highlight correctly when the user searches for them literally.
 *
 * Because the input is escaped before any markup is added, an article whose
 * title or summary contains raw HTML (e.g. `<script>...`) cannot break out
 * of the rendered text node. This is the function callers should use any
 * time they would otherwise reach for `v-html` plus a regex replace.
 */
export const highlightKeyword = (text: string, keyword: string): string => {
  const safeText = escapeHtml(text);
  const trimmedKey = keyword.trim();
  if (!trimmedKey) {
    return safeText;
  }
  const safeKey = escapeHtml(trimmedKey).replace(/[\\^$.*+?()[\]{}|]/g, "\\$&");
  if (!safeKey) {
    return safeText;
  }
  return safeText.replace(new RegExp("(" + safeKey + ")", "gi"), "<mark>$1</mark>");
};
