package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.document.CreateDocumentRequest;
import com.example.backend.dto.document.DocumentDto;
import com.example.backend.dto.document.RenameDocumentRequest;
import com.example.backend.entity.Document;
import com.example.backend.entity.Role;
import com.example.backend.security.AuthenticatedUser;
import com.example.backend.service.DocumentService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentDto>> create(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Document doc = documentService.create(request.title(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(DocumentDto.from(doc, Role.OWNER, principal.getUsername())));
    }

    @GetMapping("/owned")
    public ResponseEntity<ApiResponse<List<DocumentDto>>> owned(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<DocumentDto> docs = documentService.listOwned(principal.getId()).stream()
                .map(d -> DocumentDto.from(d, Role.OWNER, principal.getUsername()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<DocumentDto>>> shared(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<DocumentDto> docs = documentService.listSharedWith(principal.getId()).stream()
                .map(d -> {
                    Role role = permissionService.getRole(d.getId(), principal.getId());
                    return DocumentDto.from(d, role, documentService.ownerUsername(d));
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDto>> open(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Document doc = documentService.getForUser(id, principal.getId());
        Role role = permissionService.getRole(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(DocumentDto.from(doc, role, documentService.ownerUsername(doc))));
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<ApiResponse<DocumentDto>> rename(
            @PathVariable UUID id,
            @Valid @RequestBody RenameDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        permissionService.requireCanEdit(id, principal.getId());
        Document doc = documentService.rename(id, principal.getId(), request.title());
        Role role = permissionService.getRole(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(DocumentDto.from(doc, role, principal.getUsername())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        permissionService.requireOwner(id, principal.getId());
        documentService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Document deleted"));
    }
}