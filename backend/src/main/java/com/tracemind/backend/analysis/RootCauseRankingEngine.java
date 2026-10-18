package com.tracemind.backend.analysis;

import com.tracemind.backend.entity.SpanEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RootCauseRankingEngine {

    public static class CandidateEvaluation {
        private String candidateKey;
        private String serviceName;
        private String operationName;
        private String primarySpanId;
        private double finalScore; // 0.0 to 1.0 (calibrated multi-factor index)
        private double confidence; // 0.0 to 1.0
        private int rank;

        // Factor Scores (each normalized 0.0 to 1.0)
        private double temporalPrecedenceScore;
        private double errorStatusScore;
        private double latencyAnomalyScore;
        private double blastRadiusScore;
        private double recurrenceScore;
        private double counterevidenceScore;
        private double completenessPenalty;
        private double callerPenalty;

        private final List<String> supportingObservations = new ArrayList<>();
        private final List<String> counterevidence = new ArrayList<>();
        private String alternativeHypothesis;

        public String getCandidateKey() { return candidateKey; }
        public void setCandidateKey(String candidateKey) { this.candidateKey = candidateKey; }

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }

        public String getOperationName() { return operationName; }
        public void setOperationName(String operationName) { this.operationName = operationName; }

        public String getPrimarySpanId() { return primarySpanId; }
        public void setPrimarySpanId(String primarySpanId) { this.primarySpanId = primarySpanId; }

        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }

        public double getTemporalPrecedenceScore() { return temporalPrecedenceScore; }
        public void setTemporalPrecedenceScore(double s) { this.temporalPrecedenceScore = s; }

        public double getErrorStatusScore() { return errorStatusScore; }
        public void setErrorStatusScore(double s) { this.errorStatusScore = s; }

        public double getLatencyAnomalyScore() { return latencyAnomalyScore; }
        public void setLatencyAnomalyScore(double s) { this.latencyAnomalyScore = s; }

        public double getBlastRadiusScore() { return blastRadiusScore; }
        public void setBlastRadiusScore(double s) { this.blastRadiusScore = s; }

        public double getRecurrenceScore() { return recurrenceScore; }
        public void setRecurrenceScore(double s) { this.recurrenceScore = s; }

        public double getCounterevidenceScore() { return counterevidenceScore; }
        public void setCounterevidenceScore(double s) { this.counterevidenceScore = s; }

        public double getCompletenessPenalty() { return completenessPenalty; }
        public void setCompletenessPenalty(double s) { this.completenessPenalty = s; }

        public double getCallerPenalty() { return callerPenalty; }
        public void setCallerPenalty(double s) { this.callerPenalty = s; }

        public List<String> getSupportingObservations() { return supportingObservations; }
        public List<String> getCounterevidence() { return counterevidence; }
        public String getAlternativeHypothesis() { return alternativeHypothesis; }
        public void setAlternativeHypothesis(String alt) { this.alternativeHypothesis = alt; }
    }

    // Configurable deterministic weights
    private static final double W_ERROR = 0.35;
    private static final double W_LATENCY = 0.30;
    private static final double W_PRECEDENCE = 0.15;
    private static final double W_BLAST_RADIUS = 0.10;
    private static final double W_RECURRENCE = 0.10;
    private static final double W_COUNTER = 0.15;
    private static final double W_COMPLETENESS = 0.15;
    private static final double W_CALLER_PENALTY = 0.35;

    /**
     * Deterministically evaluates candidate spans within an incident's failed traces,
     * comparing against historical baseline spans.
     * Incorporates distributed call tree hierarchy to prevent blaming parent caller wrappers.
     */
    public List<CandidateEvaluation> rankRootCauses(
            List<SpanEntity> incidentSpans,
            List<SpanEntity> healthyBaselineSpans) {

        if (incidentSpans.isEmpty()) {
            return Collections.emptyList();
        }

        // Group incident spans by service:operation
        Map<String, List<SpanEntity>> candidateGroups = new LinkedHashMap<>();
        for (SpanEntity span : incidentSpans) {
            String key = span.getServiceName() + "::" + span.getOperationName();
            candidateGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(span);
        }

        // Baseline latency map
        Map<String, Double> baselineLatencyMap = new HashMap<>();
        for (SpanEntity base : healthyBaselineSpans) {
            String key = base.getServiceName() + "::" + base.getOperationName();
            baselineLatencyMap.merge(key, base.getDurationMs(), (oldVal, newVal) -> (oldVal + newVal) / 2.0);
        }

        // Map spans by ID for tree traversal
        Map<String, SpanEntity> spanById = new HashMap<>();
        for (SpanEntity s : incidentSpans) {
            spanById.put(s.getSpanId(), s);
        }

        // Detect which candidate services are callers of other failing candidate services
        Set<String> upstreamCallers = new HashSet<>();
        for (SpanEntity child : incidentSpans) {
            if ((child.isError() || child.getDurationMs() > 800.0) && child.getParentSpanId() != null) {
                SpanEntity parent = spanById.get(child.getParentSpanId());
                if (parent != null && !parent.getServiceName().equalsIgnoreCase(child.getServiceName())) {
                    String parentKey = parent.getServiceName() + "::" + parent.getOperationName();
                    upstreamCallers.add(parentKey);
                }
            }
        }

        List<CandidateEvaluation> evaluations = new ArrayList<>();

        for (Map.Entry<String, List<SpanEntity>> entry : candidateGroups.entrySet()) {
            String key = entry.getKey();
            List<SpanEntity> spans = entry.getValue();
            SpanEntity representative = spans.get(0);

            CandidateEvaluation eval = new CandidateEvaluation();
            eval.setCandidateKey(key);
            eval.setServiceName(representative.getServiceName());
            eval.setOperationName(representative.getOperationName());
            eval.setPrimarySpanId(representative.getSpanId());

            // 1. Error Status Score (0.0 to 1.0)
            long errorCount = spans.stream().filter(SpanEntity::isError).count();
            double errorScore = (double) errorCount / spans.size();
            eval.setErrorStatusScore(errorScore);
            if (errorScore > 0) {
                eval.getSupportingObservations().add(String.format(
                        "Observed %d/%d execution failures with explicit error status or exceptions.",
                        errorCount, spans.size()));
            }

            // 2. Latency Anomaly Score (0.0 to 1.0) based on relative ratio & dilation
            double avgDuration = spans.stream().mapToDouble(SpanEntity::getDurationMs).average().orElse(0.0);
            Double baselineDuration = baselineLatencyMap.get(key);
            double latencyScore = 0.0;
            if (baselineDuration != null && baselineDuration > 0) {
                if (avgDuration > baselineDuration) {
                    double ratio = avgDuration / baselineDuration;
                    latencyScore = Math.min(1.0, Math.max(0.1, (ratio - 1.0) / 2.0));
                    eval.getSupportingObservations().add(String.format(
                            "Latency ratio dilated %.1fx (observed %.1f ms vs baseline %.1f ms).",
                            ratio, avgDuration, baselineDuration));
                } else {
                    eval.getCounterevidence().add(String.format(
                            "Latency (%.1f ms) was within normal baseline limits (%.1f ms).", avgDuration, baselineDuration));
                }
            } else if (avgDuration > 800.0) {
                latencyScore = Math.min(1.0, avgDuration / 1000.0);
                eval.getSupportingObservations().add(String.format(
                        "Execution duration of %.1f ms exceeds default 800 ms warning threshold.", avgDuration));
            }
            eval.setLatencyAnomalyScore(latencyScore);

            // 3. Upstream Caller vs Downstream Callee Penalty/Bonus
            boolean isCaller = upstreamCallers.contains(key);
            double callerPenalty = isCaller ? 1.0 : 0.0;
            eval.setCallerPenalty(callerPenalty);
            if (isCaller) {
                eval.getCounterevidence().add(
                        "Identified as upstream orchestrator/caller wrapping a failing downstream dependency. Causal score discounted.");
            } else {
                eval.getSupportingObservations().add(
                        "Downstream leaf/callee operation: directly executes business or storage logic without sub-delegation.");
            }

            // 4. Temporal Precedence (0.0 to 1.0)
            // Downstream callee that initiates failure takes precedence over its wrapping caller
            double precedenceScore = isCaller ? 0.3 : 1.0;
            eval.setTemporalPrecedenceScore(precedenceScore);

            // 5. Blast Radius (0.0 to 1.0)
            long downstreamImpacts = incidentSpans.stream()
                    .filter(s -> s.getParentSpanId() != null && s.isError())
                    .count();
            double blastScore = Math.min(1.0, (double) downstreamImpacts / Math.max(1, incidentSpans.size()));
            eval.setBlastRadiusScore(blastScore);

            // 6. Recurrence across failed traces (0.0 to 1.0)
            long distinctTraces = spans.stream().map(SpanEntity::getTraceId).distinct().count();
            double recurrenceScore = Math.min(1.0, (double) distinctTraces / 5.0);
            eval.setRecurrenceScore(recurrenceScore);

            // 7. Counterevidence in healthy baseline
            double counterScore = 0.0;
            if (baselineDuration != null && errorScore == 0 && latencyScore < 0.1) {
                counterScore = 0.8;
                eval.getCounterevidence().add("Candidate exhibits normal operational behavior identical to healthy baseline traces.");
            }
            eval.setCounterevidenceScore(counterScore);

            // 8. Telemetry Completeness Penalty (0.0 to 1.0)
            boolean hasMissingParents = spans.stream().anyMatch(s -> s.getParentSpanId() != null &&
                    incidentSpans.stream().noneMatch(p -> p.getSpanId().equals(s.getParentSpanId())));
            double completenessPenalty = hasMissingParents ? 0.35 : 0.0;
            eval.setCompletenessPenalty(completenessPenalty);
            if (hasMissingParents) {
                eval.getCounterevidence().add(
                        "Incomplete telemetry: parent spans for this candidate are unobserved, reducing causal certainty.");
            }

            // Compute Final Composite Score
            double composite = (W_ERROR * errorScore) +
                    (W_LATENCY * latencyScore) +
                    (W_PRECEDENCE * precedenceScore) +
                    (W_BLAST_RADIUS * blastScore) +
                    (W_RECURRENCE * recurrenceScore) -
                    (W_CALLER_PENALTY * callerPenalty) -
                    (W_COUNTER * counterScore) -
                    (W_COMPLETENESS * completenessPenalty);

            double calibratedScore = Math.max(0.01, Math.min(0.99, composite));
            double confidence = Math.max(0.50, Math.min(0.98, (1.0 - completenessPenalty) * (0.8 + 0.2 * recurrenceScore)));

            eval.setFinalScore(Math.round(calibratedScore * 1000.0) / 1000.0);
            eval.setConfidence(Math.round(confidence * 1000.0) / 1000.0);

            evaluations.add(eval);
        }

        // Sort by final score descending
        evaluations.sort(Comparator.comparingDouble(CandidateEvaluation::getFinalScore).reversed());

        for (int i = 0; i < evaluations.size(); i++) {
            CandidateEvaluation candidate = evaluations.get(i);
            candidate.setRank(i + 1);

            if (i == 0 && evaluations.size() > 1) {
                CandidateEvaluation second = evaluations.get(1);
                candidate.setAlternativeHypothesis(String.format(
                        "Alternative hypothesis: Secondary failure observed in '%s' (%s, score: %.2f) could be a contributing co-factor or parallel bottleneck.",
                        second.getCandidateKey(), second.getServiceName(), second.getFinalScore()));
            } else if (i > 0) {
                candidate.setAlternativeHypothesis(String.format(
                        "Ranked lower than primary candidate '%s' due to lower anomaly severity or caller-wrapper status.",
                        evaluations.get(0).getCandidateKey()));
            }
        }

        return evaluations;
    }
}
