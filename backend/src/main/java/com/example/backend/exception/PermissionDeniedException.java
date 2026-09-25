package com.example.backend.exception;

public class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException() {
        super("You do not have permission to perform this action");
    }

    public PermissionDeniedException(String message) {
        super(message);
    }
}