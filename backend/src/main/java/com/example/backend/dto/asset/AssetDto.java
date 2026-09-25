package com.example.backend.dto.asset;

import com.example.backend.entity.Asset;

import java.util.UUID;

public record AssetDto(UUID id, String fileName, String mimeType, Long byteSize, String url, String createdAt) {
    public static AssetDto from(Asset a) {
        return new AssetDto(
                a.getId(),
                a.getFileName(),
                a.getMimeType(),
                a.getByteSize(),
                a.getUrl(),
                a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
    }
}