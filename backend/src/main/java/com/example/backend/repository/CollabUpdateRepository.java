package com.example.backend.repository;

import com.example.backend.entity.CollabUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CollabUpdateRepository extends JpaRepository<CollabUpdate, Long> {

    List<CollabUpdate> findByDocumentIdOrderByIdAsc(UUID documentId);

    boolean existsByDocumentIdAndHash(UUID documentId, String hash);

    @Modifying
    @Query("delete from CollabUpdate u where u.document.id = :docId")
    void deleteAllByDocumentId(UUID docId);

    long countByDocumentId(UUID documentId);
}