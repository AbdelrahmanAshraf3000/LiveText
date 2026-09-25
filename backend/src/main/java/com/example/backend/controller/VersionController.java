package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.version.CreateSnapshotRequest;
import com.example.backend.dto.version.VersionDto;
import com.example.backend.entity.DocumentVersion;
import com.example.backend.security.AuthenticatedUser;
import com.example.backend.service.VersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents/{id}/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VersionDto>>> list(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<VersionDto> versions = versionService.listVersions(id).stream()
                .map(v -> VersionDto.from(v, versionService.creatorUsername(v)))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(versions));
    }

    @GetMapping("/{versionNo}")
    public ResponseEntity<ApiResponse<VersionDto>> get(
            @PathVariable UUID id,
            @PathVariable Integer versionNo,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        DocumentVersion v = versionService.getVersion(id, versionNo);
        return ResponseEntity.ok(ApiResponse.ok(VersionDto.from(v, versionService.creatorUsername(v))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VersionDto>> createSnapshot(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody(required = false) CreateSnapshotRequest body) {
        if (body == null || body.getYjsState() == null || body.getYjsState().isBlank()) {
            throw new IllegalArgumentException("yjsState (base64) is required in the request body");
        }
        byte[] stateBytes = Base64.getDecoder().decode(body.getYjsState());
        byte[] vectorBytes = body.getStateVector() != null && !body.getStateVector().isBlank()
                ? Base64.getDecoder().decode(body.getStateVector()) : null;
        DocumentVersion v = versionService.createSnapshot(id, principal.getId(), stateBytes, vectorBytes);
        return ResponseEntity.ok(ApiResponse.ok(VersionDto.from(v, principal.getUsername()), "Snapshot saved"));
    }

    @PostMapping("/{versionNo}/rollback")
    public ResponseEntity<ApiResponse<VersionDto>> rollback(
            @PathVariable UUID id,
            @PathVariable Integer versionNo,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        DocumentVersion v = versionService.rollback(id, versionNo, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(VersionDto.from(v, principal.getUsername()),
                "Rolled back to version " + versionNo));
    }
}