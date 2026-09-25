package com.example.backend.collab;

import java.nio.ByteBuffer;

/**
 * lib0 encoding/decoding: unsigned LEB128 varint, matching the y-websocket wire protocol.
 */
public final class Varint {

    private Varint() {
    }

    public static int read(ByteBuffer buf) {
        int result = 0;
        int shift = 0;
        while (buf.hasRemaining()) {
            byte b = buf.get();
            result |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
            if (shift > 35) {
                throw new RuntimeException("varint too long");
            }
        }
        throw new RuntimeException("varint truncated");
    }

    public static void write(java.io.ByteArrayOutputStream out, int value) {
        while (value >= 0x80) {
            out.write((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        out.write(value & 0x7f);
    }
}