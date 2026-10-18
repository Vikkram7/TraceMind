package com.tracemind.backend.controller;

import com.tracemind.backend.entity.SpanEntity;
import com.tracemind.backend.service.TelemetryIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Telemetry Ingestion", description = "OpenTelemetry OTLP standard ingestion endpoints")
public class OtelTraceIngestionController {
    private static final Logger log = LoggerFactory.getLogger(OtelTraceIngestionController.class);

    private final TelemetryIngestionService ingestionService;

    public OtelTraceIngestionController(TelemetryIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = {"/v1/traces", "/api/v1/telemetry/traces"}, consumes = "application/json")
    @Operation(summary = "Ingest OpenTelemetry Traces via standard OTLP JSON or span array")
    public ResponseEntity<?> ingestTraces(@RequestBody String payload) {
        List<SpanEntity> savedSpans = ingestionService.ingestJsonPayload(payload);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "spansIngested", savedSpans.size()
        ));
    }

    @PostMapping(value = "/v1/metrics", consumes = "application/json")
    @Operation(summary = "Ingest OpenTelemetry Metrics via OTLP")
    public ResponseEntity<?> ingestMetrics(@RequestBody String payload) {
        log.debug("Received OTLP metrics payload ({} bytes)", payload.length());
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    @PostMapping(value = "/v1/logs", consumes = "application/json")
    @Operation(summary = "Ingest OpenTelemetry Logs via OTLP")
    public ResponseEntity<?> ingestLogs(@RequestBody String payload) {
        log.debug("Received OTLP logs payload ({} bytes)", payload.length());
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }
}
