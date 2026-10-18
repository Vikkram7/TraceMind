package com.tracemind.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "service_dependencies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_dependency", columnNames = {"source_service", "target_service"})
})
public class ServiceDependencyEntity {

    @Id
    @Column(length = 128)
    private String id;

    @Column(name = "source_service", nullable = false, length = 128)
    private String sourceService;

    @Column(name = "target_service", nullable = false, length = 128)
    private String targetService;

    @Column(name = "call_count")
    private long callCount = 0;

    @Column(name = "error_count")
    private long errorCount = 0;

    @Column(name = "avg_duration_ms")
    private double avgDurationMs = 0.0;

    @Column(name = "p95_duration_ms")
    private double p95DurationMs = 0.0;

    @Column(name = "last_observed")
    private Instant lastObserved = Instant.now();

    public ServiceDependencyEntity() {}

    public ServiceDependencyEntity(String sourceService, String targetService) {
        this.id = sourceService + "->" + targetService;
        this.sourceService = sourceService;
        this.targetService = targetService;
        this.lastObserved = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }

    public String getTargetService() { return targetService; }
    public void setTargetService(String targetService) { this.targetService = targetService; }

    public long getCallCount() { return callCount; }
    public void setCallCount(long callCount) { this.callCount = callCount; }

    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }

    public double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(double avgDurationMs) { this.avgDurationMs = avgDurationMs; }

    public double getP95DurationMs() { return p95DurationMs; }
    public void setP95DurationMs(double p95DurationMs) { this.p95DurationMs = p95DurationMs; }

    public Instant getLastObserved() { return lastObserved; }
    public void setLastObserved(Instant lastObserved) { this.lastObserved = lastObserved; }
}
