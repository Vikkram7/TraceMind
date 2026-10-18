package com.tracemind.inventory.telemetry;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TracingFilter extends OncePerRequestFilter {

    private final TelemetryClient telemetryClient;

    @Value("${spring.application.name:inventory-service}")
    private String serviceName;

    public TracingFilter(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startNano = System.nanoTime();
        long startEpochNano = System.currentTimeMillis() * 1_000_000L;

        String traceparent = request.getHeader("traceparent");
        W3CTracePropagator.TraceParent parsed = W3CTracePropagator.parse(traceparent);

        String traceId = (parsed != null) ? parsed.getTraceId() : W3CTracePropagator.generateTraceId();
        String parentSpanId = (parsed != null) ? parsed.getParentSpanId() : null;
        String spanId = W3CTracePropagator.generateSpanId();

        TraceContext context = new TraceContext(traceId, spanId, parentSpanId, startEpochNano, serviceName);
        TraceContextHolder.set(context);

        response.setHeader("traceparent", W3CTracePropagator.format(traceId, spanId));
        response.setHeader("X-Trace-Id", traceId);

        Exception capturedException = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            capturedException = ex;
            throw ex;
        } finally {
            long endNano = System.nanoTime();
            long endEpochNano = startEpochNano + (endNano - startNano);
            double durationMs = (endNano - startNano) / 1_000_000.0;

            int status = response.getStatus();
            boolean isError = (status >= 400) || (capturedException != null);

            TelemetrySpanDto span = new TelemetrySpanDto();
            span.setTraceId(traceId);
            span.setSpanId(spanId);
            span.setParentSpanId(parentSpanId);
            span.setServiceName(serviceName);
            span.setOperationName(request.getMethod() + " " + path);
            span.setKind("SERVER");
            span.setStatusCode(isError ? "ERROR" : "OK");
            span.setStatusMessage(capturedException != null ? capturedException.getMessage() : "HTTP " + status);
            span.setStartTimeUnixNano(startEpochNano);
            span.setEndTimeUnixNano(endEpochNano);
            span.setDurationMs(durationMs);

            Map<String, Object> attributes = new HashMap<>(context.getAttributes());
            attributes.put("http.method", request.getMethod());
            attributes.put("http.url", request.getRequestURL().toString());
            attributes.put("http.status_code", status);
            attributes.put("service.name", serviceName);
            if (capturedException != null) {
                attributes.put("error.type", capturedException.getClass().getName());
                attributes.put("error.message", capturedException.getMessage());
            }
            span.setAttributes(attributes);

            telemetryClient.reportSpanAsync(span);
            TraceContextHolder.clear();
        }
    }
}
