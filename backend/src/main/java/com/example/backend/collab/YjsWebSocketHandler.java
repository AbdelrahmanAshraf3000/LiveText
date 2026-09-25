package com.example.backend.collab;

import com.example.backend.entity.CollabUpdate;
import com.example.backend.service.CollabStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Binary WebSocket handler implementing the y-websocket protocol (Architecture B relay).
 * <p>
 * The server does NOT run a Yjs CRDT engine. It treats update bytes as opaque blobs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YjsWebSocketHandler extends BinaryWebSocketHandler {

    private final CollabStateService collabStateService;
    private final SessionRegistry sessionRegistry;
    private final RedisBroadcaster redisBroadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        UUID docId = (UUID) attrs.get("docId");
        String username = (String) attrs.get("username");
        Boolean readOnly = (Boolean) attrs.get("readOnly");

        boolean firstLocal = sessionRegistry.getSessionCount(docId) == 0;
        sessionRegistry.add(docId, session);

        if (firstLocal) {
            redisBroadcaster.subscribe(docId, messageBytes -> onRedisMessage(docId, messageBytes));
        }

        byte[] snapshot = collabStateService.getSnapshot(docId);
        if (snapshot != null && snapshot.length > 0) {
            sendMessage(session, YjsProtocol.buildSyncStep2(snapshot));
            log.debug("Sent snapshot sync-step2 ({} bytes) to {}", snapshot.length, username);
        }

        for (CollabUpdate u : collabStateService.getBufferedUpdates(docId)) {
            sendMessage(session, YjsProtocol.buildSyncStep2(u.getUpdate()));
        }

        byte[] vector = collabStateService.getSnapshotStateVector(docId);
        byte[] vectorToSend = (vector != null) ? vector : new byte[]{0};
        sendMessage(session, YjsProtocol.buildSyncStep1(vectorToSend));

        if (Boolean.TRUE.equals(readOnly)) {
            log.info("Viewer {} connected to doc {} (read-only)", username, docId);
        } else {
            log.info("Editor {} connected to doc {}", username, docId);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        Map<String, Object> attrs = session.getAttributes();
        UUID docId = (UUID) attrs.get("docId");
        Boolean readOnly = (Boolean) attrs.get("readOnly");

        ByteBuffer buf = message.getPayload();
        if (!buf.hasRemaining()) return;

        int msgType = Varint.read(buf);

        switch (msgType) {
            case YjsProtocol.MESSAGE_SYNC -> handleSync(docId, session, readOnly, buf);
            case YjsProtocol.MESSAGE_AWARENESS -> handleAwareness(docId, session, buf);
            case YjsProtocol.MESSAGE_QUERY_AWARENESS -> {
                // Awareness is local-relay only; no server-side storage.
            }
            default -> log.debug("Unknown message type {} from {}", msgType, session.getId());
        }
    }

    private void handleSync(UUID docId, WebSocketSession session, Boolean readOnly, ByteBuffer buf) {
        int syncType = Varint.read(buf);
        int len = Varint.read(buf);

        byte[] payload = new byte[len];
        buf.get(payload);

        switch (syncType) {
            case YjsProtocol.SYNC_STEP1 -> {
                byte[] snapshot = collabStateService.getSnapshot(docId);
                if (snapshot != null && snapshot.length > 0) {
                    sendMessage(session, YjsProtocol.buildSyncStep2(snapshot));
                }
                for (CollabUpdate u : collabStateService.getBufferedUpdates(docId)) {
                    sendMessage(session, YjsProtocol.buildSyncStep2(u.getUpdate()));
                }
                log.debug("Handled sync-step1 from {} (full-send, no engine diff)", session.getId());
            }
            case YjsProtocol.SYNC_STEP2, YjsProtocol.SYNC_UPDATE -> {
                if (Boolean.TRUE.equals(readOnly)) {
                    log.debug("Rejected write from read-only session {}", session.getId());
                    return;
                }
                boolean isNew = collabStateService.applyUpdate(docId, payload);
                if (isNew) {
                    byte[] broadcastMsg = YjsProtocol.buildSyncStep2(payload);
                    publishUpdate(docId, session, broadcastMsg);
                    log.debug("Relayed update ({} bytes) from {}", payload.length, session.getId());
                }
            }
            default -> log.debug("Unknown sync type {} from {}", syncType, session.getId());
        }
    }

    private void handleAwareness(UUID docId, WebSocketSession session, ByteBuffer buf) {
        int len = Varint.read(buf);
        byte[] payload = new byte[len];
        buf.get(payload);
        byte[] broadcastMsg = YjsProtocol.buildAwareness(payload);
        broadcastToLocalOthers(docId, session, broadcastMsg);
    }

    private void publishUpdate(UUID docId, WebSocketSession sender, byte[] messageBytes) {
        String payload = sender.getId() + "\n" + Base64.getEncoder().encodeToString(messageBytes);
        boolean published = false;
        try {
            published = redisBroadcaster.publishString(docId, payload);
        } catch (Exception e) {
            log.warn("Redis publish failed for doc {}: {}", docId, e.getMessage());
        }
        if (!published) {
            broadcastToLocalOthers(docId, sender, messageBytes);
        }
    }

    private void onRedisMessage(UUID docId, byte[] raw) {
        String payload = new String(raw, StandardCharsets.UTF_8);
        int sep = payload.indexOf('\n');
        if (sep < 0) return;
        String senderId = payload.substring(0, sep);
        String messageBase64 = payload.substring(sep + 1);
        byte[] messageBytes;
        try {
            messageBytes = Base64.getDecoder().decode(messageBase64);
        } catch (Exception e) {
            log.warn("Failed to decode Redis message for doc {}: {}", docId, e.getMessage());
            return;
        }
        for (WebSocketSession s : sessionRegistry.getSessions(docId)) {
            if (!s.getId().equals(senderId) && s.isOpen()) {
                sendMessage(s, messageBytes);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Map<String, Object> attrs = session.getAttributes();
        UUID docId = (UUID) attrs.get("docId");
        String username = (String) attrs.get("username");

        sessionRegistry.remove(docId, session);
        log.info("Session closed: {} (doc={}, status={})", username, docId, status);

        if (sessionRegistry.getSessionCount(docId) == 0) {
            redisBroadcaster.unsubscribe(docId);
        }
    }

    private void sendMessage(WebSocketSession session, byte[] data) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new BinaryMessage(data));
            }
        } catch (Exception e) {
            log.error("Failed to send to {}: {}", session.getId(), e.getMessage());
        }
    }

    private void broadcastToLocalOthers(UUID docId, WebSocketSession sender, byte[] data) {
        for (WebSocketSession s : sessionRegistry.getSessions(docId)) {
            if (!s.getId().equals(sender.getId()) && s.isOpen()) {
                sendMessage(s, data);
            }
        }
    }
}