package com.tracemind.backend.service;

import com.tracemind.backend.entity.IncidentEntity;
import com.tracemind.backend.entity.IncidentEvidenceEntity;
import com.tracemind.backend.entity.SpanEntity;
import com.tracemind.backend.repository.IncidentEvidenceRepository;
import com.tracemind.backend.repository.IncidentRepository;
import com.tracemind.backend.repository.SpanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class IncidentDetectionService {
    private static final Logger log = LoggerFactory.getLogger(IncidentDetectionService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEvidenceRepository evidenceRepository;
    private final SpanRepository spanRepository;
    private final RootCauseAnalysisService rcaService;

    @Value("${tracemind.detection.latency-threshold-ms:800.0}")
    private double latencyThresholdMs = 800.0;

    @Value("${tracemind.detection.error-rate-threshold:0.15}")
    private double errorRateThreshold = 0.15;

    @Value("${tracemind.detection.z-score-threshold:2.5}")
    private double zScoreThreshold = 2.5;

    public IncidentDetectionService(
            IncidentRepository incidentRepository,
            IncidentEvidenceRepository evidenceRepository,
            SpanRepository spanRepository,
            @Lazy RootCauseAnalysisService rcaService) {
        this.incidentRepository = incidentRepository;
        this.evidenceRepository = evidenceRepository;
        this.spanRepository = spanRepository;
        this.rcaService = rcaService;
    }

    @Transactional
    public void evaluateSpan(SpanEntity span) {
        // 1. Error Status Detection (Error burst / downstream failure)
        if (span.isError()) {
            handleDetectedAnomaly(
                    span,
                    "ERROR_BURST",
                    "CRITICAL",
                    String.format("Error burst detected in %s during operation %s: %s",
                            span.getServiceName(), span.getOperationName(),
                            span.getStatusMessage() != null ? span.getStatusMessage() : "Internal Error"),
                    1.0,
                    1.0,
                    0.0,
                    Double.POSITIVE_INFINITY
            );
        }

        // 2. Static Latency Spike Detection
        if (span.getDurationMs() > latencyThresholdMs) {
            double deviationFactor = span.getDurationMs() / latencyThresholdMs;
            String severity = deviationFactor >= 3.0 ? "CRITICAL" : (deviationFactor >= 1.5 ? "HIGH" : "MEDIUM");
            handleDetectedAnomaly(
                    span,
                    "LATENCY_SPIKE",
                    severity,
                    String.format("Severe latency spike of %.1f ms (exceeds static SLA threshold of %.1f ms by %.1fx) on %s",
                            span.getDurationMs(), latencyThresholdMs, deviationFactor, span.getOperationName()),
                    0.92,
                    span.getDurationMs(),
                    latencyThresholdMs,
                    deviationFactor
            );
        }

        // 3. Statistical Rolling Baseline Anomaly (Z-Score & IQR)
        evaluateStatisticalAnomaly(span);

        // 4. Timeout Cascade Detection (durations >= 3000ms or 504 status)
        if (span.getDurationMs() >= 3000.0 ||
                (span.getStatusMessage() != null && span.getStatusMessage().toLowerCase().contains("timeout"))) {
            handleDetectedAnomaly(
                    span,
                    "TIMEOUT_CASCADE",
                    "CRITICAL",
                    String.format("Timeout cascade detected in %s: operation took %.1f ms exceeding timeout SLA window",
                            span.getServiceName(), span.getDurationMs()),
                    0.98,
                    span.getDurationMs(),
                    3000.0,
                    span.getDurationMs() / 3000.0
            );
        }
    }

    private void evaluateStatisticalAnomaly(SpanEntity span) {
        List<Double> historicalDurations = spanRepository.findDurationsByServiceName(span.getServiceName());
        if (historicalDurations == null || historicalDurations.size() < 10) {
            // Insufficient baseline data for statistical Z-score
            return;
        }

        double mean = historicalDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = historicalDurations.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        if (stdDev > 5.0) { // meaningful variance
            double zScore = (span.getDurationMs() - mean) / stdDev;
            if (zScore >= zScoreThreshold) {
                handleDetectedAnomaly(
                        span,
                        "ROLLING_ANOMALY",
                        zScore >= 4.0 ? "CRITICAL" : "HIGH",
                        String.format("Statistical anomaly (Z-Score = %.2f): observed %.1f ms vs rolling mean %.1f ms (stdDev: %.1f ms)",
                                zScore, span.getDurationMs(), mean, stdDev),
                        0.95,
                        span.getDurationMs(),
                        mean,
                        zScore
                );
            }
        }
    }

    private void handleDetectedAnomaly(
            SpanEntity span,
            String detectionRule,
            String severity,
            String description,
            double confidence,
            Double metricVal,
            Double baselineVal,
            Double devFactor) {

        Optional<IncidentEntity> existingOpt = incidentRepository.findFirstByServiceNameAndDetectionRuleAndStatus(
                span.getServiceName(), detectionRule, "ACTIVE");

        IncidentEntity incident;
        if (existingOpt.isPresent()) {
            incident = existingOpt.get();
            incident.setLastSeen(Instant.now());
            incident.setImpactedTracesCount(incident.getImpactedTracesCount() + 1);
            if ("CRITICAL".equals(severity)) {
                incident.setSeverity("CRITICAL");
            }
            incident = incidentRepository.save(incident);
        } else {
            incident = new IncidentEntity();
            incident.setId("inc-" + UUID.randomUUID().toString().substring(0, 8));
            incident.setTitle(String.format("[%s] %s on %s", severity, detectionRule, span.getServiceName()));
            incident.setSeverity(severity);
            incident.setStatus("ACTIVE");
            incident.setDetectionRule(detectionRule);
            incident.setServiceName(span.getServiceName());
            incident.setOperationName(span.getOperationName());
            incident.setDescription(description);
            incident.setConfidenceScore(confidence);
            incident.setFirstSeen(Instant.now());
            incident.setLastSeen(Instant.now());
            incident.setImpactedTracesCount(1);
            incident = incidentRepository.save(incident);
        }

        // Add Evidence Reference
        IncidentEvidenceEntity evidence = new IncidentEvidenceEntity();
        evidence.setId("ev-" + UUID.randomUUID().toString().substring(0, 8));
        evidence.setIncident(incident);
        evidence.setEvidenceType("SPAN");
        evidence.setTargetId(span.getSpanId());
        evidence.setDescription(String.format("Span '%s' in trace '%s': %s",
                span.getOperationName(), span.getTraceId(), description));
        evidence.setMetricValue(metricVal);
        evidence.setBaselineValue(baselineVal);
        evidence.setDeviationFactor(devFactor);
        evidenceRepository.save(evidence);

        // Asynchronously evaluate root cause for this incident
        rcaService.analyzeIncidentAsync(incident.getId());
    }
}
