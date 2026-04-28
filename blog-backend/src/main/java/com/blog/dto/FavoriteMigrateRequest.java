package com.blog.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class FavoriteMigrateRequest {

    @NotNull(message = "收藏文章 ID 列表不能为空")
    private List<Long> articleIds;

    public List<Long> getArticleIds() {
        return articleIds;
    }

    public void setArticleIds(List<Long> articleIds) {
        this.articleIds = articleIds;
    }
}
