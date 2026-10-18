import {
  OverviewMetrics,
  TopologyGraph,
  ServiceSummary,
  Trace,
  TraceDetailResponse,
  DivergenceReport,
  Incident,
  IncidentEvidence,
  RootCauseAnalysis,
  ReplayRun
} from '../types';

const BASE_URL = '/api/v1';

async function fetchJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  });

  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(`API Error [${res.status}]: ${errorText || res.statusText}`);
  }

  return res.json();
}

export const api = {
  // Metrics & Overview
  getOverviewMetrics: () => fetchJson<OverviewMetrics>(`${BASE_URL}/metrics/overview`),

  // Topology & Services
  getTopology: () => fetchJson<TopologyGraph>(`${BASE_URL}/topology`),
  getServices: () => fetchJson<ServiceSummary[]>(`${BASE_URL}/services`),
  getServiceHealth: (name: string) => fetchJson<unknown>(`${BASE_URL}/services/${encodeURIComponent(name)}/health`),

  // Traces
  getTraces: (params?: { service?: string; status?: string; minDuration?: number; page?: number; size?: number }) => {
    const searchParams = new URLSearchParams();
    if (params?.service) searchParams.set('service', params.service);
    if (params?.status) searchParams.set('status', params.status);
    if (params?.minDuration) searchParams.set('minDuration', params.minDuration.toString());
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());
    return fetchJson<{ content: Trace[]; totalPages: number; totalElements: number }>(
      `${BASE_URL}/traces?${searchParams.toString()}`
    );
  },
  getTraceDetails: (traceId: string) => fetchJson<TraceDetailResponse>(`${BASE_URL}/traces/${traceId}`),
  compareTraces: (baselineTraceId: string, targetTraceId: string) =>
    fetchJson<DivergenceReport>(
      `${BASE_URL}/traces/compare?baselineTraceId=${encodeURIComponent(baselineTraceId)}&targetTraceId=${encodeURIComponent(targetTraceId)}`
    ),

  // Incidents & RCA
  getIncidents: (params?: { status?: string; page?: number; size?: number }) => {
    const searchParams = new URLSearchParams();
    if (params?.status) searchParams.set('status', params.status);
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());
    return fetchJson<{ content: Incident[]; totalPages: number; totalElements: number }>(
      `${BASE_URL}/incidents?${searchParams.toString()}`
    );
  },
  getIncidentDetails: (id: string) =>
    fetchJson<{ incident: Incident; evidences: IncidentEvidence[]; rootCauseAnalysis?: RootCauseAnalysis }>(
      `${BASE_URL}/incidents/${id}`
    ),
  acknowledgeIncident: (id: string, user = 'SRE-OnCall') =>
    fetchJson<Incident>(`${BASE_URL}/incidents/${id}/acknowledge`, {
      method: 'POST',
      body: JSON.stringify({ user }),
    }),
  resolveIncident: (id: string) =>
    fetchJson<Incident>(`${BASE_URL}/incidents/${id}/resolve`, {
      method: 'POST',
    }),
  getIncidentRca: (id: string) => fetchJson<RootCauseAnalysis>(`${BASE_URL}/incidents/${id}/rca`),
  reanalyzeIncidentRca: (id: string) =>
    fetchJson<RootCauseAnalysis>(`${BASE_URL}/incidents/${id}/rca/reanalyze`, {
      method: 'POST',
    }),

  // Controlled Replay & Fault Lab
  previewReplay: (request: { scenarioName: string; requestCount: number; targetEnvironment: string }) =>
    fetchJson<{
      valid: boolean;
      previewMessage: string;
      warnings: string[];
      simulatedRequests: Array<{ requestIndex: number; targetEndpoint: string; payload: unknown }>;
    }>(`${BASE_URL}/replay/preview`, {
      method: 'POST',
      body: JSON.stringify(request),
    }),
  executeReplay: (request: { scenarioName: string; requestCount: number; targetEnvironment: string; confirmed: boolean }) =>
    fetchJson<ReplayRun>(`${BASE_URL}/replay/execute`, {
      method: 'POST',
      body: JSON.stringify(request),
    }),
  getReplayHistory: () => fetchJson<ReplayRun[]>(`${BASE_URL}/replay/history`),

  // Demo Data Generator
  generateDemoData: () =>
    fetchJson<{ status: string; message: string; tracesCreated: number; traceIds: string[] }>(
      `${BASE_URL}/demo/generate`,
      { method: 'POST' }
    ),

  // Live Traffic Trigger to Order Service
  triggerLiveOrder: async () => {
    // If testing live microservice directly
    try {
      const orderBody = {
        customerId: 'cust-browser-live',
        items: [{ itemId: 'item-live-01', quantity: 2, price: 49.99 }],
        paymentMethod: 'CREDIT_CARD',
      };
      const res = await fetch('http://localhost:8081/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderBody),
      });
      return res.json();
    } catch {
      // If order-service is not running directly on localhost:8081, fallback to demo dataset
      return api.generateDemoData();
    }
  },
};
