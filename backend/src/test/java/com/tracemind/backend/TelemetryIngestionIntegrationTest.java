package com.tracemind.backend;

import com.tracemind.backend.entity.ServiceDependencyEntity;
import com.tracemind.backend.entity.SpanEntity;
import com.tracemind.backend.entity.TraceEntity;
import com.tracemind.backend.repository.ServiceDependencyRepository;
import com.tracemind.backend.repository.SpanRepository;
import com.tracemind.backend.repository.TraceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TelemetryIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private SpanRepository spanRepository;

    @Autowired
    private ServiceDependencyRepository dependencyRepository;

    @Test
    @DisplayName("End-to-End: Ingest OTLP JSON spans and query via API")
    void testOtelSpanIngestion() throws Exception {
        String testTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String rootSpanId = "00f067aa0ba902b7";
        String childSpanId = "5fb397be34d23b0f";

        String jsonPayload = """
            [
              {
                "traceId": "%s",
                "spanId": "%s",
                "parentSpanId": null,
                "serviceName": "order-service",
                "operationName": "POST /api/orders",
                "kind": "SERVER",
                "statusCode": "OK",
                "durationMs": 85.0
              },
              {
                "traceId": "%s",
                "spanId": "%s",
                "parentSpanId": "%s",
                "serviceName": "payment-service",
                "operationName": "POST /api/payments",
                "kind": "SERVER",
                "statusCode": "OK",
                "durationMs": 35.0
              }
            ]
            """.formatted(testTraceId, rootSpanId, testTraceId, childSpanId, rootSpanId);

        // 1. Ingest via POST /v1/traces
        mockMvc.perform(post("/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.spansIngested").value(2));

        // 2. Verify Database Persistence
        List<SpanEntity> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(testTraceId);
        assertEquals(2, spans.size(), "Two spans must be persisted");

        assertTrue(traceRepository.findById(testTraceId).isPresent(), "Trace aggregate must be created");
        TraceEntity trace = traceRepository.findById(testTraceId).get();
        assertEquals("order-service", trace.getRootService());
        assertEquals("OK", trace.getStatus());

        // 3. Verify Query via Trace Details API
        mockMvc.perform(get("/api/v1/traces/" + testTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trace.traceId").value(testTraceId))
                .andExpect(jsonPath("$.totalSpans").value(2));

        // 4. Verify Overview Metrics API
        mockMvc.perform(get("/api/v1/metrics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetryConnected").value(true));
    }
}
