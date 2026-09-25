package com.example.backend.repository;

import com.example.backend.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNo(UUID documentId, Integer versionNo);

    Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNoDesc(UUID documentId);
}