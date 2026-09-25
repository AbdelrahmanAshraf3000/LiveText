package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.asset.AssetDto;
import com.example.backend.entity.Asset;
import com.example.backend.security.AuthenticatedUser;
import com.example.backend.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Asset upload/download controller.
 * <p>
 * Upload is backend-proxied: browser -> backend -> MinIO. Download redirects (302)
 * to a short-lived presigned MinIO GET URL. The document embeds the stable backend
 * URL ("/api/assets/{id}"); each load gets a fresh signed URL from the backend.
 */
@Slf4j
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentId") UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser principal) throws IOException {
        Asset asset = assetService.upload(documentId, principal.getId(), file);
        return ResponseEntity.ok(ApiResponse.ok(AssetDto.from(asset), "Image uploaded"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Void> serve(@PathVariable UUID id) {
        String url = assetService.getPresignedUrl(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .header("Cache-Control", "no-store")
                .build();
    }
}