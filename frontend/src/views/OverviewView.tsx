import React from 'react';
import { OverviewMetrics, Trace, Incident } from '../types';
import { MetricCard } from '../components/common/MetricCard';
import { Badge } from '../components/common/Badge';
import {
  Activity,
  AlertTriangle,
  Clock,
  Zap,
  Server,
  Layers,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

interface OverviewViewProps {
  metrics: OverviewMetrics | null;
  onSelectTrace: (traceId: string) => void;
  onSelectIncident: (incidentId: string) => void;
  onNavigate: (view: any) => void;
}

export const OverviewView: React.FC<OverviewViewProps> = ({
  metrics,
  onSelectTrace,
  onSelectIncident,
  onNavigate,
}) => {
  if (!metrics) {
    return null;
  }

  const isHealthy = metrics.errorRate < 5.0 && metrics.activeIncidents === 0;

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="p-4 rounded-lg bg-gradient-to-r from-graphite-900 to-graphite-850 border border-graphite-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h2 className="text-lg font-bold text-graphite-100 font-sans">Distributed Fleet Telemetry</h2>
            <Badge variant={isHealthy ? 'healthy' : 'critical'}>
              {isHealthy ? 'SYSTEM OPERATIONAL' : 'ANOMALIES DETECTED'}
            </Badge>
          </div>
          <p className="text-xs font-mono text-graphite-400">
            Real-time telemetry ingestion from Order, Payment, and Inventory microservices.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="text-right font-mono text-xs">
            <span className="text-graphite-400 block">Telemetry Stream</span>
            <span className="text-cyan-400 font-semibold">
              {metrics.telemetryConnected ? 'W3C TraceContext Active' : 'Standby'}
            </span>
          </div>
          <button
            onClick={() => onNavigate('replay')}
            className="px-3 py-1.5 text-xs font-mono font-medium rounded bg-cyan-950 text-cyan-300 border border-cyan-800 hover:bg-cyan-900 transition-colors"
          >
            Launch Replay Lab
          </button>
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3">
        <MetricCard
          title="Total Traces"
          value={metrics.totalTraces.toLocaleString()}
          subtext={`Live: ${metrics.liveTracesCount} | Demo: ${metrics.demoTracesCount}`}
          icon={Layers}
        />
        <MetricCard
          title="Error Rate"
          value={`${metrics.errorRate}%`}
          subtext={`${metrics.errorTraces} failed execution traces`}
          icon={AlertCircle}
          variant={metrics.errorRate > 15 ? 'critical' : metrics.errorRate > 5 ? 'warning' : 'default'}
        />
        <MetricCard
          title="Avg Latency"
          value={`${metrics.avgLatencyMs} ms`}
          subtext="Mean end-to-end duration"
          icon={Clock}
        />
        <MetricCard
          title="P95 Latency"
          value={`${metrics.p95LatencyMs} ms`}
          subtext="95th percentile SLA boundary"
          icon={Zap}
          variant={metrics.p95LatencyMs > 800 ? 'warning' : 'default'}
        />
        <MetricCard
          title="Active Incidents"
          value={metrics.activeIncidents}
          subtext="Algorithmic anomalies detected"
          icon={AlertTriangle}
          variant={metrics.activeIncidents > 0 ? 'critical' : 'success'}
        />
        <MetricCard
          title="Services"
          value={metrics.servicesCount}
          subtext="Instrumented JVM runtimes"
          icon={Server}
        />
      </div>

      {/* Grid: Recent Incidents & Recent Traces */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Active Incidents Panel (5 cols) */}
        <div className="lg:col-span-5 rounded-lg border border-graphite-800 bg-graphite-900 flex flex-col overflow-hidden">
          <div className="px-4 py-3 bg-graphite-850 border-b border-graphite-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertTriangle size={15} className="text-crimson-400" />
              <h3 className="text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider">
                Active Operational Incidents
              </h3>
            </div>
            <button
              onClick={() => onNavigate('incidents')}
              className="text-xs font-mono text-cyan-400 hover:text-cyan-300 flex items-center gap-1"
            >
              <span>View All</span>
              <ArrowRight size={12} />
            </button>
          </div>

          <div className="divide-y divide-graphite-800/60 flex-1 overflow-y-auto max-h-[360px]">
            {metrics.recentIncidents && metrics.recentIncidents.length > 0 ? (
              metrics.recentIncidents.map((inc: Incident) => (
                <div
                  key={inc.id}
                  onClick={() => onSelectIncident(inc.id)}
                  className="p-3.5 hover:bg-graphite-800/40 cursor-pointer transition-colors"
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <Badge variant={inc.severity === 'CRITICAL' ? 'critical' : 'degraded'}>
                      {inc.severity}
                    </Badge>
                    <span className="text-[11px] font-mono text-graphite-400">
                      Impacted: {inc.impactedTracesCount} trace{inc.impactedTracesCount > 1 ? 's' : ''}
                    </span>
                  </div>
                  <h4 className="text-xs font-semibold text-graphite-100 font-mono mb-1 truncate">
                    {inc.title}
                  </h4>
                  <p className="text-[11px] text-graphite-400 line-clamp-2 mb-2 font-mono">
                    {inc.description}
                  </p>
                  <div className="flex items-center justify-between text-[10px] font-mono text-graphite-500">
                    <span>Service: {inc.serviceName}</span>
                    <span className="text-cyan-400 font-medium">Investigate RCA &rarr;</span>
                  </div>
                </div>
              ))
            ) : (
              <div className="p-8 text-center text-graphite-500 text-xs font-mono">
                <CheckCircle2 size={24} className="mx-auto mb-2 text-emerald-500/80" />
                No active operational incidents detected. System healthy.
              </div>
            )}
          </div>
        </div>

        {/* Recent Traces Table (7 cols) */}
        <div className="lg:col-span-7 rounded-lg border border-graphite-800 bg-graphite-900 flex flex-col overflow-hidden">
          <div className="px-4 py-3 bg-graphite-850 border-b border-graphite-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Activity size={15} className="text-cyan-400" />
              <h3 className="text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider">
                Recent Ingested Traces
              </h3>
            </div>
            <button
              onClick={() => onNavigate('traces')}
              className="text-xs font-mono text-cyan-400 hover:text-cyan-300 flex items-center gap-1"
            >
              <span>Explore Traces</span>
              <ArrowRight size={12} />
            </button>
          </div>

          <div className="overflow-x-auto flex-1">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-graphite-950/60 text-graphite-400 border-b border-graphite-800 text-[11px]">
                <tr>
                  <th className="px-4 py-2">Status</th>
                  <th className="px-4 py-2">Trace ID</th>
                  <th className="px-4 py-2">Root Operation</th>
                  <th className="px-4 py-2 text-right">Duration</th>
                  <th className="px-4 py-2 text-right">Spans</th>
                  <th className="px-4 py-2 text-right">Data</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-graphite-800/60">
                {metrics.recentTraces && metrics.recentTraces.length > 0 ? (
                  metrics.recentTraces.map((trace: Trace) => (
                    <tr
                      key={trace.traceId}
                      onClick={() => onSelectTrace(trace.traceId)}
                      className="hover:bg-graphite-800/40 cursor-pointer transition-colors"
                    >
                      <td className="px-4 py-2.5">
                        <Badge variant={trace.status === 'OK' ? 'healthy' : 'critical'} size="sm">
                          {trace.status}
                        </Badge>
                      </td>
                      <td className="px-4 py-2.5 text-graphite-300 font-mono">
                        {trace.traceId.substring(0, 12)}...
                      </td>
                      <td className="px-4 py-2.5 text-graphite-200 max-w-[180px] truncate">
                        {trace.rootOperation || 'POST /api/orders'}
                      </td>
                      <td className="px-4 py-2.5 text-right font-medium">
                        <span className={trace.durationMs > 800 ? 'text-amber-400' : 'text-graphite-200'}>
                          {trace.durationMs.toFixed(1)} ms
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-right text-graphite-400">
                        {trace.spanCount}
                      </td>
                      <td className="px-4 py-2.5 text-right">
                        {trace.demo ? (
                          <Badge variant="demo" size="sm">DEMO</Badge>
                        ) : (
                          <Badge variant="live" size="sm">LIVE</Badge>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-graphite-500 font-mono">
                      No traces ingested yet. Dispatch an order or seed demo data.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
