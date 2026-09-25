package com.example.backend.dto.version;

import com.example.backend.entity.DocumentVersion;

public record VersionDto(
        Long id,
        Integer versionNo,
        Integer byteSize,
        String createdByUsername,
        String createdAt) {

    public static VersionDto from(DocumentVersion v, String createdByUsername) {
        return new VersionDto(
                v.getId(),
                v.getVersionNo(),
                v.getByteSize(),
                createdByUsername,
                v.getCreatedAt() == null ? null : v.getCreatedAt().toString());
    }
}