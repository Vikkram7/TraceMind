package com.tracemind.order.telemetry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class TracingRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final TelemetryClient telemetryClient;

    @Value("${spring.application.name:order-service}")
    private String serviceName;

    public TracingRestTemplateInterceptor(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        TraceContext current = TraceContextHolder.get();
        if (current == null) {
            return execution.execute(request, body);
        }

        String traceId = current.getTraceId();
        String parentSpanId = current.getSpanId();
        String clientSpanId = W3CTracePropagator.generateSpanId();

        request.getHeaders().set("traceparent", W3CTracePropagator.format(traceId, clientSpanId));
        request.getHeaders().set("X-Trace-Id", traceId);

        long startNano = System.nanoTime();
        long startEpochNano = System.currentTimeMillis() * 1_000_000L;

        ClientHttpResponse response = null;
        Exception caughtException = null;

        try {
            response = execution.execute(request, body);
            return response;
        } catch (IOException | RuntimeException e) {
            caughtException = e;
            throw e;
        } finally {
            long endNano = System.nanoTime();
            long endEpochNano = startEpochNano + (endNano - startNano);
            double durationMs = (endNano - startNano) / 1_000_000.0;

            int statusCode = (response != null) ? response.getStatusCode().value() : 500;
            boolean isError = (statusCode >= 400) || (caughtException != null);

            TelemetrySpanDto clientSpan = new TelemetrySpanDto();
            clientSpan.setTraceId(traceId);
            clientSpan.setSpanId(clientSpanId);
            clientSpan.setParentSpanId(parentSpanId);
            clientSpan.setServiceName(serviceName);
            clientSpan.setOperationName("CALL " + request.getMethod() + " " + request.getURI().getPath());
            clientSpan.setKind("CLIENT");
            clientSpan.setStatusCode(isError ? "ERROR" : "OK");
            clientSpan.setStatusMessage(caughtException != null ? caughtException.getMessage() : "HTTP " + statusCode);
            clientSpan.setStartTimeUnixNano(startEpochNano);
            clientSpan.setEndTimeUnixNano(endEpochNano);
            clientSpan.setDurationMs(durationMs);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("http.method", request.getMethod().name());
            attributes.put("http.url", request.getURI().toString());
            attributes.put("http.status_code", statusCode);
            attributes.put("service.name", serviceName);
            attributes.put("peer.service", extractPeerService(request.getURI().toString()));
            if (caughtException != null) {
                attributes.put("error.type", caughtException.getClass().getName());
                attributes.put("error.message", caughtException.getMessage());
            }
            clientSpan.setAttributes(attributes);

            telemetryClient.reportSpanAsync(clientSpan);
        }
    }

    private String extractPeerService(String uri) {
        if (uri.contains(":8082") || uri.contains("payment-service")) return "payment-service";
        if (uri.contains(":8083") || uri.contains("inventory-service")) return "inventory-service";
        return "downstream-service";
    }
}
