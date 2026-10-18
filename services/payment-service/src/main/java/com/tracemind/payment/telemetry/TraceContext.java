package com.tracemind.payment.telemetry;

import java.util.HashMap;
import java.util.Map;

public class TraceContext {
    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final long startTimeUnixNano;
    private final String serviceName;
    private final Map<String, Object> attributes = new HashMap<>();

    public TraceContext(String traceId, String spanId, String parentSpanId, long startTimeUnixNano, String serviceName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.startTimeUnixNano = startTimeUnixNano;
        this.serviceName = serviceName;
    }

    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public long getStartTimeUnixNano() { return startTimeUnixNano; }
    public String getServiceName() { return serviceName; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }
}
