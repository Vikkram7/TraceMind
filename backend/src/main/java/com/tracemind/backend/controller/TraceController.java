package com.tracemind.backend.controller;

import com.tracemind.backend.analysis.TraceDivergenceEngine;
import com.tracemind.backend.analysis.TraceDivergenceEngine.DivergenceReport;
import com.tracemind.backend.entity.SpanEntity;
import com.tracemind.backend.entity.TraceEntity;
import com.tracemind.backend.repository.SpanRepository;
import com.tracemind.backend.repository.TraceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/traces")
@Tag(name = "Traces", description = "Distributed Trace Exploration and Divergence Analysis")
public class TraceController {

    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;
    private final TraceDivergenceEngine divergenceEngine;

    public TraceController(
            TraceRepository traceRepository,
            SpanRepository spanRepository,
            TraceDivergenceEngine divergenceEngine) {
        this.traceRepository = traceRepository;
        this.spanRepository = spanRepository;
        this.divergenceEngine = divergenceEngine;
    }

    @GetMapping
    @Operation(summary = "Search and filter distributed traces")
    public ResponseEntity<Page<TraceEntity>> getTraces(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minDuration,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, Math.min(100, size));
        Page<TraceEntity> result = traceRepository.searchTraces(
                (service != null && !service.isBlank()) ? service : null,
                (status != null && !status.isBlank()) ? status : null,
                minDuration,
                pageRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{traceId}")
    @Operation(summary = "Get full trace hierarchy and waterfall timeline")
    public ResponseEntity<?> getTraceDetails(@PathVariable String traceId) {
        Optional<TraceEntity> traceOpt = traceRepository.findById(traceId);
        if (traceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TraceEntity trace = traceOpt.get();
        List<SpanEntity> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(traceId);

        // Build waterfall tree with depth and relative start times
        long traceStartNano = trace.getStartTime().toEpochMilli() * 1_000_000L;
        List<Map<String, Object>> waterfallSpans = new ArrayList<>();

        for (SpanEntity span : spans) {
            Map<String, Object> spanDto = new LinkedHashMap<>();
            spanDto.put("spanId", span.getSpanId());
            spanDto.put("traceId", span.getTraceId());
            spanDto.put("parentSpanId", span.getParentSpanId());
            spanDto.put("serviceName", span.getServiceName());
            spanDto.put("operationName", span.getOperationName());
            spanDto.put("kind", span.getKind());
            spanDto.put("statusCode", span.getStatusCode());
            spanDto.put("statusMessage", span.getStatusMessage());
            spanDto.put("startTime", span.getStartTime());
            spanDto.put("endTime", span.getEndTime());
            spanDto.put("durationMs", span.getDurationMs());
            spanDto.put("isError", span.isError());
            spanDto.put("attributesJson", span.getAttributesJson());

            // Relative offset in milliseconds from start of trace
            long spanStartNano = span.getStartTime().toEpochMilli() * 1_000_000L;
            double relativeOffsetMs = Math.max(0.0, (spanStartNano - traceStartNano) / 1_000_000.0);
            spanDto.put("relativeOffsetMs", relativeOffsetMs);

            waterfallSpans.add(spanDto);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("trace", trace);
        response.put("spans", waterfallSpans);
        response.put("totalSpans", spans.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/compare")
    @Operation(summary = "Algorithmic divergence analysis between baseline and target traces")
    public ResponseEntity<DivergenceReport> compareTraces(
            @RequestParam String baselineTraceId,
            @RequestParam String targetTraceId) {

        List<SpanEntity> baselineSpans = spanRepository.findByTraceIdOrderByStartTimeAsc(baselineTraceId);
        List<SpanEntity> targetSpans = spanRepository.findByTraceIdOrderByStartTimeAsc(targetTraceId);

        DivergenceReport report = divergenceEngine.compareTraces(baselineSpans, targetSpans);
        return ResponseEntity.ok(report);
    }
}
