package com.blog.service;

import com.blog.dto.DashboardStatsResponse;
import com.blog.dto.DashboardTrendPointResponse;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;

    public DashboardService(
        ArticleMapper articleMapper,
        CategoryMapper categoryMapper,
        TagMapper tagMapper,
        CommentMapper commentMapper
    ) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
    }

    public DashboardStatsResponse stats() {
        return new DashboardStatsResponse(
            articleMapper.countAll(),
            articleMapper.countByStatus(1),
            articleMapper.countByStatus(0),
            categoryMapper.countAll(),
            tagMapper.countAll(),
            commentMapper.countPending()
        );
    }

    public List<DashboardTrendPointResponse> publishTrend() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, Long> dayCounts = new LinkedHashMap<>();

        for (int index = 29; index >= 0; index -= 1) {
            LocalDate date = today.minusDays(index);
            dayCounts.put(date.format(formatter), 0L);
        }

        articleMapper.selectRecentPublishTrend().forEach(row -> {
            Object keyValue = mapValue(row, "dayKey");
            Object countValue = mapValue(row, "publishCount");
            if (keyValue == null) {
                return;
            }
            String key = String.valueOf(keyValue);
            long count = countValue instanceof Number number ? number.longValue() : 0L;
            if (dayCounts.containsKey(key)) {
                dayCounts.put(key, count);
            }
        });

        return dayCounts.entrySet().stream()
            .map(entry -> {
                LocalDate date = LocalDate.parse(entry.getKey(), formatter);
                String label = String.format(Locale.ROOT, "%d/%d", date.getMonthValue(), date.getDayOfMonth());
                return new DashboardTrendPointResponse(entry.getKey(), label, entry.getValue());
            })
            .toList();
    }

    private Object mapValue(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        return row.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(key))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }}
