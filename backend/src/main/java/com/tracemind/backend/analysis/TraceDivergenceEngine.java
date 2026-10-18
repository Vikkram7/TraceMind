package com.tracemind.backend.analysis;

import com.tracemind.backend.entity.SpanEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TraceDivergenceEngine {

    public static class DivergencePoint {
        private final String divergenceType; // STATUS_MISMATCH, LATENCY_DIVERGENCE, CALL_MISSING, TOPOLOGY_DRIFT
        private final String serviceName;
        private final String operationName;
        private final String baselineSpanId;
        private final String targetSpanId;
        private final double baselineDurationMs;
        private final double targetDurationMs;
        private final double durationDeltaMs;
        private final String baselineStatus;
        private final String targetStatus;
        private final String explanation;
        private final boolean isEarliestDivergence;

        public DivergencePoint(
                String divergenceType,
                String serviceName,
                String operationName,
                String baselineSpanId,
                String targetSpanId,
                double baselineDurationMs,
                double targetDurationMs,
                String baselineStatus,
                String targetStatus,
                String explanation,
                boolean isEarliestDivergence) {
            this.divergenceType = divergenceType;
            this.serviceName = serviceName;
            this.operationName = operationName;
            this.baselineSpanId = baselineSpanId;
            this.targetSpanId = targetSpanId;
            this.baselineDurationMs = baselineDurationMs;
            this.targetDurationMs = targetDurationMs;
            this.durationDeltaMs = targetDurationMs - baselineDurationMs;
            this.baselineStatus = baselineStatus;
            this.targetStatus = targetStatus;
            this.explanation = explanation;
            this.isEarliestDivergence = isEarliestDivergence;
        }

        public String getDivergenceType() { return divergenceType; }
        public String getServiceName() { return serviceName; }
        public String getOperationName() { return operationName; }
        public String getBaselineSpanId() { return baselineSpanId; }
        public String getTargetSpanId() { return targetSpanId; }
        public double getBaselineDurationMs() { return baselineDurationMs; }
        public double getTargetDurationMs() { return targetDurationMs; }
        public double getDurationDeltaMs() { return durationDeltaMs; }
        public String getBaselineStatus() { return baselineStatus; }
        public String getTargetStatus() { return targetStatus; }
        public String getExplanation() { return explanation; }
        public boolean isEarliestDivergence() { return isEarliestDivergence; }
    }

    public static class DivergenceReport {
        private final String baselineTraceId;
        private final String targetTraceId;
        private final List<DivergencePoint> divergencePoints = new ArrayList<>();
        private DivergencePoint earliestDivergence;
        private final List<String> limitations = new ArrayList<>();

        public DivergenceReport(String baselineTraceId, String targetTraceId) {
            this.baselineTraceId = baselineTraceId;
            this.targetTraceId = targetTraceId;
        }

        public String getBaselineTraceId() { return baselineTraceId; }
        public String getTargetTraceId() { return targetTraceId; }
        public List<DivergencePoint> getDivergencePoints() { return divergencePoints; }
        public DivergencePoint getEarliestDivergence() { return earliestDivergence; }
        public void setEarliestDivergence(DivergencePoint earliest) { this.earliestDivergence = earliest; }
        public List<String> getLimitations() { return limitations; }
        public void addDivergence(DivergencePoint point) { divergencePoints.add(point); }
        public void addLimitation(String limitation) { limitations.add(limitation); }
    }

    /**
     * Compares a baseline (healthy/fast) trace against a target (failing/slow) trace.
     * Identifies the earliest meaningful point of divergence in execution.
     */
    public DivergenceReport compareTraces(List<SpanEntity> baselineSpans, List<SpanEntity> targetSpans) {
        String baselineId = baselineSpans.isEmpty() ? "none" : baselineSpans.get(0).getTraceId();
        String targetId = targetSpans.isEmpty() ? "none" : targetSpans.get(0).getTraceId();
        DivergenceReport report = new DivergenceReport(baselineId, targetId);

        if (baselineSpans.isEmpty() || targetSpans.isEmpty()) {
            report.addLimitation("Insufficient spans: one or both traces contain zero recorded spans.");
            return report;
        }

        // Map spans by normalized key: serviceName + ":" + operationName
        Map<String, SpanEntity> baselineMap = new LinkedHashMap<>();
        for (SpanEntity span : baselineSpans) {
            baselineMap.put(normalizeKey(span), span);
        }

        Map<String, SpanEntity> targetMap = new LinkedHashMap<>();
        for (SpanEntity span : targetSpans) {
            targetMap.put(normalizeKey(span), span);
        }

        List<DivergencePoint> detected = new ArrayList<>();

        // Sort target spans by start time to evaluate temporal progression
        List<SpanEntity> sortedTarget = new ArrayList<>(targetSpans);
        sortedTarget.sort(Comparator.comparing(SpanEntity::getStartTime));

        for (SpanEntity targetSpan : sortedTarget) {
            String key = normalizeKey(targetSpan);
            SpanEntity baseSpan = baselineMap.get(key);

            if (baseSpan == null) {
                // Topology drift or unexpected extra call in target trace
                detected.add(new DivergencePoint(
                        "TOPOLOGY_DRIFT",
                        targetSpan.getServiceName(),
                        targetSpan.getOperationName(),
                        null,
                        targetSpan.getSpanId(),
                        0.0,
                        targetSpan.getDurationMs(),
                        "ABSENT",
                        targetSpan.getStatusCode(),
                        String.format("Operation '%s' occurred in target trace but was not present in baseline", targetSpan.getOperationName()),
                        false
                ));
                continue;
            }

            // 1. Check Status Mismatch (OK vs ERROR)
            if (!baseSpan.getStatusCode().equalsIgnoreCase(targetSpan.getStatusCode()) || targetSpan.isError()) {
                detected.add(new DivergencePoint(
                        "STATUS_MISMATCH",
                        targetSpan.getServiceName(),
                        targetSpan.getOperationName(),
                        baseSpan.getSpanId(),
                        targetSpan.getSpanId(),
                        baseSpan.getDurationMs(),
                        targetSpan.getDurationMs(),
                        baseSpan.getStatusCode(),
                        targetSpan.getStatusCode(),
                        String.format("Operation '%s' in %s failed with status %s (baseline was %s)",
                                targetSpan.getOperationName(), targetSpan.getServiceName(), targetSpan.getStatusCode(), baseSpan.getStatusCode()),
                        false
                ));
            }

            // 2. Check Latency Divergence (>2x baseline duration and >150ms delta)
            double durationDelta = targetSpan.getDurationMs() - baseSpan.getDurationMs();
            if (durationDelta > 150.0 && targetSpan.getDurationMs() > (baseSpan.getDurationMs() * 2.0)) {
                detected.add(new DivergencePoint(
                        "LATENCY_DIVERGENCE",
                        targetSpan.getServiceName(),
                        targetSpan.getOperationName(),
                        baseSpan.getSpanId(),
                        targetSpan.getSpanId(),
                        baseSpan.getDurationMs(),
                        targetSpan.getDurationMs(),
                        baseSpan.getStatusCode(),
                        targetSpan.getStatusCode(),
                        String.format("Severe latency dilation on '%s': %.1f ms vs baseline %.1f ms (+%.1f ms)",
                                targetSpan.getOperationName(), targetSpan.getDurationMs(), baseSpan.getDurationMs(), durationDelta),
                        false
                ));
            }
        }

        // Check for missing calls in target that existed in baseline
        for (Map.Entry<String, SpanEntity> entry : baselineMap.entrySet()) {
            if (!targetMap.containsKey(entry.getKey())) {
                SpanEntity baseSpan = entry.getValue();
                detected.add(new DivergencePoint(
                        "CALL_MISSING",
                        baseSpan.getServiceName(),
                        baseSpan.getOperationName(),
                        baseSpan.getSpanId(),
                        null,
                        baseSpan.getDurationMs(),
                        0.0,
                        baseSpan.getStatusCode(),
                        "NOT_CALLED",
                        String.format("Operation '%s' in %s was expected from baseline but never executed in target (likely bypassed due to prior failure)",
                                baseSpan.getOperationName(), baseSpan.getServiceName()),
                        false
                ));
            }
        }

        if (!detected.isEmpty()) {
            // First item in temporal order is candidate earliest divergence
            DivergencePoint earliest = detected.get(0);
            DivergencePoint earliestWithFlag = new DivergencePoint(
                    earliest.getDivergenceType(),
                    earliest.getServiceName(),
                    earliest.getOperationName(),
                    earliest.getBaselineSpanId(),
                    earliest.getTargetSpanId(),
                    earliest.getBaselineDurationMs(),
                    earliest.getTargetDurationMs(),
                    earliest.getBaselineStatus(),
                    earliest.getTargetStatus(),
                    earliest.getExplanation(),
                    true
            );
            report.setEarliestDivergence(earliestWithFlag);
            report.addDivergence(earliestWithFlag);
            for (int i = 1; i < detected.size(); i++) {
                report.addDivergence(detected.get(i));
            }
        } else {
            report.addLimitation("No significant status or duration divergence detected between baseline and target traces.");
        }

        return report;
    }

    private String normalizeKey(SpanEntity span) {
        return span.getServiceName().toLowerCase() + "::" + span.getOperationName().toLowerCase();
    }
}
