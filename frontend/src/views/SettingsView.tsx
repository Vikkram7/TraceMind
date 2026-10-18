import React, { useState } from 'react';
import { api } from '../api/client';
import { Badge } from '../components/common/Badge';
import { Settings, ShieldCheck, Database, Radio, Sparkles, Check } from 'lucide-react';

export const SettingsView: React.FC = () => {
  const [seeding, setSeeding] = useState(false);
  const [seedSuccess, setSeedSuccess] = useState(false);

  const handleSeedDemo = async () => {
    setSeeding(true);
    try {
      await api.generateDemoData();
      setSeedSuccess(true);
      setTimeout(() => setSeedSuccess(false), 3000);
    } finally {
      setSeeding(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div className="pb-3 border-b border-graphite-800">
        <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
          <Settings size={20} className="text-cyan-400" />
          <span>Platform Settings & Telemetry Status</span>
        </h2>
        <p className="text-xs font-mono text-graphite-400">
          OpenTelemetry pipelines, anomaly detection thresholds, storage adapters, and LLM diagnostic engines.
        </p>
      </div>

      <div className="space-y-4">
        {/* Telemetry Connection Card */}
        <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-graphite-800">
            <div className="flex items-center gap-2 font-mono font-bold text-xs text-graphite-200">
              <Radio size={16} className="text-emerald-400" />
              <span>OpenTelemetry Ingestion Inbound Pipeline</span>
            </div>
            <Badge variant="healthy">LISTENING</Badge>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono text-graphite-300">
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">OTLP HTTP Standard Endpoint:</span>
              <code className="text-cyan-300 font-bold">http://localhost:8080/v1/traces</code>
            </div>
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">OpenTelemetry Collector Forwarder:</span>
              <code className="text-cyan-300 font-bold">http://localhost:4318 (HTTP) / 4317 (gRPC)</code>
            </div>
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">Trace Context Format:</span>
              <span className="text-graphite-200 font-medium">W3C TraceContext (traceparent)</span>
            </div>
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">Ingestion Mode:</span>
              <span className="text-emerald-400 font-medium">Asynchronous Non-Blocking Queue</span>
            </div>
          </div>
        </div>

        {/* Anomaly Detection SLA Configuration Card */}
        <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-graphite-800">
            <div className="flex items-center gap-2 font-mono font-bold text-xs text-graphite-200">
              <ShieldCheck size={16} className="text-cyan-400" />
              <span>Algorithmic Anomaly Detection Rules</span>
            </div>
            <Badge variant="accent">ACTIVE</Badge>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs font-mono text-graphite-300">
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">Static Latency SLA:</span>
              <span className="text-graphite-100 font-bold text-sm">800.0 ms</span>
            </div>
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">Error Rate Threshold:</span>
              <span className="text-graphite-100 font-bold text-sm">15.0%</span>
            </div>
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">Statistical Z-Score Threshold:</span>
              <span className="text-graphite-100 font-bold text-sm">&ge; 2.5 &sigma;</span>
            </div>
          </div>
        </div>

        {/* AI Explanation & Diagnostic Engine Card */}
        <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-graphite-800">
            <div className="flex items-center gap-2 font-mono font-bold text-xs text-graphite-200">
              <Sparkles size={16} className="text-purple-400" />
              <span>AI-Assisted Diagnostic Engine & Fallbacks</span>
            </div>
            <Badge variant="demo">DETERMINISTIC FALLBACK ACTIVE</Badge>
          </div>

          <p className="text-xs font-mono text-graphite-300">
            TraceMind operates fully local with a deterministic 7-factor ranking and template engine.
            Optional external LLM APIs (OpenAI, Gemini, Anthropic, Ollama) can be configured via environment variables.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">Configured Provider:</span>
              <span className="text-graphite-200 font-bold">Local Deterministic Engine (Fallback)</span>
            </div>
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800">
              <span className="text-graphite-500 block mb-1">Provider-Agnostic Variable:</span>
              <code className="text-cyan-400">TRACEMIND_LLM_PROVIDER</code>
            </div>
          </div>
        </div>

        {/* Telemetry Dataset Actions */}
        <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-graphite-800">
            <div className="flex items-center gap-2 font-mono font-bold text-xs text-graphite-200">
              <Database size={16} className="text-cyan-400" />
              <span>Telemetry Datasets & Demo Scenarios</span>
            </div>
          </div>

          <p className="text-xs font-mono text-graphite-300">
            Seed realistic multi-service distributed traces including payment timeout cascades, inventory deadlocks,
            and baseline healthy traffic. All seeded telemetry is explicitly labeled as <strong className="text-purple-300">DEMO</strong>.
          </p>

          <div className="pt-2">
            <button
              onClick={handleSeedDemo}
              disabled={seeding}
              className="flex items-center gap-1.5 px-4 py-2 text-xs font-mono font-bold rounded bg-purple-950 text-purple-300 border border-purple-800 hover:bg-purple-900 transition-colors disabled:opacity-50"
            >
              {seedSuccess ? (
                <>
                  <Check size={14} className="text-emerald-400" />
                  <span>Demo Dataset Successfully Seeded!</span>
                </>
              ) : (
                <>
                  <Sparkles size={14} className={seeding ? 'animate-spin' : ''} />
                  <span>{seeding ? 'Seeding Multi-Service Traces...' : 'Seed Realistic Demo Telemetry'}</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
