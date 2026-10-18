package com.tracemind.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemind.backend.analysis.RootCauseRankingEngine.CandidateEvaluation;
import com.tracemind.backend.analysis.TraceDivergenceEngine.DivergenceReport;
import com.tracemind.backend.entity.IncidentEntity;
import com.tracemind.backend.entity.SpanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LlmExplanationService {
    private static final Logger log = LoggerFactory.getLogger(LlmExplanationService.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${tracemind.llm.provider:disabled}")
    private String llmProvider; // disabled, openai, gemini, anthropic, ollama

    @Value("${tracemind.llm.api-key:}")
    private String apiKey;

    @Value("${tracemind.llm.base-url:}")
    private String baseUrl;

    @Value("${tracemind.llm.model:gpt-4o-mini}")
    private String modelName;

    public LlmExplanationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static class ExplanationResult {
        private String summary;
        private String failureNarrative;
        private List<String> diagnosticActions = new ArrayList<>();
        private List<Map<String, String>> remediations = new ArrayList<>();
        private boolean aiGenerated;
        private String modelUsed;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public String getFailureNarrative() { return failureNarrative; }
        public void setFailureNarrative(String narrative) { this.failureNarrative = narrative; }

        public List<String> getDiagnosticActions() { return diagnosticActions; }
        public void setDiagnosticActions(List<String> actions) { this.diagnosticActions = actions; }

        public List<Map<String, String>> getRemediations() { return remediations; }
        public void setRemediations(List<Map<String, String>> rems) { this.remediations = rems; }

        public boolean isAiGenerated() { return aiGenerated; }
        public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }

        public String getModelUsed() { return modelUsed; }
        public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    }

    public ExplanationResult generateExplanation(
            IncidentEntity incident,
            List<CandidateEvaluation> rankedCandidates,
            DivergenceReport divergenceReport,
            List<SpanEntity> incidentSpans) {

        if (!"disabled".equalsIgnoreCase(llmProvider) && apiKey != null && !apiKey.isBlank()) {
            try {
                return callLlmApi(incident, rankedCandidates, divergenceReport, incidentSpans);
            } catch (Exception ex) {
                log.warn("External LLM API call failed ({}), activating deterministic template engine fallback.", ex.getMessage());
            }
        }

        // Deterministic template engine fallback
        return generateDeterministicTemplate(incident, rankedCandidates, divergenceReport, incidentSpans);
    }

    private ExplanationResult generateDeterministicTemplate(
            IncidentEntity incident,
            List<CandidateEvaluation> rankedCandidates,
            DivergenceReport divergenceReport,
            List<SpanEntity> incidentSpans) {

        ExplanationResult res = new ExplanationResult();
        res.setAiGenerated(false);
        res.setModelUsed("DETERMINISTIC_EXPLANATION_ENGINE_V1");

        CandidateEvaluation topCandidate = rankedCandidates.isEmpty() ? null : rankedCandidates.get(0);
        String topCause = (topCandidate != null) ?
                String.format("%s (%s, score: %.2f)", topCandidate.getOperationName(), topCandidate.getServiceName(), topCandidate.getFinalScore())
                : incident.getServiceName() + " operation failure";

        // 1. Structured Summary
        res.setSummary(String.format(
                "Incident %s: %s detected on service '%s' during operation '%s'. " +
                "Algorithmic root-cause analysis identified '%s' as the primary causal origin with confidence %.1f%% based on %d observed execution spans across %d traces.",
                incident.getId(), incident.getDetectionRule(), incident.getServiceName(),
                incident.getOperationName() != null ? incident.getOperationName() : "N/A",
                topCause,
                (topCandidate != null ? topCandidate.getConfidence() * 100.0 : 85.0),
                incidentSpans.size(),
                incident.getImpactedTracesCount()
        ));

        // 2. Step-by-Step Failure Propagation Narrative
        StringBuilder narrative = new StringBuilder();
        narrative.append("### Step-by-Step Failure Propagation Narrative\n\n");

        if (divergenceReport != null && divergenceReport.getEarliestDivergence() != null) {
            var earliest = divergenceReport.getEarliestDivergence();
            narrative.append(String.format("1. **Origin of Divergence (%s)**: At the downstream boundary, span `%s` in service `%s` triggered a %s (%s). Measured latency: %.1f ms vs baseline %.1f ms.\n",
                    earliest.getServiceName(),
                    earliest.getTargetSpanId() != null ? earliest.getTargetSpanId() : "unrecorded",
                    earliest.getServiceName(),
                    earliest.getDivergenceType(),
                    earliest.getExplanation(),
                    earliest.getTargetDurationMs(),
                    earliest.getBaselineDurationMs()));
        } else {
            narrative.append(String.format("1. **Trigger Phase**: Anomaly initiated in `%s` under detection rule `%s`.\n",
                    incident.getServiceName(), incident.getDetectionRule()));
        }

        if (topCandidate != null) {
            narrative.append(String.format("2. **Causal Impact on Downstream Dependencies**: Operation `%s` exhibited %s with blast radius index %.2f.\n",
                    topCandidate.getOperationName(),
                    topCandidate.getErrorStatusScore() > 0 ? "execution exceptions" : "latency dilation",
                    topCandidate.getBlastRadiusScore()));

            for (String obs : topCandidate.getSupportingObservations()) {
                narrative.append(String.format("   - *Observation*: %s\n", obs));
            }
        }

        narrative.append("3. **Cascade Propagation**: Upstream callers (Order Service orchestration) awaited downstream response until client timeouts were reached or error responses were propagated to clients.\n");

        if (topCandidate != null && topCandidate.getAlternativeHypothesis() != null) {
            narrative.append(String.format("\n4. **Hypothesis Evaluation**: %s\n", topCandidate.getAlternativeHypothesis()));
        }

        res.setFailureNarrative(narrative.toString());

        // 3. Diagnostic Actions
        res.getDiagnosticActions().add(String.format(
                "Inspect downstream service logs for '%s' around incident onset timestamp (%s).",
                topCandidate != null ? topCandidate.getServiceName() : incident.getServiceName(),
                incident.getFirstSeen()));
        res.getDiagnosticActions().add(String.format(
                "Verify database connection pool and lock contention metrics on '%s' data layer.",
                topCandidate != null ? topCandidate.getServiceName() : "downstream dependencies"));
        res.getDiagnosticActions().add(
                "Cross-reference downstream acquiring/partner gateway SLA metrics for external network timeouts.");

        // 4. Remediation Actions with Risk Warnings
        Map<String, String> rem1 = new LinkedHashMap<>();
        rem1.put("action", "Adjust Service Client Timeout & Circuit Breaker Thresholds");
        rem1.put("targetService", incident.getServiceName());
        rem1.put("riskLevel", "LOW");
        rem1.put("riskWarning", "Ensure fallback responses are safely handled by upstream clients to avoid silent data truncation.");
        res.getRemediations().add(rem1);

        Map<String, String> rem2 = new LinkedHashMap<>();
        rem2.put("action", "Restart or Horizontally Scale Impacted Service Instance");
        rem2.put("targetService", topCandidate != null ? topCandidate.getServiceName() : incident.getServiceName());
        rem2.put("riskLevel", "MEDIUM");
        rem2.put("riskWarning", "Transient connection drops may occur during pod/process cycling. Graceful drain is recommended.");
        res.getRemediations().add(rem2);

        Map<String, String> rem3 = new LinkedHashMap<>();
        rem3.put("action", "Release Row Lock / Rollback Stalled Distributed Transaction");
        rem3.put("targetService", topCandidate != null ? topCandidate.getServiceName() : "database");
        rem3.put("riskLevel", "HIGH");
        rem3.put("riskWarning", "Manual transaction termination can cause state inconsistency if saga compensating events are skipped.");
        res.getRemediations().add(rem3);

        return res;
    }

    private ExplanationResult callLlmApi(
            IncidentEntity incident,
            List<CandidateEvaluation> rankedCandidates,
            DivergenceReport divergenceReport,
            List<SpanEntity> incidentSpans) throws Exception {

        String url = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "https://api.openai.com/v1/chat/completions";

        String prompt = buildPrompt(incident, rankedCandidates, divergenceReport, incidentSpans);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("temperature", 0.2);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You are TraceMind AI root-cause investigator. Analyze distributed telemetry facts strictly. " +
                           "Do not hallucinate unobserved traces or spans. Return valid JSON only with keys: " +
                           "summary (string), failureNarrative (string markdown), diagnosticActions (array of strings), " +
                           "remediations (array of objects with action, targetService, riskLevel, riskWarning)."
        ));
        messages.add(Map.of("role", "user", "content", prompt));
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String responseBody = restTemplate.postForObject(url, entity, String.class);

        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").get(0).path("message").path("content").asText();

        // Extract JSON from markdown code block if present
        if (content.contains("```json")) {
            content = content.substring(content.indexOf("```json") + 7);
            content = content.substring(0, content.lastIndexOf("```"));
        } else if (content.contains("```")) {
            content = content.substring(content.indexOf("```") + 3);
            content = content.substring(0, content.lastIndexOf("```"));
        }

        JsonNode parsedContent = objectMapper.readTree(content.trim());
        ExplanationResult res = new ExplanationResult();
        res.setAiGenerated(true);
        res.setModelUsed(modelName);
        res.setSummary(parsedContent.path("summary").asText());
        res.setFailureNarrative(parsedContent.path("failureNarrative").asText());

        if (parsedContent.has("diagnosticActions") && parsedContent.get("diagnosticActions").isArray()) {
            for (JsonNode action : parsedContent.get("diagnosticActions")) {
                res.getDiagnosticActions().add(action.asText());
            }
        }

        if (parsedContent.has("remediations") && parsedContent.get("remediations").isArray()) {
            for (JsonNode remNode : parsedContent.get("remediations")) {
                Map<String, String> rem = new LinkedHashMap<>();
                rem.put("action", remNode.path("action").asText());
                rem.put("targetService", remNode.path("targetService").asText());
                rem.put("riskLevel", remNode.path("riskLevel").asText("MEDIUM"));
                rem.put("riskWarning", remNode.path("riskWarning").asText());
                res.getRemediations().add(rem);
            }
        }

        return res;
    }

    private String buildPrompt(
            IncidentEntity incident,
            List<CandidateEvaluation> rankedCandidates,
            DivergenceReport divergenceReport,
            List<SpanEntity> incidentSpans) throws Exception {

        Map<String, Object> telemetryFacts = new LinkedHashMap<>();
        telemetryFacts.put("incidentId", incident.getId());
        telemetryFacts.put("serviceName", incident.getServiceName());
        telemetryFacts.put("detectionRule", incident.getDetectionRule());
        telemetryFacts.put("severity", incident.getSeverity());
        telemetryFacts.put("impactedTracesCount", incident.getImpactedTracesCount());
        telemetryFacts.put("firstSeen", incident.getFirstSeen().toString());
        telemetryFacts.put("lastSeen", incident.getLastSeen().toString());

        List<Map<String, Object>> candidateList = new ArrayList<>();
        for (CandidateEvaluation c : rankedCandidates) {
            Map<String, Object> cMap = new LinkedHashMap<>();
            cMap.put("rank", c.getRank());
            cMap.put("candidateKey", c.getCandidateKey());
            cMap.put("service", c.getServiceName());
            cMap.put("operation", c.getOperationName());
            cMap.put("compositeScore", c.getFinalScore());
            cMap.put("confidence", c.getConfidence());
            cMap.put("supportingObservations", c.getSupportingObservations());
            cMap.put("counterevidence", c.getCounterevidence());
            candidateList.add(cMap);
        }
        telemetryFacts.put("rankedCandidates", candidateList);

        if (divergenceReport != null && divergenceReport.getEarliestDivergence() != null) {
            var earliest = divergenceReport.getEarliestDivergence();
            telemetryFacts.put("earliestDivergence", Map.of(
                    "service", earliest.getServiceName(),
                    "operation", earliest.getOperationName(),
                    "type", earliest.getDivergenceType(),
                    "targetDurationMs", earliest.getTargetDurationMs(),
                    "baselineDurationMs", earliest.getBaselineDurationMs(),
                    "explanation", earliest.getExplanation()
            ));
        }

        return "Analyze this telemetry fact payload and produce an explainable root cause diagnosis:\n" +
                objectMapper.writeValueAsString(telemetryFacts);
    }
}
