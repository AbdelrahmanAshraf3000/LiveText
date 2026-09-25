package com.example.backend.collab;

/**
 * y-websocket message types — matches the reference y-websocket server.
 */
public final class YjsProtocol {

    private YjsProtocol() {
    }

    public static final int MESSAGE_SYNC = 0;
    public static final int MESSAGE_AWARENESS = 1;
    public static final int MESSAGE_AUTH = 2;
    public static final int MESSAGE_QUERY_AWARENESS = 3;

    // Sync subtypes
    public static final int SYNC_STEP1 = 0;
    public static final int SYNC_STEP2 = 1;
    public static final int SYNC_UPDATE = 2;

    // WebSocket close codes
    public static final int CLOSE_OK = 1000;
    public static final int CLOSE_UNAUTHORIZED = 4001;
    public static final int CLOSE_FORBIDDEN = 4003;
    public static final int CLOSE_NOT_FOUND = 4004;
    public static final int CLOSE_ERROR = 4500;

    /**
     * Build a sync-step2 message (send update bytes to a client).
     */
    public static byte[] buildSyncStep2(byte[] update) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(64 + update.length);
        Varint.write(out, MESSAGE_SYNC);
        Varint.write(out, SYNC_STEP2);
        Varint.write(out, update.length);
        out.writeBytes(update);
        return out.toByteArray();
    }

    /**
     * Build a sync-step1 message (send state vector to a client).
     */
    public static byte[] buildSyncStep1(byte[] stateVector) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(64 + stateVector.length);
        Varint.write(out, MESSAGE_SYNC);
        Varint.write(out, SYNC_STEP1);
        Varint.write(out, stateVector.length);
        out.writeBytes(stateVector);
        return out.toByteArray();
    }

    /**
     * Build an awareness message (relay awareness to other clients).
     */
    public static byte[] buildAwareness(byte[] awarenessUpdate) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(64 + awarenessUpdate.length);
        Varint.write(out, MESSAGE_AWARENESS);
        Varint.write(out, awarenessUpdate.length);
        out.writeBytes(awarenessUpdate);
        return out.toByteArray();
    }

    /**
     * Build a query-awareness response (send current awareness).
     */
    public static byte[] buildQueryAwarenessResponse(byte[] awarenessUpdate) {
        return buildAwareness(awarenessUpdate);
    }
}