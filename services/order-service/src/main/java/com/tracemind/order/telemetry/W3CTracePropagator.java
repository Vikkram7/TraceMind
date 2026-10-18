package com.tracemind.order.telemetry;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class W3CTracePropagator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern TRACEPARENT_PATTERN = Pattern.compile("^00-([a-f0-9]{32})-([a-f0-9]{16})-([a-f0-9]{2})$");

    public static class TraceParent {
        private final String traceId;
        private final String parentSpanId;
        private final String flags;

        public TraceParent(String traceId, String parentSpanId, String flags) {
            this.traceId = traceId;
            this.parentSpanId = parentSpanId;
            this.flags = flags;
        }

        public String getTraceId() { return traceId; }
        public String getParentSpanId() { return parentSpanId; }
        public String getFlags() { return flags; }
    }

    public static TraceParent parse(String header) {
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = TRACEPARENT_PATTERN.matcher(header.trim().toLowerCase());
        if (matcher.matches()) {
            return new TraceParent(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        return null;
    }

    public static String format(String traceId, String spanId) {
        return "00-" + traceId + "-" + spanId + "-01";
    }

    public static String generateTraceId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    public static String generateSpanId() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
