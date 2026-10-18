import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Incident, RootCauseAnalysis, RankedHypothesis } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Badge } from '../components/common/Badge';
import {
  FileText,
  Bot,
  RefreshCw,
  AlertTriangle,
} from 'lucide-react';

interface RcaReportViewProps {
  initialIncidentId?: string | null;
}

export const RcaReportView: React.FC<RcaReportViewProps> = ({ initialIncidentId }) => {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [activeIncidentId, setActiveIncidentId] = useState<string>(initialIncidentId || '');
  const [loading, setLoading] = useState(false);
  const [reanalyzing, setReanalyzing] = useState(false);
  const [rca, setRca] = useState<RootCauseAnalysis | null>(null);

  useEffect(() => {
    api.getIncidents({ size: 50 }).then((resp) => {
      const list = resp.content || [];
      setIncidents(list);
      if (!activeIncidentId && list.length > 0) {
        setActiveIncidentId(list[0].id);
      }
    });
  }, []);

  const loadRca = async (id: string) => {
    setLoading(true);
    try {
      const data = await api.getIncidentRca(id);
      setRca(data);
    } catch {
      setRca(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (activeIncidentId) {
      loadRca(activeIncidentId);
    }
  }, [activeIncidentId]);

  const handleReanalyze = async () => {
    if (!activeIncidentId) return;
    setReanalyzing(true);
    try {
      const fresh = await api.reanalyzeIncidentRca(activeIncidentId);
      setRca(fresh);
    } finally {
      setReanalyzing(false);
    }
  };

  // Parse ranked hypotheses JSON
  const hypotheses: RankedHypothesis[] = rca?.rankedHypothesesJson
    ? JSON.parse(rca.rankedHypothesesJson)
    : [];

  // Parse suggested actions JSON
  let diagnosticActions: string[] = [];
  let remediations: Array<{ action: string; targetService: string; riskLevel: string; riskWarning: string }> = [];

  if (rca?.suggestedActionsJson) {
    try {
      const parsed = JSON.parse(rca.suggestedActionsJson);
      diagnosticActions = parsed.diagnosticActions || [];
      remediations = parsed.remediations || [];
    } catch (e) {
      console.error(e);
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-graphite-800">
        <div>
          <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
            <FileText size={20} className="text-cyan-400" />
            <span>Root-Cause Analysis & Failure Narrative</span>
          </h2>
          <p className="text-xs font-mono text-graphite-400">
            Multi-factor deterministic ranking corroborated by trace divergence and AI diagnostic explanations.
          </p>
        </div>

        <button
          onClick={handleReanalyze}
          disabled={reanalyzing || !activeIncidentId}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-medium bg-cyan-950 text-cyan-300 border border-cyan-800 hover:bg-cyan-900 transition-colors disabled:opacity-50"
        >
          <RefreshCw size={13} className={reanalyzing ? 'animate-spin' : ''} />
          <span>{reanalyzing ? 'Re-analyzing...' : 'Re-run Ranking Engine'}</span>
        </button>
      </div>

      {/* Incident Picker */}
      <div className="p-3 rounded-lg bg-graphite-900 border border-graphite-800 flex items-center gap-3 text-xs font-mono">
        <span className="text-graphite-400 font-medium">Incident:</span>
        <select
          value={activeIncidentId}
          onChange={(e) => setActiveIncidentId(e.target.value)}
          className="flex-1 bg-graphite-950 border border-graphite-700 rounded px-3 py-1.5 text-graphite-200 outline-none focus:border-cyan-400"
        >
          {incidents.map((inc) => (
            <option key={inc.id} value={inc.id}>
              [{inc.severity}] {inc.title} - {inc.serviceName}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <LoadingSpinner message="Synthesizing multi-factor root cause diagnosis..." />
      ) : rca ? (
        <div className="space-y-6">
          {/* Primary Root Cause Banner */}
          <div className="p-5 rounded-lg bg-gradient-to-br from-graphite-900 via-graphite-900 to-cyan-950/20 border border-graphite-700 shadow-xl space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="text-xs font-mono font-bold uppercase tracking-wider text-cyan-400">
                  Primary Causal Origin Identified
                </span>
                <Badge variant={rca.aiGenerated ? 'accent' : 'demo'}>
                  {rca.aiModelUsed}
                </Badge>
              </div>

              <div className="flex items-center gap-2 text-xs font-mono">
                <span className="text-graphite-400">Algorithmic Confidence:</span>
                <span className="text-cyan-300 font-bold text-sm">
                  {(rca.confidenceScore * 100).toFixed(1)}%
                </span>
              </div>
            </div>

            <div className="text-lg font-bold font-mono text-graphite-100 flex items-center gap-2">
              <AlertTriangle className="text-crimson-400 shrink-0" size={20} />
              <span>{rca.primaryCandidate}</span>
            </div>

            <p className="text-xs font-mono text-graphite-300 leading-relaxed">
              {rca.explanationSummary}
            </p>
          </div>

          {/* Failure Narrative Section */}
          <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
            <div className="flex items-center gap-2 pb-2 border-b border-graphite-800">
              <Bot size={16} className="text-cyan-400" />
              <h3 className="text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider">
                Failure Propagation Narrative
              </h3>
            </div>

            <div className="prose prose-invert max-w-none text-xs font-mono text-graphite-300 whitespace-pre-wrap leading-relaxed">
              {rca.failureNarrative}
            </div>
          </div>

          {/* 7-Factor Deterministic Ranking Table */}
          <div className="rounded-lg border border-graphite-800 bg-graphite-900 overflow-hidden">
            <div className="px-4 py-3 bg-graphite-850 border-b border-graphite-800 flex items-center justify-between">
              <span className="text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider">
                Deterministic Multi-Factor Cause Ranking ({hypotheses.length} Candidates Evaluated)
              </span>
              <span className="text-[11px] font-mono text-graphite-400">
                Formula: w_err(30%) + w_lat(25%) + w_prec(20%) + w_blast(15%) + w_rec(10%)
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead className="bg-graphite-950/60 text-graphite-400 border-b border-graphite-800 text-[11px]">
                  <tr>
                    <th className="px-4 py-2">Rank</th>
                    <th className="px-4 py-2">Candidate Operation</th>
                    <th className="px-4 py-2">Service</th>
                    <th className="px-4 py-2 text-right">Composite Score</th>
                    <th className="px-4 py-2 text-right">Precedence</th>
                    <th className="px-4 py-2 text-right">Error Index</th>
                    <th className="px-4 py-2 text-right">Latency Anomaly</th>
                    <th className="px-4 py-2 text-right">Blast Radius</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-graphite-800/60">
                  {hypotheses.map((h) => (
                    <tr
                      key={h.candidateKey}
                      className={h.rank === 1 ? 'bg-cyan-950/20' : 'hover:bg-graphite-800/40'}
                    >
                      <td className="px-4 py-3 font-bold text-cyan-400">
                        #{h.rank}
                      </td>
                      <td className="px-4 py-3 text-graphite-100 font-semibold">
                        {h.operationName}
                      </td>
                      <td className="px-4 py-3 text-graphite-300">
                        <Badge variant="neutral" size="sm">{h.serviceName}</Badge>
                      </td>
                      <td className="px-4 py-3 text-right font-bold text-cyan-300">
                        {h.finalScore.toFixed(3)}
                      </td>
                      <td className="px-4 py-3 text-right text-graphite-400">
                        {h.temporalPrecedenceScore.toFixed(2)}
                      </td>
                      <td className="px-4 py-3 text-right text-graphite-400">
                        <span className={h.errorStatusScore > 0 ? 'text-crimson-400 font-bold' : ''}>
                          {h.errorStatusScore.toFixed(2)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right text-graphite-400">
                        {h.latencyAnomalyScore.toFixed(2)}
                      </td>
                      <td className="px-4 py-3 text-right text-graphite-400">
                        {h.blastRadiusScore.toFixed(2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Diagnostic Actions & Remediation with Risk Warnings */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Suggested Diagnostic Actions */}
            <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
              <h4 className="text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider pb-2 border-b border-graphite-800">
                Recommended Diagnostic Actions
              </h4>
              <ul className="space-y-2 text-xs font-mono text-graphite-300">
                {diagnosticActions.map((action, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-cyan-400 font-bold">{idx + 1}.</span>
                    <span>{action}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Suggested Remediation with Risk Warnings */}
            <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
              <h4 className="text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider pb-2 border-b border-graphite-800">
                Suggested Remediations (With Risk Ratings)
              </h4>
              <div className="space-y-2.5">
                {remediations.map((rem, idx) => (
                  <div key={idx} className="p-2.5 rounded bg-graphite-850 border border-graphite-800 text-xs font-mono">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-semibold text-graphite-100">{rem.action}</span>
                      <Badge
                        variant={
                          rem.riskLevel === 'HIGH'
                            ? 'critical'
                            : rem.riskLevel === 'MEDIUM'
                            ? 'degraded'
                            : 'healthy'
                        }
                        size="sm"
                      >
                        {rem.riskLevel} RISK
                      </Badge>
                    </div>
                    <div className="text-[11px] text-graphite-400">Target: {rem.targetService}</div>
                    <div className="text-[11px] text-amber-400/90 mt-1">
                      <strong>Warning:</strong> {rem.riskWarning}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="p-12 text-center border border-dashed border-graphite-800 rounded-lg bg-graphite-900/40 text-graphite-400 text-xs font-mono">
          No root cause analysis found for this incident. Click "Re-run Ranking Engine" to compute.
        </div>
      )}
    </div>
  );
};
