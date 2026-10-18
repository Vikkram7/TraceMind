import { useState, useEffect, useCallback } from 'react';
import { api } from './api/client';
import { OverviewMetrics } from './types';
import { Navbar } from './components/layout/Navbar';
import { Sidebar, ViewType } from './components/layout/Sidebar';
import { OverviewView } from './views/OverviewView';
import { TopologyView } from './views/TopologyView';
import { ServicesView } from './views/ServicesView';
import { TraceExplorerView } from './views/TraceExplorerView';
import { TraceComparisonView } from './views/TraceComparisonView';
import { IncidentsView } from './views/IncidentsView';
import { InvestigationView } from './views/InvestigationView';
import { RcaReportView } from './views/RcaReportView';
import { ReplayLabView } from './views/ReplayLabView';
import { SettingsView } from './views/SettingsView';

export default function App() {
  const [currentView, setCurrentView] = useState<ViewType>('overview');
  const [metrics, setMetrics] = useState<OverviewMetrics | null>(null);

  // Navigation states
  const [selectedTraceId, setSelectedTraceId] = useState<string | null>(null);
  const [selectedIncidentId, setSelectedIncidentId] = useState<string | null>(null);
  const [selectedServiceFilter, setSelectedServiceFilter] = useState<string | null>(null);
  const [compareBaselineId, setCompareBaselineId] = useState<string | null>(null);
  const [compareTargetId, setCompareTargetId] = useState<string | null>(null);

  const fetchMetrics = useCallback(async () => {
    try {
      const data = await api.getOverviewMetrics();
      setMetrics(data);
    } catch {
      // Fallback empty metrics structure if backend is booting
      setMetrics({
        totalTraces: 0,
        errorTraces: 0,
        errorRate: 0,
        avgLatencyMs: 0,
        p95LatencyMs: 0,
        activeIncidents: 0,
        servicesCount: 3,
        telemetryConnected: false,
        liveTracesCount: 0,
        demoTracesCount: 0,
        recentTraces: [],
        recentIncidents: [],
      });
    }
  }, []);

  useEffect(() => {
    fetchMetrics();
    const interval = setInterval(fetchMetrics, 8000);
    return () => clearInterval(interval);
  }, [fetchMetrics]);

  return (
    <div className="min-h-screen bg-graphite-950 text-graphite-100 flex flex-col font-sans selection:bg-cyan-900 selection:text-cyan-300">
      {/* Top Navbar */}
      <Navbar
        telemetryConnected={metrics?.telemetryConnected ?? false}
        liveCount={metrics?.liveTracesCount ?? 0}
        demoCount={metrics?.demoTracesCount ?? 0}
        onRefresh={fetchMetrics}
      />

      <div className="flex-1 flex overflow-hidden">
        {/* Left Sidebar */}
        <Sidebar
          currentView={currentView}
          onSelectView={(view) => {
            if (view !== 'traces') setSelectedServiceFilter(null);
            setCurrentView(view);
          }}
          activeIncidentsCount={metrics?.activeIncidents ?? 0}
        />

        {/* Main Content Area */}
        <main className="flex-1 overflow-y-auto p-6 bg-graphite-950">
          {currentView === 'overview' && (
            <OverviewView
              metrics={metrics}
              onSelectTrace={(tId) => {
                setSelectedTraceId(tId);
                setCurrentView('traces');
              }}
              onSelectIncident={(incId) => {
                setSelectedIncidentId(incId);
                setCurrentView('investigation');
              }}
              onNavigate={(v) => setCurrentView(v)}
            />
          )}

          {currentView === 'topology' && (
            <TopologyView
              onSelectServiceTraces={(service) => {
                setSelectedServiceFilter(service);
                setCurrentView('traces');
              }}
            />
          )}

          {currentView === 'services' && (
            <ServicesView
              onSelectService={(service) => {
                setSelectedServiceFilter(service);
                setCurrentView('traces');
              }}
            />
          )}

          {currentView === 'traces' && (
            <TraceExplorerView
              initialTraceId={selectedTraceId}
              initialServiceFilter={selectedServiceFilter}
              onNavigateToCompare={(base, target) => {
                setCompareBaselineId(base);
                setCompareTargetId(target);
                setCurrentView('compare');
              }}
            />
          )}

          {currentView === 'compare' && (
            <TraceComparisonView
              initialBaselineId={compareBaselineId}
              initialTargetId={compareTargetId}
            />
          )}

          {currentView === 'incidents' && (
            <IncidentsView
              onInvestigateIncident={(id) => {
                setSelectedIncidentId(id);
                setCurrentView('investigation');
              }}
            />
          )}

          {currentView === 'investigation' && (
            <InvestigationView
              selectedIncidentId={selectedIncidentId}
              onNavigateToRca={(id) => {
                setSelectedIncidentId(id);
                setCurrentView('rca');
              }}
              onNavigateToTrace={(tId) => {
                setSelectedTraceId(tId);
                setCurrentView('traces');
              }}
            />
          )}

          {currentView === 'rca' && (
            <RcaReportView initialIncidentId={selectedIncidentId} />
          )}

          {currentView === 'replay' && <ReplayLabView />}

          {currentView === 'settings' && <SettingsView />}
        </main>
      </div>
    </div>
  );
}
