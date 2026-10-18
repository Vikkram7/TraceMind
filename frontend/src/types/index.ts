export interface Trace {
  traceId: string;
  rootService: string;
  rootOperation: string;
  status: 'OK' | 'ERROR' | 'UNSET';
  startTime: string;
  endTime: string;
  durationMs: number;
  spanCount: number;
  errorCount: number;
  hasDivergence: boolean;
  demo: boolean;
}

export interface Span {
  spanId: string;
  traceId: string;
  parentSpanId?: string | null;
  serviceName: string;
  operationName: string;
  kind: 'SERVER' | 'CLIENT' | 'PRODUCER' | 'CONSUMER' | 'INTERNAL';
  statusCode: 'OK' | 'ERROR' | 'UNSET';
  statusMessage?: string | null;
  startTime: string;
  endTime: string;
  durationMs: number;
  isError: boolean;
  attributesJson?: string | null;
  relativeOffsetMs?: number;
}

export interface TraceDetailResponse {
  trace: Trace;
  spans: Span[];
  totalSpans: number;
}

export interface IncidentEvidence {
  id: string;
  incidentId: string;
  evidenceType: 'TRACE' | 'SPAN' | 'LOG' | 'METRIC_ANOMALY';
  targetId: string;
  description: string;
  metricValue?: number | null;
  baselineValue?: number | null;
  deviationFactor?: number | null;
  createdAt: string;
}

export interface Incident {
  id: string;
  title: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'ACTIVE' | 'ACKNOWLEDGED' | 'RESOLVED';
  detectionRule: 'LATENCY_SPIKE' | 'ERROR_BURST' | 'TIMEOUT_CASCADE' | 'ROLLING_ANOMALY';
  serviceName: string;
  operationName?: string;
  description: string;
  confidenceScore: number;
  firstSeen: string;
  lastSeen: string;
  impactedTracesCount: number;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
}

export interface RankedHypothesis {
  candidateKey: string;
  serviceName: string;
  operationName: string;
  primarySpanId: string;
  finalScore: number;
  confidence: number;
  rank: number;
  temporalPrecedenceScore: number;
  errorStatusScore: number;
  latencyAnomalyScore: number;
  blastRadiusScore: number;
  recurrenceScore: number;
  counterevidenceScore: number;
  completenessPenalty: number;
  supportingObservations: string[];
  counterevidence: string[];
  alternativeHypothesis?: string;
}

export interface RootCauseAnalysis {
  id: string;
  incidentId: string;
  primaryCandidate: string;
  confidenceScore: number;
  explanationSummary: string;
  failureNarrative: string;
  rankedHypothesesJson?: string;
  suggestedActionsJson?: string;
  aiGenerated: boolean;
  aiModelUsed: string;
  createdAt: string;
}

export interface ServiceSummary {
  id: string;
  name: string;
  environment: string;
  version: string;
  status: 'HEALTHY' | 'DEGRADED' | 'CRITICAL';
  lastHeartbeat: string;
  totalSpans: number;
  errorSpans: number;
  errorRate: number;
  avgDurationMs: number;
}

export interface TopologyNodeData {
  label: string;
  status: 'HEALTHY' | 'DEGRADED' | 'CRITICAL';
  callCount: number;
  errorCount: number;
  errorRate: number;
  avgDurationMs: number;
  activeIncidents: number;
  [key: string]: unknown;
}

export interface TopologyNode {
  id: string;
  type: string;
  position: { x: number; y: number };
  data: TopologyNodeData;
}

export interface TopologyEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  animated?: boolean;
  data?: {
    callCount: number;
    errorCount: number;
    errorRate: number;
    avgDurationMs: number;
    p95DurationMs: number;
    status: 'NORMAL' | 'SLOW' | 'ERROR';
  };
}

export interface TopologyGraph {
  nodes: TopologyNode[];
  edges: TopologyEdge[];
}

export interface DivergencePoint {
  divergenceType: 'STATUS_MISMATCH' | 'LATENCY_DIVERGENCE' | 'CALL_MISSING' | 'TOPOLOGY_DRIFT';
  serviceName: string;
  operationName: string;
  baselineSpanId?: string | null;
  targetSpanId?: string | null;
  baselineDurationMs: number;
  targetDurationMs: number;
  durationDeltaMs: number;
  baselineStatus: string;
  targetStatus: string;
  explanation: string;
  earliestDivergence: boolean;
}

export interface DivergenceReport {
  baselineTraceId: string;
  targetTraceId: string;
  divergencePoints: DivergencePoint[];
  earliestDivergence?: DivergencePoint | null;
  limitations: string[];
}

export interface OverviewMetrics {
  totalTraces: number;
  errorTraces: number;
  errorRate: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
  activeIncidents: number;
  servicesCount: number;
  telemetryConnected: boolean;
  liveTracesCount: number;
  demoTracesCount: number;
  recentTraces: Trace[];
  recentIncidents: Incident[];
}

export interface ReplayRun {
  id: string;
  scenarioName: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  targetEnvironment: string;
  configurationJson?: string;
  resultSummary?: string;
  startedAt: string;
  completedAt?: string;
}
