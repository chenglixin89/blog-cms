package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.MediaAssetCategoryRequest;
import com.blog.dto.MediaAssetResponse;
import com.blog.dto.PageResponse;
import com.blog.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<MediaAssetResponse>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "12") Integer size
    ) {
        return ApiResponse.ok(mediaService.page(keyword, category, page, size));
    }

    @PostMapping("/upload")
    public ApiResponse<MediaAssetResponse> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "category", required = false) String category,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(mediaService.upload(file, category, request));
    }

    @PutMapping("/{id}/category")
    public ApiResponse<MediaAssetResponse> changeCategory(
        @PathVariable Long id,
        @RequestBody @Valid MediaAssetCategoryRequest request,
        HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(mediaService.changeCategory(id, request.getCategory(), httpRequest));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        mediaService.delete(id, request);
        return ApiResponse.ok(null);
    }
}
