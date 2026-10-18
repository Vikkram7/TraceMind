package com.tracemind.backend.controller;

import com.tracemind.backend.entity.ServiceNodeEntity;
import com.tracemind.backend.repository.IncidentRepository;
import com.tracemind.backend.repository.ServiceNodeRepository;
import com.tracemind.backend.repository.SpanRepository;
import com.tracemind.backend.service.ServiceTopologyService;
import com.tracemind.backend.service.ServiceTopologyService.TopologyGraphDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Topology & Services", description = "Dynamic Service Graph and Health Inventory")
public class ServiceTopologyController {

    private final ServiceTopologyService topologyService;
    private final ServiceNodeRepository serviceNodeRepository;
    private final SpanRepository spanRepository;
    private final IncidentRepository incidentRepository;

    public ServiceTopologyController(
            ServiceTopologyService topologyService,
            ServiceNodeRepository serviceNodeRepository,
            SpanRepository spanRepository,
            IncidentRepository incidentRepository) {
        this.topologyService = topologyService;
        this.serviceNodeRepository = serviceNodeRepository;
        this.spanRepository = spanRepository;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/topology")
    @Operation(summary = "Get directed service dependency graph for React Flow visualization")
    public ResponseEntity<TopologyGraphDto> getTopology() {
        return ResponseEntity.ok(topologyService.getTopologyGraph());
    }

    @GetMapping("/services")
    @Operation(summary = "List registered microservices and telemetry health summaries")
    public ResponseEntity<List<Map<String, Object>>> getServices() {
        List<ServiceNodeEntity> services = serviceNodeRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ServiceNodeEntity s : services) {
            long totalSpans = spanRepository.countByServiceName(s.getName());
            long errorSpans = spanRepository.countByServiceNameAndIsErrorTrue(s.getName());
            double errorRate = (totalSpans > 0) ? (double) errorSpans / totalSpans : 0.0;
            Double avgDuration = spanRepository.findAvgDurationByServiceName(s.getName());

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", s.getId());
            dto.put("name", s.getName());
            dto.put("environment", s.getEnvironment());
            dto.put("version", s.getVersion());
            dto.put("status", s.getStatus());
            dto.put("lastHeartbeat", s.getLastHeartbeat());
            dto.put("totalSpans", totalSpans);
            dto.put("errorSpans", errorSpans);
            dto.put("errorRate", Math.round(errorRate * 1000.0) / 10.0);
            dto.put("avgDurationMs", avgDuration != null ? Math.round(avgDuration * 10.0) / 10.0 : 0.0);
            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/services/{name}/health")
    @Operation(summary = "Get health and operational metrics for a specific service")
    public ResponseEntity<?> getServiceHealth(@PathVariable String name) {
        Optional<ServiceNodeEntity> sOpt = serviceNodeRepository.findByName(name);
        if (sOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceNodeEntity s = sOpt.get();
        long totalSpans = spanRepository.countByServiceName(name);
        long errorSpans = spanRepository.countByServiceNameAndIsErrorTrue(name);
        Double avgDuration = spanRepository.findAvgDurationByServiceName(name);
        var activeIncidents = incidentRepository.findByServiceNameOrderByFirstSeenDesc(name);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("service", s);
        details.put("totalSpans", totalSpans);
        details.put("errorSpans", errorSpans);
        details.put("avgDurationMs", avgDuration != null ? avgDuration : 0.0);
        details.put("activeIncidentsCount", activeIncidents.size());
        details.put("activeIncidents", activeIncidents);

        return ResponseEntity.ok(details);
    }
}
