package com.tracemind.inventory.controller;

import com.tracemind.inventory.fault.FaultConfiguration;
import com.tracemind.inventory.telemetry.TraceContext;
import com.tracemind.inventory.telemetry.TraceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final FaultConfiguration faultConfig;

    public InventoryController(FaultConfiguration faultConfig) {
        this.faultConfig = faultConfig;
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveInventory(@RequestBody(required = false) Map<String, Object> request) {
        TraceContext ctx = TraceContextHolder.get();
        String traceId = (ctx != null) ? ctx.getTraceId() : "unknown";

        log.info("Received inventory reservation request [traceId: {}]: {}", traceId, request);

        if (faultConfig.isServiceUnavailable()) {
            log.warn("InventoryService fault: 503 Service Unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Inventory service temporarily unavailable"));
        }

        if (faultConfig.isDbDeadlock()) {
            log.error("InventoryService fault: DB lock timeout / deadlock on inventory SKU allocation");
            try {
                Thread.sleep(1200); // Simulate DB lock acquisition wait before timeout
            } catch (InterruptedException ignored) {}
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Deadlock detected on row lock in relation 'inventory_reservations'",
                            "exception", "org.springframework.dao.CannotAcquireLockException"
                    ));
        }

        if (faultConfig.getLatencyMs() > 0) {
            try {
                log.info("Injecting {} ms latency into inventory reservation SQL query", faultConfig.getLatencyMs());
                Thread.sleep(faultConfig.getLatencyMs());
            } catch (InterruptedException ignored) {}
        }

        if (faultConfig.getErrorRate() > 0 && Math.random() < faultConfig.getErrorRate()) {
            String msg = faultConfig.getCustomErrorMessage() != null ?
                    faultConfig.getCustomErrorMessage() : "Stock allocation failed: insufficient items in warehouse partition";
            log.error("InventoryService fault: {}", msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", msg));
        }

        String reservationId = "res-" + UUID.randomUUID().toString().substring(0, 8);
        int itemsCount = 1;
        if (request != null && request.get("items") instanceof List<?> list) {
            itemsCount = list.size();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("reservationId", reservationId);
        response.put("orderId", (request != null && request.containsKey("orderId")) ? request.get("orderId") : "unknown");
        response.put("status", "RESERVED");
        response.put("itemsReserved", itemsCount);
        response.put("warehouse", "WH-CENTRAL-01");
        response.put("traceId", traceId);

        log.info("Inventory reserved successfully [reservationId: {}] for order {}", reservationId, response.get("orderId"));
        return ResponseEntity.ok(response);
    }
}
