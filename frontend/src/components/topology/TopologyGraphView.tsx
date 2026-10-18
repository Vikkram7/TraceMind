import React, { useMemo } from 'react';
import {
  ReactFlow,
  Controls,
  Background,
  BackgroundVariant,
  NodeProps,
  Handle,
  Position,
  Node,
  Edge,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { Server, AlertTriangle } from 'lucide-react';
import { TopologyNodeData } from '../../types';

const CustomServiceNode: React.FC<NodeProps> = ({ data, selected }) => {
  const nodeData = data as unknown as TopologyNodeData;

  const statusColors = {
    HEALTHY: 'border-emerald-700 bg-emerald-950/40 text-emerald-300',
    DEGRADED: 'border-amber-700 bg-amber-950/40 text-amber-300',
    CRITICAL: 'border-crimson-700 bg-crimson-950/40 text-crimson-300',
  };

  const statusBorder = statusColors[nodeData.status] || statusColors.HEALTHY;

  return (
    <div
      className={`px-4 py-3 rounded-lg border bg-graphite-900 shadow-xl min-w-[210px] transition-all ${
        selected ? 'ring-2 ring-cyan-400 border-cyan-500' : 'border-graphite-700 hover:border-graphite-600'
      }`}
    >
      <Handle type="target" position={Position.Left} className="w-2.5 h-2.5 !bg-cyan-400 border-2 !border-graphite-950" />

      <div className="flex items-center justify-between gap-2 mb-2">
        <div className="flex items-center gap-1.5 font-semibold text-sm text-graphite-100 font-mono">
          <Server size={14} className="text-cyan-400" />
          <span>{nodeData.label}</span>
        </div>
        <span className={`text-[10px] font-mono font-medium px-1.5 py-0.5 rounded border ${statusBorder}`}>
          {nodeData.status}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-2 text-[11px] font-mono text-graphite-400 pt-1 border-t border-graphite-800">
        <div>
          <span className="text-graphite-500">Calls:</span>{' '}
          <span className="text-graphite-200 font-medium">{nodeData.callCount}</span>
        </div>
        <div>
          <span className="text-graphite-500">Err:</span>{' '}
          <span className={nodeData.errorRate > 0 ? 'text-crimson-400 font-bold' : 'text-graphite-200'}>
            {nodeData.errorRate}%
          </span>
        </div>
        <div className="col-span-2">
          <span className="text-graphite-500">Avg Latency:</span>{' '}
          <span className="text-graphite-200 font-medium">{nodeData.avgDurationMs} ms</span>
        </div>
      </div>

      {nodeData.activeIncidents > 0 && (
        <div className="mt-2 text-[10px] font-mono flex items-center gap-1 px-1.5 py-0.5 rounded bg-crimson-950/80 border border-crimson-800 text-crimson-300">
          <AlertTriangle size={11} />
          <span>{nodeData.activeIncidents} Active Incident{nodeData.activeIncidents > 1 ? 's' : ''}</span>
        </div>
      )}

      <Handle type="source" position={Position.Right} className="w-2.5 h-2.5 !bg-cyan-400 border-2 !border-graphite-950" />
    </div>
  );
};

interface TopologyGraphViewProps {
  nodes: Node[];
  edges: Edge[];
  onNodeClick?: (nodeId: string) => void;
}

export const TopologyGraphView: React.FC<TopologyGraphViewProps> = ({ nodes, edges, onNodeClick }) => {
  const nodeTypes = useMemo(() => ({ serviceNode: CustomServiceNode }), []);

  return (
    <div className="w-full h-[520px] bg-graphite-950 rounded-lg border border-graphite-800 overflow-hidden relative">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onNodeClick={(_, node) => onNodeClick && onNodeClick(node.id)}
        fitView
        className="dark"
        proOptions={{ hideAttribution: true }}
      >
        <Background variant={BackgroundVariant.Dots} gap={16} size={1} color="#26313F" />
        <Controls className="!bg-graphite-900 !border-graphite-800 !fill-graphite-300" />
      </ReactFlow>
    </div>
  );
};
