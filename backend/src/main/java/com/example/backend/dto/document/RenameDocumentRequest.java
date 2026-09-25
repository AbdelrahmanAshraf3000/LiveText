package com.example.backend.dto.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameDocumentRequest(@NotBlank @Size(min = 1, max = 200) String title) {
}