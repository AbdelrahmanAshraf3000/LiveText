package com.example.backend.repository;

import com.example.backend.entity.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(UUID documentId, Long userId);

    List<DocumentPermission> findByDocumentIdOrderByGrantedAtAsc(UUID documentId);

    boolean existsByDocumentIdAndUserId(UUID documentId, Long userId);

    void deleteByDocumentIdAndUserId(UUID documentId, Long userId);
}