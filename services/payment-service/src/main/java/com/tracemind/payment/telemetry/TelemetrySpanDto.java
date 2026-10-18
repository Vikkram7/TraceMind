package com.tracemind.payment.telemetry;

import java.util.Map;

public class TelemetrySpanDto {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String operationName;
    private String kind;
    private String statusCode;
    private String statusMessage;
    private long startTimeUnixNano;
    private long endTimeUnixNano;
    private double durationMs;
    private Map<String, Object> attributes;

    public TelemetrySpanDto() {}

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    public String getParentSpanId() { return parentSpanId; }
    public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getOperationName() { return operationName; }
    public void setOperationName(String operationName) { this.operationName = operationName; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public long getStartTimeUnixNano() { return startTimeUnixNano; }
    public void setStartTimeUnixNano(long startTimeUnixNano) { this.startTimeUnixNano = startTimeUnixNano; }

    public long getEndTimeUnixNano() { return endTimeUnixNano; }
    public void setEndTimeUnixNano(long endTimeUnixNano) { this.endTimeUnixNano = endTimeUnixNano; }

    public double getDurationMs() { return durationMs; }
    public void setDurationMs(double durationMs) { this.durationMs = durationMs; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
