package com.example.backend.collab;

import com.example.backend.entity.Role;
import com.example.backend.service.JwtService;
import com.example.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Validates JWT + loads user role for the document before the WebSocket handshake.
 * Stores userId, username, role, and docId in session attributes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YjsHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final PermissionService permissionService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            URI uri = request.getURI();
            String path = uri.getPath();
            String[] parts = path.split("/");
            String docIdStr = parts[parts.length - 1];
            UUID docId = UUID.fromString(docIdStr);

            String query = uri.getQuery();
            String token = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if ("token".equals(kv[0]) && kv.length > 1) {
                        token = kv[1];
                    }
                }
            }

            if (token == null || !jwtService.isValid(token)) {
                log.warn("WS handshake rejected: invalid token for doc {}", docId);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long userId = jwtService.getUserId(token);
            String username = jwtService.getUsername(token);
            Role role = permissionService.getRole(docId, userId);

            if (role == null) {
                log.warn("WS handshake rejected: no access for user {} to doc {}", username, docId);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put("docId", docId);
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("role", role);
            attributes.put("readOnly", !role.canEdit());

            log.info("WS handshake OK: user={}, doc={}, role={}", username, docId, role);
            return true;
        } catch (Exception e) {
            log.error("WS handshake error: {}", e.getMessage());
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}