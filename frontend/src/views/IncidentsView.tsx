import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Incident } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Badge } from '../components/common/Badge';
import { AlertTriangle, CheckCircle, ArrowRight, RefreshCw } from 'lucide-react';

interface IncidentsViewProps {
  onInvestigateIncident: (incidentId: string) => void;
}

export const IncidentsView: React.FC<IncidentsViewProps> = ({ onInvestigateIncident }) => {
  const [loading, setLoading] = useState(true);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');

  const loadIncidents = async () => {
    setLoading(true);
    try {
      const resp = await api.getIncidents({ status: statusFilter || undefined, size: 50 });
      setIncidents(resp.content || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadIncidents();
  }, [statusFilter]);

  const handleAcknowledge = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    await api.acknowledgeIncident(id);
    loadIncidents();
  };

  const handleResolve = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    await api.resolveIncident(id);
    loadIncidents();
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-graphite-800">
        <div>
          <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
            <AlertTriangle size={20} className="text-crimson-400" />
            <span>Operational Incidents & Anomalies</span>
          </h2>
          <p className="text-xs font-mono text-graphite-400">
            Deduplicated incident groups detected via static SLA thresholds, error bursts, and rolling statistical baselines.
          </p>
        </div>

        <button
          onClick={loadIncidents}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-medium bg-graphite-850 text-graphite-300 border border-graphite-700 hover:bg-graphite-800"
        >
          <RefreshCw size={13} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 text-xs font-mono">
        {['', 'ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'].map((status) => (
          <button
            key={status}
            onClick={() => setStatusFilter(status)}
            className={`px-3 py-1.5 rounded border transition-colors ${
              statusFilter === status
                ? 'bg-cyan-950 text-cyan-300 border-cyan-800'
                : 'bg-graphite-900 text-graphite-400 border-graphite-800 hover:bg-graphite-850'
            }`}
          >
            {status === '' ? 'All Incidents' : status}
          </button>
        ))}
      </div>

      {/* Incidents List */}
      {loading ? (
        <LoadingSpinner message="Scanning active operational incidents..." />
      ) : incidents.length > 0 ? (
        <div className="space-y-3">
          {incidents.map((inc) => (
            <div
              key={inc.id}
              onClick={() => onInvestigateIncident(inc.id)}
              className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 hover:border-graphite-700 cursor-pointer transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <Badge variant={inc.severity === 'CRITICAL' ? 'critical' : 'degraded'}>
                    {inc.severity}
                  </Badge>
                  <span className="text-xs font-mono px-2 py-0.5 rounded bg-graphite-800 text-graphite-300 border border-graphite-700">
                    Rule: {inc.detectionRule}
                  </span>
                  <Badge variant={inc.status === 'ACTIVE' ? 'critical' : inc.status === 'ACKNOWLEDGED' ? 'degraded' : 'healthy'}>
                    {inc.status}
                  </Badge>
                  <span className="text-xs font-mono text-cyan-400 font-semibold ml-2">
                    Confidence: {(inc.confidenceScore * 100).toFixed(0)}%
                  </span>
                </div>

                <h3 className="text-sm font-bold text-graphite-100 font-mono">
                  {inc.title}
                </h3>

                <p className="text-xs text-graphite-400 font-mono">
                  {inc.description}
                </p>

                <div className="flex flex-wrap items-center gap-4 text-[11px] font-mono text-graphite-500 pt-1">
                  <span>Service: <strong className="text-graphite-300">{inc.serviceName}</strong></span>
                  <span>Impacted Traces: <strong className="text-graphite-300">{inc.impactedTracesCount}</strong></span>
                  <span>First Seen: <strong className="text-graphite-300">{new Date(inc.firstSeen).toLocaleTimeString()}</strong></span>
                  <span>Last Seen: <strong className="text-graphite-300">{new Date(inc.lastSeen).toLocaleTimeString()}</strong></span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-2 shrink-0">
                {inc.status === 'ACTIVE' && (
                  <button
                    onClick={(e) => handleAcknowledge(inc.id, e)}
                    className="px-2.5 py-1.5 rounded text-xs font-mono bg-graphite-800 text-amber-300 border border-graphite-700 hover:bg-graphite-750 transition-colors"
                  >
                    Acknowledge
                  </button>
                )}
                {inc.status !== 'RESOLVED' && (
                  <button
                    onClick={(e) => handleResolve(inc.id, e)}
                    className="px-2.5 py-1.5 rounded text-xs font-mono bg-graphite-800 text-emerald-300 border border-graphite-700 hover:bg-graphite-750 transition-colors"
                  >
                    Resolve
                  </button>
                )}
                <button
                  onClick={() => onInvestigateIncident(inc.id)}
                  className="flex items-center gap-1 px-3 py-1.5 rounded text-xs font-mono font-medium bg-cyan-950 text-cyan-300 border border-cyan-800 hover:bg-cyan-900 transition-colors"
                >
                  <span>Investigate Evidence</span>
                  <ArrowRight size={13} />
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="p-12 text-center border border-dashed border-graphite-800 rounded-lg bg-graphite-900/40 text-graphite-400 text-xs font-mono">
          <CheckCircle size={28} className="mx-auto mb-2 text-emerald-400" />
          No incidents found matching the selected filter.
        </div>
      )}
    </div>
  );
};
