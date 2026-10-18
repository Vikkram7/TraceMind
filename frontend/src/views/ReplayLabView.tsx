import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { ReplayRun } from '../types';
import { Badge } from '../components/common/Badge';
import { PlayCircle, ShieldCheck, RefreshCw, Send } from 'lucide-react';

export const ReplayLabView: React.FC = () => {
  const [scenarioName, setScenarioName] = useState<string>('PAYMENT_TIMEOUT');
  const [requestCount, setRequestCount] = useState<number>(5);
  const [confirmed, setConfirmed] = useState<boolean>(false);
  const [previewing, setPreviewing] = useState<boolean>(false);
  const [executing, setExecuting] = useState<boolean>(false);
  const [previewResult, setPreviewResult] = useState<{
    valid: boolean;
    previewMessage: string;
    warnings: string[];
    simulatedRequests: Array<{ requestIndex: number; targetEndpoint: string; payload: unknown }>;
  } | null>(null);

  const [lastRun, setLastRun] = useState<ReplayRun | null>(null);
  const [history, setHistory] = useState<ReplayRun[]>([]);

  const loadHistory = async () => {
    try {
      const runs = await api.getReplayHistory();
      setHistory(runs);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadHistory();
  }, []);

  const handleValidatePreview = async () => {
    setPreviewing(true);
    try {
      const preview = await api.previewReplay({
        scenarioName,
        requestCount,
        targetEnvironment: 'sandbox',
      });
      setPreviewResult(preview);
    } finally {
      setPreviewing(false);
    }
  };

  const handleExecuteReplay = async () => {
    if (!confirmed) return;
    setExecuting(true);
    try {
      const run = await api.executeReplay({
        scenarioName,
        requestCount,
        targetEnvironment: 'sandbox',
        confirmed: true,
      });
      setLastRun(run);
      loadHistory();
    } finally {
      setExecuting(false);
    }
  };

  const scenarios = [
    {
      id: 'HEALTHY_BASELINE',
      name: 'Healthy Baseline Traffic',
      description: 'Dispatches standard eCommerce orders across Order, Payment, and Inventory services under normal SLA.',
    },
    {
      id: 'PAYMENT_TIMEOUT',
      name: 'Downstream Payment Timeout',
      description: 'Injects simulated upstream bank network timeout (3500ms) in Payment Service, causing Order Service cascade failure.',
    },
    {
      id: 'INVENTORY_DEADLOCK',
      name: 'Inventory DB Row Lock Deadlock',
      description: 'Simulates database deadlock exception (CannotAcquireLockException) on inventory row allocation.',
    },
    {
      id: 'LATENCY_SPIKE',
      name: 'Inventory Database Query Latency',
      description: 'Injects 1500ms latency dilation into inventory SQL reservation queries.',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-3 border-b border-graphite-800">
        <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
          <PlayCircle size={20} className="text-cyan-400" />
          <span>Controlled Replay & Fault-Scenario Laboratory</span>
        </h2>
        <p className="text-xs font-mono text-graphite-400">
          Safe reproduction of microservice failure topologies with explicit validation and sandbox isolation.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Configuration Form (6 cols) */}
        <div className="lg:col-span-6 space-y-4">
          <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-4">
            <h3 className="text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider pb-2 border-b border-graphite-800">
              Select Reproducible Scenario
            </h3>

            <div className="space-y-2.5">
              {scenarios.map((sc) => (
                <div
                  key={sc.id}
                  onClick={() => {
                    setScenarioName(sc.id);
                    setPreviewResult(null);
                    setConfirmed(false);
                  }}
                  className={`p-3 rounded-lg border text-xs font-mono cursor-pointer transition-all ${
                    scenarioName === sc.id
                      ? 'bg-cyan-950/40 border-cyan-500 shadow-sm'
                      : 'bg-graphite-850/60 border-graphite-800 hover:border-graphite-700'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-bold text-graphite-100">{sc.name}</span>
                    <Badge variant={scenarioName === sc.id ? 'accent' : 'neutral'} size="sm">
                      {sc.id}
                    </Badge>
                  </div>
                  <p className="text-graphite-400 text-[11px] leading-relaxed">{sc.description}</p>
                </div>
              ))}
            </div>

            {/* Request Volume Slider */}
            <div className="pt-2 border-t border-graphite-800 space-y-2">
              <div className="flex justify-between text-xs font-mono">
                <span className="text-graphite-400">Synthetic Request Volume:</span>
                <span className="text-cyan-300 font-bold">{requestCount} Requests</span>
              </div>
              <input
                type="range"
                min="1"
                max="20"
                value={requestCount}
                onChange={(e) => {
                  setRequestCount(parseInt(e.target.value));
                  setPreviewResult(null);
                  setConfirmed(false);
                }}
                className="w-full accent-cyan-400 cursor-pointer"
              />
            </div>

            {/* Target Environment Banner */}
            <div className="p-2.5 rounded bg-graphite-950 border border-graphite-800 text-xs font-mono flex items-center justify-between">
              <div className="flex items-center gap-1.5 text-graphite-300">
                <ShieldCheck size={14} className="text-emerald-400" />
                <span>Target Sandbox:</span>
              </div>
              <span className="text-emerald-400 font-bold">Local Microservices (Order :8081)</span>
            </div>

            {/* Step 1: Validate & Preview Button */}
            <button
              onClick={handleValidatePreview}
              disabled={previewing}
              className="w-full py-2 px-4 rounded text-xs font-mono font-medium bg-graphite-800 text-cyan-300 border border-graphite-700 hover:bg-graphite-750 transition-colors disabled:opacity-50"
            >
              {previewing ? 'Validating Sandbox Isolation...' : 'Step 1: Validate & Preview Scenario'}
            </button>
          </div>
        </div>

        {/* Right: Preview & Execution Panel (6 cols) */}
        <div className="lg:col-span-6 space-y-4">
          {previewResult ? (
            <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-graphite-800">
                <span className="text-xs font-mono font-bold uppercase tracking-wider text-graphite-200">
                  Validation & Sandbox Preview
                </span>
                <Badge variant={previewResult.valid ? 'healthy' : 'critical'}>
                  {previewResult.valid ? 'SAFETY VERIFIED' : 'FAILED'}
                </Badge>
              </div>

              <p className="text-xs font-mono text-graphite-300">
                {previewResult.previewMessage}
              </p>

              {previewResult.warnings.length > 0 && (
                <div className="p-3 rounded bg-amber-950/40 border border-amber-900 text-xs font-mono text-amber-300">
                  {previewResult.warnings.map((w, i) => (
                    <div key={i}>&bull; {w}</div>
                  ))}
                </div>
              )}

              {/* Sample Dispatch Preview */}
              <div className="space-y-1">
                <span className="text-[11px] font-mono text-graphite-500 block">Sample Request Payload Preview:</span>
                <pre className="p-2.5 rounded bg-graphite-950 border border-graphite-800 text-[11px] font-mono text-cyan-300 overflow-x-auto">
                  {JSON.stringify(previewResult.simulatedRequests[0], null, 2)}
                </pre>
              </div>

              {/* Confirmation Checkbox */}
              <label className="flex items-center gap-2 text-xs font-mono text-graphite-300 cursor-pointer pt-2 border-t border-graphite-800">
                <input
                  type="checkbox"
                  checked={confirmed}
                  onChange={(e) => setConfirmed(e.target.checked)}
                  className="rounded border-graphite-700 accent-cyan-400 w-4 h-4"
                />
                <span>I confirm execution into dedicated local test environment</span>
              </label>

              {/* Step 2: Execute Replay Button */}
              <button
                onClick={handleExecuteReplay}
                disabled={!confirmed || executing}
                className="w-full py-2.5 px-4 rounded text-xs font-mono font-bold bg-cyan-600 text-graphite-950 hover:bg-cyan-500 transition-colors disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                <Send size={14} className={executing ? 'animate-bounce' : ''} />
                <span>{executing ? 'Executing Replay Traffic...' : 'Step 2: Confirm & Run Scenario'}</span>
              </button>
            </div>
          ) : (
            <div className="p-12 rounded-lg border border-dashed border-graphite-800 bg-graphite-900/40 text-center text-graphite-500 font-mono text-xs">
              Select a scenario on the left and click "Validate & Preview" to proceed.
            </div>
          )}

          {/* Execution Result Banner */}
          {lastRun && (
            <div className="p-4 rounded-lg bg-graphite-900 border border-cyan-800 shadow-xl space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-cyan-400 uppercase">
                  Replay Run Completed [{lastRun.id}]
                </span>
                <Badge variant={lastRun.status === 'COMPLETED' ? 'healthy' : 'critical'}>
                  {lastRun.status}
                </Badge>
              </div>
              <p className="text-xs font-mono text-graphite-200">{lastRun.resultSummary}</p>
              <div className="flex items-center gap-4 text-xs font-mono text-graphite-400 pt-2 border-t border-graphite-800">
                <span>Total: <strong className="text-graphite-100">{lastRun.totalRequests}</strong></span>
                <span>Success: <strong className="text-emerald-400">{lastRun.successfulRequests}</strong></span>
                <span>Failed: <strong className="text-crimson-400">{lastRun.failedRequests}</strong></span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Audit History Table */}
      <div className="rounded-lg border border-graphite-800 bg-graphite-900 overflow-hidden">
        <div className="px-4 py-3 bg-graphite-850 border-b border-graphite-800 text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider flex items-center justify-between">
          <span>Replay Audit History ({history.length} Runs)</span>
          <button onClick={loadHistory} className="text-graphite-400 hover:text-cyan-300">
            <RefreshCw size={12} />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="bg-graphite-950/60 text-graphite-400 border-b border-graphite-800 text-[11px]">
              <tr>
                <th className="px-4 py-2">Run ID</th>
                <th className="px-4 py-2">Scenario</th>
                <th className="px-4 py-2 text-right">Requests</th>
                <th className="px-4 py-2 text-right">Success</th>
                <th className="px-4 py-2 text-right">Failed</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2">Executed At</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-graphite-800/60">
              {history.map((run) => (
                <tr key={run.id} className="hover:bg-graphite-800/40">
                  <td className="px-4 py-2.5 text-cyan-300 font-bold">{run.id}</td>
                  <td className="px-4 py-2.5 text-graphite-200">{run.scenarioName}</td>
                  <td className="px-4 py-2.5 text-right text-graphite-300">{run.totalRequests}</td>
                  <td className="px-4 py-2.5 text-right text-emerald-400 font-semibold">
                    {run.successfulRequests}
                  </td>
                  <td className="px-4 py-2.5 text-right text-crimson-400 font-semibold">
                    {run.failedRequests}
                  </td>
                  <td className="px-4 py-2.5">
                    <Badge variant={run.status === 'COMPLETED' ? 'healthy' : 'critical'} size="sm">
                      {run.status}
                    </Badge>
                  </td>
                  <td className="px-4 py-2.5 text-graphite-400">
                    {new Date(run.startedAt).toLocaleTimeString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
