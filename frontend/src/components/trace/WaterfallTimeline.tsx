import React, { useState } from 'react';
import { Span } from '../../types';
import { Badge } from '../common/Badge';
import { CheckCircle2, AlertCircle, Clock, Copy, Check } from 'lucide-react';

interface WaterfallTimelineProps {
  spans: Span[];
  totalDurationMs: number;
}

export const WaterfallTimeline: React.FC<WaterfallTimelineProps> = ({ spans, totalDurationMs }) => {
  const [selectedSpan, setSelectedSpan] = useState<Span | null>(null);
  const [copied, setCopied] = useState(false);

  // Compute depth for tree hierarchy
  const depthMap = new Map<string, number>();
  const calculateDepth = (span: Span): number => {
    if (!span.parentSpanId) return 0;
    if (depthMap.has(span.spanId)) return depthMap.get(span.spanId)!;
    const parent = spans.find((s) => s.spanId === span.parentSpanId);
    const d = parent ? calculateDepth(parent) + 1 : 1;
    depthMap.set(span.spanId, d);
    return d;
  };

  const safeTotalDuration = Math.max(totalDurationMs, 1.0);

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div className="flex flex-col gap-4">
      {/* Waterfall Table */}
      <div className="border border-graphite-800 rounded-lg bg-graphite-900 overflow-hidden">
        {/* Header */}
        <div className="grid grid-cols-12 px-4 py-2 bg-graphite-850 border-b border-graphite-800 text-xs font-mono text-graphite-400 font-medium">
          <div className="col-span-6">Service & Operation</div>
          <div className="col-span-2 text-right">Duration</div>
          <div className="col-span-4 pl-4">Timeline (0 ms - {safeTotalDuration.toFixed(1)} ms)</div>
        </div>

        {/* Span Rows */}
        <div className="divide-y divide-graphite-800/60 max-h-[500px] overflow-y-auto">
          {spans.map((span) => {
            const depth = calculateDepth(span);
            const offsetMs = span.relativeOffsetMs || 0;
            const leftPercent = Math.min(96, Math.max(0, (offsetMs / safeTotalDuration) * 100));
            const widthPercent = Math.min(
              100 - leftPercent,
              Math.max(2, (span.durationMs / safeTotalDuration) * 100)
            );

            const isSelected = selectedSpan?.spanId === span.spanId;

            return (
              <div
                key={span.spanId}
                onClick={() => setSelectedSpan(span)}
                className={`grid grid-cols-12 px-4 py-2.5 items-center text-xs font-mono cursor-pointer transition-colors ${
                  isSelected
                    ? 'bg-cyan-950/30 border-l-2 border-cyan-400'
                    : 'hover:bg-graphite-800/40'
                }`}
              >
                {/* Left: Hierarchy & Names */}
                <div className="col-span-6 flex items-center gap-2 overflow-hidden">
                  <div style={{ marginLeft: `${depth * 18}px` }} className="flex items-center gap-2 min-w-0">
                    {span.isError ? (
                      <AlertCircle size={14} className="text-crimson-400 shrink-0" />
                    ) : (
                      <CheckCircle2 size={14} className="text-emerald-400 shrink-0" />
                    )}

                    <Badge
                      size="sm"
                      variant={
                        span.serviceName.includes('payment')
                          ? 'accent'
                          : span.serviceName.includes('inventory')
                          ? 'live'
                          : 'neutral'
                      }
                    >
                      {span.serviceName}
                    </Badge>

                    <span className="text-graphite-200 truncate font-medium" title={span.operationName}>
                      {span.operationName}
                    </span>

                    <span className="text-[10px] text-graphite-500 font-mono">[{span.kind}]</span>
                  </div>
                </div>

                {/* Duration */}
                <div className="col-span-2 text-right font-medium">
                  <span className={span.isError ? 'text-crimson-400 font-bold' : 'text-graphite-200'}>
                    {span.durationMs.toFixed(1)} ms
                  </span>
                </div>

                {/* Timeline Bar */}
                <div className="col-span-4 pl-4 relative h-4 flex items-center">
                  <div className="w-full bg-graphite-800/40 h-2 rounded-full relative overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${
                        span.isError
                          ? 'bg-crimson-500 shadow-[0_0_8px_rgba(239,68,68,0.5)]'
                          : 'bg-cyan-500 shadow-[0_0_6px_rgba(6,182,212,0.3)]'
                      }`}
                      style={{
                        marginLeft: `${leftPercent}%`,
                        width: `${widthPercent}%`,
                      }}
                      title={`Offset: ${offsetMs.toFixed(1)} ms | Duration: ${span.durationMs.toFixed(1)} ms`}
                    />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Selected Span Details Inspector */}
      {selectedSpan && (
        <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 shadow-xl">
          <div className="flex items-center justify-between pb-3 border-b border-graphite-800 mb-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-mono font-semibold text-graphite-400 uppercase tracking-wider">
                Span Inspector
              </span>
              <Badge variant={selectedSpan.isError ? 'critical' : 'healthy'}>
                {selectedSpan.statusCode}
              </Badge>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => handleCopy(selectedSpan.spanId)}
                className="flex items-center gap-1 text-[11px] font-mono text-graphite-400 hover:text-cyan-300 transition-colors"
              >
                {copied ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
                <span>{selectedSpan.spanId}</span>
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs font-mono mb-4">
            <div>
              <span className="text-graphite-500">Service:</span>
              <p className="text-graphite-100 font-medium">{selectedSpan.serviceName}</p>
            </div>
            <div>
              <span className="text-graphite-500">Operation:</span>
              <p className="text-graphite-100 font-medium truncate" title={selectedSpan.operationName}>
                {selectedSpan.operationName}
              </p>
            </div>
            <div>
              <span className="text-graphite-500">Duration:</span>
              <p className="text-graphite-100 font-medium flex items-center gap-1">
                <Clock size={12} className="text-cyan-400" />
                {selectedSpan.durationMs.toFixed(2)} ms
              </p>
            </div>
            <div>
              <span className="text-graphite-500">Parent Span:</span>
              <p className="text-graphite-100 font-medium">
                {selectedSpan.parentSpanId || 'None (Root Span)'}
              </p>
            </div>
          </div>

          {selectedSpan.statusMessage && (
            <div className="mb-3 p-2 rounded bg-graphite-850 border border-graphite-800 text-xs font-mono">
              <span className="text-graphite-500 block mb-1">Status Message / Diagnostics:</span>
              <span className={selectedSpan.isError ? 'text-crimson-400' : 'text-graphite-300'}>
                {selectedSpan.statusMessage}
              </span>
            </div>
          )}

          {selectedSpan.attributesJson && (
            <div>
              <span className="text-graphite-500 text-xs font-mono block mb-1">OpenTelemetry Attributes:</span>
              <pre className="p-3 rounded bg-graphite-950 border border-graphite-800 text-[11px] font-mono text-cyan-300 overflow-x-auto max-h-40">
                {JSON.stringify(JSON.parse(selectedSpan.attributesJson), null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
