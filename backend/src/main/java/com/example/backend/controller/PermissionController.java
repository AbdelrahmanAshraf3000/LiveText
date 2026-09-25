package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.document.CollaboratorDto;
import com.example.backend.dto.document.ShareRequest;
import com.example.backend.entity.DocumentPermission;
import com.example.backend.security.AuthenticatedUser;
import com.example.backend.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents/{id}/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollaboratorDto>>> list(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        permissionService.requireAccess(id, principal.getId());
        List<CollaboratorDto> collaborators = permissionService.listCollaborators(id).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(collaborators));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollaboratorDto>> share(
            @PathVariable UUID id,
            @Valid @RequestBody ShareRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        permissionService.requireCanShare(id, principal.getId());
        DocumentPermission perm = permissionService.share(id, principal.getId(), request.username(), request.role());
        return ResponseEntity.ok(ApiResponse.ok(toDto(perm), "Shared with " + request.username()));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<CollaboratorDto>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ShareRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        permissionService.requireCanShare(id, principal.getId());
        DocumentPermission perm = permissionService.changeRole(id, principal.getId(), request.username(), request.role());
        return ResponseEntity.ok(ApiResponse.ok(toDto(perm), "Role updated for " + request.username()));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> revoke(
            @PathVariable UUID id,
            @RequestParam String username,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        permissionService.requireCanShare(id, principal.getId());
        permissionService.revoke(id, principal.getId(), username);
        return ResponseEntity.ok(ApiResponse.ok(null, "Access revoked for " + username));
    }

    private CollaboratorDto toDto(DocumentPermission perm) {
        return new CollaboratorDto(
                perm.getUser().getId(),
                perm.getUser().getUsername(),
                perm.getRole(),
                perm.getGrantedBy().getUsername(),
                perm.getGrantedAt() == null ? null : perm.getGrantedAt().toString());
    }
}