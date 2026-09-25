package com.example.backend.collab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.CloseStatus;

/**
 * Tracks active WebSocket sessions per document for broadcasting updates.
 */
@Slf4j
@Component
public class SessionRegistry {

    private final Map<UUID, Set<WebSocketSession>> sessionsByDoc = new ConcurrentHashMap<>();

    public void add(UUID docId, WebSocketSession session) {
        sessionsByDoc.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Session {} added to doc {} (total: {})", session.getId(), docId,
                sessionsByDoc.get(docId).size());
    }

    public void remove(UUID docId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByDoc.get(docId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByDoc.remove(docId);
            }
        }
        log.debug("Session {} removed from doc {}", session.getId(), docId);
    }

    public Set<WebSocketSession> getSessions(UUID docId) {
        return sessionsByDoc.getOrDefault(docId, Set.of());
    }

    public int getSessionCount(UUID docId) {
        return sessionsByDoc.getOrDefault(docId, Set.of()).size();
    }

    /**
     * Close all WS sessions for a document (used during rollback).
     */
    public void closeAllSessions(UUID docId) {
        Set<WebSocketSession> sessions = sessionsByDoc.remove(docId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    session.close(CloseStatus.GOING_AWAY.withReason("Document rolled back"));
                } catch (Exception e) {
                    log.warn("Failed to close session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }
}