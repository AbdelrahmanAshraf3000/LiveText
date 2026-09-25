package com.example.backend.service;

import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentPermission;
import com.example.backend.entity.Role;
import com.example.backend.exception.DocumentNotFoundException;
import com.example.backend.exception.PermissionDeniedException;
import com.example.backend.repository.DocumentPermissionRepository;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Document create(String title, Long ownerId) {
        Document doc = Document.createNew(title, ownerId);
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<Document> listOwned(Long ownerId) {
        return documentRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Document> listSharedWith(Long userId) {
        return documentRepository.findSharedWithUser(userId);
    }

    @Transactional(readOnly = true)
    public Document getForUser(UUID documentId, Long userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);
        if (doc.getOwnerId().equals(userId)) {
            return doc;
        }
        permissionRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseThrow(PermissionDeniedException::new);
        return doc;
    }

    @Transactional
    public Document rename(UUID documentId, Long userId, String newTitle) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);
        Role role = effectiveRole(doc, userId);
        if (!role.canEdit()) {
            throw new PermissionDeniedException("Editor access required to rename");
        }
        doc.setTitle(newTitle);
        return documentRepository.save(doc);
    }

    @Transactional
    public void delete(UUID documentId, Long userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);
        if (!doc.getOwnerId().equals(userId)) {
            throw new PermissionDeniedException("Only the owner can delete this document");
        }
        documentRepository.delete(doc);
    }

    @Transactional
    public Document touch(UUID documentId) {
        Document doc = documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
        return documentRepository.save(doc);
    }

    private Role effectiveRole(Document doc, Long userId) {
        if (doc.getOwnerId().equals(userId)) {
            return Role.OWNER;
        }
        return permissionRepository.findByDocumentIdAndUserId(doc.getId(), userId)
                .map(DocumentPermission::getRole)
                .orElseThrow(PermissionDeniedException::new);
    }

    public String ownerUsername(Document doc) {
        return userRepository.findById(doc.getOwnerId())
                .map(com.example.backend.entity.User::getUsername)
                .orElse("unknown");
    }
}