package com.example.backend.service;

import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentPermission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.DocumentNotFoundException;
import com.example.backend.exception.DuplicatePermissionException;
import com.example.backend.exception.PermissionDeniedException;
import com.example.backend.exception.UserNotFoundException;
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
public class PermissionService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Role getRole(UUID documentId, Long userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);
        if (doc.getOwnerId().equals(userId)) {
            return Role.OWNER;
        }
        return permissionRepository.findByDocumentIdAndUserId(documentId, userId)
                .map(DocumentPermission::getRole)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Role requireAccess(UUID documentId, Long userId) {
        Role role = getRole(documentId, userId);
        if (role == null) {
            throw new PermissionDeniedException();
        }
        return role;
    }

    @Transactional(readOnly = true)
    public Role requireCanEdit(UUID documentId, Long userId) {
        Role role = requireAccess(documentId, userId);
        if (!role.canEdit()) {
            throw new PermissionDeniedException("Editor access required");
        }
        return role;
    }

    @Transactional(readOnly = true)
    public Role requireCanShare(UUID documentId, Long userId) {
        Role role = requireAccess(documentId, userId);
        if (!role.canShare()) {
            throw new PermissionDeniedException("Share access required");
        }
        return role;
    }

    @Transactional(readOnly = true)
    public Role requireOwner(UUID documentId, Long userId) {
        Role role = requireAccess(documentId, userId);
        if (!role.canDelete()) {
            throw new PermissionDeniedException("Owner access required");
        }
        return role;
    }

    @Transactional
    public DocumentPermission share(UUID documentId, Long grantorId, String username, Role role) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);
        User grantor = userRepository.findById(grantorId)
                .orElseThrow(() -> new UserNotFoundException(grantorId));
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (doc.getOwnerId().equals(target.getId())) {
            throw new DuplicatePermissionException("Cannot share with the owner");
        }
        if (permissionRepository.existsByDocumentIdAndUserId(documentId, target.getId())) {
            throw new DuplicatePermissionException("Already shared with " + username);
        }

        DocumentPermission perm = new DocumentPermission(doc, target, role, grantor);
        return permissionRepository.save(perm);
    }

    @Transactional
    public DocumentPermission changeRole(UUID documentId, Long grantorId, String username, Role role) {
        documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        DocumentPermission perm = permissionRepository
                .findByDocumentIdAndUserId(documentId, target.getId())
                .orElseThrow(() -> new PermissionDeniedException("Not shared with " + username));
        perm.setRole(role);
        return permissionRepository.save(perm);
    }

    @Transactional
    public void revoke(UUID documentId, Long grantorId, String username) {
        documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        permissionRepository.deleteByDocumentIdAndUserId(documentId, target.getId());
    }

    @Transactional(readOnly = true)
    public List<DocumentPermission> listCollaborators(UUID documentId) {
        documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
        return permissionRepository.findByDocumentIdOrderByGrantedAtAsc(documentId);
    }
}