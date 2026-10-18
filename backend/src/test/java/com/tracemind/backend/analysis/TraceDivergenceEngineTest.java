package com.tracemind.backend.analysis;

import com.tracemind.backend.analysis.TraceDivergenceEngine.DivergencePoint;
import com.tracemind.backend.analysis.TraceDivergenceEngine.DivergenceReport;
import com.tracemind.backend.entity.SpanEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TraceDivergenceEngineTest {

    private TraceDivergenceEngine divergenceEngine;

    @BeforeEach
    void setUp() {
        divergenceEngine = new TraceDivergenceEngine();
    }

    @Test
    @DisplayName("Compare identical traces - Reports zero divergence")
    void testIdenticalTraces() {
        Instant now = Instant.now();
        List<SpanEntity> base = List.of(
                createSpan("t-base", "s1", null, "order-service", "POST /api/orders", "OK", 50.0, now)
        );
        List<SpanEntity> target = List.of(
                createSpan("t-target", "s2", null, "order-service", "POST /api/orders", "OK", 52.0, now)
        );

        DivergenceReport report = divergenceEngine.compareTraces(base, target);

        assertTrue(report.getDivergencePoints().isEmpty(), "Identical traces should yield zero divergence points");
        assertNull(report.getEarliestDivergence());
    }

    @Test
    @DisplayName("Detect Status Mismatch and Earliest Divergence")
    void testStatusMismatchAndEarliestDivergence() {
        Instant now = Instant.now();
        List<SpanEntity> base = List.of(
                createSpan("t-base", "b1", null, "order-service", "POST /api/orders", "OK", 80.0, now),
                createSpan("t-base", "b2", "b1", "payment-service", "POST /api/payments", "OK", 35.0, now.plusMillis(5))
        );

        List<SpanEntity> target = List.of(
                createSpan("t-target", "t1", null, "order-service", "POST /api/orders", "ERROR", 120.0, now),
                createSpan("t-target", "t2", "t1", "payment-service", "POST /api/payments", "ERROR", 40.0, now.plusMillis(5))
        );

        DivergenceReport report = divergenceEngine.compareTraces(base, target);

        assertFalse(report.getDivergencePoints().isEmpty());
        assertNotNull(report.getEarliestDivergence());
        assertTrue(report.getEarliestDivergence().isEarliestDivergence());
    }

    @Test
    @DisplayName("Detect Latency Dilation")
    void testLatencyDilation() {
        Instant now = Instant.now();
        List<SpanEntity> base = List.of(
                createSpan("t-base", "b1", null, "inventory-service", "POST /api/inventory/reserve", "OK", 30.0, now)
        );

        List<SpanEntity> target = List.of(
                createSpan("t-target", "t1", null, "inventory-service", "POST /api/inventory/reserve", "OK", 1500.0, now)
        );

        DivergenceReport report = divergenceEngine.compareTraces(base, target);

        assertEquals(1, report.getDivergencePoints().size());
        DivergencePoint dp = report.getDivergencePoints().get(0);
        assertEquals("LATENCY_DIVERGENCE", dp.getDivergenceType());
        assertEquals(1470.0, dp.getDurationDeltaMs());
    }

    private SpanEntity createSpan(
            String traceId, String spanId, String parentSpanId,
            String service, String operation, String status, double durationMs, Instant startTime) {
        SpanEntity s = new SpanEntity();
        s.setTraceId(traceId);
        s.setSpanId(spanId);
        s.setParentSpanId(parentSpanId);
        s.setServiceName(service);
        s.setOperationName(operation);
        s.setStatusCode(status);
        s.setDurationMs(durationMs);
        s.setStartTime(startTime);
        s.setEndTime(startTime.plusMillis((long) durationMs));
        s.setError("ERROR".equalsIgnoreCase(status));
        return s;
    }
}
