package com.blog.service;

import com.blog.dto.DailyQuoteResponse;
import com.blog.dto.SiteSettingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class DailyQuoteService {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final DailyQuoteResponse DISABLED_RESPONSE = new DailyQuoteResponse(
        false,
        "",
        "",
        "",
        "",
        "Hitokoto",
        false,
        null
    );

    private final SiteSettingService settingService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private DailyQuoteResponse cachedQuote;
    private LocalDateTime cachedAt;
    private String cachedUrl;

    public DailyQuoteService(SiteSettingService settingService, ObjectMapper objectMapper) {
        this.settingService = settingService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    }

    public DailyQuoteResponse current() {
        SiteSettingResponse setting = settingService.current();
        if (setting.dailyQuoteEnabled() == null || setting.dailyQuoteEnabled() == 0) {
            return DISABLED_RESPONSE;
        }

        String apiUrl = setting.dailyQuoteApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = SiteSettingService.DEFAULT_DAILY_QUOTE_API_URL;
        }
        if (isCacheFresh(apiUrl)) {
            return cachedQuote;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Quote API returned " + response.statusCode());
            }
            DailyQuoteResponse quote = parseHitokoto(response.body());
            cachedQuote = quote;
            cachedAt = quote.fetchedAt();
            cachedUrl = apiUrl;
            return quote;
        } catch (Exception exception) {
            DailyQuoteResponse fallback = fallbackQuote();
            cachedQuote = fallback;
            cachedAt = fallback.fetchedAt();
            cachedUrl = apiUrl;
            return fallback;
        }
    }

    private boolean isCacheFresh(String apiUrl) {
        return cachedQuote != null
            && cachedAt != null
            && apiUrl.equals(cachedUrl)
            && cachedAt.plus(CACHE_TTL).isAfter(LocalDateTime.now());
    }

    private DailyQuoteResponse parseHitokoto(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String content = text(root, "hitokoto");
        if (content.isBlank()) {
            content = text(root, "content");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("Quote content is empty");
        }

        String source = text(root, "from");
        String author = text(root, "from_who");
        String uuid = text(root, "uuid");
        String sourceUrl = uuid.isBlank() ? "https://hitokoto.cn/" : "https://hitokoto.cn?uuid=" + uuid;

        return new DailyQuoteResponse(
            true,
            content,
            source,
            author,
            sourceUrl,
            "Hitokoto",
            false,
            LocalDateTime.now()
        );
    }

    private DailyQuoteResponse fallbackQuote() {
        return new DailyQuoteResponse(
            true,
            "把细小的灵感认真保存，日子就会慢慢长出自己的光。",
            "Blog CMS",
            "Admin",
            "",
            "Local fallback",
            true,
            LocalDateTime.now()
        );
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }
}
