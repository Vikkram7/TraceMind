# TraceMind — Distributed Systems Observability & Automated Root-Cause Analysis Platform

> **Tagline:** *Every failure has a story. Find its origin.*

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-W3C%20Standard-purple.svg)](https://opentelemetry.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 1. Executive Overview & Mission

**TraceMind** is an enterprise-grade distributed systems observability and automated root-cause analysis platform. Unlike passive monitoring tools or basic log dashboards, TraceMind ingests distributed telemetry conforming to the **W3C TraceContext recommendation**, dynamically reconstructs directed service topologies, detects operational anomalies via static SLAs and rolling statistical baselines, isolates execution divergences between healthy and failing traces, and deterministically ranks candidate root causes using an explainable **7-factor causal index**.

### Why TraceMind?
In modern microservice architectures, an upstream orchestrator (e.g., `Order Service`) calling a downstream microservice (e.g., `Payment Service`) that experiences a 3500ms timeout will record a total duration of 3550ms. Traditional APM heuristics commit the **Root Caller Fallacy** — blindly blaming `Order Service` because it started first and had the longest duration.

**TraceMind solves this.** Through tree-depth-aware causal ranking, upstream caller penalties, and sequence alignment divergence analysis, TraceMind correctly isolates the downstream origin of failure with **100% Top-1 accuracy** across standardized benchmark scenarios.

---

## 2. High-Level Architecture

```mermaid
flowchart TD
    subgraph PresentationLayer["Web Console (Port 5173)"]
        UI["React 18 + Vite + React Flow Developer Console"]
    end

    subgraph MicroserviceEcosystem["Monitored Microservices Fleet"]
        OS["Order Service (:8081)"] -->|"W3C traceparent (HTTP)"| PS["Payment Service (:8082)"]
        OS -->|"W3C traceparent (HTTP)"| IS["Inventory Service (:8083)"]
    end

    subgraph TelemetryPipeline["Telemetry Ingestion"]
        OS & PS & IS -->|OTLP JSON| Collector["OpenTelemetry Collector (:4317 / :4318)"]
        OS & PS & IS -.->|Direct Fallback| BackendAPI["TraceMind Ingestion API (:8080)"]
        Collector -->|Forward OTLP| BackendAPI
    end

    subgraph CoreEngine["TraceMind Core Backend (Spring Boot 3.3 / Java 21)"]
        BackendAPI --> TopoEngine["Dynamic Topology Engine"]
        BackendAPI --> AnomalyEngine["Anomaly & Incident Engine"]
        BackendAPI --> DivergenceEngine["Trace Divergence Analyzer"]
        BackendAPI --> RCAEngine["7-Factor Causal Ranking Engine"]
        BackendAPI --> LLMEngine["AI Diagnostic Narrative Generator"]
        BackendAPI --> ReplayLab["Controlled Sandbox Replay Lab"]
        
        BackendAPI --> DB[("PostgreSQL / In-Memory H2")]
    end

    UI -->|REST / SSE| BackendAPI
```

---

## 3. Technology Stack & Pinned Versions

| Layer | Technology | Pinned Version | Purpose |
|---|---|---|---|
| **Language** | Java (Oracle/OpenJDK) | `21.0.6 LTS` | Microservices runtime & core backend engine |
| **Framework** | Spring Boot | `3.3.5` | Dependency injection, REST API, JPA persistence |
| **Build System** | Apache Maven | `3.9.11` | Multi-module build orchestrator |
| **Frontend** | React + TypeScript + Vite | `React 18.3`, `Vite 5.4`, `TS 5.6` | Dark graphite dev-tool console |
| **Graph Visualizer** | `@xyflow/react` (React Flow) | `^12.3.6` | Interactive directed service dependency graphs |
| **Distributed Tracing** | OpenTelemetry / W3C TraceContext | `1.43.0` | Ingestion of traces, spans, attributes, and baggage |
| **Database** | PostgreSQL / H2 | `PostgreSQL 16` / `H2 2.3` | Relational storage of traces, incidents, and RCA |
| **Containers** | Docker & Docker Compose | `Compose v2` | Containerized infrastructure |

---

## 4. Key Platform Features

1. **Full-Stack Observability**: Ingests standard OTLP JSON spans (`/v1/traces`), tracking latency percentiles (P50, P95), error rates, throughput, and status.
2. **Dynamic Service Topology**: Graph reconstructed from observed span boundaries with real-time health indicators and animated error cascade edges.
3. **Multidimensional Incident Detection**: Static latency SLAs, rolling baseline statistical anomalies (Z-Score $\ge 2.5\sigma$, IQR), timeout cascades, and deduplication into active incidents.
4. **Hierarchical Trace Waterfall Explorer**: Visualizes span parent-child depths, relative offsets, and raw telemetry attributes with sub-millisecond precision.
5. **Execution Trace Divergence Engine**: Aligns execution sequences between healthy baseline traces and failing target traces, isolating the earliest point of divergence.
6. **7-Factor Deterministic Root-Cause Ranking**: Multi-factor causal scoring evaluating temporal precedence, error status, latency dilation ratio, blast radius, recurrence, caller penalties, and epistemic completeness.
7. **AI-Assisted Diagnostic Narratives**: Step-by-step failure propagation narrative with risk-rated remediations and zero-cost local deterministic fallback.
8. **Controlled Replay & Fault Laboratory**: Two-phase validated synthetic request dispatcher with strict sandbox isolation preventing external calls.

---

## 5. Prerequisites & Quickstart

### Prerequisites:
- **Java 21 LTS** installed (`java -version`)
- **Maven 3.9+** installed (`mvn -v`)
- **Node.js 22+** installed (`node -v`)
- *(Optional)* Docker Desktop for containerized deployment

---

### Option A: Direct Local Startup (Recommended for Zero-Docker Development)

TraceMind includes standalone scripts that launch the backend engine, microservices fleet, and frontend console simultaneously:

1. **Build all modules:**
   ```powershell
   mvn clean compile
   ```

2. **Launch all services:**
   ```powershell
   .\start-all.ps1
   ```
   *This starts:*
   - **TraceMind Backend**: [http://localhost:8080](http://localhost:8080)
   - **OpenAPI / Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **Web Console**: [http://localhost:5173](http://localhost:5173)
   - **Order Service**: [http://localhost:8081](http://localhost:8081)
   - **Payment Service**: [http://localhost:8082](http://localhost:8082)
   - **Inventory Service**: [http://localhost:8083](http://localhost:8083)

3. **To stop all services:**
   ```powershell
   .\stop-all.ps1
   ```

---

### Option B: Docker Compose Deployment

```bash
docker compose -f infrastructure/docker-compose.yml up --build
```

---

## 6. End-to-End Demonstration Walkthrough (5 Minutes)

1. **Explore Overview**: Navigate to `http://localhost:5173`. Inspect the system KPI cards and telemetry status badge.
2. **Dispatch Live Traffic**: Click the **Dispatch Live Order** button in the top navbar. Order Service receives the request, injects a W3C `traceparent` header, calls Payment and Inventory services, and reports spans to TraceMind.
3. **Inspect Topology**: Open **Service Topology** in the sidebar. Click on `payment-service` to inspect call counts and latency.
4. **Waterfall Inspection**: Open **Trace Explorer**. Click on any trace to inspect the waterfall timeline and JSON attributes.
5. **Trigger Downstream Fault**: Open **Replay & Fault Lab**. Select **Downstream Payment Timeout** (5 requests). Click **Step 1: Validate & Preview**, check confirmation, and click **Step 2: Confirm & Run Scenario**.
6. **Review Incident & RCA**: Open **Incidents** to see the deduplicated `TIMEOUT_CASCADE` alert. Click **Investigate Evidence**, then click **Open Root-Cause Report** to view the 7-factor ranking and failure propagation narrative.

---

## 7. Evaluation Benchmark Results

TraceMind was evaluated against six benchmark scenarios:

| Scenario | Simulated Failure Mechanism | TraceMind Multi-Factor Index | Naive Duration Baseline |
|---|---|---|---|
| **S1: Healthy Baseline** | Normal eCommerce SLA | **0.0% False Positives** | 0.0% False Positives |
| **S2: Slow Downstream** | 1620ms Inventory DB Query | **100% Top-1 (`inventory-service`)** | 0% (Blamed `order-service`) |
| **S3: Downstream Timeout** | 3500ms Bank Gateway Timeout | **100% Top-1 (`payment-service`)** | 0% (Blamed `order-service`) |
| **S4: Error Burst** | Inventory Deadlock Spikes | **100% Top-1 (`inventory-service`)** | 100% |
| **S5: Competing Causes** | Inventory Deadlock + Rollback Timeout | **100% Top-1 (`inventory-service`)** | 0% (Blamed `order-service`) |
| **S6: Incomplete Telemetry** | Unobserved Intermediate Spans | **Confidence Discounted (<75%)** | High False Certainty |
| **Overall Top-1 Accuracy** | | **100.0%** (6/6) | 50.0% (3/6) |

To run the evaluation tests:
```powershell
mvn test -pl backend
```

---

## 8. Documentation Index

- [System Architecture & Data Flows](docs/architecture.md)
- [REST API & OpenAPI Specifications](docs/api-reference.md)
- [Empirical Evaluation Benchmark Report](docs/evaluation-benchmark.md)
- [Academic Final-Year CSE Project Dissertation Outline](docs/final-year-report-outline.md)
- [5-Minute Demo Script & Viva Voce Q&A](docs/viva-qa.md)

---

## 9. License

This project is licensed under the Apache 2.0 License.
