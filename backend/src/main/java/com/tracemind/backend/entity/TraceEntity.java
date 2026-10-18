package com.tracemind.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "traces", indexes = {
        @Index(name = "idx_traces_start_time", columnList = "start_time"),
        @Index(name = "idx_traces_status", columnList = "status"),
        @Index(name = "idx_traces_root_service", columnList = "root_service")
})
public class TraceEntity {

    @Id
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "root_service", length = 128)
    private String rootService;

    @Column(name = "root_operation", length = 256)
    private String rootOperation;

    @Column(nullable = false, length = 32)
    private String status = "OK"; // OK, ERROR, UNSET

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_ms", nullable = false)
    private double durationMs;

    @Column(name = "span_count")
    private int spanCount = 1;

    @Column(name = "error_count")
    private int errorCount = 0;

    @Column(name = "has_divergence")
    private boolean hasDivergence = false;

    @Column(name = "is_demo")
    private boolean isDemo = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public TraceEntity() {}

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getRootService() { return rootService; }
    public void setRootService(String rootService) { this.rootService = rootService; }

    public String getRootOperation() { return rootOperation; }
    public void setRootOperation(String rootOperation) { this.rootOperation = rootOperation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public double getDurationMs() { return durationMs; }
    public void setDurationMs(double durationMs) { this.durationMs = durationMs; }

    public int getSpanCount() { return spanCount; }
    public void setSpanCount(int spanCount) { this.spanCount = spanCount; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public boolean isHasDivergence() { return hasDivergence; }
    public void setHasDivergence(boolean hasDivergence) { this.hasDivergence = hasDivergence; }

    public boolean isDemo() { return isDemo; }
    public void setDemo(boolean demo) { isDemo = demo; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
