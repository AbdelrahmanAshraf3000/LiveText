package com.example.backend.exception;

public class DuplicatePermissionException extends RuntimeException {
    public DuplicatePermissionException(String message) {
        super(message);
    }
}