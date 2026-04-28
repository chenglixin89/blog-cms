package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.DailyQuoteResponse;
import com.blog.service.DailyQuoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front/daily-quote")
public class FrontDailyQuoteController {

    private final DailyQuoteService dailyQuoteService;

    public FrontDailyQuoteController(DailyQuoteService dailyQuoteService) {
        this.dailyQuoteService = dailyQuoteService;
    }

    @GetMapping
    public ApiResponse<DailyQuoteResponse> current() {
        return ApiResponse.ok(dailyQuoteService.current());
    }
}
