package com.tracemind.order.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TelemetryClient {
    private static final Logger log = LoggerFactory.getLogger(TelemetryClient.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Value("${tracemind.telemetry.endpoint:http://localhost:8080/v1/traces}")
    private String telemetryEndpoint;

    public void reportSpanAsync(TelemetrySpanDto span) {
        executor.submit(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<?> entity = new HttpEntity<>(Collections.singletonList(span), headers);
                restTemplate.postForLocation(telemetryEndpoint, entity);
            } catch (Exception e) {
                log.debug("Telemetry ingestion buffer delayed: {}", e.getMessage());
            }
        });
    }
}
