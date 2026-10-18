package com.tracemind.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident_evidence")
public class IncidentEvidenceEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @JsonIgnore
    private IncidentEntity incident;

    @Column(name = "evidence_type", nullable = false, length = 64)
    private String evidenceType; // TRACE, SPAN, LOG, METRIC_ANOMALY

    @Column(name = "target_id", nullable = false, length = 128)
    private String targetId; // trace_id, span_id, metric name

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "baseline_value")
    private Double baselineValue;

    @Column(name = "deviation_factor")
    private Double deviationFactor;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public IncidentEvidenceEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public IncidentEntity getIncident() { return incident; }
    public void setIncident(IncidentEntity incident) { this.incident = incident; }

    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getMetricValue() { return metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }

    public Double getBaselineValue() { return baselineValue; }
    public void setBaselineValue(Double baselineValue) { this.baselineValue = baselineValue; }

    public Double getDeviationFactor() { return deviationFactor; }
    public void setDeviationFactor(Double deviationFactor) { this.deviationFactor = deviationFactor; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
