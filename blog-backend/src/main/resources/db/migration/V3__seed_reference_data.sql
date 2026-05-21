-- =====================================================================
-- V3 seeds the static reference data the frontend expects on first boot.
-- Every INSERT is guarded by `WHERE NOT EXISTS`, so running this against
-- a database that was previously populated by the legacy seeders is a no-op.
-- =====================================================================

INSERT INTO blog_category (id, name, slug, description, sort_order)
SELECT 1, 'Technology', 'technology', 'Technical notes and engineering articles.', 10
WHERE NOT EXISTS (SELECT 1 FROM blog_category WHERE id = 1);

INSERT INTO blog_tag (id, name, color)
SELECT 1, 'Vue3', '#34c759'
WHERE NOT EXISTS (SELECT 1 FROM blog_tag WHERE id = 1);

INSERT INTO blog_tag (id, name, color)
SELECT 2, 'Spring Boot', '#0a84ff'
WHERE NOT EXISTS (SELECT 1 FROM blog_tag WHERE id = 2);

INSERT INTO blog_friend_link (id, name, url, description, logo, status, sort_order)
SELECT 1001, 'Vue', 'https://vuejs.org/', 'The Progressive JavaScript Framework.', '', 1, 20
WHERE NOT EXISTS (SELECT 1 FROM blog_friend_link WHERE id = 1001);

INSERT INTO blog_friend_link (id, name, url, description, logo, status, sort_order)
SELECT 1002, 'Spring', 'https://spring.io/', 'Java application framework and ecosystem.', '', 1, 10
WHERE NOT EXISTS (SELECT 1 FROM blog_friend_link WHERE id = 1002);

INSERT INTO blog_site_setting (
  id, site_name, site_subtitle, logo, avatar, hero_title, hero_subtitle,
  author_name, author_bio, footer_text, daily_quote_enabled, daily_quote_api_url
)
SELECT
  1,
  'Personal Blog',
  'Personal Blog CMS',
  '',
  '',
  'Notes on tech, projects and steady growth.',
  'Front page for article search, categories, tags, archives, reading stats, likes and comments.',
  'Admin',
  'Documenting daily exploration with code and writing.',
  'Powered by Blog CMS',
  1,
  'https://v1.hitokoto.cn/?encode=json&max_length=60'
WHERE NOT EXISTS (SELECT 1 FROM blog_site_setting WHERE id = 1);

INSERT INTO blog_guestbook_message (id, parent_id, nickname, email, content, status)
SELECT 1001, NULL, 'Reader', 'reader@example.com', 'Guestbook is ready. Leave your thoughts here.', 1
WHERE NOT EXISTS (SELECT 1 FROM blog_guestbook_message WHERE id = 1001);

INSERT INTO blog_ai_provider (name, base_url, api_key, model, temperature, max_tokens, enabled)
SELECT 'Xiaomi MiMo Token Plan', 'https://token-plan-cn.xiaomimimo.com/v1', '', 'mimo-v2.5-pro', 0.4, 4000, 1
WHERE NOT EXISTS (SELECT 1 FROM blog_ai_provider);
