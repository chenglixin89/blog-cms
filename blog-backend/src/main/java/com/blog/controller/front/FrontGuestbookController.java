package com.blog.controller.front;

import com.blog.common.ApiResponse;
import com.blog.dto.FrontGuestbookMessageRequest;
import com.blog.dto.FrontGuestbookMessageResponse;
import com.blog.service.GuestbookMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/front/messages")
public class FrontGuestbookController {

    private final GuestbookMessageService messageService;

    public FrontGuestbookController(GuestbookMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<FrontGuestbookMessageResponse>> list() {
        return ApiResponse.ok(messageService.approvedList());
    }

    @PostMapping
    public ApiResponse<Void> submit(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody @Valid FrontGuestbookMessageRequest request
    ) {
        messageService.submit(request, authorization);
        return ApiResponse.ok(null);
    }
}