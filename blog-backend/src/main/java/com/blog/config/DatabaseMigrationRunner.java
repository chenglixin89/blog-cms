package com.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final AdminAuthProperties adminAuthProperties;
    private final SymmetricCipher symmetricCipher;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate, AdminAuthProperties adminAuthProperties, SymmetricCipher symmetricCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminAuthProperties = adminAuthProperties;
        this.symmetricCipher = symmetricCipher;
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("blog_article", "category_id", "ALTER TABLE blog_article ADD COLUMN category_id BIGINT NULL AFTER content");
        addColumnIfMissing("blog_article", "cover_image", "ALTER TABLE blog_article ADD COLUMN cover_image VARCHAR(500) NOT NULL DEFAULT '' AFTER content");
        addColumnIfMissing("blog_article", "seo_title", "ALTER TABLE blog_article ADD COLUMN seo_title VARCHAR(200) NOT NULL DEFAULT '' AFTER summary");
        addColumnIfMissing("blog_article", "seo_description", "ALTER TABLE blog_article ADD COLUMN seo_description VARCHAR(500) NOT NULL DEFAULT '' AFTER seo_title");
        addColumnIfMissing("blog_article", "seo_keywords", "ALTER TABLE blog_article ADD COLUMN seo_keywords VARCHAR(255) NOT NULL DEFAULT '' AFTER seo_description");
        addColumnIfMissing("blog_article", "published_at", "ALTER TABLE blog_article ADD COLUMN published_at DATETIME NULL AFTER status");
        addColumnIfMissing("blog_article", "is_pinned", "ALTER TABLE blog_article ADD COLUMN is_pinned TINYINT NOT NULL DEFAULT 0 AFTER published_at");
        addColumnIfMissing("blog_article", "view_count", "ALTER TABLE blog_article ADD COLUMN view_count INT NOT NULL DEFAULT 0 AFTER status");
        addColumnIfMissing("blog_article", "like_count", "ALTER TABLE blog_article ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER view_count");
        addColumnIfMissing("blog_comment", "parent_id", "ALTER TABLE blog_comment ADD COLUMN parent_id BIGINT NULL AFTER article_id");
        addColumnIfMissing("blog_site_setting", "daily_quote_enabled", "ALTER TABLE blog_site_setting ADD COLUMN daily_quote_enabled TINYINT NOT NULL DEFAULT 1 AFTER footer_text");
        addColumnIfMissing("blog_site_setting", "daily_quote_api_url", "ALTER TABLE blog_site_setting ADD COLUMN daily_quote_api_url VARCHAR(500) NOT NULL DEFAULT 'https://v1.hitokoto.cn/?encode=json&max_length=60' AFTER daily_quote_enabled");
        addColumnIfMissing("blog_site_setting", "roadmap_json", "ALTER TABLE blog_site_setting ADD COLUMN roadmap_json LONGTEXT NULL AFTER daily_quote_api_url");
        addColumnIfMissing("blog_user", "email", "ALTER TABLE blog_user ADD COLUMN email VARCHAR(120) NOT NULL DEFAULT '' AFTER nickname");
        addColumnIfMissing("blog_user", "avatar", "ALTER TABLE blog_user ADD COLUMN avatar VARCHAR(500) NOT NULL DEFAULT '' AFTER email");
        addColumnIfMissing("blog_user", "bio", "ALTER TABLE blog_user ADD COLUMN bio VARCHAR(500) NOT NULL DEFAULT '' AFTER avatar");
        addColumnIfMissing("blog_user", "last_login_at", "ALTER TABLE blog_user ADD COLUMN last_login_at DATETIME NULL AFTER status");
        addColumnIfMissing("blog_comment", "user_id", "ALTER TABLE blog_comment ADD COLUMN user_id BIGINT NULL AFTER parent_id");
        addColumnIfMissing("blog_guestbook_message", "user_id", "ALTER TABLE blog_guestbook_message ADD COLUMN user_id BIGINT NULL AFTER parent_id");
        createAdminAccountTable();
        seedAdminAccount();
        createMediaAssetTable();
        createAuditLogTable();
        createAiTables();
        addColumnIfMissing("blog_ai_call_log", "input_text", "ALTER TABLE blog_ai_call_log ADD COLUMN input_text LONGTEXT NULL AFTER skill_name");
        addColumnIfMissing("blog_ai_call_log", "output_text", "ALTER TABLE blog_ai_call_log ADD COLUMN output_text LONGTEXT NULL AFTER input_text");
        seedAiData();
        encryptLegacyAiApiKeys();
        backfillPublishedAt();
        insertDemoData();
        addArticleFulltextIndex();
    }

    private void createAdminAccountTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS blog_admin_account (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              username VARCHAR(60) NOT NULL,
              password_hash VARCHAR(255) NOT NULL,
              nickname VARCHAR(80) NOT NULL DEFAULT 'Admin',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uk_blog_admin_account_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void seedAdminAccount() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_admin_account WHERE username = ?",
            Integer.class,
            defaultText(adminAuthProperties.getUsername(), "admin")
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO blog_admin_account (username, password_hash, nickname) VALUES (?, ?, ?)",
                defaultText(adminAuthProperties.getUsername(), "admin"),
                defaultText(adminAuthProperties.getPassword(), "{noop}123456"),
                defaultText(adminAuthProperties.getNickname(), "Admin")
            );
        }
    }

    private void createMediaAssetTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS blog_media_asset (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              url VARCHAR(500) NOT NULL,
              original_name VARCHAR(255) NOT NULL DEFAULT '',
              file_name VARCHAR(255) NOT NULL DEFAULT '',
              extension VARCHAR(20) NOT NULL DEFAULT '',
              content_type VARCHAR(120) NOT NULL DEFAULT '',
              size BIGINT NOT NULL DEFAULT 0,
              category VARCHAR(40) NOT NULL DEFAULT 'general',
              is_deleted TINYINT NOT NULL DEFAULT 0,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              KEY idx_blog_media_asset_created (created_at),
              KEY idx_blog_media_asset_category (category)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void createAuditLogTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS blog_audit_log (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              operator VARCHAR(80) NOT NULL DEFAULT '',
              module VARCHAR(60) NOT NULL,
              action VARCHAR(60) NOT NULL,
              target_type VARCHAR(60) DEFAULT '',
              target_id BIGINT NULL,
              detail VARCHAR(1000) DEFAULT '',
              ip VARCHAR(64) DEFAULT '',
              user_agent VARCHAR(500) DEFAULT '',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              KEY idx_blog_audit_log_created (created_at),
              KEY idx_blog_audit_log_module_action (module, action)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }


    private void createAiTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS blog_ai_provider (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              name VARCHAR(80) NOT NULL,
              base_url VARCHAR(500) NOT NULL,
              api_key VARCHAR(1000) NOT NULL DEFAULT '',
              model VARCHAR(120) NOT NULL,
              temperature DECIMAL(4,2) NOT NULL DEFAULT 0.40,
              max_tokens INT NOT NULL DEFAULT 4000,
              enabled TINYINT NOT NULL DEFAULT 1,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS blog_ai_skill (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              name VARCHAR(100) NOT NULL,
              code VARCHAR(80) NOT NULL,
              description VARCHAR(500) NOT NULL DEFAULT '',
              scene VARCHAR(60) NOT NULL DEFAULT 'article',
              prompt_template LONGTEXT NOT NULL,
              enabled TINYINT NOT NULL DEFAULT 1,
              sort_order INT NOT NULL DEFAULT 100,
              is_deleted TINYINT NOT NULL DEFAULT 0,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uk_blog_ai_skill_code (code),
              KEY idx_blog_ai_skill_scene (scene)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS blog_ai_call_log (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              provider_name VARCHAR(80) NOT NULL DEFAULT '',
              model VARCHAR(120) NOT NULL DEFAULT '',
              skill_code VARCHAR(80) NOT NULL DEFAULT '',
              skill_name VARCHAR(100) NOT NULL DEFAULT '',
              input_text LONGTEXT NULL,
              output_text LONGTEXT NULL,
              input_preview VARCHAR(1200) NOT NULL DEFAULT '',
              output_preview VARCHAR(1200) NOT NULL DEFAULT '',
              success TINYINT NOT NULL DEFAULT 0,
              error_message VARCHAR(1000) NOT NULL DEFAULT '',
              elapsed_ms BIGINT NOT NULL DEFAULT 0,
              prompt_tokens INT NULL,
              completion_tokens INT NULL,
              total_tokens INT NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              KEY idx_blog_ai_call_log_created (created_at),
              KEY idx_blog_ai_call_log_skill (skill_code),
              KEY idx_blog_ai_call_log_success (success)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void seedAiData() {
        Integer providerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blog_ai_provider", Integer.class);
        if (providerCount == null || providerCount == 0) {
            jdbcTemplate.update(
                "INSERT INTO blog_ai_provider (name, base_url, api_key, model, temperature, max_tokens, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "Xiaomi MiMo Token Plan",
                "https://token-plan-cn.xiaomimimo.com/v1",
                "",
                "mimo-v2.5-pro",
                0.4,
                4000,
                1
            );
        }

        seedAiSkill(
            "article_title",
            "Article title generator",
            "Generate candidate titles for a blog article.",
            "article",
            "You are a multilingual blog title editor. Detect the main language from the article title and content, then generate 5 natural titles in the same language. Requirements: no clickbait, concise and readable, return JSON only: {\"titles\":[\"title1\",\"title2\"]}. Article title: {{title}} Article content: {{content}}",
            10
        );
        seedAiSkill(
            "article_summary",
            "Article summary generator",
            "Generate a short summary for a blog article.",
            "article",
            "You are a multilingual blog summary assistant. Detect the main language from the article title and content, then generate one summary in the same language. For Chinese, write 80 to 140 Chinese characters; for English, write 45 to 80 words. Do not start with phrases like article, this article, 闁哄牜鍓氶弸? or 閺夆晜鐟ч惁鎺楀棘閸モ晝褰? Return JSON only: {\"summary\":\"summary text\"}. Article title: {{title}} Article content: {{content}}",
            20
        );
        seedAiSkill(
            "article_tags",
            "Article tag recommender",
            "Recommend tags from existing tags and optional new tags.",
            "article",
            "You are a multilingual blog tag recommendation assistant. Detect the main language from the article title, summary and content. Existing tags: {{existingTags}}. Recommend 3 to 6 tags for the article. When creating new tags, use the same language as the article. Prefer existing tags, and optionally add 1 to 2 new tags. Return JSON only: {\"existingTags\":[\"tag\"],\"newTags\":[\"tag\"]}. Article title: {{title}} Article summary: {{summary}} Article content: {{content}}",
            30
        );
        seedAiSkill(
            "article_outline",
            "Article outline generator",
            "Generate a Markdown outline before drafting.",
            "article",
            "You are a multilingual blog planning assistant. Detect the main language from the article title, summary and content, then generate a clear Markdown outline in the same language. Return JSON only: {\"content\":\"markdown outline\"}. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article summary: {{summary}} Article content: {{content}}",
            50
        );
        seedAiSkill(
            "article_expand",
            "Article draft expander",
            "Expand a title or outline into a Markdown draft.",
            "article",
            "You are a multilingual technical blog writer. Detect the main language from the article title, summary and existing content or outline, then expand it into a complete Markdown draft in the same language. Write a detailed and useful article, not a short note. For Chinese drafts, normally write 1200 to 2500 Chinese characters unless the provided material is very small; for English drafts, normally write 700 to 1400 words. Use clear headings, paragraphs, transitions, examples, and a conclusion. Avoid unsupported facts. Return JSON only: {\"content\":\"markdown draft\"}. The content field must contain only the final Markdown article body, not your analysis or task interpretation. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article summary: {{summary}} Existing content or outline: {{content}}",
            60
        );
        seedAiSkill(
            "article_continue",
            "Article continuation assistant",
            "Continue writing from the existing Markdown content.",
            "article",
            "You are a multilingual blog continuation assistant. Detect the main language from the article title, summary and existing content, then continue naturally in the same language. Output only the next section or next few complete sections, not the whole article. Do not make the continuation too short; include useful detail, transitions and examples when appropriate. Return JSON only: {\"content\":\"continued markdown\"}. The content field must contain only the continued Markdown, not your analysis or task interpretation. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article summary: {{summary}} Existing content: {{content}}",
            70
        );
        seedAiSkill(
            "article_seo",
            "Article SEO generator",
            "Generate SEO title, description and keywords.",
            "article",
            "You are a multilingual SEO assistant for a personal blog CMS. Detect the main language from the article title, summary and content, then generate SEO metadata in the same language. Requirements: concise seoTitle, seoDescription suitable for search snippets, seoKeywords 3 to 8 short keywords. Return JSON only: {\"seoTitle\":\"title\",\"seoDescription\":\"description\",\"seoKeywords\":[\"keyword\"]}. Article title: {{title}} Article summary: {{summary}} Article content: {{content}}",
            80
        );
        seedAiSkill(
            "article_polish",
            "Article polish assistant",
            "Polish Markdown content without changing technical meaning.",
            "article",
            "You are a multilingual technical blog editor. Detect the main language from the article title and content, then polish the Markdown article in the same language. Keep Markdown structure and code blocks, do not change technical meaning, do not delete code. Return JSON only: {\"content\":\"polished markdown\"}. The content field must contain only the polished Markdown, not your analysis or task interpretation. Return exactly one strict JSON object. Do not include Markdown fences, analysis, reasoning, explanations, or restated requirements. The JSON string values may contain Markdown, but the response itself must be JSON only. Article title: {{title}} Article content: {{content}}",
            40
        );
    }

    private void seedAiSkill(String code, String name, String description, String scene, String promptTemplate, int sortOrder) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blog_ai_skill WHERE code = ?", Integer.class, code);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO blog_ai_skill (name, code, description, scene, prompt_template, enabled, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?)",
                name,
                code,
                description,
                scene,
                promptTemplate,
                1,
                sortOrder
            );
        } else {
            jdbcTemplate.update(
                "UPDATE blog_ai_skill SET name = ?, description = ?, scene = ?, prompt_template = ?, sort_order = ?, updated_at = NOW() WHERE code = ? AND is_deleted = 0",
                name,
                description,
                scene,
                promptTemplate,
                sortOrder,
                code
            );
        }
    }

    /**
     * One-shot migration that re-encrypts any AI provider rows whose
     * {@code api_key} column still holds raw plaintext (i.e. predates the
     * AES-GCM encryption introduced in this PR). After this runs once on a
     * given database every row is in {@code "enc1:..."} format and the loop
     * below becomes a no-op on subsequent boots.
     */
    private void encryptLegacyAiApiKeys() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, api_key FROM blog_ai_provider WHERE api_key IS NOT NULL AND api_key <> '' AND api_key NOT LIKE ?",
            SymmetricCipher.VERSION_PREFIX + "%"
        );
        if (rows.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String plaintext = (String) row.get("api_key");
            jdbcTemplate.update(
                "UPDATE blog_ai_provider SET api_key = ?, updated_at = NOW() WHERE id = ?",
                symmetricCipher.encrypt(plaintext),
                id
            );
        }
        log.info("Encrypted {} legacy plaintext blog_ai_provider.api_key row(s) to AES-GCM.", rows.size());
    }

    private void backfillPublishedAt() {
        jdbcTemplate.update(
            "UPDATE blog_article SET published_at = COALESCE(published_at, created_at, updated_at, NOW()) WHERE status = 1 AND published_at IS NULL"
        );
    }

    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """,
            Integer.class,
            tableName,
            columnName
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private void insertDemoData() {
        Integer articleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_article WHERE id = 1001",
            Integer.class
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
            Integer.class,
            articleId,
            tagId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO blog_article_tag (article_id, tag_id) VALUES (?, ?)",
                articleId,
                tagId
            );
        }
    }

    private void insertDemoCommentIfMissing() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_comment WHERE id = 1001",
            Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                """
                INSERT INTO blog_comment (id, article_id, nickname, email, content, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                1001L,
                1001L,
                "Reader",
                "reader@example.com",
                "This is a pending demo comment.",
                0
            );
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Adds a FULLTEXT index on {@code blog_article (title, summary, content)} so the keyword
     * search path can switch from {@code LIKE '%k%'} (which scans every row of the LONGTEXT
     * column) to {@code MATCH ... AGAINST}.
     *
     * <p>The index is built {@code WITH PARSER ngram} so it works for Chinese as well as
     * English. Server-wide tokenisation size lives in MySQL's {@code ngram_token_size}
     * variable; with the default of 2, queries shorter than 2 characters cannot be indexed
     * and the mapper falls back to LIKE for them. See ArticleMapper for that fallback.</p>
     *
     * <p>Idempotent via {@code information_schema.statistics}; safe to call on every boot.</p>
     */
    private void addArticleFulltextIndex() {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'blog_article'
              AND index_name = 'ft_blog_article_search'
            """,
            Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(
                "CREATE FULLTEXT INDEX ft_blog_article_search ON blog_article (title, summary, content) WITH PARSER ngram"
            );
        }
    }
}
