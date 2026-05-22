package com.blog.service;

import com.blog.dto.AiCallLogDetailResponse;
import com.blog.dto.AiCallLogResponse;
import com.blog.dto.AiProviderRequest;
import com.blog.dto.AiProviderResponse;
import com.blog.dto.AiSkillRequest;
import com.blog.dto.AiSkillResponse;
import com.blog.dto.AiSkillRunRequest;
import com.blog.dto.AiSkillRunResponse;
import com.blog.dto.PageResponse;
import com.blog.utils.SecretRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final int INPUT_LIMIT = 20000;
    private static final int PREVIEW_LIMIT = 1000;
    private static final String OUTPUT_RULES = "\n\nOutput rules (must follow): Return the final result only. Do not explain your reasoning. Do not restate the user request. Do not include phrases like 'first', 'the user asks', 'I will', 'output must be'. Return one strict JSON object only, without Markdown code fences. Detect the main language from the article title, summary and content; keep the output in that same language. For article writing skills, the content value must contain only the final Markdown article body in the detected language. Do not intentionally shorten article drafts; when the task is expansion or polishing, provide enough detail, examples, transitions and complete sections.";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public AiService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    public AiProviderResponse getProvider() {
        return toProviderResponse(requireProvider());
    }

    @Transactional
    public AiProviderResponse updateProvider(AiProviderRequest request, HttpServletRequest servletRequest) {
        AiProvider provider = requireProvider();
        String apiKey = request.getApiKey() == null || request.getApiKey().isBlank()
            ? provider.apiKey()
            : request.getApiKey().trim();
        jdbcTemplate.update(
            """
            UPDATE blog_ai_provider
            SET name = ?, base_url = ?, api_key = ?, model = ?, temperature = ?, max_tokens = ?, enabled = ?, updated_at = NOW()
            WHERE id = ?
            """,
            normalizeText(request.getName(), "Xiaomi MiMo"),
            normalizeBaseUrl(request.getBaseUrl()),
            apiKey,
            normalizeText(request.getModel(), "mimo-v2.5-pro"),
            normalizeTemperature(request.getTemperature()),
            normalizeMaxTokens(request.getMaxTokens()),
            normalizeEnabled(request.getEnabled()),
            provider.id()
        );
        auditLogService.record(servletRequest, "AI", "Update provider", "AI_PROVIDER", provider.id(), "Update AI provider config");
        return getProvider();
    }

    public AiSkillRunResponse testProvider(HttpServletRequest servletRequest) {
        AiProvider provider = requireUsableProvider();
        long started = System.currentTimeMillis();
        String output = "";
        String error = "";
        TokenUsage usage = new TokenUsage(null, null, null);
        try {
            AiCallResult result = callModel(provider, "Please reply exactly: CONNECTED", 80, false);
            output = result.content();
            usage = result.usage();
            return new AiSkillRunResponse("provider_test", output, List.of(), "", List.of(), List.of(), output, "", "", List.of(), System.currentTimeMillis() - started);
        } catch (Exception ex) {
            error = cleanError(ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, error);
        } finally {
            recordLog(provider, "provider_test", "Provider test", "Please reply exactly: CONNECTED", output, error, System.currentTimeMillis() - started, usage);
            auditLogService.record(servletRequest, "AI", "Test provider", "AI_PROVIDER", provider.id(), error.isBlank() ? "AI provider test success" : "AI provider test failed: " + error);
        }
    }

    public List<AiSkillResponse> listSkills() {
        return jdbcTemplate.query(
            """
            SELECT id, name, code, description, scene, prompt_template, enabled, sort_order, updated_at
            FROM blog_ai_skill
            WHERE is_deleted = 0
            ORDER BY sort_order ASC, id ASC
            """,
            (rs, rowNum) -> new AiSkillResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("code"),
                rs.getString("description"),
                rs.getString("scene"),
                rs.getString("prompt_template"),
                rs.getInt("enabled"),
                rs.getInt("sort_order"),
                rs.getObject("updated_at", LocalDateTime.class)
            )
        );
    }

    @Transactional
    public AiSkillResponse createSkill(AiSkillRequest request, HttpServletRequest servletRequest) {
        jdbcTemplate.update(
            """
            INSERT INTO blog_ai_skill (name, code, description, scene, prompt_template, enabled, sort_order)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            normalizeText(request.getName(), "AI Skill"),
            normalizeCode(request.getCode()),
            defaultText(request.getDescription()),
            normalizeText(request.getScene(), "article"),
            defaultText(request.getPromptTemplate()),
            normalizeEnabled(request.getEnabled()),
            request.getSortOrder() == null ? 100 : request.getSortOrder()
        );
        AiSkillResponse skill = findSkillByCode(normalizeCode(request.getCode()));
        auditLogService.record(servletRequest, "AI", "Create skill", "AI_SKILL", skill.id(), "Create AI skill: " + skill.code());
        return skill;
    }

    @Transactional
    public AiSkillResponse updateSkill(Long id, AiSkillRequest request, HttpServletRequest servletRequest) {
        AiSkillResponse existing = findSkillById(id);
        jdbcTemplate.update(
            """
            UPDATE blog_ai_skill
            SET name = ?, code = ?, description = ?, scene = ?, prompt_template = ?, enabled = ?, sort_order = ?, updated_at = NOW()
            WHERE id = ? AND is_deleted = 0
            """,
            normalizeText(request.getName(), existing.name()),
            normalizeCode(request.getCode()),
            defaultText(request.getDescription()),
            normalizeText(request.getScene(), "article"),
            defaultText(request.getPromptTemplate()),
            normalizeEnabled(request.getEnabled()),
            request.getSortOrder() == null ? existing.sortOrder() : request.getSortOrder(),
            id
        );
        AiSkillResponse skill = findSkillById(id);
        auditLogService.record(servletRequest, "AI", "Update skill", "AI_SKILL", id, "Update AI skill: " + skill.code());
        return skill;
    }

    @Transactional
    public void deleteSkill(Long id, HttpServletRequest servletRequest) {
        findSkillById(id);
        jdbcTemplate.update("UPDATE blog_ai_skill SET is_deleted = 1, updated_at = NOW() WHERE id = ?", id);
        auditLogService.record(servletRequest, "AI", "Delete skill", "AI_SKILL", id, "Delete AI skill: " + id);
    }

    public AiSkillRunResponse runSkill(String code, AiSkillRunRequest request, HttpServletRequest servletRequest) {
        AiProvider provider = requireUsableProvider();
        AiSkillResponse skill = findSkillByCode(code);
        if (skill.enabled() == null || skill.enabled() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI skill is disabled");
        }
        String prompt = renderPrompt(skill.promptTemplate(), request);
        int promptLimit = Math.max(1000, INPUT_LIMIT - OUTPUT_RULES.length());
        if (prompt.length() > promptLimit) {
            prompt = prompt.substring(0, promptLimit);
        }
        prompt = prompt + OUTPUT_RULES;

        long started = System.currentTimeMillis();
        String output = "";
        String error = "";
        TokenUsage usage = new TokenUsage(null, null, null);
        try {
            AiCallResult result = callModel(provider, prompt, resolveSkillMaxTokens(provider, skill.code()), true);
            output = result.content();
            usage = result.usage();
            AiSkillRunResponse response = parseSkillOutput(skill.code(), output, System.currentTimeMillis() - started);
            auditLogService.record(servletRequest, "AI", "Run skill", "AI_SKILL", skill.id(), "Run AI skill: " + skill.code());
            return response;
        } catch (Exception ex) {
            error = cleanError(ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, error);
        } finally {
            recordLog(provider, skill.code(), skill.name(), prompt, output, error, System.currentTimeMillis() - started, usage);
        }
    }

    public PageResponse<AiCallLogResponse> pageLogs(String keyword, Integer success, Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedSize = Math.min(size == null || size < 1 ? 10 : size, 100);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = defaultText(keyword);
        Integer normalizedSuccess = success == null || success < 0 || success > 1 ? null : success;
        Long totalValue = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM blog_ai_call_log
            WHERE (? = '' OR provider_name LIKE CONCAT('%', ?, '%') OR skill_code LIKE CONCAT('%', ?, '%') OR skill_name LIKE CONCAT('%', ?, '%') OR error_message LIKE CONCAT('%', ?, '%'))
              AND (? IS NULL OR success = ?)
            """,
            Long.class,
            normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword,
            normalizedSuccess, normalizedSuccess
        );
        long total = totalValue == null ? 0 : totalValue;
        List<AiCallLogResponse> records = jdbcTemplate.query(
            """
            SELECT id, provider_name, model, skill_code, skill_name, input_preview, output_preview, success, error_message,
                   elapsed_ms, prompt_tokens, completion_tokens, total_tokens, created_at
            FROM blog_ai_call_log
            WHERE (? = '' OR provider_name LIKE CONCAT('%', ?, '%') OR skill_code LIKE CONCAT('%', ?, '%') OR skill_name LIKE CONCAT('%', ?, '%') OR error_message LIKE CONCAT('%', ?, '%'))
              AND (? IS NULL OR success = ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new AiCallLogResponse(
                rs.getLong("id"),
                rs.getString("provider_name"),
                rs.getString("model"),
                rs.getString("skill_code"),
                rs.getString("skill_name"),
                rs.getString("input_preview"),
                rs.getString("output_preview"),
                rs.getInt("success"),
                rs.getString("error_message"),
                rs.getLong("elapsed_ms"),
                (Integer) rs.getObject("prompt_tokens"),
                (Integer) rs.getObject("completion_tokens"),
                (Integer) rs.getObject("total_tokens"),
                rs.getObject("created_at", LocalDateTime.class)
            ),
            normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword,
            normalizedSuccess, normalizedSuccess,
            normalizedSize, offset
        );
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    public AiCallLogDetailResponse detailLog(Long id) {
        try {
            return jdbcTemplate.queryForObject(
                """
                SELECT id, provider_name, model, skill_code, skill_name, input_preview, output_preview,
                       COALESCE(input_text, input_preview) AS input_text,
                       COALESCE(output_text, output_preview) AS output_text,
                       success, error_message, elapsed_ms, prompt_tokens, completion_tokens, total_tokens, created_at
                FROM blog_ai_call_log
                WHERE id = ?
                """,
                (rs, rowNum) -> new AiCallLogDetailResponse(
                    rs.getLong("id"),
                    rs.getString("provider_name"),
                    rs.getString("model"),
                    rs.getString("skill_code"),
                    rs.getString("skill_name"),
                    rs.getString("input_preview"),
                    rs.getString("output_preview"),
                    rs.getString("input_text"),
                    rs.getString("output_text"),
                    rs.getInt("success"),
                    rs.getString("error_message"),
                    rs.getLong("elapsed_ms"),
                    (Integer) rs.getObject("prompt_tokens"),
                    (Integer) rs.getObject("completion_tokens"),
                    (Integer) rs.getObject("total_tokens"),
                    rs.getObject("created_at", LocalDateTime.class)
                ),
                id
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI log not found");
        }
    }

    private AiCallResult callModel(AiProvider provider, String prompt, int maxTokens, boolean requireJson) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", provider.model());
        payload.put("temperature", provider.temperature());
        payload.put("max_tokens", maxTokens);
        payload.put("messages", List.of(
            Map.of("role", "system", "content", requireJson
                ? "You are a strict JSON API for a blog CMS. Return only one valid JSON object. Never output reasoning, analysis, planning, explanations, markdown fences, or restated instructions."
                : "You are a helpful assistant inside a personal blog CMS. Follow the user request exactly."),
            Map.of("role", "user", "content", prompt)
        ));
        if (requireJson) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        HttpResponse<String> response = sendAiRequest(provider, payload);
        if ((response.statusCode() < 200 || response.statusCode() >= 300) && requireJson && response.body().toLowerCase().contains("response_format")) {
            payload.remove("response_format");
            response = sendAiRequest(provider, payload);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // Some providers echo the Authorization header back on a 4xx page; redact before the
            // body becomes part of an exception message that flows into logs and the HTTP reply.
            throw new IllegalStateException("AI provider returned HTTP " + response.statusCode() + ": "
                + truncate(SecretRedactor.redact(response.body(), provider.apiKey()), 500));
        }
        JsonNode root = objectMapper.readTree(response.body());
        String content = extractAssistantContent(root).trim();
        if (content.isBlank()) {
            String finishReason = root.path("choices").path(0).path("finish_reason").asText("unknown");
            throw new IllegalStateException("AI provider returned empty content (finish_reason=" + finishReason
                + "). Increase Max Tokens or shorten the article content. Raw: "
                + truncate(SecretRedactor.redact(response.body(), provider.apiKey()), 500));
        }
        JsonNode usage = root.path("usage");
        return new AiCallResult(content, new TokenUsage(
            usage.path("prompt_tokens").isMissingNode() ? null : usage.path("prompt_tokens").asInt(),
            usage.path("completion_tokens").isMissingNode() ? null : usage.path("completion_tokens").asInt(),
            usage.path("total_tokens").isMissingNode() ? null : usage.path("total_tokens").asInt()
        ));
    }

    private HttpResponse<String> sendAiRequest(AiProvider provider, Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(provider.baseUrl() + "/chat/completions"))
            .timeout(Duration.ofSeconds(120))
            .header("Authorization", "Bearer " + provider.apiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private int resolveSkillMaxTokens(AiProvider provider, String code) {
        int configured = provider.maxTokens() == null ? 4000 : provider.maxTokens();
        if ("article_expand".equals(code)) return Math.max(configured, 8000);
        if ("article_polish".equals(code)) return Math.max(configured, 7000);
        if ("article_continue".equals(code)) return Math.max(configured, 4000);
        if ("article_outline".equals(code)) return Math.max(configured, 2500);
        if ("article_summary".equals(code)) return Math.max(configured, 1500);
        if ("article_seo".equals(code)) return Math.max(configured, 1500);
        return Math.max(configured, 2000);
    }

    private String extractAssistantContent(JsonNode root) {
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        String content = textFromNode(message.path("content"));
        if (content.isBlank()) content = textFromNode(choice.path("text"));
        if (content.isBlank()) content = textFromNode(message.path("reasoning_content"));
        return content;
    }

    private String textFromNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        if (node.isTextual()) return node.asText("");
        if (node.isArray()) {
            StringBuilder builder = new StringBuilder();
            node.forEach(item -> {
                if (item.isTextual()) builder.append(item.asText(""));
                else if (item.has("text")) builder.append(item.path("text").asText(""));
                else if (item.has("content")) builder.append(item.path("content").asText(""));
            });
            return builder.toString();
        }
        if (node.isObject()) {
            if (node.has("text")) return node.path("text").asText("");
            if (node.has("content")) return node.path("content").asText("");
        }
        return "";
    }
    private AiSkillRunResponse parseSkillOutput(String code, String rawText, long elapsedMs) {
        List<String> titles = new ArrayList<>();
        String summary = "";
        List<String> existingTags = new ArrayList<>();
        List<String> newTags = new ArrayList<>();
        String content = "";
        String seoTitle = "";
        String seoDescription = "";
        List<String> seoKeywords = new ArrayList<>();
        String cleanedText = cleanAiText(rawText);
        JsonNode root = parseJsonOutput(rawText);
        if (root != null) {
            titles = stringArray(root.path("titles"));
            summary = extractFinalAnswer(jsonText(root, "summary"));
            existingTags = stringArray(root.path("existingTags"));
            newTags = stringArray(root.path("newTags"));
            content = extractFinalAnswer(jsonText(root, "content"));
            seoTitle = extractFinalAnswer(jsonText(root, "seoTitle"));
            seoDescription = extractFinalAnswer(jsonText(root, "seoDescription"));
            seoKeywords = stringArray(root.path("seoKeywords"));
        } else {
            // Some providers return plain Markdown/text even when JSON is requested.
            // Keep article-writing skills usable instead of failing the whole request.
            if (isArticleContentSkill(code)) {
                content = extractFinalAnswer(cleanedText);
            } else if ("article_title".equals(code)) {
                titles = fallbackLines(extractFinalAnswer(cleanedText)).stream().limit(5).toList();
            } else if ("article_summary".equals(code)) {
                summary = extractFinalAnswer(cleanedText);
            } else if ("article_tags".equals(code)) {
                newTags = fallbackLines(extractFinalAnswer(cleanedText)).stream().limit(8).toList();
            } else {
                throw new IllegalStateException("AI provider did not return valid JSON. Please retry the generation.");
            }
        }
        validateSkillOutput(code, titles, summary, existingTags, newTags, content, seoTitle, seoDescription, seoKeywords, cleanedText);
        return new AiSkillRunResponse(code, rawText, titles, summary, existingTags, newTags, content, seoTitle, seoDescription, seoKeywords, elapsedMs);
    }

    private void validateSkillOutput(String code, List<String> titles, String summary, List<String> existingTags, List<String> newTags, String content, String seoTitle, String seoDescription, List<String> seoKeywords, String rawText) {
        if ("article_title".equals(code) && titles.isEmpty()) {
            throw new IllegalStateException("AI provider returned JSON without titles. Please retry the generation.");
        }
        if ("article_summary".equals(code) && summary.isBlank()) {
            throw new IllegalStateException("AI provider returned JSON without summary. Please retry the generation.");
        }
        if ("article_tags".equals(code) && existingTags.isEmpty() && newTags.isEmpty()) {
            throw new IllegalStateException("AI provider returned JSON without tags. Please retry the generation.");
        }
        if (("article_polish".equals(code) || "article_outline".equals(code) || "article_expand".equals(code) || "article_continue".equals(code)) && content.isBlank()) {
            throw new IllegalStateException("AI provider returned JSON without content. Please retry the generation.");
        }
        if ("article_seo".equals(code) && (seoTitle.isBlank() || seoDescription.isBlank() || seoKeywords.isEmpty())) {
            throw new IllegalStateException("AI provider returned incomplete SEO JSON. Please retry the generation.");
        }
        if (containsReasoningLeak(content) || containsReasoningLeak(summary) || containsReasoningLeak(seoDescription) || titles.stream().anyMatch(this::containsReasoningLeak)) {
            throw new IllegalStateException("AI provider returned reasoning text instead of final content. Please retry the generation.");
        }
    }

    private boolean isArticleContentSkill(String code) {
        return "article_polish".equals(code)
            || "article_outline".equals(code)
            || "article_expand".equals(code)
            || "article_continue".equals(code);
    }

    private String extractFinalAnswer(String value) {
        String text = cleanAiText(value);
        if (text.isBlank()) return "";
        text = stripJsonWrapper(text);
        String finalMarker = findLastMarker(text,
            "final answer:", "final output:", "final result:", "here is the final json:", "here is the final markdown:",
            "\u6700\u7ec8\u7ed3\u679c\uff1a", "\u6700\u7ec8\u7ed3\u679c:", "\u6700\u7ec8\u6b63\u6587\uff1a", "\u6700\u7ec8\u6b63\u6587:", "\u6700\u7ec8\u8f93\u51fa\uff1a", "\u6700\u7ec8\u8f93\u51fa:");
        if (!finalMarker.isBlank()) {
            text = finalMarker;
        }
        String markdownTail = tailFromMarkdownHeading(text);
        if (!markdownTail.isBlank() && containsReasoningLeak(text.substring(0, Math.max(0, text.length() - markdownTail.length())))) {
            text = markdownTail;
        }
        return cleanAiText(text);
    }
    private String stripJsonWrapper(String value) {
        String text = cleanAiText(value);
        if (text.startsWith("{") && text.endsWith("}")) {
            try {
                JsonNode node = objectMapper.readTree(text);
                if (node.has("content")) return node.path("content").asText("");
                if (node.has("summary")) return node.path("summary").asText("");
                if (node.has("text")) return node.path("text").asText("");
            } catch (Exception ignored) {
                return text;
            }
        }
        return text;
    }

    private String findLastMarker(String text, String... markers) {
        String lower = text.toLowerCase();
        int bestIndex = -1;
        String bestMarker = "";
        for (String marker : markers) {
            int index = lower.lastIndexOf(marker.toLowerCase());
            if (index >= 0 && index > bestIndex) {
                bestIndex = index;
                bestMarker = marker;
            }
        }
        if (bestIndex < 0) return "";
        return text.substring(bestIndex + bestMarker.length()).trim();
    }

    private String tailFromMarkdownHeading(String text) {
        String[] lines = text.split("\\R");
        int index = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (isMarkdownHeading(line) || isNumberedHeading(line)) {
                index = i;
                break;
            }
        }
        if (index < 0) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = index; i < lines.length; i++) {
            builder.append(lines[i]);
            if (i < lines.length - 1) builder.append("\n");
        }
        return builder.toString().trim();
    }

    private boolean isMarkdownHeading(String line) {
        return line.matches("^#{1,4}\\s+.+");
    }

    private boolean isNumberedHeading(String line) {
        if (line.length() < 2) return false;
        char first = line.charAt(0);
        char second = line.charAt(1);
        boolean asciiNumber = first >= '0' && first <= '9' && (second == '.' || second == ')' || second == ' ');
        boolean commonChineseNumber = isCommonChineseNumber(first) && (second == '\u3001' || second == '.' || second == '\uff0e');
        return asciiNumber || commonChineseNumber;
    }

    private boolean isCommonChineseNumber(char value) {
        return value == '\u4e00'
            || value == '\u4e8c'
            || value == '\u4e09'
            || value == '\u56db'
            || value == '\u4e94'
            || value == '\u516d'
            || value == '\u4e03'
            || value == '\u516b'
            || value == '\u4e5d'
            || value == '\u5341';
    }
    private boolean containsReasoningLeak(String value) {
        String text = defaultText(value).toLowerCase();
        return text.contains("the user has specified")
            || text.contains("the user asks")
            || text.contains("my task")
            || text.contains("i need to")
            || text.contains("i must")
            || text.contains("output rules")
            || text.contains("return exactly one strict json")
            || text.contains("no explanations, no reasoning")
            || text.contains("task interpretation");
    }
    private JsonNode parseJsonOutput(String rawText) {
        String text = cleanAiText(rawText);
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String cleanAiText(String value) {
        String text = defaultText(value).replace("\r\n", "\n").trim();
        text = text.replaceFirst("(?is)^\\s*markdown\\s+draft\\s*[:\\uFF1A]?\\s*", "").trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("(?is)^```(?:markdown|md|text|json)?\\s*\\n?", "")
                .replaceFirst("(?is)\\n?```\\s*$", "")
                .trim();
        }
        return text.replaceFirst("(?is)^\\s*markdown\\s+draft\\s*[:\\uFF1A]?\\s*", "").trim();
    }

    private String jsonText(JsonNode root, String key) {
        if (root == null || root.path(key).isMissingNode() || root.path(key).isNull()) return "";
        return cleanAiText(root.path(key).asText(""));
    }

    private List<String> stringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) return values;
        node.forEach(item -> {
            String value = cleanAiText(item.asText(""));
            if (!value.isBlank()) values.add(value);
        });
        return values;
    }

    private List<String> fallbackLines(String rawText) {
        List<String> values = new ArrayList<>();
        for (String line : defaultText(rawText).split("\\R")) {
            String cleaned = line.replaceAll("^[\\s\\-\\d\\.]+", "").trim();
            if (!cleaned.isBlank()) values.add(cleaned);
            if (values.size() >= 8) break;
        }
        return values;
    }

    private String renderPrompt(String template, AiSkillRunRequest request) {
        String existingTags = request.getExistingTags() == null ? "" : String.join(", ", request.getExistingTags());
        return defaultText(template)
            .replace("{{title}}", truncate(defaultText(request.getTitle()), 500))
            .replace("{{summary}}", truncate(defaultText(request.getSummary()), 1000))
            .replace("{{content}}", truncate(defaultText(request.getContent()), INPUT_LIMIT))
            .replace("{{existingTags}}", existingTags);
    }

    private void recordLog(AiProvider provider, String skillCode, String skillName, String input, String output, String error, long elapsedMs, TokenUsage usage) {
        // Defence in depth: input/output/error must not retain a credential even if a caller
        // forgot to redact upstream. The provider's known api_key is also stripped via exact
        // match so unknown-shape vendor keys are still scrubbed.
        String safeInput = SecretRedactor.redact(defaultText(input), provider.apiKey());
        String safeOutput = SecretRedactor.redact(defaultText(output), provider.apiKey());
        String safeError = SecretRedactor.redact(truncate(error, 1000), provider.apiKey());
        jdbcTemplate.update(
            """
            INSERT INTO blog_ai_call_log (provider_name, model, skill_code, skill_name, input_text, output_text, input_preview, output_preview, success, error_message, elapsed_ms, prompt_tokens, completion_tokens, total_tokens)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            provider.name(),
            provider.model(),
            skillCode,
            skillName,
            safeInput,
            safeOutput,
            truncate(safeInput, PREVIEW_LIMIT),
            truncate(safeOutput, PREVIEW_LIMIT),
            error == null || error.isBlank() ? 1 : 0,
            safeError == null ? "" : safeError,
            elapsedMs,
            usage.promptTokens(),
            usage.completionTokens(),
            usage.totalTokens()
        );
    }

    private AiProvider requireUsableProvider() {
        AiProvider provider = requireProvider();
        if (provider.enabled() != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI provider is disabled");
        if (provider.apiKey() == null || provider.apiKey().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI API Key is not configured");
        return provider;
    }

    private AiProvider requireProvider() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id, name, base_url, api_key, model, temperature, max_tokens, enabled, updated_at FROM blog_ai_provider ORDER BY id ASC LIMIT 1",
                (rs, rowNum) -> new AiProvider(
                    rs.getLong("id"), rs.getString("name"), rs.getString("base_url"), rs.getString("api_key"), rs.getString("model"),
                    rs.getDouble("temperature"), rs.getInt("max_tokens"), rs.getInt("enabled"), rs.getObject("updated_at", LocalDateTime.class)
                )
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI provider config not found");
        }
    }

    private AiSkillResponse findSkillById(Long id) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id, name, code, description, scene, prompt_template, enabled, sort_order, updated_at FROM blog_ai_skill WHERE id = ? AND is_deleted = 0",
                (rs, rowNum) -> new AiSkillResponse(rs.getLong("id"), rs.getString("name"), rs.getString("code"), rs.getString("description"), rs.getString("scene"), rs.getString("prompt_template"), rs.getInt("enabled"), rs.getInt("sort_order"), rs.getObject("updated_at", LocalDateTime.class)),
                id
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI skill not found");
        }
    }

    private AiSkillResponse findSkillByCode(String code) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id, name, code, description, scene, prompt_template, enabled, sort_order, updated_at FROM blog_ai_skill WHERE code = ? AND is_deleted = 0",
                (rs, rowNum) -> new AiSkillResponse(rs.getLong("id"), rs.getString("name"), rs.getString("code"), rs.getString("description"), rs.getString("scene"), rs.getString("prompt_template"), rs.getInt("enabled"), rs.getInt("sort_order"), rs.getObject("updated_at", LocalDateTime.class)),
                normalizeCode(code)
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI skill not found");
        }
    }

    private AiProviderResponse toProviderResponse(AiProvider provider) {
        return new AiProviderResponse(provider.id(), provider.name(), provider.baseUrl(), maskKey(provider.apiKey()), provider.model(), provider.temperature(), provider.maxTokens(), provider.enabled(), provider.updatedAt());
    }

    private String maskKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return "";
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) return "********";
        return trimmed.substring(0, Math.min(4, trimmed.length())) + "************************" + trimmed.substring(trimmed.length() - 4);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = normalizeText(baseUrl, "https://token-plan-cn.xiaomimimo.com/v1");
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private String normalizeCode(String code) {
        return normalizeText(code, "ai_skill").trim().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
    }

    private Double normalizeTemperature(Double value) {
        if (value == null) return 0.4;
        return Math.max(0, Math.min(value, 2));
    }

    private Integer normalizeMaxTokens(Integer value) {
        if (value == null || value < 50) return 4000;
        return Math.min(value, 16000);
    }

    private Integer normalizeEnabled(Integer value) {
        return value != null && value == 0 ? 0 : 1;
    }

    private String normalizeText(String value, String fallback) {
        String normalized = defaultText(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int max) {
        String normalized = value == null ? "" : value;
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String cleanError(Exception ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getReason() == null ? rse.getMessage() : rse.getReason();
        }
        return ex.getMessage() == null ? "AI call failed" : ex.getMessage();
    }

    private record AiProvider(Long id, String name, String baseUrl, String apiKey, String model, Double temperature, Integer maxTokens, Integer enabled, LocalDateTime updatedAt) {}
    private record TokenUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {}
    private record AiCallResult(String content, TokenUsage usage) {}
}
