package com.tracemind.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemind.backend.analysis.RootCauseRankingEngine;
import com.tracemind.backend.analysis.RootCauseRankingEngine.CandidateEvaluation;
import com.tracemind.backend.analysis.TraceDivergenceEngine;
import com.tracemind.backend.analysis.TraceDivergenceEngine.DivergenceReport;
import com.tracemind.backend.entity.*;
import com.tracemind.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class RootCauseAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(RootCauseAnalysisService.class);

    private final IncidentRepository incidentRepository;
    private final RootCauseAnalysisRepository rcaRepository;
    private final SpanRepository spanRepository;
    private final TraceRepository traceRepository;
    private final RootCauseRankingEngine rankingEngine;
    private final TraceDivergenceEngine divergenceEngine;
    private final LlmExplanationService llmExplanationService;
    private final ObjectMapper objectMapper;

    public RootCauseAnalysisService(
            IncidentRepository incidentRepository,
            RootCauseAnalysisRepository rcaRepository,
            SpanRepository spanRepository,
            TraceRepository traceRepository,
            RootCauseRankingEngine rankingEngine,
            TraceDivergenceEngine divergenceEngine,
            LlmExplanationService llmExplanationService,
            ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.rcaRepository = rcaRepository;
        this.spanRepository = spanRepository;
        this.traceRepository = traceRepository;
        this.rankingEngine = rankingEngine;
        this.divergenceEngine = divergenceEngine;
        this.llmExplanationService = llmExplanationService;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void analyzeIncidentAsync(String incidentId) {
        try {
            runAnalysis(incidentId);
        } catch (Exception e) {
            log.error("Asynchronous RCA failed for incident {}: {}", incidentId, e.getMessage(), e);
        }
    }

    @Transactional
    public RootCauseAnalysisEntity runAnalysis(String incidentId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        log.info("Running deterministic root cause analysis for incident {}", incidentId);

        // Fetch spans for this service or failing spans
        List<SpanEntity> incidentSpans = spanRepository.findByServiceName(incident.getServiceName());
        if (incidentSpans.isEmpty()) {
            incidentSpans = spanRepository.findTop50ByIsErrorTrueOrderByStartTimeDesc();
        }

        // Fetch healthy baseline spans
        List<TraceEntity> healthyTraces = traceRepository.findTop50ByStatusOrderByStartTimeDesc("OK");
        List<SpanEntity> baselineSpans = new ArrayList<>();
        for (TraceEntity ht : healthyTraces) {
            baselineSpans.addAll(spanRepository.findByTraceIdOrderByStartTimeAsc(ht.getTraceId()));
            if (baselineSpans.size() >= 50) break;
        }

        // 1. Rank Root Causes
        List<CandidateEvaluation> rankedCandidates = rankingEngine.rankRootCauses(incidentSpans, baselineSpans);

        // 2. Compute Trace Divergence
        DivergenceReport divergenceReport = null;
        if (!healthyTraces.isEmpty() && !incidentSpans.isEmpty()) {
            String targetTraceId = incidentSpans.get(0).getTraceId();
            List<SpanEntity> targetTraceSpans = spanRepository.findByTraceIdOrderByStartTimeAsc(targetTraceId);
            String baselineTraceId = healthyTraces.get(0).getTraceId();
            List<SpanEntity> healthyTraceSpans = spanRepository.findByTraceIdOrderByStartTimeAsc(baselineTraceId);
            divergenceReport = divergenceEngine.compareTraces(healthyTraceSpans, targetTraceSpans);
        }

        // 3. Generate Explanation (LLM or Deterministic Template)
        LlmExplanationService.ExplanationResult explanation = llmExplanationService.generateExplanation(
                incident, rankedCandidates, divergenceReport, incidentSpans);

        CandidateEvaluation top = rankedCandidates.isEmpty() ? null : rankedCandidates.get(0);
        String primaryCandidate = (top != null) ?
                top.getCandidateKey() + " (" + top.getServiceName() + ")" : incident.getServiceName();
        double confidence = (top != null) ? top.getConfidence() : 0.85;

        RootCauseAnalysisEntity rca = rcaRepository.findByIncidentId(incidentId).orElse(new RootCauseAnalysisEntity());
        rca.setId("rca-" + UUID.randomUUID().toString().substring(0, 8));
        rca.setIncidentId(incidentId);
        rca.setPrimaryCandidate(primaryCandidate);
        rca.setConfidenceScore(confidence);
        rca.setExplanationSummary(explanation.getSummary());
        rca.setFailureNarrative(explanation.getFailureNarrative());
        rca.setAiGenerated(explanation.isAiGenerated());
        rca.setAiModelUsed(explanation.getModelUsed());
        rca.setCreatedAt(Instant.now());

        try {
            rca.setRankedHypothesesJson(objectMapper.writeValueAsString(rankedCandidates));
            rca.setSuggestedActionsJson(objectMapper.writeValueAsString(Map.of(
                    "diagnosticActions", explanation.getDiagnosticActions(),
                    "remediations", explanation.getRemediations()
            )));
        } catch (Exception e) {
            log.error("Failed to serialize RCA hypotheses: {}", e.getMessage());
        }

        return rcaRepository.save(rca);
    }
}
