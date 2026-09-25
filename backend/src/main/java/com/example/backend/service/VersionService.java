package com.example.backend.service;

import com.example.backend.collab.SessionRegistry;
import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentVersion;
import com.example.backend.entity.User;
import com.example.backend.exception.DocumentNotFoundException;
import com.example.backend.exception.VersionNotFoundException;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.DocumentVersionRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private final DocumentVersionRepository versionRepo;
    private final DocumentRepository documentRepo;
    private final UserRepository userRepo;
    private final CollabStateService collabStateService;
    private final SessionRegistry sessionRegistry;
    private final PermissionService permissionService;

    /**
     * Create a new version snapshot from the client's current Yjs state.
     * In Architecture B snapshots are always client-produced (the server has no Yjs engine).
     */
    @Transactional
    public DocumentVersion createSnapshot(UUID docId, Long userId, byte[] yjsStateBytes, byte[] stateVectorBytes) {
        Document doc = documentRepo.findById(docId)
                .orElseThrow(DocumentNotFoundException::new);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        Integer latestVersionNo = versionRepo.findTopByDocumentIdOrderByVersionNoDesc(docId)
                .map(DocumentVersion::getVersionNo)
                .orElse(0);
        int nextVersionNo = latestVersionNo + 1;

        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setVersionNo(nextVersionNo);
        version.setYjsState(yjsStateBytes);
        version.setByteSize(yjsStateBytes.length);
        version.setCreatedBy(user);
        version = versionRepo.save(version);

        collabStateService.saveSnapshot(docId, yjsStateBytes, stateVectorBytes);

        log.info("Created version {} for doc {} ({} bytes)", nextVersionNo, docId, yjsStateBytes.length);
        return version;
    }

    @Transactional(readOnly = true)
    public List<DocumentVersion> listVersions(UUID docId) {
        documentRepo.findById(docId).orElseThrow(DocumentNotFoundException::new);
        return versionRepo.findByDocumentIdOrderByVersionNoDesc(docId);
    }

    @Transactional(readOnly = true)
    public DocumentVersion getVersion(UUID docId, Integer versionNo) {
        return versionRepo.findByDocumentIdAndVersionNo(docId, versionNo)
                .orElseThrow(() -> new VersionNotFoundException(
                        "Version " + versionNo + " not found for document " + docId));
    }

    @Transactional(readOnly = true)
    public byte[] getVersionState(UUID docId, Integer versionNo) {
        return getVersion(docId, versionNo).getYjsState();
    }

    /**
     * Rollback the live document to a stored version's state. Closes all sessions so
     * clients reconnect and resync, and records the rollback as a NEW version (append-only).
     */
    @Transactional
    public DocumentVersion rollback(UUID docId, Integer versionNo, Long userId) {
        permissionService.requireCanEdit(docId, userId);

        DocumentVersion targetVersion = getVersion(docId, versionNo);
        byte[] targetState = targetVersion.getYjsState();

        sessionRegistry.closeAllSessions(docId);
        collabStateService.rollbackToState(docId, targetState);

        Document doc = documentRepo.findById(docId).orElseThrow(DocumentNotFoundException::new);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        Integer latestVersionNo = versionRepo.findTopByDocumentIdOrderByVersionNoDesc(docId)
                .map(DocumentVersion::getVersionNo)
                .orElse(0);
        int nextVersionNo = latestVersionNo + 1;

        DocumentVersion rollbackVersion = new DocumentVersion();
        rollbackVersion.setDocument(doc);
        rollbackVersion.setVersionNo(nextVersionNo);
        rollbackVersion.setYjsState(targetState);
        rollbackVersion.setByteSize(targetState.length);
        rollbackVersion.setCreatedBy(user);
        rollbackVersion = versionRepo.save(rollbackVersion);

        log.info("Rolled back doc {} to version {} (recorded as new version {})",
                docId, versionNo, nextVersionNo);
        return rollbackVersion;
    }

    public String creatorUsername(DocumentVersion version) {
        return userRepo.findById(version.getCreatedBy().getId())
                .map(User::getUsername)
                .orElse("unknown");
    }
}