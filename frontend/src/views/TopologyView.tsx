import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { TopologyGraph, TopologyNode, TopologyEdge } from '../types';
import { TopologyGraphView } from '../components/topology/TopologyGraphView';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Badge } from '../components/common/Badge';
import { Network, Server, ArrowRight, RefreshCw, AlertCircle } from 'lucide-react';
import { Node, Edge } from '@xyflow/react';

interface TopologyViewProps {
  onSelectServiceTraces: (serviceName: string) => void;
}

export const TopologyView: React.FC<TopologyViewProps> = ({ onSelectServiceTraces }) => {
  const [loading, setLoading] = useState(true);
  const [topology, setTopology] = useState<TopologyGraph | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);

  const loadTopology = async () => {
    setLoading(true);
    try {
      const data = await api.getTopology();
      setTopology(data);
      if (data.nodes.length > 0 && !selectedNodeId) {
        setSelectedNodeId(data.nodes[0].id);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTopology();
  }, []);

  const selectedNode = topology?.nodes.find((n) => n.id === selectedNodeId);

  // Convert to ReactFlow Node/Edge types
  const flowNodes: Node[] = (topology?.nodes || []).map((n: TopologyNode) => ({
    id: n.id,
    type: 'serviceNode',
    position: n.position,
    data: n.data,
  }));

  const flowEdges: Edge[] = (topology?.edges || []).map((e: TopologyEdge) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    label: e.label,
    animated: e.animated,
    style: {
      stroke: e.data?.status === 'ERROR' ? '#EF4444' : e.data?.status === 'SLOW' ? '#F59E0B' : '#0891B2',
      strokeWidth: 2,
    },
    labelStyle: { fill: '#CBD6E2', fontFamily: 'monospace', fontSize: 11 },
    labelBgStyle: { fill: '#11161D', fillOpacity: 0.85 },
  }));

  if (loading && !topology) {
    return <LoadingSpinner message="Reconstructing live service topology graph..." />;
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-graphite-800">
        <div>
          <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
            <Network size={20} className="text-cyan-400" />
            <span>Service Call Graph & Topology</span>
          </h2>
          <p className="text-xs font-mono text-graphite-400">
            Real-time directed graph inferred from distributed spans and HTTP call parentage.
          </p>
        </div>

        <button
          onClick={loadTopology}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-medium bg-graphite-850 text-graphite-300 border border-graphite-700 hover:bg-graphite-800"
        >
          <RefreshCw size={13} />
          <span>Refresh Graph</span>
        </button>
      </div>

      {/* Main Grid: React Flow Canvas & Detail Drawer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left: React Flow View (8 cols) */}
        <div className="lg:col-span-8">
          <TopologyGraphView
            nodes={flowNodes}
            edges={flowEdges}
            onNodeClick={(id) => setSelectedNodeId(id)}
          />
        </div>

        {/* Right: Selected Service Inspector (4 cols) */}
        <div className="lg:col-span-4 rounded-lg border border-graphite-800 bg-graphite-900 p-4 flex flex-col justify-between">
          {selectedNode ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-graphite-800">
                <div className="flex items-center gap-2">
                  <Server size={18} className="text-cyan-400" />
                  <h3 className="text-sm font-bold font-mono text-graphite-100">{selectedNode.data.label}</h3>
                </div>
                <Badge
                  variant={
                    selectedNode.data.status === 'HEALTHY'
                      ? 'healthy'
                      : selectedNode.data.status === 'CRITICAL'
                      ? 'critical'
                      : 'degraded'
                  }
                >
                  {selectedNode.data.status}
                </Badge>
              </div>

              <div className="space-y-2 text-xs font-mono">
                <div className="flex justify-between py-1 border-b border-graphite-800/60">
                  <span className="text-graphite-400">Total Recorded Calls:</span>
                  <span className="text-graphite-100 font-semibold">{selectedNode.data.callCount}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-graphite-800/60">
                  <span className="text-graphite-400">Observed Errors:</span>
                  <span className={selectedNode.data.errorCount > 0 ? 'text-crimson-400 font-bold' : 'text-graphite-100'}>
                    {selectedNode.data.errorCount} ({selectedNode.data.errorRate}%)
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-graphite-800/60">
                  <span className="text-graphite-400">Mean Duration:</span>
                  <span className="text-graphite-100 font-semibold">{selectedNode.data.avgDurationMs} ms</span>
                </div>
                <div className="flex justify-between py-1 border-b border-graphite-800/60">
                  <span className="text-graphite-400">Active Incidents:</span>
                  <span className={selectedNode.data.activeIncidents > 0 ? 'text-crimson-400 font-bold' : 'text-graphite-100'}>
                    {selectedNode.data.activeIncidents}
                  </span>
                </div>
              </div>

              {selectedNode.data.activeIncidents > 0 && (
                <div className="p-3 rounded bg-crimson-950/60 border border-crimson-900 text-xs font-mono text-crimson-300">
                  <div className="flex items-center gap-1.5 font-bold mb-1">
                    <AlertCircle size={14} />
                    <span>Active Incident Warning</span>
                  </div>
                  <p className="text-[11px] text-crimson-400">
                    High anomaly rate detected in this service. Review Root Cause Analysis and trace divergence.
                  </p>
                </div>
              )}

              <button
                onClick={() => onSelectServiceTraces(selectedNode.data.label)}
                className="w-full mt-4 flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-mono font-medium rounded bg-cyan-950 text-cyan-300 border border-cyan-800 hover:bg-cyan-900 transition-colors"
              >
                <span>Filter Traces by {selectedNode.data.label}</span>
                <ArrowRight size={13} />
              </button>
            </div>
          ) : (
            <div className="text-center p-8 text-graphite-500 font-mono text-xs">
              Click any service node in the graph to inspect dependency health.
            </div>
          )}

          {/* Graph Legend */}
          <div className="pt-4 border-t border-graphite-800 text-[10px] font-mono text-graphite-400 space-y-1">
            <span className="text-graphite-500 font-bold uppercase tracking-wider block mb-1">Graph Legend</span>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block" />
              <span>Healthy Edge (&lt;5% error, &lt;800ms)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-crimson-400 inline-block" />
              <span>Error Cascade (animated stroke)</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
