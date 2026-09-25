package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.security.AuthenticatedUser;
import com.example.backend.service.CollabStateService;
import com.example.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint for client-led collab state snapshot uploads (Architecture B).
 * <p>
 * Unlike the {@code /versions} POST (which creates a named version entry), this endpoint
 * only updates the live collab_state + prunes the update log. Used by the designated leader
 * client on a periodic timer to keep the server's persisted state fresh without cluttering
 * the user-visible version history.
 */
@RestController
@RequestMapping("/api/documents/{id}/collab")
@RequiredArgsConstructor
public class CollabController {

    private final CollabStateService collabStateService;
    private final PermissionService permissionService;

    @PostMapping("/snapshot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> snapshot(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, String> body) {

        permissionService.requireCanEdit(id, principal.getId());

        String yjsStateBase64 = body.get("yjsState");
        String stateVectorBase64 = body.get("stateVector");
        if (yjsStateBase64 == null || yjsStateBase64.isBlank()) {
            throw new IllegalArgumentException("yjsState (base64) is required");
        }
        byte[] stateBytes = Base64.getDecoder().decode(yjsStateBase64);
        byte[] vectorBytes = (stateVectorBase64 != null && !stateVectorBase64.isBlank())
                ? Base64.getDecoder().decode(stateVectorBase64) : null;

        collabStateService.saveSnapshot(id, stateBytes, vectorBytes);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("byteSize", stateBytes.length), "Snapshot saved"));
    }
}