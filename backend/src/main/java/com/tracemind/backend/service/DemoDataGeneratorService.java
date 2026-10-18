package com.tracemind.backend.service;

import com.tracemind.backend.entity.SpanEntity;
import com.tracemind.backend.entity.TraceEntity;
import com.tracemind.backend.repository.SpanRepository;
import com.tracemind.backend.repository.TraceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class DemoDataGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(DemoDataGeneratorService.class);
    private final TelemetryIngestionService ingestionService;
    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;
    private final SecureRandom random = new SecureRandom();

    public DemoDataGeneratorService(
            TelemetryIngestionService ingestionService,
            TraceRepository traceRepository,
            SpanRepository spanRepository) {
        this.ingestionService = ingestionService;
        this.traceRepository = traceRepository;
        this.spanRepository = spanRepository;
    }

    @Transactional
    public Map<String, Object> generateDemoScenarioSet() {
        log.info("Generating realistic multi-service demo traces and fault scenarios");

        int healthyCount = 5;
        List<String> generatedTraceIds = new ArrayList<>();

        // 1. Generate 5 Healthy Baseline Traces
        for (int i = 0; i < healthyCount; i++) {
            String traceId = generateHex(16);
            generatedTraceIds.add(traceId);
            generateSingleTrace(traceId, false, false, 0);
        }

        // 2. Generate 1 Downstream Payment Timeout Scenario
        String timeoutTraceId = generateHex(16);
        generatedTraceIds.add(timeoutTraceId);
        generatePaymentTimeoutTrace(timeoutTraceId);

        // 3. Generate 1 Inventory DB Deadlock Scenario
        String deadlockTraceId = generateHex(16);
        generatedTraceIds.add(deadlockTraceId);
        generateInventoryDeadlockTrace(deadlockTraceId);

        // 4. Generate 1 Latency Spike Scenario
        String latencyTraceId = generateHex(16);
        generatedTraceIds.add(latencyTraceId);
        generateSingleTrace(latencyTraceId, true, false, 1600);

        // Mark generated traces as demo data
        for (String tId : generatedTraceIds) {
            traceRepository.findById(tId).ifPresent(t -> {
                t.setDemo(true);
                traceRepository.save(t);
            });
        }

        return Map.of(
                "status", "SUCCESS",
                "message", "Generated realistic multi-service demo telemetry labeled explicitly as DEMO data",
                "tracesCreated", generatedTraceIds.size(),
                "traceIds", generatedTraceIds
        );
    }

    private void generateSingleTrace(String traceId, boolean slowInventory, boolean failPayment, int extraLatencyMs) {
        Instant now = Instant.now().minusSeconds(10);
        String rootSpanId = generateHex(8);
        String paymentClientSpanId = generateHex(8);
        String paymentServerSpanId = generateHex(8);
        String inventoryClientSpanId = generateHex(8);
        String inventoryServerSpanId = generateHex(8);

        double paymentDuration = failPayment ? 3200.0 : (35.0 + random.nextInt(25));
        double inventoryDuration = slowInventory ? (1200.0 + extraLatencyMs) : (25.0 + random.nextInt(20));
        double rootDuration = paymentDuration + inventoryDuration + 15.0;

        // 1. Root Span (Order Service)
        SpanEntity root = createSpan(traceId, rootSpanId, null, "order-service",
                "POST /api/orders", "SERVER", failPayment ? "ERROR" : "OK",
                failPayment ? "Downstream payment failure" : "HTTP 200",
                now, rootDuration, failPayment);
        ingestionService.processAndPersistSpan(root);

        // 2. Client Span to Payment
        SpanEntity callPayment = createSpan(traceId, paymentClientSpanId, rootSpanId, "order-service",
                "CALL POST /api/payments", "CLIENT", failPayment ? "ERROR" : "OK",
                failPayment ? "HTTP 504 Gateway Timeout" : "HTTP 200",
                now.plusMillis(5), paymentDuration, failPayment);
        ingestionService.processAndPersistSpan(callPayment);

        // 3. Server Span in Payment Service
        SpanEntity paymentServer = createSpan(traceId, paymentServerSpanId, paymentClientSpanId, "payment-service",
                "POST /api/payments", "SERVER", failPayment ? "ERROR" : "OK",
                failPayment ? "Upstream payment gateway timeout" : "HTTP 200",
                now.plusMillis(6), paymentDuration - 2.0, failPayment);
        ingestionService.processAndPersistSpan(paymentServer);

        if (!failPayment) {
            // 4. Client Span to Inventory
            SpanEntity callInventory = createSpan(traceId, inventoryClientSpanId, rootSpanId, "order-service",
                    "CALL POST /api/inventory/reserve", "CLIENT", "OK", "HTTP 200",
                    now.plusMillis((long) paymentDuration + 10), inventoryDuration, false);
            ingestionService.processAndPersistSpan(callInventory);

            // 5. Server Span in Inventory Service
            SpanEntity inventoryServer = createSpan(traceId, inventoryServerSpanId, inventoryClientSpanId, "inventory-service",
                    "POST /api/inventory/reserve", "SERVER", "OK", "HTTP 200",
                    now.plusMillis((long) paymentDuration + 12), inventoryDuration - 2.0, false);
            ingestionService.processAndPersistSpan(inventoryServer);
        }
    }

    private void generatePaymentTimeoutTrace(String traceId) {
        generateSingleTrace(traceId, false, true, 0);
    }

    private void generateInventoryDeadlockTrace(String traceId) {
        Instant now = Instant.now().minusSeconds(5);
        String rootSpanId = generateHex(8);
        String paymentClientSpanId = generateHex(8);
        String paymentServerSpanId = generateHex(8);
        String inventoryClientSpanId = generateHex(8);
        String inventoryServerSpanId = generateHex(8);

        double paymentDuration = 45.0;
        double inventoryDuration = 1350.0;
        double rootDuration = paymentDuration + inventoryDuration + 20.0;

        // Root Span (Order Service)
        SpanEntity root = createSpan(traceId, rootSpanId, null, "order-service",
                "POST /api/orders", "SERVER", "ERROR", "HTTP 409 Conflict: Inventory deadlock",
                now, rootDuration, true);
        ingestionService.processAndPersistSpan(root);

        // Payment Call (Healthy)
        SpanEntity callPayment = createSpan(traceId, paymentClientSpanId, rootSpanId, "order-service",
                "CALL POST /api/payments", "CLIENT", "OK", "HTTP 200",
                now.plusMillis(5), paymentDuration, false);
        ingestionService.processAndPersistSpan(callPayment);

        SpanEntity paymentServer = createSpan(traceId, paymentServerSpanId, paymentClientSpanId, "payment-service",
                "POST /api/payments", "SERVER", "OK", "HTTP 200",
                now.plusMillis(6), paymentDuration - 2.0, false);
        ingestionService.processAndPersistSpan(paymentServer);

        // Inventory Call (Deadlock Anomaly)
        SpanEntity callInventory = createSpan(traceId, inventoryClientSpanId, rootSpanId, "order-service",
                "CALL POST /api/inventory/reserve", "CLIENT", "ERROR", "HTTP 409 Conflict",
                now.plusMillis(55), inventoryDuration, true);
        ingestionService.processAndPersistSpan(callInventory);

        SpanEntity inventoryServer = createSpan(traceId, inventoryServerSpanId, inventoryClientSpanId, "inventory-service",
                "POST /api/inventory/reserve", "SERVER", "ERROR",
                "Deadlock detected on row lock in relation 'inventory_reservations'",
                now.plusMillis(57), inventoryDuration - 4.0, true);
        ingestionService.processAndPersistSpan(inventoryServer);
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
        span.setAttributesJson(String.format("{\"service.name\":\"%s\",\"http.status_code\":%s,\"demo\":true}",
                service, isError ? "500" : "200"));
        return span;
    }

    private String generateHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(byteLength * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
