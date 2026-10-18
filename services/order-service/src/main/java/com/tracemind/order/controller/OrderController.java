package com.tracemind.order.controller;

import com.tracemind.order.fault.FaultConfiguration;
import com.tracemind.order.model.OrderModels.*;
import com.tracemind.order.telemetry.TraceContext;
import com.tracemind.order.telemetry.TraceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestTemplate restTemplate;
    private final FaultConfiguration faultConfig;

    @Value("${tracemind.services.payment-url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Value("${tracemind.services.inventory-url:http://localhost:8083}")
    private String inventoryServiceUrl;

    public OrderController(RestTemplate restTemplate, FaultConfiguration faultConfig) {
        this.restTemplate = restTemplate;
        this.faultConfig = faultConfig;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody(required = false) OrderRequest request) {
        // Apply fault injection if configured
        applyFaults();

        if (request == null) {
            request = new OrderRequest();
            request.setCustomerId("cust-" + UUID.randomUUID().toString().substring(0, 8));
            request.setItems(Collections.singletonList(new OrderItem("item-demo", 1, 99.0)));
        }

        String orderId = "ord-" + UUID.randomUUID().toString().substring(0, 8);
        TraceContext ctx = TraceContextHolder.get();
        String traceId = (ctx != null) ? ctx.getTraceId() : "unknown";

        double totalAmount = 0.0;
        if (request.getItems() != null) {
            for (OrderItem item : request.getItems()) {
                totalAmount += item.getPrice() * item.getQuantity();
            }
        }
        if (totalAmount <= 0) totalAmount = 49.99;

        OrderResponse response = new OrderResponse();
        response.setOrderId(orderId);
        response.setTraceId(traceId);
        response.setTotalAmount(totalAmount);

        log.info("Processing order {} [traceId: {}], total: ${}", orderId, traceId, totalAmount);

        // 1. Call Payment Service
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", orderId);
        paymentRequest.put("customerId", request.getCustomerId());
        paymentRequest.put("amount", totalAmount);
        paymentRequest.put("paymentMethod", request.getPaymentMethod());

        Object paymentResult = null;
        try {
            log.info("Calling Payment Service at {}/api/payments", paymentServiceUrl);
            paymentResult = restTemplate.postForObject(paymentServiceUrl + "/api/payments", paymentRequest, Map.class);
            response.setPaymentResult(paymentResult);
        } catch (HttpStatusCodeException ex) {
            log.error("Payment Service failed with status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            response.setStatus("FAILED");
            response.setMessage("Payment failed: " + ex.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        } catch (Exception ex) {
            log.error("Payment Service communication error: {}", ex.getMessage());
            response.setStatus("FAILED");
            response.setMessage("Payment downstream error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        // 2. Call Inventory Service
        Map<String, Object> inventoryRequest = new HashMap<>();
        inventoryRequest.put("orderId", orderId);
        inventoryRequest.put("items", request.getItems());

        Object inventoryResult = null;
        try {
            log.info("Calling Inventory Service at {}/api/inventory/reserve", inventoryServiceUrl);
            inventoryResult = restTemplate.postForObject(inventoryServiceUrl + "/api/inventory/reserve", inventoryRequest, Map.class);
            response.setInventoryResult(inventoryResult);
        } catch (HttpStatusCodeException ex) {
            log.error("Inventory Service failed with status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            response.setStatus("FAILED");
            response.setMessage("Inventory reservation failed: " + ex.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception ex) {
            log.error("Inventory Service communication error: {}", ex.getMessage());
            response.setStatus("FAILED");
            response.setMessage("Inventory downstream error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        response.setStatus("CONFIRMED");
        response.setMessage("Order successfully created and fulfilled");
        return ResponseEntity.ok(response);
    }

    private void applyFaults() {
        if (faultConfig.isServiceUnavailable()) {
            throw new RuntimeException("OrderService simulated 503: Service temporarily unavailable");
        }
        if (faultConfig.getLatencyMs() > 0) {
            try {
                Thread.sleep(faultConfig.getLatencyMs());
            } catch (InterruptedException ignored) {}
        }
        if (faultConfig.getErrorRate() > 0) {
            if (Math.random() < faultConfig.getErrorRate()) {
                String msg = faultConfig.getCustomErrorMessage() != null ?
                        faultConfig.getCustomErrorMessage() : "OrderService simulated internal processing fault";
                throw new RuntimeException(msg);
            }
        }
    }
}
