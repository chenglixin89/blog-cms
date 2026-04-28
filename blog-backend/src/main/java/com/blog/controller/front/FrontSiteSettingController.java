package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.SiteSettingResponse;
import com.blog.service.SiteSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front/settings")
public class FrontSiteSettingController {

    private final SiteSettingService settingService;

    public FrontSiteSettingController(SiteSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public ApiResponse<SiteSettingResponse> current() {
        return ApiResponse.ok(settingService.current());
    }
}
