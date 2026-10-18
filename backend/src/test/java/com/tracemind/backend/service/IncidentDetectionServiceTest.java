package com.tracemind.backend.service;

import com.tracemind.backend.entity.IncidentEntity;
import com.tracemind.backend.entity.IncidentEvidenceEntity;
import com.tracemind.backend.entity.SpanEntity;
import com.tracemind.backend.repository.IncidentEvidenceRepository;
import com.tracemind.backend.repository.IncidentRepository;
import com.tracemind.backend.repository.SpanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IncidentDetectionServiceTest {

    private IncidentRepository incidentRepository;
    private IncidentEvidenceRepository evidenceRepository;
    private SpanRepository spanRepository;
    private RootCauseAnalysisService rcaService;
    private IncidentDetectionService detectionService;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        evidenceRepository = mock(IncidentEvidenceRepository.class);
        spanRepository = mock(SpanRepository.class);
        rcaService = mock(RootCauseAnalysisService.class);

        detectionService = new IncidentDetectionService(
                incidentRepository,
                evidenceRepository,
                spanRepository,
                rcaService
        );

        when(incidentRepository.save(any(IncidentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(IncidentEvidenceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Detects error burst and captures evidence")
    void testDetectsErrorBurst() {
        SpanEntity errorSpan = new SpanEntity();
        errorSpan.setSpanId("span-err-1");
        errorSpan.setTraceId("trace-err-1");
        errorSpan.setServiceName("payment-service");
        errorSpan.setOperationName("POST /api/payments");
        errorSpan.setError(true);
        errorSpan.setStatusCode("ERROR");
        errorSpan.setStatusMessage("Internal acquiring bank authorization failure");
        errorSpan.setDurationMs(200.0);
        errorSpan.setStartTime(Instant.now());
        errorSpan.setEndTime(Instant.now().plusMillis(200));

        when(incidentRepository.findFirstByServiceNameAndDetectionRuleAndStatus(any(), any(), any()))
                .thenReturn(Optional.empty());

        detectionService.evaluateSpan(errorSpan);

        ArgumentCaptor<IncidentEntity> incCaptor = ArgumentCaptor.forClass(IncidentEntity.class);
        verify(incidentRepository, atLeastOnce()).save(incCaptor.capture());

        IncidentEntity saved = incCaptor.getValue();
        assertEquals("payment-service", saved.getServiceName());
        assertEquals("ERROR_BURST", saved.getDetectionRule());
        assertEquals("CRITICAL", saved.getSeverity());

        verify(evidenceRepository, atLeastOnce()).save(any(IncidentEvidenceEntity.class));
    }

    @Test
    @DisplayName("Deduplicates multiple errors into a single active incident")
    void testDeduplication() {
        IncidentEntity existing = new IncidentEntity();
        existing.setId("inc-existing-1");
        existing.setServiceName("inventory-service");
        existing.setDetectionRule("ERROR_BURST");
        existing.setStatus("ACTIVE");
        existing.setImpactedTracesCount(1);
        existing.setSeverity("HIGH");

        when(incidentRepository.findFirstByServiceNameAndDetectionRuleAndStatus("inventory-service", "ERROR_BURST", "ACTIVE"))
                .thenReturn(Optional.of(existing));

        SpanEntity secondError = new SpanEntity();
        secondError.setSpanId("span-err-2");
        secondError.setTraceId("trace-err-2");
        secondError.setServiceName("inventory-service");
        secondError.setOperationName("POST /api/inventory/reserve");
        secondError.setError(true);
        secondError.setStatusCode("ERROR");
        secondError.setDurationMs(300.0);
        secondError.setStartTime(Instant.now());
        secondError.setEndTime(Instant.now().plusMillis(300));

        detectionService.evaluateSpan(secondError);

        // Should increment impacted count rather than creating new incident
        assertEquals(2, existing.getImpactedTracesCount());
        verify(incidentRepository, times(1)).save(existing);
    }
}
