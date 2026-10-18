package com.tracemind.backend.controller;

import com.tracemind.backend.entity.IncidentEntity;
import com.tracemind.backend.entity.IncidentEvidenceEntity;
import com.tracemind.backend.entity.RootCauseAnalysisEntity;
import com.tracemind.backend.repository.IncidentEvidenceRepository;
import com.tracemind.backend.repository.IncidentRepository;
import com.tracemind.backend.repository.RootCauseAnalysisRepository;
import com.tracemind.backend.service.RootCauseAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Operational Anomaly and Incident Management")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final IncidentEvidenceRepository evidenceRepository;
    private final RootCauseAnalysisRepository rcaRepository;
    private final RootCauseAnalysisService rcaService;

    public IncidentController(
            IncidentRepository incidentRepository,
            IncidentEvidenceRepository evidenceRepository,
            RootCauseAnalysisRepository rcaRepository,
            RootCauseAnalysisService rcaService) {
        this.incidentRepository = incidentRepository;
        this.evidenceRepository = evidenceRepository;
        this.rcaRepository = rcaRepository;
        this.rcaService = rcaService;
    }

    @GetMapping
    @Operation(summary = "List and filter operational incidents")
    public ResponseEntity<Page<IncidentEntity>> getIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, Math.min(100, size));
        Page<IncidentEntity> incidents = incidentRepository.findAllByOrderByFirstSeenDesc(pageRequest);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident details and supporting evidence references")
    public ResponseEntity<?> getIncidentById(@PathVariable String id) {
        Optional<IncidentEntity> incOpt = incidentRepository.findById(id);
        if (incOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncidentEntity inc = incOpt.get();
        List<IncidentEvidenceEntity> evidences = evidenceRepository.findByIncidentId(id);
        Optional<RootCauseAnalysisEntity> rcaOpt = rcaRepository.findByIncidentId(id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("incident", inc);
        response.put("evidences", evidences);
        response.put("rootCauseAnalysis", rcaOpt.orElse(null));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge active incident")
    public ResponseEntity<?> acknowledgeIncident(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        Optional<IncidentEntity> incOpt = incidentRepository.findById(id);
        if (incOpt.isEmpty()) return ResponseEntity.notFound().build();

        IncidentEntity inc = incOpt.get();
        inc.setStatus("ACKNOWLEDGED");
        inc.setAcknowledgedAt(Instant.now());
        inc.setAcknowledgedBy((body != null && body.containsKey("user")) ? body.get("user") : "SRE-OnCall");
        incidentRepository.save(inc);

        return ResponseEntity.ok(inc);
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve operational incident")
    public ResponseEntity<?> resolveIncident(@PathVariable String id) {
        Optional<IncidentEntity> incOpt = incidentRepository.findById(id);
        if (incOpt.isEmpty()) return ResponseEntity.notFound().build();

        IncidentEntity inc = incOpt.get();
        inc.setStatus("RESOLVED");
        inc.setResolvedAt(Instant.now());
        incidentRepository.save(inc);

        return ResponseEntity.ok(inc);
    }

    @GetMapping("/{id}/rca")
    @Operation(summary = "Get Root Cause Analysis report for incident")
    public ResponseEntity<?> getRcaForIncident(@PathVariable String id) {
        Optional<RootCauseAnalysisEntity> rcaOpt = rcaRepository.findByIncidentId(id);
        if (rcaOpt.isEmpty()) {
            // Generate on-demand if not already computed
            RootCauseAnalysisEntity generated = rcaService.runAnalysis(id);
            return ResponseEntity.ok(generated);
        }
        return ResponseEntity.ok(rcaOpt.get());
    }

    @PostMapping("/{id}/rca/reanalyze")
    @Operation(summary = "Re-run root cause ranking with updated telemetry evidence")
    public ResponseEntity<?> reanalyzeRca(@PathVariable String id) {
        RootCauseAnalysisEntity fresh = rcaService.runAnalysis(id);
        return ResponseEntity.ok(fresh);
    }
}
