package com.example.backend.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException() {
        super("Document not found");
    }

    public DocumentNotFoundException(UUID id) {
        super("Document not found: " + id);
    }
}