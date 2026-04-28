package com.blog.controller.admin;

import com.blog.common.ApiResponse;
import com.blog.dto.MediaAssetResponse;
import com.blog.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/upload")
public class UploadController {

    private final MediaService mediaService;

    public UploadController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/cover")
    public ApiResponse<String> uploadCover(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        MediaAssetResponse response = mediaService.upload(file, "cover", request);
        return ApiResponse.ok(response.url());
    }
}
