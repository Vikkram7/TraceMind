import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Incident, IncidentEvidence, RootCauseAnalysis } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Badge } from '../components/common/Badge';
import { Search, FileText, ArrowRight } from 'lucide-react';

interface InvestigationViewProps {
  selectedIncidentId?: string | null;
  onNavigateToRca: (incidentId: string) => void;
  onNavigateToTrace?: (traceId: string) => void;
}

export const InvestigationView: React.FC<InvestigationViewProps> = ({
  selectedIncidentId,
  onNavigateToRca,
}) => {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [activeId, setActiveId] = useState<string>(selectedIncidentId || '');
  const [loading, setLoading] = useState(false);
  const [incidentDetail, setIncidentDetail] = useState<{
    incident: Incident;
    evidences: IncidentEvidence[];
    rootCauseAnalysis?: RootCauseAnalysis;
  } | null>(null);

  useEffect(() => {
    api.getIncidents({ size: 50 }).then((resp) => {
      const list = resp.content || [];
      setIncidents(list);
      if (!activeId && list.length > 0) {
        setActiveId(list[0].id);
      }
    });
  }, []);

  useEffect(() => {
    if (activeId) {
      setLoading(true);
      api
        .getIncidentDetails(activeId)
        .then((data) => setIncidentDetail(data))
        .catch(() => setIncidentDetail(null))
        .finally(() => setLoading(false));
    }
  }, [activeId]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-3 border-b border-graphite-800">
        <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
          <Search size={20} className="text-cyan-400" />
          <span>Incident Investigation & Evidence Dossier</span>
        </h2>
        <p className="text-xs font-mono text-graphite-400">
          Inspection of raw telemetry evidence, anomaly deviation factors, and causal links.
        </p>
      </div>

      {/* Incident Switcher */}
      <div className="p-3 rounded-lg bg-graphite-900 border border-graphite-800 flex items-center gap-3 text-xs font-mono">
        <span className="text-graphite-400 font-medium">Select Incident:</span>
        <select
          value={activeId}
          onChange={(e) => setActiveId(e.target.value)}
          className="flex-1 bg-graphite-950 border border-graphite-700 rounded px-3 py-1.5 text-graphite-200 outline-none focus:border-cyan-400"
        >
          {incidents.map((inc) => (
            <option key={inc.id} value={inc.id}>
              [{inc.severity}] {inc.title} ({inc.serviceName})
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <LoadingSpinner message="Retrieving incident evidence records..." />
      ) : incidentDetail ? (
        <div className="space-y-6">
          {/* Incident Overview Card */}
          <div className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Badge variant={incidentDetail.incident.severity === 'CRITICAL' ? 'critical' : 'degraded'}>
                  {incidentDetail.incident.severity}
                </Badge>
                <span className="text-xs font-mono text-graphite-400">
                  Rule: <strong className="text-graphite-200">{incidentDetail.incident.detectionRule}</strong>
                </span>
                <Badge variant="accent">
                  Confidence: {(incidentDetail.incident.confidenceScore * 100).toFixed(0)}%
                </Badge>
              </div>

              <button
                onClick={() => onNavigateToRca(incidentDetail.incident.id)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-medium bg-cyan-950 text-cyan-300 border border-cyan-800 hover:bg-cyan-900 transition-colors"
              >
                <FileText size={13} />
                <span>Open Root-Cause Report</span>
                <ArrowRight size={13} />
              </button>
            </div>

            <h3 className="text-base font-bold text-graphite-100 font-mono">
              {incidentDetail.incident.title}
            </h3>

            <p className="text-xs text-graphite-300 font-mono">
              {incidentDetail.incident.description}
            </p>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs font-mono pt-3 border-t border-graphite-800 text-graphite-400">
              <div>
                <span className="text-graphite-500 block">Service:</span>
                <span className="text-graphite-200 font-semibold">{incidentDetail.incident.serviceName}</span>
              </div>
              <div>
                <span className="text-graphite-500 block">Impacted Traces:</span>
                <span className="text-graphite-200 font-semibold">{incidentDetail.incident.impactedTracesCount}</span>
              </div>
              <div>
                <span className="text-graphite-500 block">First Seen:</span>
                <span className="text-graphite-200 font-semibold">{new Date(incidentDetail.incident.firstSeen).toLocaleString()}</span>
              </div>
              <div>
                <span className="text-graphite-500 block">Last Observed:</span>
                <span className="text-graphite-200 font-semibold">{new Date(incidentDetail.incident.lastSeen).toLocaleString()}</span>
              </div>
            </div>
          </div>

          {/* Evidence Table */}
          <div className="rounded-lg border border-graphite-800 bg-graphite-900 overflow-hidden">
            <div className="px-4 py-3 bg-graphite-850 border-b border-graphite-800 text-xs font-mono font-bold text-graphite-200 uppercase tracking-wider flex items-center justify-between">
              <span>Supporting Telemetry Evidence Records ({incidentDetail.evidences.length})</span>
              <span className="text-[11px] text-graphite-400">Cryptographically & Temporally Correlated</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead className="bg-graphite-950/60 text-graphite-400 border-b border-graphite-800 text-[11px]">
                  <tr>
                    <th className="px-4 py-2">Evidence Type</th>
                    <th className="px-4 py-2">Target ID</th>
                    <th className="px-4 py-2 text-right">Measured Metric</th>
                    <th className="px-4 py-2 text-right">Baseline SLA</th>
                    <th className="px-4 py-2 text-right">Deviation Factor</th>
                    <th className="px-4 py-2">Description / Context</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-graphite-800/60">
                  {incidentDetail.evidences.map((ev) => (
                    <tr key={ev.id} className="hover:bg-graphite-800/40">
                      <td className="px-4 py-3">
                        <Badge variant="accent" size="sm">
                          {ev.evidenceType}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-cyan-300 font-semibold">
                        {ev.targetId}
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-graphite-200">
                        {ev.metricValue ? `${ev.metricValue.toFixed(1)} ms` : 'N/A'}
                      </td>
                      <td className="px-4 py-3 text-right text-graphite-400">
                        {ev.baselineValue ? `${ev.baselineValue.toFixed(1)} ms` : 'N/A'}
                      </td>
                      <td className="px-4 py-3 text-right font-bold text-amber-400">
                        {ev.deviationFactor && ev.deviationFactor !== Infinity
                          ? `${ev.deviationFactor.toFixed(1)}x`
                          : 'MAX'}
                      </td>
                      <td className="px-4 py-3 text-graphite-300 max-w-sm truncate" title={ev.description}>
                        {ev.description}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};
