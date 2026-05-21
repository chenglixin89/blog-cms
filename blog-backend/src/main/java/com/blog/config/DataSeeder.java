package com.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Application-data seeder. Runs after Flyway has finished schema migrations.
 *
 * <p>This component is intentionally limited to operations that depend on
 * runtime configuration (such as the BCrypt-hashed admin password) or that
 * need conditional, soft-delete-aware logic (such as AI skill prompt updates).
 * Pure schema work and static reference data live in {@code db/migration}.</p>
 */
@Component
@Order(100) // Run after Flyway (default order) so all tables exist before we INSERT.
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String NOOP_PREFIX = "{noop}";

    private final JdbcTemplate jdbcTemplate;
    private final AdminAuthProperties adminAuthProperties;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public DataSeeder(JdbcTemplate jdbcTemplate, AdminAuthProperties adminAuthProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminAuthProperties = adminAuthProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdminAccount();
        rehashLegacyAdminPasswords();
        seedAiSkills();
        seedDemoArticle();
        backfillPublishedAt();
    }

    // -------------------------------------------------------------------
    // Admin account: BCrypt-hashed seed + one-time legacy {noop} rehash.
    // -------------------------------------------------------------------

    private void seedAdminAccount() {
        String username = defaultText(adminAuthProperties.getUsername(), "admin");
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_admin_account WHERE username = ?",
            Integer.class,
            username
        );
        if (count != null && count > 0) {
            return;
        }

        String configured = defaultText(adminAuthProperties.getPassword(), "");
        String rawPassword = stripNoopPrefix(configured);
        if (rawPassword.isBlank()) {
            log.warn("Skipping default admin seeding: blog.auth.admin.password is blank. " +
                "Set BLOG_ADMIN_PASSWORD or configure it in application-{profile}.yml.");
            return;
        }

        jdbcTemplate.update(
            "INSERT INTO blog_admin_account (username, password_hash, nickname) VALUES (?, ?, ?)",
            username,
            bCryptPasswordEncoder.encode(rawPassword),
            defaultText(adminAuthProperties.getNickname(), "Admin")
        );
        log.info("Seeded default admin account '{}' with a BCrypt-hashed password.", username);
    }

    private void rehashLegacyAdminPasswords() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, password_hash FROM blog_admin_account WHERE password_hash LIKE ?",
            NOOP_PREFIX + "%"
        );
        if (rows.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String legacyHash = (String) row.get("password_hash");
            String raw = stripNoopPrefix(legacyHash);
            if (raw.isBlank()) {
                continue;
            }
            jdbcTemplate.update(
                "UPDATE blog_admin_account SET password_hash = ?, updated_at = NOW() WHERE id = ?",
                bCryptPasswordEncoder.encode(raw),
                id
            );
        }
        log.info("Rehashed {} legacy '{}' admin password row(s) to BCrypt.", rows.size(), NOOP_PREFIX);
    }

    // -------------------------------------------------------------------
    // AI skills: upsert that preserves admin's soft-deletes.
    // -------------------------------------------------------------------

    private void seedAiSkills() {
        upsertAiSkill(
            "article_title",
            "Article title generator",
            "Generate candidate titles for a blog article.",
            "article",
            "You are a multilingual blog title editor. Detect the main language from the article title and content, then generate 5 natural titles in the same language. Requirements: no clickbait, concise and readable, return JSON only: {\"titles\":[\"title1\",\"title2\"]}. Article title: {{title}} Article content: {{content}}",
            10
        );
        upsertAiSkill(
            "article_summary",
            "Article summary generator",
            "Generate a short summary for a blog article.",
            "article",
            "You are a multilingual blog summary assistant. Detect the main language from the article title and content, then generate one summary in the same language. For Chinese, write 80 to 140 Chinese characters; for English, write 45 to 80 words. Do not start with phrases like article, this article, 闁哄牜鍓氶弸? or 閺夆晜鐟ч惁鎺楀棘閸モ晝褰? Return JSON only: {\"summary\":\"summary text\"}. Article title: {{title}} Article content: {{content}}",
            20
        );
        upsertAiSkill(
            "article_tags",
            "Article tag recommender",
            "Recommend tags from existing tags and optional new tags.",
            "article",
            "You are a multilingual blog tag recommendation assistant. Detect the main language from the article title, summary and content. Existing tags: {{existingTags}}. Recommend 3 to 6 tags for the article. When creating new tags, use the same language as the article. Prefer existing tags, and optionally add 1 to 2 new tags. Return JSON only: {\"existingTags\":[\"tag\"],\"newTags\":[\"tag\"]}. Article title: {{title}} Article summary: {{summary}} Article content: {{content}}",
            30
        );
        upsertAiSkill(
            "article_polish",
            "Article polish assistant",
            "Polish Markdown content without changing technical meaning.",
            "article",
            "You are a multilingual technical blog editor. Detect the main language from the article title and content, then polish the Markdown article in the same language. Keep Markdown structure and code blocks, do not change technical meaning, do not delete code. Return JSON only: {\"content\":\"polished markdown\"}. The content field must contain only the polished Markdown, not your analysis or task interpretation. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article content: {{content}}",
            40
        );
        upsertAiSkill(
            "article_outline",
            "Article outline generator",
            "Generate a Markdown outline before drafting.",
            "article",
            "You are a multilingual blog planning assistant. Detect the main language from the article title, summary and content, then generate a clear Markdown outline in the same language. Return JSON only: {\"content\":\"markdown outline\"}. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article summary: {{summary}} Article content: {{content}}",
            50
        );
        upsertAiSkill(
            "article_expand",
            "Article draft expander",
            "Expand a title or outline into a Markdown draft.",
            "article",
            "You are a multilingual technical blog writer. Detect the main language from the article title, summary and existing content or outline, then expand it into a complete Markdown draft in the same language. Write a detailed and useful article, not a short note. For Chinese drafts, normally write 1200 to 2500 Chinese characters unless the provided material is very small; for English drafts, normally write 700 to 1400 words. Use clear headings, paragraphs, transitions, examples, and a conclusion. Avoid unsupported facts. Return JSON only: {\"content\":\"markdown draft\"}. The content field must contain only the final Markdown article body, not your analysis or task interpretation. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article summary: {{summary}} Existing content or outline: {{content}}",
            60
        );
        upsertAiSkill(
            "article_continue",
            "Article continuation assistant",
            "Continue writing from the existing Markdown content.",
            "article",
            "You are a multilingual blog continuation assistant. Detect the main language from the article title, summary and existing content, then continue naturally in the same language. Output only the next section or next few complete sections, not the whole article. Do not make the continuation too short; include useful detail, transitions and examples when appropriate. Return JSON only: {\"content\":\"continued markdown\"}. The content field must contain only the continued Markdown, not your analysis or task interpretation. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article summary: {{summary}} Existing content: {{content}}",
            70
        );
        upsertAiSkill(
            "article_seo",
            "Article SEO generator",
            "Generate SEO title, description and keywords.",
            "article",
            "You are a multilingual SEO assistant for a personal blog CMS. Detect the main language from the article title, summary and content, then generate SEO metadata in the same language. Requirements: concise seoTitle, seoDescription suitable for search snippets, seoKeywords 3 to 8 short keywords. Return JSON only: {\"seoTitle\":\"title\",\"seoDescription\":\"description\",\"seoKeywords\":[\"keyword\"]}. Article title: {{title}} Article summary: {{summary}} Article content: {{content}}",
            80
        );
    }

    private void upsertAiSkill(String code, String name, String description, String scene, String promptTemplate, int sortOrder) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_ai_skill WHERE code = ?", Integer.class, code
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO blog_ai_skill (name, code, description, scene, prompt_template, enabled, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?)",
                name, code, description, scene, promptTemplate, 1, sortOrder
            );
        } else {
            // Only update non-deleted rows so admin's soft-deletes survive a redeploy.
            jdbcTemplate.update(
                "UPDATE blog_ai_skill SET name = ?, description = ?, scene = ?, prompt_template = ?, sort_order = ?, updated_at = NOW() " +
                    "WHERE code = ? AND is_deleted = 0",
                name, description, scene, promptTemplate, sortOrder, code
            );
        }
    }

    // -------------------------------------------------------------------
    // Demo data + back-fills.
    // -------------------------------------------------------------------

    private void seedDemoArticle() {
        Integer articleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_article WHERE id = 1001", Integer.class
        );
        if (articleCount == null || articleCount == 0) {
            jdbcTemplate.update(
                """
                INSERT INTO blog_article (id, title, summary, content, cover_image, category_id, status, published_at, is_pinned)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?)
                """,
                1001L,
                "Welcome to Blog CMS",
                "This is a demo article stored in MySQL.",
                "# Hello\n\nThis article is loaded from MySQL. You can edit and save it from the admin panel.",
                "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1400&q=80",
                1L,
                1,
                1
            );
        }
        insertArticleTagIfMissing(1001L, 1L);
        insertArticleTagIfMissing(1001L, 2L);
        insertDemoCommentIfMissing();
    }

    private void insertArticleTagIfMissing(Long articleId, Long tagId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_article_tag WHERE article_id = ? AND tag_id = ?",
            Integer.class, articleId, tagId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO blog_article_tag (article_id, tag_id) VALUES (?, ?)", articleId, tagId
            );
        }
    }

    private void insertDemoCommentIfMissing() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_comment WHERE id = 1001", Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                """
                INSERT INTO blog_comment (id, article_id, nickname, email, content, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                1001L, 1001L, "Reader", "reader@example.com", "This is a pending demo comment.", 0
            );
        }
    }

    private void backfillPublishedAt() {
        jdbcTemplate.update(
            "UPDATE blog_article SET published_at = COALESCE(published_at, created_at, updated_at, NOW()) " +
                "WHERE status = 1 AND published_at IS NULL"
        );
    }

    // -------------------------------------------------------------------
    // Helpers.
    // -------------------------------------------------------------------

    private static String stripNoopPrefix(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith(NOOP_PREFIX) ? value.substring(NOOP_PREFIX.length()) : value;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
