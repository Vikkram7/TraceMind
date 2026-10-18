import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { ServiceSummary } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Badge } from '../components/common/Badge';
import { Server, ArrowRight, RefreshCw, ShieldCheck } from 'lucide-react';

interface ServicesViewProps {
  onSelectService: (serviceName: string) => void;
}

export const ServicesView: React.FC<ServicesViewProps> = ({ onSelectService }) => {
  const [loading, setLoading] = useState(true);
  const [services, setServices] = useState<ServiceSummary[]>([]);

  const loadServices = async () => {
    setLoading(true);
    try {
      const data = await api.getServices();
      setServices(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadServices();
  }, []);

  if (loading && services.length === 0) {
    return <LoadingSpinner message="Querying microservices registry..." />;
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-graphite-800">
        <div>
          <h2 className="text-lg font-bold text-graphite-100 font-sans flex items-center gap-2">
            <Server size={20} className="text-cyan-400" />
            <span>Monitored Microservices Fleet</span>
          </h2>
          <p className="text-xs font-mono text-graphite-400">
            Runtime inventory, instrumentation status, and operational SLAs per service.
          </p>
        </div>

        <button
          onClick={loadServices}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-medium bg-graphite-850 text-graphite-300 border border-graphite-700 hover:bg-graphite-800"
        >
          <RefreshCw size={13} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Services Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {services.map((svc) => (
          <div
            key={svc.id}
            className="p-4 rounded-lg bg-graphite-900 border border-graphite-800 hover:border-graphite-700 transition-all flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div className="p-2 rounded bg-graphite-800 text-cyan-400">
                    <Server size={18} />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold font-mono text-graphite-100">{svc.name}</h3>
                    <span className="text-[10px] font-mono text-graphite-400">{svc.environment} / v{svc.version}</span>
                  </div>
                </div>
                <Badge
                  variant={
                    svc.status === 'HEALTHY'
                      ? 'healthy'
                      : svc.status === 'CRITICAL'
                      ? 'critical'
                      : 'degraded'
                  }
                >
                  {svc.status}
                </Badge>
              </div>

              <div className="space-y-2 text-xs font-mono pt-2 border-t border-graphite-800/80">
                <div className="flex justify-between">
                  <span className="text-graphite-400">Total Spans:</span>
                  <span className="text-graphite-100 font-semibold">{svc.totalSpans}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-graphite-400">Error Spans:</span>
                  <span className={svc.errorSpans > 0 ? 'text-crimson-400 font-bold' : 'text-graphite-100'}>
                    {svc.errorSpans} ({svc.errorRate}%)
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-graphite-400">Mean Duration:</span>
                  <span className="text-graphite-100 font-semibold">{svc.avgDurationMs} ms</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-graphite-400">Instrumentation:</span>
                  <span className="text-emerald-400 font-semibold flex items-center gap-1">
                    <ShieldCheck size={13} />
                    OpenTelemetry W3C
                  </span>
                </div>
              </div>
            </div>

            <button
              onClick={() => onSelectService(svc.name)}
              className="mt-4 flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs font-mono font-medium rounded bg-graphite-850 text-cyan-300 border border-graphite-700 hover:bg-graphite-800 transition-colors"
            >
              <span>Explore Spans</span>
              <ArrowRight size={13} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
