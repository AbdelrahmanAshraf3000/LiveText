package com.example.backend.collab;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis pub/sub broadcaster for cross-instance WebSocket fan-out.
 * <p>
 * Each document has a Redis channel "{prefix}:doc:{docId}". When a backend instance
 * receives an update from a local client, it publishes to the channel. All subscribed
 * instances (including itself) receive the message and broadcast to their local sessions.
 * <p>
 * If Redis is unavailable, publish falls back to local-only broadcast (the caller handles
 * local broadcast as a fallback).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBroadcaster {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;

    @Value("${app.redis.prefix:livetext}")
    private String prefix;

    private final Map<UUID, DocSubscription> subscriptions = new ConcurrentHashMap<>();

    private static String channel(String prefix, UUID docId) {
        return prefix + ":doc:" + docId;
    }

    /**
     * Publish a binary message to all instances subscribed to the document channel.
     * Returns false if publishing failed (caller should broadcast locally as fallback).
     */
    public boolean publish(UUID docId, byte[] message) {
        try {
            String payload = Base64.getEncoder().encodeToString(message);
            redisTemplate.convertAndSend(channel(prefix, docId), payload);
            return true;
        } catch (Exception e) {
            log.warn("Redis publish failed for doc {}: {}", docId, e.getMessage());
            return false;
        }
    }

    /**
     * Publish a raw string payload to the document channel (used for sender-ID-tagged messages).
     * Returns false if publishing failed.
     */
    public boolean publishString(UUID docId, String payload) {
        try {
            redisTemplate.convertAndSend(channel(prefix, docId), payload);
            return true;
        } catch (Exception e) {
            log.warn("Redis publish failed for doc {}: {}", docId, e.getMessage());
            return false;
        }
    }

    /**
     * Subscribe to a document channel. The callback is invoked for each message received
     * from any instance (including this one).
     */
    public void subscribe(UUID docId, java.util.function.Consumer<byte[]> callback) {
        subscriptions.computeIfAbsent(docId, id -> {
            ChannelTopic topic = new ChannelTopic(channel(prefix, id));
            MessageListener listener = (Message message, byte[] pattern) -> {
                try {
                    // Pass the raw message bytes through — the callback decodes the payload
                    // (sender-ID-tagged base64 string) according to the handler's format.
                    callback.accept(message.getBody());
                } catch (Exception e) {
                    log.warn("Failed to handle Redis message for doc {}: {}", id, e.getMessage());
                }
            };
            listenerContainer.addMessageListener(listener, topic);
            log.info("Subscribed to Redis channel for doc {}", id);
            return new DocSubscription(listener, topic);
        });
    }

    /**
     * Unsubscribe from a document channel (when last local session leaves).
     */
    public void unsubscribe(UUID docId) {
        DocSubscription sub = subscriptions.remove(docId);
        if (sub != null) {
            listenerContainer.removeMessageListener(sub.listener, sub.topic);
            log.info("Unsubscribed from Redis channel for doc {}", docId);
        }
    }

    private record DocSubscription(MessageListener listener, ChannelTopic topic) {
    }
}