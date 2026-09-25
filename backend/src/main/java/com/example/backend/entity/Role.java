package com.example.backend.entity;

public enum Role {
    OWNER,
    EDITOR,
    VIEWER;

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canShare() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canDelete() {
        return this == OWNER;
    }

    public boolean canRollback() {
        return this == OWNER || this == EDITOR;
    }
}