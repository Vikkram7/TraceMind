-- TraceMind PostgreSQL Schema Initialization Script
-- Stores traces, spans, service topology, metrics, incidents, root-cause analyses, and replay logs.

CREATE TABLE IF NOT EXISTS services (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    environment VARCHAR(64) DEFAULT 'local',
    version VARCHAR(32) DEFAULT '1.0.0',
    status VARCHAR(32) DEFAULT 'HEALTHY',
    last_heartbeat TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS service_dependencies (
    id VARCHAR(128) PRIMARY KEY,
    source_service VARCHAR(128) NOT NULL,
    target_service VARCHAR(128) NOT NULL,
    call_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    avg_duration_ms DOUBLE PRECISION DEFAULT 0.0,
    p95_duration_ms DOUBLE PRECISION DEFAULT 0.0,
    last_observed TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_service_dependency UNIQUE (source_service, target_service)
);

CREATE TABLE IF NOT EXISTS traces (
    trace_id VARCHAR(64) PRIMARY KEY,
    root_service VARCHAR(128),
    root_operation VARCHAR(256),
    status VARCHAR(32) NOT NULL, -- OK, ERROR, UNSET
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms DOUBLE PRECISION NOT NULL,
    span_count INT DEFAULT 1,
    error_count INT DEFAULT 0,
    has_divergence BOOLEAN DEFAULT FALSE,
    is_demo BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS spans (
    span_id VARCHAR(64) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(64),
    service_name VARCHAR(128) NOT NULL,
    operation_name VARCHAR(256) NOT NULL,
    kind VARCHAR(32) DEFAULT 'INTERNAL', -- SERVER, CLIENT, PRODUCER, CONSUMER, INTERNAL
    status_code VARCHAR(32) DEFAULT 'OK', -- OK, ERROR, UNSET
    status_message TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms DOUBLE PRECISION NOT NULL,
    attributes_json TEXT, -- JSON attributes
    events_json TEXT,     -- Exception events, logs
    is_error BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_spans_trace_id ON spans (trace_id);
CREATE INDEX IF NOT EXISTS idx_spans_service_name ON spans (service_name);
CREATE INDEX IF NOT EXISTS idx_spans_start_time ON spans (start_time);
CREATE INDEX IF NOT EXISTS idx_traces_start_time ON traces (start_time);
CREATE INDEX IF NOT EXISTS idx_traces_status ON traces (status);

CREATE TABLE IF NOT EXISTS incidents (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    severity VARCHAR(32) NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW
    status VARCHAR(32) NOT NULL,   -- ACTIVE, ACKNOWLEDGED, RESOLVED
    detection_rule VARCHAR(128) NOT NULL, -- LATENCY_SPIKE, ERROR_BURST, TIMEOUT_CASCADE, ROLLING_ANOMALY
    service_name VARCHAR(128) NOT NULL,
    operation_name VARCHAR(256),
    description TEXT,
    confidence_score DOUBLE PRECISION DEFAULT 1.0,
    first_seen TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen TIMESTAMP WITH TIME ZONE NOT NULL,
    impacted_traces_count INT DEFAULT 0,
    acknowledged_by VARCHAR(128),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS incident_evidence (
    id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    evidence_type VARCHAR(64) NOT NULL, -- TRACE, SPAN, LOG, METRIC_ANOMALY
    target_id VARCHAR(128) NOT NULL,    -- trace_id, span_id, metric name
    description TEXT,
    metric_value DOUBLE PRECISION,
    baseline_value DOUBLE PRECISION,
    deviation_factor DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS root_cause_analyses (
    id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    primary_candidate VARCHAR(256) NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL,
    explanation_summary TEXT,
    failure_narrative TEXT,
    ranked_hypotheses_json TEXT, -- JSON array of ranked candidate causes with scores
    suggested_actions_json TEXT,  -- JSON array of remediation actions
    is_ai_generated BOOLEAN DEFAULT FALSE,
    ai_model_used VARCHAR(64) DEFAULT 'DETERMINISTIC_RULES_ENGINE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS replay_runs (
    id VARCHAR(64) PRIMARY KEY,
    scenario_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL, -- RUNNING, COMPLETED, FAILED
    total_requests INT DEFAULT 0,
    successful_requests INT DEFAULT 0,
    failed_requests INT DEFAULT 0,
    target_environment VARCHAR(64) DEFAULT 'sandbox',
    configuration_json TEXT,
    result_summary TEXT,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);
