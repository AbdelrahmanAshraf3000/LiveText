package com.example.backend.dto.document;

import com.example.backend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareRequest(@NotBlank String username, @NotNull Role role) {
}