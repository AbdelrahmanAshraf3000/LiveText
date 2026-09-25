package com.example.backend.dto.document;

import com.example.backend.entity.Role;

public record CollaboratorDto(
        Long userId,
        String username,
        Role role,
        String grantedByUsername,
        String grantedAt) {
}