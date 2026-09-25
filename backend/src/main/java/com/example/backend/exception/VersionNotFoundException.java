package com.example.backend.exception;

public class VersionNotFoundException extends RuntimeException {
    public VersionNotFoundException() {
        super("Version not found");
    }

    public VersionNotFoundException(String message) {
        super(message);
    }
}