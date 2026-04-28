package com.blog.service;

import com.blog.dto.MediaAssetResponse;
import com.blog.dto.PageResponse;
import com.blog.entity.MediaAsset;
import com.blog.mapper.MediaAssetMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 5L * 1024L * 1024L;

    private final Path uploadRoot;
    private final MediaAssetMapper mediaAssetMapper;
    private final MediaCategoryService mediaCategoryService;
    private final AuditLogService auditLogService;

    public MediaService(
        @Value("${blog.upload.dir:uploads}") String uploadDir,
        MediaAssetMapper mediaAssetMapper,
        MediaCategoryService mediaCategoryService,
        AuditLogService auditLogService
    ) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.mediaAssetMapper = mediaAssetMapper;
        this.mediaCategoryService = mediaCategoryService;
        this.auditLogService = auditLogService;
    }

    public PageResponse<MediaAssetResponse> page(String keyword, String category, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = normalizeText(keyword);
        String normalizedCategory = normalizeText(category);
        long total = mediaAssetMapper.countPage(normalizedKeyword, normalizedCategory);
        List<MediaAssetResponse> records = mediaAssetMapper.selectPage(normalizedKeyword, normalizedCategory, offset, normalizedSize)
            .stream()
            .map(this::toResponse)
            .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    @Transactional
    public MediaAssetResponse upload(MultipartFile file, String category, HttpServletRequest request) {
        validate(file);
        String extension = getExtension(file.getOriginalFilename());
        String storageFolder = resolveStorageFolder(category);
        try {
            Path uploadDir = uploadRoot.resolve(storageFolder).normalize();
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = uploadDir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = storageFolder + "/" + filename;
            MediaAsset mediaAsset = new MediaAsset();
            mediaAsset.setUrl(buildPublicUrl(relativePath, request));
            mediaAsset.setOriginalName(defaultText(file.getOriginalFilename()));
            mediaAsset.setFileName(relativePath);
            mediaAsset.setExtension(extension);
            mediaAsset.setContentType(defaultText(file.getContentType()));
            mediaAsset.setSize(file.getSize());
            mediaAsset.setCategory(normalizeCategory(category));
            mediaAssetMapper.insert(mediaAsset);
            auditLogService.record(request, "Media", "Upload", "MEDIA", mediaAsset.getId(), "Upload media: " + mediaAsset.getOriginalName() + ", url: " + mediaAsset.getUrl());
            return toResponse(mediaAsset);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed");
        }
    }

    @Transactional
    public MediaAssetResponse changeCategory(Long id, String category, HttpServletRequest request) {
        MediaAsset existing = mediaAssetMapper.selectById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        String normalizedCategory = normalizeCategory(category);
        int updated = mediaAssetMapper.updateCategory(id, normalizedCategory);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        auditLogService.record(request, "Media", "Change category", "MEDIA", id, "Change media category: " + existing.getCategory() + " -> " + normalizedCategory);
        return toResponse(mediaAssetMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        int deleted = mediaAssetMapper.logicalDelete(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        auditLogService.record(request, "Media", "Delete", "MEDIA", id, "Delete media asset");
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload file is empty");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image size must be less than 5MB");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only jpg, png, gif and webp images are allowed");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String buildPublicUrl(String relativePath, HttpServletRequest request) {
        String normalizedPath = relativePath.replace("\\", "/");
        return ServletUriComponentsBuilder.fromRequestUri(request)
            .replacePath("/uploads/" + normalizedPath)
            .replaceQuery(null)
            .build()
            .toUriString();
    }

    private String resolveStorageFolder(String category) {
        String normalizedCategory = normalizeText(category).toLowerCase(Locale.ROOT);
        if ("cover".equals(normalizedCategory) || "covers".equals(normalizedCategory)) {
            return "covers";
        }
        if ("avatar".equals(normalizedCategory) || "avatars".equals(normalizedCategory)) {
            return "avatars";
        }
        return "media";
    }

    private String normalizeCategory(String category) {
        return mediaCategoryService.normalizeExistingCode(category);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return Math.min(size == null || size < 1 ? 12 : size, 100);
    }

    private MediaAssetResponse toResponse(MediaAsset mediaAsset) {
        return new MediaAssetResponse(
            mediaAsset.getId(),
            mediaAsset.getUrl(),
            mediaAsset.getOriginalName(),
            mediaAsset.getFileName(),
            mediaAsset.getExtension(),
            mediaAsset.getContentType(),
            mediaAsset.getSize(),
            mediaAsset.getCategory(),
            mediaAsset.getCreatedAt()
        );
    }
}
