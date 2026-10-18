package com.tracemind.backend.service;

import com.tracemind.backend.entity.ServiceDependencyEntity;
import com.tracemind.backend.entity.ServiceNodeEntity;
import com.tracemind.backend.repository.IncidentRepository;
import com.tracemind.backend.repository.ServiceDependencyRepository;
import com.tracemind.backend.repository.ServiceNodeRepository;
import com.tracemind.backend.repository.SpanRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServiceTopologyService {

    private final ServiceNodeRepository serviceNodeRepository;
    private final ServiceDependencyRepository dependencyRepository;
    private final SpanRepository spanRepository;
    private final IncidentRepository incidentRepository;

    public ServiceTopologyService(
            ServiceNodeRepository serviceNodeRepository,
            ServiceDependencyRepository dependencyRepository,
            SpanRepository spanRepository,
            IncidentRepository incidentRepository) {
        this.serviceNodeRepository = serviceNodeRepository;
        this.dependencyRepository = dependencyRepository;
        this.spanRepository = spanRepository;
        this.incidentRepository = incidentRepository;
    }

    public static class TopologyGraphDto {
        private List<Map<String, Object>> nodes = new ArrayList<>();
        private List<Map<String, Object>> edges = new ArrayList<>();

        public List<Map<String, Object>> getNodes() { return nodes; }
        public void setNodes(List<Map<String, Object>> nodes) { this.nodes = nodes; }

        public List<Map<String, Object>> getEdges() { return edges; }
        public void setEdges(List<Map<String, Object>> edges) { this.edges = edges; }
    }

    public TopologyGraphDto getTopologyGraph() {
        TopologyGraphDto graph = new TopologyGraphDto();

        List<ServiceNodeEntity> services = serviceNodeRepository.findAll();
        List<ServiceDependencyEntity> dependencies = dependencyRepository.findAll();

        // Fallback default sample nodes if services have not registered yet
        if (services.isEmpty()) {
            services = Arrays.asList(
                    new ServiceNodeEntity("order-service", "order-service"),
                    new ServiceNodeEntity("payment-service", "payment-service"),
                    new ServiceNodeEntity("inventory-service", "inventory-service")
            );
        }

        // Layout coordinates for React Flow
        Map<String, int[]> positions = Map.of(
                "order-service", new int[]{100, 150},
                "payment-service", new int[]{450, 60},
                "inventory-service", new int[]{450, 240}
        );

        int defaultX = 100;
        int defaultY = 100;

        for (ServiceNodeEntity s : services) {
            int[] pos = positions.getOrDefault(s.getName().toLowerCase(), new int[]{defaultX, defaultY});
            defaultX += 200;

            long totalSpans = spanRepository.countByServiceName(s.getName());
            long errorSpans = spanRepository.countByServiceNameAndIsErrorTrue(s.getName());
            double errorRate = (totalSpans > 0) ? (double) errorSpans / totalSpans : 0.0;
            Double avgDuration = spanRepository.findAvgDurationByServiceName(s.getName());
            long activeIncidents = incidentRepository.findByServiceNameOrderByFirstSeenDesc(s.getName())
                    .stream().filter(inc -> "ACTIVE".equals(inc.getStatus())).count();

            String status = "HEALTHY";
            if (activeIncidents > 0 || errorRate > 0.20) {
                status = "CRITICAL";
            } else if (errorRate > 0.05 || (avgDuration != null && avgDuration > 800)) {
                status = "DEGRADED";
            }

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", s.getName());
            node.put("type", "serviceNode");
            node.put("position", Map.of("x", pos[0], "y", pos[1]));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("label", s.getName());
            data.put("status", status);
            data.put("callCount", totalSpans);
            data.put("errorCount", errorSpans);
            data.put("errorRate", Math.round(errorRate * 1000.0) / 10.0); // e.g. 12.5%
            data.put("avgDurationMs", avgDuration != null ? Math.round(avgDuration * 10.0) / 10.0 : 0.0);
            data.put("activeIncidents", activeIncidents);
            node.put("data", data);

            graph.getNodes().add(node);
        }

        // Generate edges
        for (ServiceDependencyEntity dep : dependencies) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("id", dep.getId());
            edge.put("source", dep.getSourceService());
            edge.put("target", dep.getTargetService());
            edge.put("animated", dep.getErrorCount() > 0);

            double errorRate = (dep.getCallCount() > 0) ? (double) dep.getErrorCount() / dep.getCallCount() : 0.0;
            String edgeStatus = (errorRate > 0.15) ? "ERROR" : ((dep.getAvgDurationMs() > 800) ? "SLOW" : "NORMAL");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("callCount", dep.getCallCount());
            data.put("errorCount", dep.getErrorCount());
            data.put("errorRate", Math.round(errorRate * 1000.0) / 10.0);
            data.put("avgDurationMs", Math.round(dep.getAvgDurationMs() * 10.0) / 10.0);
            data.put("p95DurationMs", Math.round(dep.getP95DurationMs() * 10.0) / 10.0);
            data.put("status", edgeStatus);

            edge.put("data", data);
            edge.put("label", String.format("%d calls (%.1f ms)", dep.getCallCount(), dep.getAvgDurationMs()));

            graph.getEdges().add(edge);
        }

        // Fallback default edges if zero dependencies observed yet
        if (graph.getEdges().isEmpty() && services.size() >= 3) {
            graph.getEdges().add(createEdge("order-service->payment-service", "order-service", "payment-service", 0, 0, 0.0, "NORMAL"));
            graph.getEdges().add(createEdge("order-service->inventory-service", "order-service", "inventory-service", 0, 0, 0.0, "NORMAL"));
        }

        return graph;
    }

    private Map<String, Object> createEdge(String id, String source, String target, long calls, long errors, double avgMs, String status) {
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("id", id);
        edge.put("source", source);
        edge.put("target", target);
        edge.put("animated", false);
        edge.put("data", Map.of(
                "callCount", calls,
                "errorCount", errors,
                "errorRate", 0.0,
                "avgDurationMs", avgMs,
                "status", status
        ));
        edge.put("label", calls > 0 ? String.format("%d calls (%.1f ms)", calls, avgMs) : "pending telemetry");
        return edge;
    }
}
