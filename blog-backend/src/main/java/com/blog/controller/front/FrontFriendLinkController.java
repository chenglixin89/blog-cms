package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.FriendLinkRequest;
import com.blog.dto.FriendLinkResponse;
import com.blog.service.FriendLinkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/front/links")
public class FrontFriendLinkController {

    private final FriendLinkService linkService;

    public FrontFriendLinkController(FriendLinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping
    public ApiResponse<List<FriendLinkResponse>> list() {
        return ApiResponse.ok(linkService.approvedList());
    }

    @PostMapping
    public ApiResponse<Void> apply(@RequestBody @Valid FriendLinkRequest request) {
        linkService.apply(request);
        return ApiResponse.ok(null);
    }
}
