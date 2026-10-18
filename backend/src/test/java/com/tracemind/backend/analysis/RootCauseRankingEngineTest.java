package com.tracemind.backend.analysis;

import com.tracemind.backend.analysis.RootCauseRankingEngine.CandidateEvaluation;
import com.tracemind.backend.entity.SpanEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RootCauseRankingEngineTest {

    private RootCauseRankingEngine rankingEngine;
    private List<SpanEntity> healthyBaseline;

    @BeforeEach
    void setUp() {
        rankingEngine = new RootCauseRankingEngine();
        healthyBaseline = createHealthyBaselineSpans();
    }

    private List<SpanEntity> createHealthyBaselineSpans() {
        Instant now = Instant.now().minusSeconds(60);
        return List.of(
                createSpan("base-1", "s1", null, "order-service", "POST /api/orders", "SERVER", "OK", null, now, 80.0, false),
                createSpan("base-1", "s2", "s1", "order-service", "CALL POST /api/payments", "CLIENT", "OK", null, now.plusMillis(5), 38.0, false),
                createSpan("base-1", "s3", "s2", "payment-service", "POST /api/payments", "SERVER", "OK", null, now.plusMillis(6), 35.0, false),
                createSpan("base-1", "s4", "s1", "order-service", "CALL POST /api/inventory/reserve", "CLIENT", "OK", null, now.plusMillis(45), 28.0, false),
                createSpan("base-1", "s5", "s4", "inventory-service", "POST /api/inventory/reserve", "SERVER", "OK", null, now.plusMillis(47), 25.0, false)
        );
    }

    @Test
    @DisplayName("Scenario 1: Healthy Baseline - False positive rate must be zero")
    void testScenario1_HealthyBaseline() {
        Instant now = Instant.now();
        List<SpanEntity> healthyTrace = List.of(
                createSpan("h-1", "h1", null, "order-service", "POST /api/orders", "SERVER", "OK", null, now, 78.0, false),
                createSpan("h-1", "h2", "h1", "payment-service", "POST /api/payments", "SERVER", "OK", null, now.plusMillis(5), 34.0, false),
                createSpan("h-1", "h3", "h1", "inventory-service", "POST /api/inventory/reserve", "SERVER", "OK", null, now.plusMillis(42), 26.0, false)
        );

        List<CandidateEvaluation> ranked = rankingEngine.rankRootCauses(healthyTrace, healthyBaseline);

        // No candidate should have high anomaly score
        for (CandidateEvaluation c : ranked) {
            assertTrue(c.getFinalScore() < 0.40, "Healthy candidate should not exceed anomaly threshold: " + c.getCandidateKey());
            assertEquals(0.0, c.getErrorStatusScore(), "Healthy trace must have zero error score");
        }
    }

    @Test
    @DisplayName("Scenario 2: One Slow Downstream Service - Accurately identifies slow inventory")
    void testScenario2_SlowDownstreamService() {
        Instant now = Instant.now();
        List<SpanEntity> slowTrace = List.of(
                createSpan("t-2", "s1", null, "order-service", "POST /api/orders", "SERVER", "OK", null, now, 1680.0, false),
                createSpan("t-2", "s2", "s1", "payment-service", "POST /api/payments", "SERVER", "OK", null, now.plusMillis(5), 36.0, false),
                createSpan("t-2", "s3", "s1", "inventory-service", "POST /api/inventory/reserve", "SERVER", "OK", null, now.plusMillis(45), 1620.0, false)
        );

        List<CandidateEvaluation> ranked = rankingEngine.rankRootCauses(slowTrace, healthyBaseline);

        assertFalse(ranked.isEmpty());
        CandidateEvaluation top = ranked.get(0);

        assertEquals("inventory-service", top.getServiceName(), "Top-1 root cause must be inventory-service");
        assertEquals("POST /api/inventory/reserve", top.getOperationName());
        assertTrue(top.getLatencyAnomalyScore() >= 0.7, "Latency anomaly score must be high for dilated span");
        assertTrue(top.getFinalScore() > 0.40);
    }

    @Test
    @DisplayName("Scenario 3: Downstream Timeout - Identifies upstream payment gateway timeout vs victim caller")
    void testScenario3_DownstreamTimeout() {
        Instant now = Instant.now();
        List<SpanEntity> timeoutTrace = List.of(
                createSpan("t-3", "s1", null, "order-service", "POST /api/orders", "SERVER", "ERROR", "HTTP 504 Gateway Timeout", now, 3550.0, true),
                createSpan("t-3", "s2", "s1", "payment-service", "POST /api/payments", "SERVER", "ERROR", "Gateway timeout communicating with upstream acquiring bank", now.plusMillis(5), 3500.0, true)
        );

        List<CandidateEvaluation> ranked = rankingEngine.rankRootCauses(timeoutTrace, healthyBaseline);

        assertFalse(ranked.isEmpty());
        CandidateEvaluation top = ranked.get(0);

        // Payment Service is the initiating origin; Order Service is the victim caller
        assertEquals("payment-service", top.getServiceName(), "Top-1 cause must be payment-service where timeout initiated");
        assertEquals(1, top.getRank());
        assertTrue(top.getErrorStatusScore() > 0);
    }

    @Test
    @DisplayName("Scenario 4: Increased Error Rate - Detects recurrent failure across traces")
    void testScenario4_IncreasedErrorRate() {
        Instant now = Instant.now();
        List<SpanEntity> multiTraceFails = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            String tId = "fail-trace-" + i;
            multiTraceFails.add(createSpan(tId, "ord-" + i, null, "order-service", "POST /api/orders", "SERVER", "ERROR", "Internal Error", now.plusSeconds(i), 90.0, true));
            multiTraceFails.add(createSpan(tId, "pay-" + i, "ord-" + i, "payment-service", "POST /api/payments", "SERVER", "OK", null, now.plusSeconds(i).plusMillis(5), 35.0, false));
            multiTraceFails.add(createSpan(tId, "inv-" + i, "ord-" + i, "inventory-service", "POST /api/inventory/reserve", "SERVER", "ERROR", "Deadlock detected on row lock", now.plusSeconds(i).plusMillis(45), 40.0, true));
        }

        List<CandidateEvaluation> ranked = rankingEngine.rankRootCauses(multiTraceFails, healthyBaseline);

        CandidateEvaluation top = ranked.get(0);
        assertEquals("inventory-service", top.getServiceName());
        assertEquals("POST /api/inventory/reserve", top.getOperationName());
        assertEquals(1.0, top.getErrorStatusScore());
        assertEquals(1.0, top.getRecurrenceScore(), "Recurrence score must be 1.0 across 5 failed traces");
    }

    @Test
    @DisplayName("Scenario 5: Competing Causes - Causal precedence distinguishes initiator from cascading victim")
    void testScenario5_CompetingCauses() {
        Instant now = Instant.now();
        List<SpanEntity> competingTrace = List.of(
                createSpan("t-5", "s1", null, "order-service", "POST /api/orders", "SERVER", "ERROR", "HTTP 500", now, 1400.0, true),
                createSpan("t-5", "s2", "s1", "inventory-service", "POST /api/inventory/reserve", "SERVER", "ERROR", "Deadlock row lock", now.plusMillis(10), 1200.0, true),
                createSpan("t-5", "s3", "s1", "payment-service", "POST /api/payments", "SERVER", "ERROR", "Compensation rollback timeout", now.plusMillis(1220), 150.0, true)
        );

        List<CandidateEvaluation> ranked = rankingEngine.rankRootCauses(competingTrace, healthyBaseline);

        assertEquals(3, ranked.size());
        assertEquals("inventory-service", ranked.get(0).getServiceName(), "Inventory failed first and must rank #1");
        assertNotNull(ranked.get(0).getAlternativeHypothesis(), "Must report secondary competing candidate in hypothesis");
        assertTrue(ranked.get(0).getAlternativeHypothesis().contains("payment-service"));
    }

    @Test
    @DisplayName("Scenario 6: Incomplete Telemetry - Engine reduces confidence when spans are unobserved")
    void testScenario6_IncompleteTelemetry() {
        Instant now = Instant.now();
        // Trace missing the parent span (unobserved root)
        List<SpanEntity> incompleteSpans = List.of(
                createSpan("t-6", "orphan-span", "missing-parent-id", "inventory-service", "POST /api/inventory/reserve", "SERVER", "ERROR", "Failure with unobserved parent", now, 500.0, true)
        );

        List<CandidateEvaluation> ranked = rankingEngine.rankRootCauses(incompleteSpans, healthyBaseline);

        assertFalse(ranked.isEmpty());
        CandidateEvaluation top = ranked.get(0);
        assertTrue(top.getCompletenessPenalty() > 0, "Completeness penalty must be triggered for missing parent");
        assertTrue(top.getConfidence() < 0.75, "Confidence should be discounted for incomplete telemetry");
        assertTrue(top.getCounterevidence().stream().anyMatch(c -> c.contains("Incomplete telemetry")));
    }

    @Test
    @DisplayName("Benchmark: TraceMind Multi-Factor vs Naive Threshold Baseline")
    void testBenchmarkComparison() {
        // Evaluation on Scenario 3 (Downstream timeout):
        // In this scenario: Order Service duration = 3550ms; Payment Service duration = 3500ms.
        Instant now = Instant.now();
        List<SpanEntity> timeoutTrace = List.of(
                createSpan("t-bench", "s1", null, "order-service", "POST /api/orders", "SERVER", "ERROR", "HTTP 504", now, 3550.0, true),
                createSpan("t-bench", "s2", "s1", "payment-service", "POST /api/payments", "SERVER", "ERROR", "Bank Gateway Timeout", now.plusMillis(5), 3500.0, true)
        );

        // Naive Baseline: picks candidate with max duration
        String naiveTop1 = timeoutTrace.stream()
                .max(Comparator.comparingDouble(SpanEntity::getDurationMs))
                .map(SpanEntity::getServiceName)
                .orElse("none");

        // TraceMind Multi-Factor: evaluates temporal precedence, blast radius, error status
        List<CandidateEvaluation> tracemindRanked = rankingEngine.rankRootCauses(timeoutTrace, healthyBaseline);
        String tracemindTop1 = tracemindRanked.get(0).getServiceName();

        // Naive fails because caller duration wraps downstream duration
        assertEquals("order-service", naiveTop1, "Naive baseline incorrectly blames the parent caller");
        assertEquals("payment-service", tracemindTop1, "TraceMind correctly isolates downstream root origin");
    }

    private SpanEntity createSpan(
            String traceId, String spanId, String parentSpanId,
            String service, String operation, String kind, String status, String statusMsg,
            Instant startTime, double durationMs, boolean isError) {

        SpanEntity span = new SpanEntity();
        span.setTraceId(traceId);
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setServiceName(service);
        span.setOperationName(operation);
        span.setKind(kind);
        span.setStatusCode(status);
        span.setStatusMessage(statusMsg);
        span.setStartTime(startTime);
        span.setEndTime(startTime.plusMillis((long) durationMs));
        span.setDurationMs(durationMs);
        span.setError(isError);
        return span;
    }
}
