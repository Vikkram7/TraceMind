package com.tracemind.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemind.backend.entity.ReplayRunEntity;
import com.tracemind.backend.repository.ReplayRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class ReplayService {
    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

    private final ReplayRunRepository replayRunRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${tracemind.services.order-url:http://localhost:8081}")
    private String orderServiceUrl;

    @Value("${tracemind.services.payment-url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Value("${tracemind.services.inventory-url:http://localhost:8083}")
    private String inventoryServiceUrl;

    public ReplayService(ReplayRunRepository replayRunRepository, ObjectMapper objectMapper) {
        this.replayRunRepository = replayRunRepository;
        this.objectMapper = objectMapper;
    }

    public static class ReplayRequestDto {
        private String scenarioName; // HEALTHY_BASELINE, PAYMENT_TIMEOUT, INVENTORY_DEADLOCK, LATENCY_SPIKE
        private int requestCount = 5;
        private String targetEnvironment = "sandbox"; // strictly sandbox / local
        private boolean confirmed = false;

        public String getScenarioName() { return scenarioName; }
        public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

        public int getRequestCount() { return requestCount; }
        public void setRequestCount(int requestCount) { this.requestCount = requestCount; }

        public String getTargetEnvironment() { return targetEnvironment; }
        public void setTargetEnvironment(String targetEnvironment) { this.targetEnvironment = targetEnvironment; }

        public boolean isConfirmed() { return confirmed; }
        public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    }

    public static class ReplayValidationResult {
        private boolean valid;
        private String previewMessage;
        private List<String> warnings = new ArrayList<>();
        private List<Map<String, Object>> simulatedRequests = new ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getPreviewMessage() { return previewMessage; }
        public void setPreviewMessage(String previewMessage) { this.previewMessage = previewMessage; }

        public List<String> getWarnings() { return warnings; }
        public List<Map<String, Object>> getSimulatedRequests() { return simulatedRequests; }
    }

    public ReplayValidationResult previewAndValidate(ReplayRequestDto request) {
        ReplayValidationResult result = new ReplayValidationResult();

        if (request.getRequestCount() < 1 || request.getRequestCount() > 50) {
            result.setValid(false);
            result.getWarnings().add("Request count must be between 1 and 50 to prevent local sandbox exhaustion.");
            return result;
        }

        if (!"sandbox".equalsIgnoreCase(request.getTargetEnvironment()) && !"local".equalsIgnoreCase(request.getTargetEnvironment())) {
            result.setValid(false);
            result.getWarnings().add("Replay is strictly restricted to local sandbox microservices. External endpoints prohibited.");
            return result;
        }

        result.setValid(true);
        result.setPreviewMessage(String.format("Safe Sandbox Replay Preview: Will dispatch %d synthetic requests for scenario '%s' against local microservices.",
                request.getRequestCount(), request.getScenarioName()));

        for (int i = 1; i <= Math.min(3, request.getRequestCount()); i++) {
            result.getSimulatedRequests().add(Map.of(
                    "requestIndex", i,
                    "targetEndpoint", orderServiceUrl + "/api/orders",
                    "payload", Map.of("customerId", "replay-cust-" + i, "itemsCount", 2, "scenario", request.getScenarioName())
            ));
        }

        return result;
    }

    public ReplayRunEntity executeReplay(ReplayRequestDto request) {
        ReplayValidationResult validation = previewAndValidate(request);
        if (!validation.isValid() || !request.isConfirmed()) {
            throw new IllegalArgumentException("Replay validation failed or explicit user confirmation missing.");
        }

        String runId = "rep-" + UUID.randomUUID().toString().substring(0, 8);
        ReplayRunEntity run = new ReplayRunEntity();
        run.setId(runId);
        run.setScenarioName(request.getScenarioName());
        run.setStatus("RUNNING");
        run.setTotalRequests(request.getRequestCount());
        run.setTargetEnvironment(request.getTargetEnvironment());
        run.setStartedAt(Instant.now());
        try {
            run.setConfigurationJson(objectMapper.writeValueAsString(request));
        } catch (Exception ignored) {}
        run = replayRunRepository.save(run);

        // Configure targeted fault if needed
        applyScenarioFaults(request.getScenarioName());

        int success = 0;
        int failure = 0;

        for (int i = 0; i < request.getRequestCount(); i++) {
            try {
                Map<String, Object> orderBody = Map.of(
                        "customerId", "replay-cust-" + (i + 1),
                        "items", List.of(Map.of("itemId", "item-" + (100 + i), "quantity", 1, "price", 49.99)),
                        "paymentMethod", "CREDIT_CARD"
                );
                ResponseEntity<Map> resp = restTemplate.postForEntity(orderServiceUrl + "/api/orders", orderBody, Map.class);
                if (resp.getStatusCode().is2xxSuccessful()) {
                    success++;
                } else {
                    failure++;
                }
            } catch (Exception ex) {
                failure++;
            }
        }

        // Reset faults after replay completes
        resetScenarioFaults();

        run.setSuccessfulRequests(success);
        run.setFailedRequests(failure);
        run.setStatus("COMPLETED");
        run.setCompletedAt(Instant.now());
        run.setResultSummary(String.format("Completed %d requests: %d succeeded, %d failed under scenario '%s'.",
                request.getRequestCount(), success, failure, request.getScenarioName()));

        return replayRunRepository.save(run);
    }

    private void applyScenarioFaults(String scenario) {
        try {
            if ("PAYMENT_TIMEOUT".equalsIgnoreCase(scenario)) {
                restTemplate.postForLocation(paymentServiceUrl + "/api/faults/configure",
                        Map.of("paymentGatewayTimeout", true));
            } else if ("INVENTORY_DEADLOCK".equalsIgnoreCase(scenario)) {
                restTemplate.postForLocation(inventoryServiceUrl + "/api/faults/configure",
                        Map.of("dbDeadlock", true));
            } else if ("LATENCY_SPIKE".equalsIgnoreCase(scenario)) {
                restTemplate.postForLocation(inventoryServiceUrl + "/api/faults/configure",
                        Map.of("latencyMs", 1500));
            }
        } catch (Exception e) {
            log.warn("Could not apply scenario fault to local services: {}", e.getMessage());
        }
    }

    private void resetScenarioFaults() {
        try {
            restTemplate.postForLocation(orderServiceUrl + "/api/faults/reset", null);
            restTemplate.postForLocation(paymentServiceUrl + "/api/faults/reset", null);
            restTemplate.postForLocation(inventoryServiceUrl + "/api/faults/reset", null);
        } catch (Exception ignored) {}
    }
}
