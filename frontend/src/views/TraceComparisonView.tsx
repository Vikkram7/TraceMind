import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Trace, DivergenceReport } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Badge } from '../components/common/Badge';
import { GitCompare, AlertTriangle, ArrowRight, CheckCircle2, Zap } from 'lucide-react';

interface TraceComparisonViewProps {
  initialBaselineId?: string | null;
  initialTargetId?: string | null;
}

export const TraceComparisonView: React.FC<TraceComparisonViewProps> = ({
  initialBaselineId,
  initialTargetId,
}) => {
  const [traces, setTraces] = useState<Trace[]>([]);
  const [baselineId, setBaselineId] = useState<string>(initialBaselineId || '');
  const [targetId, setTargetId] = useState<string>(initialTargetId || '');
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState<DivergenceReport | null>(null);

  useEffect(() => {
    api.getTraces({ size: 50 }).then((resp) => {
      const list = resp.content || [];
      setTraces(list);

      // Auto-select baseline (OK) and target (ERROR or slowest) if not set
      if (!baselineId || !targetId) {
        const okTrace = list.find((t) => t.status === 'OK');
        const errTrace = list.find((t) => t.status === 'ERROR') || list[list.length - 1];

        if (okTrace && !baselineId) setBaselineId(okTrace.traceId);
        if (errTrace && !targetId && errTrace.traceId !== okTrace?.traceId) setTargetId(errTrace.traceId);
      }
    });
  }, []);

  const runComparison = async () => {
    if (!baselineId || !targetId) return;
    setLoading(true);
    try {
      const result = await api.compareTraces(baselineId, targetId);
      setReport(result);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (baselineId && targetId && baselineId !== targetId) {
      runComparison();
    }
  }, [baselineId, targetId]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-3 border-b border-graphite-800">
        <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
          <GitCompare size={20} className="text-cyan-400" />
          <span>Execution Trace Divergence Engine</span>
        </h2>
        <p className="text-xs font-mono text-graphite-400">
          Algorithmic alignment of execution graphs to isolate the earliest causal divergence between successful and failing traces.
        </p>
      </div>

      {/* Selectors Bar */}
      <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 grid grid-cols-1 md:grid-cols-12 gap-4 items-center">
        {/* Baseline Selector (5 cols) */}
        <div className="md:col-span-5 space-y-1">
          <label className="text-xs font-mono text-graphite-400 flex items-center gap-1.5 font-medium">
            <CheckCircle2 size={13} className="text-emerald-400" />
            <span>Baseline Trace (Healthy / Fast):</span>
          </label>
          <select
            value={baselineId}
            onChange={(e) => setBaselineId(e.target.value)}
            className="w-full bg-graphite-950 border border-graphite-700 rounded px-3 py-2 text-xs font-mono text-graphite-200 outline-none focus:border-cyan-400"
          >
            <option value="">Select baseline trace...</option>
            {traces.map((t) => (
              <option key={t.traceId} value={t.traceId}>
                [{t.status}] {t.traceId.substring(0, 14)}... ({t.durationMs.toFixed(1)} ms) - {t.rootService}
              </option>
            ))}
          </select>
        </div>

        {/* Arrow (2 cols) */}
        <div className="md:col-span-2 flex justify-center text-graphite-500">
          <ArrowRight size={22} className="hidden md:block" />
        </div>

        {/* Target Selector (5 cols) */}
        <div className="md:col-span-5 space-y-1">
          <label className="text-xs font-mono text-graphite-400 flex items-center gap-1.5 font-medium">
            <AlertTriangle size={13} className="text-crimson-400" />
            <span>Target Trace (Failing / Slow):</span>
          </label>
          <select
            value={targetId}
            onChange={(e) => setTargetId(e.target.value)}
            className="w-full bg-graphite-950 border border-graphite-700 rounded px-3 py-2 text-xs font-mono text-graphite-200 outline-none focus:border-cyan-400"
          >
            <option value="">Select target trace...</option>
            {traces.map((t) => (
              <option key={t.traceId} value={t.traceId}>
                [{t.status}] {t.traceId.substring(0, 14)}... ({t.durationMs.toFixed(1)} ms) - {t.rootService}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner message="Executing algorithmic divergence analysis..." />
      ) : report ? (
        <div className="space-y-6">
          {/* Earliest Point of Divergence Banner */}
          {report.earliestDivergence ? (
            <div className="p-4 rounded-lg bg-crimson-950/40 border border-crimson-800 shadow-xl space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Zap size={18} className="text-crimson-400" />
                  <span className="text-xs font-mono font-bold uppercase tracking-wider text-crimson-300">
                    Earliest Point of Divergence Identified
                  </span>
                </div>
                <Badge variant="critical">
                  {report.earliestDivergence.divergenceType}
                </Badge>
              </div>

              <div className="text-sm font-semibold text-graphite-100 font-mono">
                {report.earliestDivergence.serviceName} &rarr; {report.earliestDivergence.operationName}
              </div>

              <p className="text-xs font-mono text-graphite-300">
                {report.earliestDivergence.explanation}
              </p>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pt-2 border-t border-crimson-900/60 text-xs font-mono text-graphite-300">
                <div>
                  <span className="text-graphite-500 block">Baseline Latency:</span>
                  <span>{report.earliestDivergence.baselineDurationMs.toFixed(1)} ms</span>
                </div>
                <div>
                  <span className="text-graphite-500 block">Target Latency:</span>
                  <span className="text-crimson-400 font-bold">
                    {report.earliestDivergence.targetDurationMs.toFixed(1)} ms
                  </span>
                </div>
                <div>
                  <span className="text-graphite-500 block">Duration Delta:</span>
                  <span className="text-amber-400 font-bold">
                    +{report.earliestDivergence.durationDeltaMs.toFixed(1)} ms
                  </span>
                </div>
                <div>
                  <span className="text-graphite-500 block">Target Status:</span>
                  <span className="text-crimson-400 font-bold">
                    {report.earliestDivergence.targetStatus}
                  </span>
                </div>
              </div>
            </div>
          ) : (
            <div className="p-4 rounded-lg bg-emerald-950/30 border border-emerald-800 text-xs font-mono text-emerald-300 flex items-center gap-2">
              <CheckCircle2 size={16} />
              <span>No critical divergence detected between baseline and target traces.</span>
            </div>
          )}

          {/* Divergence Table */}
          <div className="rounded-lg border border-graphite-800 bg-graphite-900 overflow-hidden">
            <div className="px-4 py-3 bg-graphite-850 border-b border-graphite-800 text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider">
              Observed Operation Divergences ({report.divergencePoints.length})
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead className="bg-graphite-950/60 text-graphite-400 border-b border-graphite-800 text-[11px]">
                  <tr>
                    <th className="px-4 py-2">Divergence Type</th>
                    <th className="px-4 py-2">Service</th>
                    <th className="px-4 py-2">Operation</th>
                    <th className="px-4 py-2 text-right">Baseline Latency</th>
                    <th className="px-4 py-2 text-right">Target Latency</th>
                    <th className="px-4 py-2 text-right">Delta</th>
                    <th className="px-4 py-2">Diagnostics</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-graphite-800/60">
                  {report.divergencePoints.map((dp, idx) => (
                    <tr
                      key={idx}
                      className={dp.earliestDivergence ? 'bg-crimson-950/20' : 'hover:bg-graphite-800/40'}
                    >
                      <td className="px-4 py-3">
                        <Badge
                          variant={
                            dp.divergenceType === 'STATUS_MISMATCH'
                              ? 'critical'
                              : dp.divergenceType === 'LATENCY_DIVERGENCE'
                              ? 'degraded'
                              : 'neutral'
                          }
                          size="sm"
                        >
                          {dp.divergenceType}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-graphite-200 font-semibold">{dp.serviceName}</td>
                      <td className="px-4 py-3 text-graphite-300 max-w-xs truncate">{dp.operationName}</td>
                      <td className="px-4 py-3 text-right text-graphite-400">
                        {dp.baselineDurationMs.toFixed(1)} ms
                      </td>
                      <td className="px-4 py-3 text-right font-medium">
                        <span className={dp.targetDurationMs > dp.baselineDurationMs * 2 ? 'text-amber-400' : 'text-graphite-200'}>
                          {dp.targetDurationMs.toFixed(1)} ms
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-bold text-cyan-400">
                        {dp.durationDeltaMs > 0 ? `+${dp.durationDeltaMs.toFixed(1)} ms` : `${dp.durationDeltaMs.toFixed(1)} ms`}
                      </td>
                      <td className="px-4 py-3 text-graphite-400 max-w-sm truncate" title={dp.explanation}>
                        {dp.explanation}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Limitations */}
          {report.limitations.length > 0 && (
            <div className="p-3 rounded bg-graphite-950 border border-graphite-800 text-xs font-mono text-graphite-400 space-y-1">
              <span className="font-semibold text-graphite-300 block mb-1">Telemetry Comparison Scope & Limitations:</span>
              {report.limitations.map((lim, idx) => (
                <div key={idx} className="flex items-center gap-1.5">
                  <span className="text-graphite-600">&bull;</span>
                  <span>{lim}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
};
