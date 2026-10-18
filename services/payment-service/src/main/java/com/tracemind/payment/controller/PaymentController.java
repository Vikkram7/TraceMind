package com.tracemind.payment.controller;

import com.tracemind.payment.fault.FaultConfiguration;
import com.tracemind.payment.telemetry.TraceContext;
import com.tracemind.payment.telemetry.TraceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final FaultConfiguration faultConfig;

    public PaymentController(FaultConfiguration faultConfig) {
        this.faultConfig = faultConfig;
    }

    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody(required = false) Map<String, Object> request) {
        TraceContext ctx = TraceContextHolder.get();
        String traceId = (ctx != null) ? ctx.getTraceId() : "unknown";

        log.info("Received payment processing request [traceId: {}]: {}", traceId, request);

        // Check simulated faults
        if (faultConfig.isServiceUnavailable()) {
            log.warn("PaymentService fault triggered: 503 Service Unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "PaymentService unavailable (circuit breaker open)"));
        }

        if (faultConfig.isPaymentGatewayTimeout()) {
            log.warn("PaymentService fault triggered: upstream acquiring bank gateway timeout");
            try {
                Thread.sleep(3500); // Exceeds typical microservice 3000ms SLA
            } catch (InterruptedException ignored) {}
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(Map.of("error", "Upstream payment gateway timeout communicating with acquiring network"));
        }

        if (faultConfig.getLatencyMs() > 0) {
            try {
                log.info("Injecting {} ms latency into payment processing", faultConfig.getLatencyMs());
                Thread.sleep(faultConfig.getLatencyMs());
            } catch (InterruptedException ignored) {}
        }

        if (faultConfig.getErrorRate() > 0 && Math.random() < faultConfig.getErrorRate()) {
            String msg = faultConfig.getCustomErrorMessage() != null ?
                    faultConfig.getCustomErrorMessage() : "Acquiring bank authorization declined / settlement failed";
            log.error("PaymentService fault triggered: {}", msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", msg));
        }

        String paymentId = "pay-" + UUID.randomUUID().toString().substring(0, 8);
        String authCode = "AUTH-" + (100000 + (int)(Math.random() * 900000));

        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("orderId", (request != null && request.containsKey("orderId")) ? request.get("orderId") : "unknown");
        response.put("amount", (request != null && request.containsKey("amount")) ? request.get("amount") : 0.0);
        response.put("status", "COMPLETED");
        response.put("authCode", authCode);
        response.put("traceId", traceId);

        log.info("Payment {} processed successfully with authCode {}", paymentId, authCode);
        return ResponseEntity.ok(response);
    }
}
