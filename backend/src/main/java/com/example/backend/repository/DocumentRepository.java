package com.example.backend.repository;

import com.example.backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    @Query("""
            select d from Document d
            join DocumentPermission p on p.document = d
            where p.user.id = :userId and d.ownerId <> :userId
            order by d.updatedAt desc
            """)
    List<Document> findSharedWithUser(Long userId);
}