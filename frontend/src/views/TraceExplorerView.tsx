import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Trace, TraceDetailResponse } from '../types';
import { WaterfallTimeline } from '../components/trace/WaterfallTimeline';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Badge } from '../components/common/Badge';
import { GitCommit, Filter, X, GitCompare, RefreshCw } from 'lucide-react';

interface TraceExplorerViewProps {
  initialTraceId?: string | null;
  initialServiceFilter?: string | null;
  onNavigateToCompare?: (baselineId: string, targetId: string) => void;
}

export const TraceExplorerView: React.FC<TraceExplorerViewProps> = ({
  initialTraceId,
  initialServiceFilter,
  onNavigateToCompare,
}) => {
  const [loading, setLoading] = useState(true);
  const [traces, setTraces] = useState<Trace[]>([]);
  const [selectedTraceId, setSelectedTraceId] = useState<string | null>(initialTraceId || null);
  const [traceDetail, setTraceDetail] = useState<TraceDetailResponse | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  // Filters
  const [serviceFilter, setServiceFilter] = useState<string>(initialServiceFilter || '');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [minDuration, setMinDuration] = useState<string>('');

  const loadTraces = async () => {
    setLoading(true);
    try {
      const resp = await api.getTraces({
        service: serviceFilter || undefined,
        status: statusFilter || undefined,
        minDuration: minDuration ? parseFloat(minDuration) : undefined,
        size: 50,
      });
      setTraces(resp.content || []);
      if (!selectedTraceId && resp.content && resp.content.length > 0) {
        setSelectedTraceId(resp.content[0].traceId);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTraces();
  }, [serviceFilter, statusFilter, minDuration]);

  useEffect(() => {
    if (selectedTraceId) {
      setLoadingDetail(true);
      api
        .getTraceDetails(selectedTraceId)
        .then((detail) => setTraceDetail(detail))
        .catch(() => setTraceDetail(null))
        .finally(() => setLoadingDetail(false));
    } else {
      setTraceDetail(null);
    }
  }, [selectedTraceId]);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-graphite-800">
        <div>
          <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
            <GitCommit size={20} className="text-cyan-400" />
            <span>Distributed Trace Explorer</span>
          </h2>
          <p className="text-xs font-mono text-graphite-400">
            End-to-end execution graphs with hierarchical span waterfall timelines.
          </p>
        </div>

        <button
          onClick={loadTraces}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-medium bg-graphite-850 text-graphite-300 border border-graphite-700 hover:bg-graphite-800"
        >
          <RefreshCw size={13} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="p-3 rounded-lg bg-graphite-900 border border-graphite-800 flex flex-wrap items-center gap-3 text-xs font-mono">
        <div className="flex items-center gap-1.5 text-graphite-400">
          <Filter size={14} className="text-cyan-400" />
          <span>Filters:</span>
        </div>

        {/* Service filter */}
        <select
          value={serviceFilter}
          onChange={(e) => setServiceFilter(e.target.value)}
          className="bg-graphite-950 border border-graphite-700 rounded px-2.5 py-1 text-graphite-200 outline-none focus:border-cyan-400"
        >
          <option value="">All Services</option>
          <option value="order-service">order-service</option>
          <option value="payment-service">payment-service</option>
          <option value="inventory-service">inventory-service</option>
        </select>

        {/* Status filter */}
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="bg-graphite-950 border border-graphite-700 rounded px-2.5 py-1 text-graphite-200 outline-none focus:border-cyan-400"
        >
          <option value="">All Statuses</option>
          <option value="OK">OK (Successful)</option>
          <option value="ERROR">ERROR (Failed)</option>
        </select>

        {/* Min duration */}
        <div className="flex items-center gap-1">
          <span className="text-graphite-400">Min Duration:</span>
          <input
            type="number"
            placeholder="e.g. 500"
            value={minDuration}
            onChange={(e) => setMinDuration(e.target.value)}
            className="w-24 bg-graphite-950 border border-graphite-700 rounded px-2 py-1 text-graphite-200 outline-none focus:border-cyan-400"
          />
          <span className="text-graphite-500">ms</span>
        </div>

        {(serviceFilter || statusFilter || minDuration) && (
          <button
            onClick={() => {
              setServiceFilter('');
              setStatusFilter('');
              setMinDuration('');
            }}
            className="flex items-center gap-1 text-graphite-400 hover:text-cyan-300 ml-auto"
          >
            <X size={13} />
            <span>Clear Filters</span>
          </button>
        )}
      </div>

      {/* Main View: Traces List & Detail Waterfall */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left Column: Traces Table (5 cols) */}
        <div className="lg:col-span-5 rounded-lg border border-graphite-800 bg-graphite-900 overflow-hidden flex flex-col">
          <div className="px-4 py-2.5 bg-graphite-850 border-b border-graphite-800 text-xs font-mono text-graphite-300 font-semibold flex items-center justify-between">
            <span>Observed Traces ({traces.length})</span>
            <span className="text-[11px] text-graphite-500">Latest First</span>
          </div>

          <div className="divide-y divide-graphite-800/60 overflow-y-auto max-h-[650px]">
            {loading ? (
              <LoadingSpinner message="Searching traces..." />
            ) : traces.length > 0 ? (
              traces.map((trace) => {
                const isSelected = selectedTraceId === trace.traceId;
                return (
                  <div
                    key={trace.traceId}
                    onClick={() => setSelectedTraceId(trace.traceId)}
                    className={`p-3 text-xs font-mono cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-cyan-950/40 border-l-2 border-cyan-400'
                        : 'hover:bg-graphite-800/40'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-bold text-graphite-200">
                        {trace.traceId.substring(0, 14)}...
                      </span>
                      <div className="flex items-center gap-1.5">
                        <Badge variant={trace.status === 'OK' ? 'healthy' : 'critical'} size="sm">
                          {trace.status}
                        </Badge>
                        {trace.demo && <Badge variant="demo" size="sm">DEMO</Badge>}
                      </div>
                    </div>

                    <div className="text-graphite-400 truncate mb-1">
                      {trace.rootOperation || 'POST /api/orders'}
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-graphite-500 pt-1 border-t border-graphite-800/40">
                      <span>Service: {trace.rootService}</span>
                      <span className={trace.durationMs > 800 ? 'text-amber-400 font-medium' : 'text-graphite-300'}>
                        {trace.durationMs.toFixed(1)} ms ({trace.spanCount} spans)
                      </span>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="p-8 text-center text-graphite-500 text-xs font-mono">
                No matching traces found for the active filter set.
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Waterfall Hierarchy Tree (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          {loadingDetail ? (
            <div className="p-12 rounded-lg border border-graphite-800 bg-graphite-900">
              <LoadingSpinner message="Loading span hierarchy and waterfall timeline..." />
            </div>
          ) : traceDetail ? (
            <div className="space-y-4">
              {/* Trace Summary Banner */}
              <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-mono text-graphite-400">Trace:</span>
                    <span className="text-xs font-mono font-bold text-cyan-300 select-all">
                      {traceDetail.trace.traceId}
                    </span>
                    <Badge variant={traceDetail.trace.status === 'OK' ? 'healthy' : 'critical'}>
                      {traceDetail.trace.status}
                    </Badge>
                  </div>
                  <div className="text-xs font-mono text-graphite-400 flex items-center gap-3">
                    <span>Root: {traceDetail.trace.rootService}</span>
                    <span>Total Spans: {traceDetail.totalSpans}</span>
                    <span>Total Duration: {traceDetail.trace.durationMs.toFixed(1)} ms</span>
                  </div>
                </div>

                {onNavigateToCompare && (
                  <button
                    onClick={() => {
                      const otherTrace = traces.find((t) => t.traceId !== traceDetail.trace.traceId);
                      if (otherTrace) {
                        onNavigateToCompare(
                          traceDetail.trace.status === 'OK' ? traceDetail.trace.traceId : otherTrace.traceId,
                          traceDetail.trace.status === 'ERROR' ? traceDetail.trace.traceId : otherTrace.traceId
                        );
                      }
                    }}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-mono font-medium rounded bg-graphite-850 text-cyan-300 border border-graphite-700 hover:bg-graphite-800 transition-colors"
                  >
                    <GitCompare size={14} />
                    <span>Compare Trace</span>
                  </button>
                )}
              </div>

              {/* Waterfall Timeline Component */}
              <WaterfallTimeline
                spans={traceDetail.spans}
                totalDurationMs={traceDetail.trace.durationMs}
              />
            </div>
          ) : (
            <div className="p-12 rounded-lg border border-graphite-800 bg-graphite-900 text-center text-graphite-500 font-mono text-xs">
              Select a trace from the left panel to inspect its waterfall timeline.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
