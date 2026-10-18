package com.tracemind.inventory.fault;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/faults")
public class FaultController {

    private final FaultConfiguration faultConfig;
    private final Environment environment;

    public FaultController(FaultConfiguration faultConfig, Environment environment) {
        this.faultConfig = faultConfig;
        this.environment = environment;
    }

    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("production") ||
               Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    @GetMapping
    public ResponseEntity<?> getFaultConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "inventory-service");
        response.put("latencyMs", faultConfig.getLatencyMs());
        response.put("errorRate", faultConfig.getErrorRate());
        response.put("serviceUnavailable", faultConfig.isServiceUnavailable());
        response.put("dbDeadlock", faultConfig.isDbDeadlock());
        response.put("customErrorMessage", faultConfig.getCustomErrorMessage());
        response.put("faultInjectionAllowed", !isProduction());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/configure")
    public ResponseEntity<?> configureFaults(@RequestBody Map<String, Object> payload) {
        if (isProduction()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Fault injection is disabled in production environments"));
        }

        if (payload.containsKey("latencyMs")) {
            faultConfig.setLatencyMs(((Number) payload.get("latencyMs")).intValue());
        }
        if (payload.containsKey("errorRate")) {
            faultConfig.setErrorRate(((Number) payload.get("errorRate")).doubleValue());
        }
        if (payload.containsKey("serviceUnavailable")) {
            faultConfig.setServiceUnavailable(Boolean.parseBoolean(payload.get("serviceUnavailable").toString()));
        }
        if (payload.containsKey("dbDeadlock")) {
            faultConfig.setDbDeadlock(Boolean.parseBoolean(payload.get("dbDeadlock").toString()));
        }
        if (payload.containsKey("customErrorMessage")) {
            faultConfig.setCustomErrorMessage((String) payload.get("customErrorMessage"));
        }

        return getFaultConfiguration();
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetFaults() {
        faultConfig.reset();
        return ResponseEntity.ok(Map.of("message", "Fault configuration reset to baseline"));
    }
}
