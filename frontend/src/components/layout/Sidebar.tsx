import React from 'react';
import {
  LayoutDashboard,
  Network,
  Server,
  GitCommit,
  GitCompare,
  AlertTriangle,
  Search,
  FileText,
  PlayCircle,
  Settings,
} from 'lucide-react';

export type ViewType =
  | 'overview'
  | 'topology'
  | 'services'
  | 'traces'
  | 'compare'
  | 'incidents'
  | 'investigation'
  | 'rca'
  | 'replay'
  | 'settings';

interface SidebarProps {
  currentView: ViewType;
  onSelectView: (view: ViewType) => void;
  activeIncidentsCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentView,
  onSelectView,
  activeIncidentsCount,
}) => {
  const menuItems = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'topology', label: 'Service Topology', icon: Network },
    { id: 'services', label: 'Services & Health', icon: Server },
    { id: 'traces', label: 'Trace Explorer', icon: GitCommit },
    { id: 'compare', label: 'Trace Comparison', icon: GitCompare },
    {
      id: 'incidents',
      label: 'Incidents',
      icon: AlertTriangle,
      badge: activeIncidentsCount > 0 ? activeIncidentsCount : undefined,
    },
    { id: 'investigation', label: 'Investigation & Evidence', icon: Search },
    { id: 'rca', label: 'Root-Cause Analysis', icon: FileText },
    { id: 'replay', label: 'Replay & Fault Lab', icon: PlayCircle },
    { id: 'settings', label: 'Settings & Status', icon: Settings },
  ];

  return (
    <aside className="w-64 border-r border-graphite-800 bg-graphite-950 flex flex-col justify-between shrink-0 select-none">
      <div className="p-3 space-y-1">
        <div className="px-3 py-2 text-[11px] font-mono uppercase tracking-wider text-graphite-500 font-semibold">
          Platform Navigation
        </div>

        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentView === item.id;

          return (
            <button
              key={item.id}
              onClick={() => onSelectView(item.id as ViewType)}
              className={`w-full flex items-center justify-between px-3 py-2 rounded-md text-xs font-mono font-medium transition-all ${
                isActive
                  ? 'bg-cyan-950/70 text-cyan-300 border border-cyan-800/80 shadow-sm'
                  : 'text-graphite-400 hover:text-graphite-100 hover:bg-graphite-900 border border-transparent'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Icon size={16} className={isActive ? 'text-cyan-400' : 'text-graphite-500'} />
                <span>{item.label}</span>
              </div>

              {item.badge !== undefined && (
                <span className="px-1.5 py-0.2 rounded-full text-[10px] font-mono bg-crimson-950 text-crimson-400 border border-crimson-800 font-bold">
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* System Footer info */}
      <div className="p-3 border-t border-graphite-850 text-[11px] font-mono text-graphite-500">
        <div className="flex items-center justify-between mb-1">
          <span>Engine Runtime:</span>
          <span className="text-graphite-300 font-medium">Java 21 LTS</span>
        </div>
        <div className="flex items-center justify-between">
          <span>Telemetry Protocol:</span>
          <span className="text-cyan-400 font-medium">OTel W3C</span>
        </div>
      </div>
    </aside>
  );
};
