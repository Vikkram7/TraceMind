package com.tracemind.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemind.backend.entity.*;
import com.tracemind.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TelemetryIngestionService {
    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);

    private final SpanRepository spanRepository;
    private final TraceRepository traceRepository;
    private final ServiceNodeRepository serviceNodeRepository;
    private final ServiceDependencyRepository dependencyRepository;
    private final IncidentDetectionService incidentDetectionService;
    private final ObjectMapper objectMapper;

    public TelemetryIngestionService(
            SpanRepository spanRepository,
            TraceRepository traceRepository,
            ServiceNodeRepository serviceNodeRepository,
            ServiceDependencyRepository dependencyRepository,
            @Lazy IncidentDetectionService incidentDetectionService,
            ObjectMapper objectMapper) {
        this.spanRepository = spanRepository;
        this.traceRepository = traceRepository;
        this.serviceNodeRepository = serviceNodeRepository;
        this.dependencyRepository = dependencyRepository;
        this.incidentDetectionService = incidentDetectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<SpanEntity> ingestJsonPayload(String rawJson) {
        List<SpanEntity> parsedSpans = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);

            if (rootNode.isArray()) {
                // Direct array of span objects
                for (JsonNode item : rootNode) {
                    SpanEntity span = parseSingleSpanNode(item, null);
                    if (span != null) parsedSpans.add(span);
                }
            } else if (rootNode.has("resourceSpans")) {
                // Standard OTLP JSON structure
                JsonNode resourceSpans = rootNode.get("resourceSpans");
                for (JsonNode resSpan : resourceSpans) {
                    String serviceName = extractServiceName(resSpan.path("resource"));
                    JsonNode scopeSpans = resSpan.path("scopeSpans");
                    for (JsonNode scopeSpan : scopeSpans) {
                        JsonNode spans = scopeSpan.path("spans");
                        for (JsonNode spanNode : spans) {
                            SpanEntity span = parseSingleSpanNode(spanNode, serviceName);
                            if (span != null) parsedSpans.add(span);
                        }
                    }
                }
            } else if (rootNode.has("traceId") && rootNode.has("spanId")) {
                // Single span object
                SpanEntity span = parseSingleSpanNode(rootNode, null);
                if (span != null) parsedSpans.add(span);
            }

            for (SpanEntity span : parsedSpans) {
                processAndPersistSpan(span);
            }

        } catch (Exception e) {
            log.error("Failed to parse telemetry JSON payload: {}", e.getMessage(), e);
        }

        return parsedSpans;
    }

    private String extractServiceName(JsonNode resourceNode) {
        if (resourceNode == null) return "unknown-service";
        JsonNode attributes = resourceNode.path("attributes");
        if (attributes.isArray()) {
            for (JsonNode attr : attributes) {
                if ("service.name".equals(attr.path("key").asText())) {
                    return attr.path("value").path("stringValue").asText("unknown-service");
                }
            }
        }
        return "unknown-service";
    }

    private SpanEntity parseSingleSpanNode(JsonNode node, String defaultService) {
        try {
            String traceId = node.path("traceId").asText();
            String spanId = node.path("spanId").asText();
            if (traceId == null || traceId.isEmpty() || spanId == null || spanId.isEmpty()) {
                return null;
            }

            SpanEntity span = new SpanEntity();
            span.setTraceId(traceId);
            span.setSpanId(spanId);

            String parentSpanId = node.path("parentSpanId").asText(null);
            if (parentSpanId != null && !parentSpanId.isEmpty()) {
                span.setParentSpanId(parentSpanId);
            }

            String serviceName = node.path("serviceName").asText(defaultService != null ? defaultService : "unknown-service");
            span.setServiceName(serviceName);

            String operationName = node.path("operationName").asText(node.path("name").asText("unknown-op"));
            span.setOperationName(operationName);

            span.setKind(node.path("kind").asText("INTERNAL").replace("SPAN_KIND_", ""));

            String statusCode = node.path("statusCode").asText(
                    node.path("status").path("code").asText("OK").replace("STATUS_CODE_", "")
            );
            span.setStatusCode(statusCode);

            span.setStatusMessage(node.path("statusMessage").asText(node.path("status").path("message").asText(null)));

            long startNano = node.path("startTimeUnixNano").asLong(System.currentTimeMillis() * 1_000_000L);
            long endNano = node.path("endTimeUnixNano").asLong(startNano + 10_000_000L);

            span.setStartTime(Instant.ofEpochMilli(startNano / 1_000_000L));
            span.setEndTime(Instant.ofEpochMilli(endNano / 1_000_000L));

            double durationMs = node.path("durationMs").asDouble((endNano - startNano) / 1_000_000.0);
            span.setDurationMs(Math.max(0.1, durationMs));

            boolean isError = "ERROR".equalsIgnoreCase(statusCode) ||
                    (span.getStatusMessage() != null && !span.getStatusMessage().isEmpty() && !"HTTP 200".equalsIgnoreCase(span.getStatusMessage()));
            span.setError(isError);

            if (node.has("attributes")) {
                span.setAttributesJson(node.get("attributes").toString());
            }

            return span;
        } catch (Exception e) {
            log.warn("Could not parse span JSON node: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void processAndPersistSpan(SpanEntity span) {
        spanRepository.save(span);

        // 1. Maintain Service Node
        maintainServiceNode(span.getServiceName());

        // 2. Maintain Service Dependency if parent span exists
        if (span.getParentSpanId() != null) {
            Optional<SpanEntity> parentOpt = spanRepository.findById(span.getParentSpanId());
            if (parentOpt.isPresent()) {
                SpanEntity parent = parentOpt.get();
                if (!parent.getServiceName().equalsIgnoreCase(span.getServiceName())) {
                    maintainServiceDependency(parent.getServiceName(), span.getServiceName(), span.getDurationMs(), span.isError());
                }
            }
        }

        // 3. Upsert Trace Entity
        updateTraceAggregate(span);

        // 4. Trigger Incident Detection evaluation
        incidentDetectionService.evaluateSpan(span);
    }

    private void maintainServiceNode(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) return;
        serviceNodeRepository.findByName(serviceName).ifPresentOrElse(
                service -> {
                    service.setLastHeartbeat(Instant.now());
                    serviceNodeRepository.save(service);
                },
                () -> {
                    ServiceNodeEntity newService = new ServiceNodeEntity(serviceName.toLowerCase(), serviceName);
                    serviceNodeRepository.save(newService);
                }
        );
    }

    private void maintainServiceDependency(String source, String target, double durationMs, boolean isError) {
        String depId = source + "->" + target;
        dependencyRepository.findById(depId).ifPresentOrElse(
                dep -> {
                    long totalCalls = dep.getCallCount() + 1;
                    dep.setCallCount(totalCalls);
                    if (isError) dep.setErrorCount(dep.getErrorCount() + 1);
                    double newAvg = (dep.getAvgDurationMs() * (totalCalls - 1) + durationMs) / totalCalls;
                    dep.setAvgDurationMs(newAvg);
                    if (durationMs > dep.getP95DurationMs()) {
                        dep.setP95DurationMs(durationMs);
                    }
                    dep.setLastObserved(Instant.now());
                    dependencyRepository.save(dep);
                },
                () -> {
                    ServiceDependencyEntity newDep = new ServiceDependencyEntity(source, target);
                    newDep.setCallCount(1);
                    newDep.setErrorCount(isError ? 1 : 0);
                    newDep.setAvgDurationMs(durationMs);
                    newDep.setP95DurationMs(durationMs);
                    dependencyRepository.save(newDep);
                }
        );
    }

    private void updateTraceAggregate(SpanEntity span) {
        String traceId = span.getTraceId();
        traceRepository.findById(traceId).ifPresentOrElse(
                trace -> {
                    trace.setSpanCount(trace.getSpanCount() + 1);
                    if (span.isError()) {
                        trace.setErrorCount(trace.getErrorCount() + 1);
                        trace.setStatus("ERROR");
                    }
                    if (span.getStartTime().isBefore(trace.getStartTime())) {
                        trace.setStartTime(span.getStartTime());
                    }
                    if (span.getEndTime().isAfter(trace.getEndTime())) {
                        trace.setEndTime(span.getEndTime());
                        trace.setDurationMs(Math.max(trace.getDurationMs(),
                                (trace.getEndTime().toEpochMilli() - trace.getStartTime().toEpochMilli())));
                    }
                    if (span.getParentSpanId() == null) {
                        trace.setRootService(span.getServiceName());
                        trace.setRootOperation(span.getOperationName());
                    }
                    traceRepository.save(trace);
                },
                () -> {
                    TraceEntity newTrace = new TraceEntity();
                    newTrace.setTraceId(traceId);
                    newTrace.setRootService(span.getServiceName());
                    newTrace.setRootOperation(span.getOperationName());
                    newTrace.setStartTime(span.getStartTime());
                    newTrace.setEndTime(span.getEndTime());
                    newTrace.setDurationMs(span.getDurationMs());
                    newTrace.setStatus(span.isError() ? "ERROR" : "OK");
                    newTrace.setSpanCount(1);
                    newTrace.setErrorCount(span.isError() ? 1 : 0);
                    traceRepository.save(newTrace);
                }
        );
    }
}
