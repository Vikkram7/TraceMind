package com.tracemind.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "spans", indexes = {
        @Index(name = "idx_spans_trace_id", columnList = "trace_id"),
        @Index(name = "idx_spans_service_name", columnList = "service_name"),
        @Index(name = "idx_spans_start_time", columnList = "start_time"),
        @Index(name = "idx_spans_is_error", columnList = "is_error")
})
public class SpanEntity {

    @Id
    @Column(name = "span_id", length = 64)
    private String spanId;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "parent_span_id", length = 64)
    private String parentSpanId;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(name = "operation_name", nullable = false, length = 256)
    private String operationName;

    @Column(length = 32)
    private String kind = "INTERNAL"; // SERVER, CLIENT, PRODUCER, CONSUMER, INTERNAL

    @Column(name = "status_code", length = 32)
    private String statusCode = "OK"; // OK, ERROR, UNSET

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_ms", nullable = false)
    private double durationMs;

    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    @Column(name = "events_json", columnDefinition = "TEXT")
    private String eventsJson;

    @Column(name = "is_error")
    private boolean isError = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public SpanEntity() {}

    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

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

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public double getDurationMs() { return durationMs; }
    public void setDurationMs(double durationMs) { this.durationMs = durationMs; }

    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }

    public String getEventsJson() { return eventsJson; }
    public void setEventsJson(String eventsJson) { this.eventsJson = eventsJson; }

    public boolean isError() { return isError; }
    public void setError(boolean error) { isError = error; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
