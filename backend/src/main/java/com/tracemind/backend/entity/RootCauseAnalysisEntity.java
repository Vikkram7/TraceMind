package com.tracemind.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "root_cause_analyses")
public class RootCauseAnalysisEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "incident_id", nullable = false, length = 64)
    private String incidentId;

    @Column(name = "primary_candidate", nullable = false, length = 256)
    private String primaryCandidate;

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Column(name = "explanation_summary", columnDefinition = "TEXT")
    private String explanationSummary;

    @Column(name = "failure_narrative", columnDefinition = "TEXT")
    private String failureNarrative;

    @Column(name = "ranked_hypotheses_json", columnDefinition = "TEXT")
    private String rankedHypothesesJson;

    @Column(name = "suggested_actions_json", columnDefinition = "TEXT")
    private String suggestedActionsJson;

    @Column(name = "is_ai_generated")
    private boolean isAiGenerated = false;

    @Column(name = "ai_model_used", length = 64)
    private String aiModelUsed = "DETERMINISTIC_RULES_ENGINE";

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public RootCauseAnalysisEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getPrimaryCandidate() { return primaryCandidate; }
    public void setPrimaryCandidate(String primaryCandidate) { this.primaryCandidate = primaryCandidate; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getExplanationSummary() { return explanationSummary; }
    public void setExplanationSummary(String explanationSummary) { this.explanationSummary = explanationSummary; }

    public String getFailureNarrative() { return failureNarrative; }
    public void setFailureNarrative(String failureNarrative) { this.failureNarrative = failureNarrative; }

    public String getRankedHypothesesJson() { return rankedHypothesesJson; }
    public void setRankedHypothesesJson(String rankedHypothesesJson) { this.rankedHypothesesJson = rankedHypothesesJson; }

    public String getSuggestedActionsJson() { return suggestedActionsJson; }
    public void setSuggestedActionsJson(String suggestedActionsJson) { this.suggestedActionsJson = suggestedActionsJson; }

    public boolean isAiGenerated() { return isAiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }

    public String getAiModelUsed() { return aiModelUsed; }
    public void setAiModelUsed(String aiModelUsed) { this.aiModelUsed = aiModelUsed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
