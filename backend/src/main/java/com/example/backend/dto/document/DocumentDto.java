package com.example.backend.dto.document;

import com.example.backend.entity.Document;
import com.example.backend.entity.Role;

import java.util.UUID;

public record DocumentDto(
        UUID id,
        String title,
        Long ownerId,
        String ownerUsername,
        Role viewerRole,
        String createdAt,
        String updatedAt) {

    public static DocumentDto from(Document doc, Role viewerRole, String ownerUsername) {
        return new DocumentDto(
                doc.getId(),
                doc.getTitle(),
                doc.getOwnerId(),
                ownerUsername,
                viewerRole,
                doc.getCreatedAt() == null ? null : doc.getCreatedAt().toString(),
                doc.getUpdatedAt() == null ? null : doc.getUpdatedAt().toString());
    }
}