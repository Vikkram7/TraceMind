package com.tracemind.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "replay_runs")
public class ReplayRunEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "scenario_name", nullable = false, length = 128)
    private String scenarioName;

    @Column(nullable = false, length = 32)
    private String status = "RUNNING"; // RUNNING, COMPLETED, FAILED

    @Column(name = "total_requests")
    private int totalRequests = 0;

    @Column(name = "successful_requests")
    private int successfulRequests = 0;

    @Column(name = "failed_requests")
    private int failedRequests = 0;

    @Column(name = "target_environment", length = 64)
    private String targetEnvironment = "sandbox";

    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "started_at")
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public ReplayRunEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }

    public int getSuccessfulRequests() { return successfulRequests; }
    public void setSuccessfulRequests(int successfulRequests) { this.successfulRequests = successfulRequests; }

    public int getFailedRequests() { return failedRequests; }
    public void setFailedRequests(int failedRequests) { this.failedRequests = failedRequests; }

    public String getTargetEnvironment() { return targetEnvironment; }
    public void setTargetEnvironment(String targetEnvironment) { this.targetEnvironment = targetEnvironment; }

    public String getConfigurationJson() { return configurationJson; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
