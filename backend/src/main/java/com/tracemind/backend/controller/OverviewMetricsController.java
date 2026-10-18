package com.tracemind.backend.controller;

import com.tracemind.backend.entity.TraceEntity;
import com.tracemind.backend.repository.IncidentRepository;
import com.tracemind.backend.repository.ServiceNodeRepository;
import com.tracemind.backend.repository.SpanRepository;
import com.tracemind.backend.repository.TraceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Overview Metrics", description = "Global Observability Metrics and Telemetry Health")
public class OverviewMetricsController {

    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;
    private final ServiceNodeRepository serviceNodeRepository;
    private final IncidentRepository incidentRepository;

    public OverviewMetricsController(
            TraceRepository traceRepository,
            SpanRepository spanRepository,
            ServiceNodeRepository serviceNodeRepository,
            IncidentRepository incidentRepository) {
        this.traceRepository = traceRepository;
        this.spanRepository = spanRepository;
        this.serviceNodeRepository = serviceNodeRepository;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get aggregated system-wide observability metrics")
    public ResponseEntity<Map<String, Object>> getOverviewMetrics() {
        long totalTraces = traceRepository.count();
        long errorTraces = traceRepository.countByStatus("ERROR");
        double errorRate = (totalTraces > 0) ? ((double) errorTraces / totalTraces) * 100.0 : 0.0;
        Double avgDuration = traceRepository.findAverageDurationMs();
        long activeIncidents = incidentRepository.countByStatus("ACTIVE");
        long servicesCount = serviceNodeRepository.count();

        // Calculate P95 latency across recent traces
        List<TraceEntity> recent = traceRepository.findTop100ByOrderByStartTimeDesc();
        double p95Latency = 0.0;
        if (!recent.isEmpty()) {
            List<Double> durations = recent.stream().map(TraceEntity::getDurationMs).sorted().toList();
            int p95Index = (int) Math.ceil(durations.size() * 0.95) - 1;
            p95Latency = durations.get(Math.max(0, p95Index));
        }

        long demoTracesCount = recent.stream().filter(TraceEntity::isDemo).count();
        long liveTracesCount = recent.size() - demoTracesCount;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalTraces", totalTraces);
        metrics.put("errorTraces", errorTraces);
        metrics.put("errorRate", Math.round(errorRate * 10.0) / 10.0);
        metrics.put("avgLatencyMs", avgDuration != null ? Math.round(avgDuration * 10.0) / 10.0 : 0.0);
        metrics.put("p95LatencyMs", Math.round(p95Latency * 10.0) / 10.0);
        metrics.put("activeIncidents", activeIncidents);
        metrics.put("servicesCount", Math.max(3, servicesCount)); // at least sample 3 services
        metrics.put("telemetryConnected", totalTraces > 0);
        metrics.put("liveTracesCount", liveTracesCount);
        metrics.put("demoTracesCount", demoTracesCount);
        metrics.put("recentTraces", traceRepository.findAllByOrderByStartTimeDesc(PageRequest.of(0, 5)).getContent());
        metrics.put("recentIncidents", incidentRepository.findByStatusOrderByFirstSeenDesc("ACTIVE"));

        return ResponseEntity.ok(metrics);
    }
}
