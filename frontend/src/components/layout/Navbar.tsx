import React, { useState } from 'react';
import { Activity, Sparkles, RefreshCw, Send } from 'lucide-react';
import { Badge } from '../common/Badge';
import { api } from '../../api/client';

interface NavbarProps {
  telemetryConnected: boolean;
  liveCount: number;
  demoCount: number;
  onRefresh: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  telemetryConnected,
  liveCount,
  demoCount,
  onRefresh,
}) => {
  const [triggering, setTriggering] = useState(false);
  const [generatingDemo, setGeneratingDemo] = useState(false);

  const handleSimulateOrder = async () => {
    setTriggering(true);
    try {
      await api.triggerLiveOrder();
      onRefresh();
    } finally {
      setTriggering(false);
    }
  };

  const handleGenerateDemo = async () => {
    setGeneratingDemo(true);
    try {
      await api.generateDemoData();
      onRefresh();
    } finally {
      setGeneratingDemo(false);
    }
  };

  return (
    <header className="h-14 border-b border-graphite-800 bg-graphite-900/90 backdrop-blur px-5 flex items-center justify-between sticky top-0 z-30">
      {/* Brand & Logo */}
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-950/50">
          <Activity size={18} className="text-white" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-base font-bold text-graphite-100 tracking-tight font-sans">TraceMind</h1>
            <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-graphite-800 text-cyan-400 border border-graphite-700">
              v1.0-LTS
            </span>
          </div>
          <p className="text-[10px] text-graphite-400 font-mono hidden md:block">
            Every failure has a story. Find its origin.
          </p>
        </div>
      </div>

      {/* Telemetry Status & Action Buttons */}
      <div className="flex items-center gap-3">
        {/* Connection status */}
        <div className="flex items-center gap-2 px-2.5 py-1 rounded bg-graphite-850 border border-graphite-800 text-xs font-mono">
          <span className="flex h-2 w-2 relative">
            <span
              className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
                telemetryConnected ? 'bg-emerald-400' : 'bg-amber-400'
              }`}
            />
            <span
              className={`relative inline-flex rounded-full h-2 w-2 ${
                telemetryConnected ? 'bg-emerald-500' : 'bg-amber-500'
              }`}
            />
          </span>
          <span className="text-graphite-300">
            {telemetryConnected ? 'OTel Ingestion Active' : 'Awaiting Telemetry'}
          </span>
        </div>

        {/* Live vs Demo Pill */}
        <div className="hidden lg:flex items-center gap-1.5 text-xs font-mono">
          <Badge variant="live" size="sm">
            Live: {liveCount}
          </Badge>
          <Badge variant="demo" size="sm">
            Demo: {demoCount}
          </Badge>
        </div>

        {/* Action: Simulate Real Traffic */}
        <button
          onClick={handleSimulateOrder}
          disabled={triggering}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-medium font-mono text-cyan-300 bg-cyan-950 border border-cyan-800 hover:bg-cyan-900/60 transition-all disabled:opacity-50"
          title="Dispatches a real distributed request to Order Service (:8081)"
        >
          <Send size={13} className={triggering ? 'animate-bounce' : ''} />
          <span>{triggering ? 'Dispatching...' : 'Dispatch Live Order'}</span>
        </button>

        {/* Action: Generate Demo Data */}
        <button
          onClick={handleGenerateDemo}
          disabled={generatingDemo}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-medium font-mono text-purple-300 bg-purple-950 border border-purple-800 hover:bg-purple-900/60 transition-all disabled:opacity-50"
          title="Generates multi-service demo traces with timeout and deadlock scenarios"
        >
          <Sparkles size={13} className={generatingDemo ? 'animate-spin' : ''} />
          <span>{generatingDemo ? 'Generating...' : 'Seed Scenarios'}</span>
        </button>

        {/* Refresh button */}
        <button
          onClick={onRefresh}
          className="p-1.5 text-graphite-400 hover:text-cyan-400 hover:bg-graphite-800 rounded transition-colors"
          title="Refresh telemetry"
        >
          <RefreshCw size={15} />
        </button>
      </div>
    </header>
  );
};
