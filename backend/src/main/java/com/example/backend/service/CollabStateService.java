package com.example.backend.service;

import com.example.backend.entity.CollabState;
import com.example.backend.entity.CollabUpdate;
import com.example.backend.entity.Document;
import com.example.backend.repository.CollabStateRepository;
import com.example.backend.repository.CollabUpdateRepository;
import com.example.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manages CRDT state persistence for Architecture B (relay + persistence, no server-side Yjs engine).
 * <p>
 * The server treats Yjs update bytes as opaque. It stores a base snapshot (uploaded by a
 * designated leader client) + an append-only update log with SHA-256 dedup. On reconnect,
 * a client receives the snapshot + all buffered updates and merges them in its own Yjs
 * document (Yjs updates are commutative/idempotent — convergence is guaranteed regardless
 * of server involvement).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabStateService {

    private final CollabStateRepository stateRepo;
    private final CollabUpdateRepository updateRepo;
    private final DocumentRepository documentRepo;

    @Transactional(readOnly = true)
    public byte[] getSnapshot(UUID docId) {
        return stateRepo.findById(docId)
                .map(CollabState::getSnapshot)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public byte[] getSnapshotStateVector(UUID docId) {
        return stateRepo.findById(docId)
                .map(CollabState::getStateVector)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CollabUpdate> getBufferedUpdates(UUID docId) {
        return updateRepo.findByDocumentIdOrderByIdAsc(docId);
    }

    @Transactional
    public boolean applyUpdate(UUID docId, byte[] updateBytes) {
        String hash = sha256Hex(updateBytes);
        if (updateRepo.existsByDocumentIdAndHash(docId, hash)) {
            log.debug("Duplicate update for doc {} (hash {}), skipping", docId, hash);
            return false;
        }

        Document doc = documentRepo.findById(docId).orElse(null);
        if (doc != null) {
            CollabUpdate update = new CollabUpdate();
            update.setDocument(doc);
            update.setUpdate(updateBytes);
            update.setHash(hash);
            updateRepo.save(update);
        }
        return true;
    }

    @Transactional
    public void saveSnapshot(UUID docId, byte[] snapshotBytes, byte[] stateVectorBytes) {
        CollabState state = stateRepo.findById(docId).orElseGet(() -> {
            CollabState s = new CollabState();
            s.setDocumentId(docId);
            return s;
        });
        state.setSnapshot(snapshotBytes);
        state.setStateVector(stateVectorBytes);
        state.setSnapshotAt(LocalDateTime.now());
        state.setUpdatedAt(LocalDateTime.now());
        stateRepo.save(state);

        updateRepo.deleteAllByDocumentId(docId);

        log.info("Snapshot saved for doc {} ({} bytes, {} updates cleared)",
                docId, snapshotBytes.length, updateRepo.countByDocumentId(docId));
    }

    @Transactional
    public void rollbackToState(UUID docId, byte[] yjsStateBytes) {
        CollabState state = stateRepo.findById(docId).orElseGet(() -> {
            CollabState s = new CollabState();
            s.setDocumentId(docId);
            return s;
        });
        state.setSnapshot(yjsStateBytes);
        state.setSnapshotAt(LocalDateTime.now());
        state.setUpdatedAt(LocalDateTime.now());
        stateRepo.save(state);

        updateRepo.deleteAllByDocumentId(docId);

        log.info("Rolled back doc {} to stored state ({} bytes)", docId, yjsStateBytes.length);
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }
}