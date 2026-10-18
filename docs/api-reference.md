# TraceMind — REST API Specification & OpenAPI Reference

TraceMind exposes versioned REST APIs (`/api/v1/`) and standard OpenTelemetry OTLP endpoints (`/v1/`). All APIs return standard JSON, include HTTP status codes, and are fully documented via Swagger/OpenAPI UI at `http://localhost:8080/swagger-ui.html`.

---

## 1. Telemetry Ingestion Endpoints (OpenTelemetry Standard)

### Ingest OTLP Traces
`POST /v1/traces` or `POST /api/v1/telemetry/traces`
- **Content-Type**: `application/json`
- **Description**: Ingests distributed trace spans in standard OTLP JSON (with `resourceSpans`) or flat span array format.
- **Request Body Example**:
  ```json
  [
    {
      "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
      "spanId": "00f067aa0ba902b7",
      "parentSpanId": null,
      "serviceName": "order-service",
      "operationName": "POST /api/orders",
      "kind": "SERVER",
      "statusCode": "OK",
      "durationMs": 85.0,
      "startTimeUnixNano": 1718000000000000,
      "endTimeUnixNano": 1718000085000000
    }
  ]
  ```
- **Response**: `200 OK`
  ```json
  { "status": "SUCCESS", "spansIngested": 1 }
  ```

---

## 2. Overview & Fleet Metrics

### Get Global Observability Metrics
`GET /api/v1/metrics/overview`
- **Response**: `200 OK`
  ```json
  {
    "totalTraces": 142,
    "errorTraces": 12,
    "errorRate": 8.5,
    "avgLatencyMs": 94.2,
    "p95LatencyMs": 780.0,
    "activeIncidents": 2,
    "servicesCount": 3,
    "telemetryConnected": true,
    "liveTracesCount": 118,
    "demoTracesCount": 24,
    "recentTraces": [ ... ],
    "recentIncidents": [ ... ]
  }
  ```

---

## 3. Service Topology & Health Registry

### Get Directed Service Graph (React Flow format)
`GET /api/v1/topology`
- **Response**: `200 OK`
  ```json
  {
    "nodes": [
      {
        "id": "order-service",
        "type": "serviceNode",
        "position": { "x": 100, "y": 150 },
        "data": {
          "label": "order-service",
          "status": "HEALTHY",
          "callCount": 142,
          "errorCount": 12,
          "errorRate": 8.5,
          "avgDurationMs": 94.2,
          "activeIncidents": 0
        }
      }
    ],
    "edges": [
      {
        "id": "order-service->payment-service",
        "source": "order-service",
        "target": "payment-service",
        "animated": true,
        "data": {
          "callCount": 142,
          "errorCount": 8,
          "errorRate": 5.6,
          "avgDurationMs": 42.1,
          "status": "NORMAL"
        }
      }
    ]
  }
  ```

### List Services
`GET /api/v1/services`
- **Response**: `200 OK` array of `ServiceSummary` objects.

---

## 4. Distributed Trace Explorer & Divergence Analysis

### Search Traces
`GET /api/v1/traces?service={service}&status={status}&minDuration={ms}&page={page}&size={size}`
- **Query Parameters**:
  - `service` (optional): Filter by root service (`order-service`).
  - `status` (optional): `OK` or `ERROR`.
  - `minDuration` (optional): Minimum duration in ms.
  - `page`, `size`: Pagination controls.
- **Response**: `200 OK` Page of `TraceEntity`.

### Get Trace Hierarchy & Waterfall
`GET /api/v1/traces/{traceId}`
- **Response**: `200 OK` with full waterfall span tree, depth offsets, and OpenTelemetry JSON attributes.

### Algorithmic Trace Comparison
`GET /api/v1/traces/compare?baselineTraceId={id}&targetTraceId={id}`
- **Response**: `200 OK` with `earliestDivergence`, `divergencePoints` list, latency deltas, and telemetry comparison limitations.

---

## 5. Incidents & Root-Cause Analysis (RCA)

### List Incidents
`GET /api/v1/incidents?status={status}&page={page}&size={size}`
- **Response**: `200 OK` Page of `IncidentEntity`.

### Get Incident Dossier & Evidence
`GET /api/v1/incidents/{id}`
- **Response**: `200 OK` containing incident metadata, supporting telemetry evidence list, and computed RCA summary.

### Acknowledge Incident
`POST /api/v1/incidents/{id}/acknowledge`
- **Request Body**: `{ "user": "SRE-OnCall" }`
- **Response**: `200 OK`

### Resolve Incident
`POST /api/v1/incidents/{id}/resolve`
- **Response**: `200 OK`

### Get / Re-analyze Root Cause
`GET /api/v1/incidents/{id}/rca` or `POST /api/v1/incidents/{id}/rca/reanalyze`
- **Response**: `200 OK` `RootCauseAnalysisEntity` with:
  - `primaryCandidate`: string
  - `confidenceScore`: double
  - `explanationSummary`: string
  - `failureNarrative`: markdown string
  - `rankedHypothesesJson`: 7-factor ranked candidate causes array
  - `suggestedActionsJson`: diagnostic actions and risk-rated remediations

---

## 6. Controlled Replay & Fault Injection

### Validate and Preview Replay
`POST /api/v1/replay/preview`
- **Request Body**:
  ```json
  {
    "scenarioName": "PAYMENT_TIMEOUT",
    "requestCount": 5,
    "targetEnvironment": "sandbox"
  }
  ```
- **Response**: `200 OK` with validation status, warnings, and payload preview.

### Execute Replay
`POST /api/v1/replay/execute`
- **Request Body**: `{ "scenarioName": "...", "requestCount": 5, "targetEnvironment": "sandbox", "confirmed": true }`
- **Response**: `200 OK` `ReplayRunEntity`.

---

## 7. Demo Telemetry Generator

### Seed Realistic Multi-Service Telemetry
`POST /api/v1/demo/generate`
- **Response**: `200 OK`
  ```json
  {
    "status": "SUCCESS",
    "message": "Generated realistic multi-service demo telemetry labeled explicitly as DEMO data",
    "tracesCreated": 8,
    "traceIds": [ ... ]
  }
  ```
