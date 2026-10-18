import React from 'react';
import { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  subtext?: string;
  icon: LucideIcon;
  variant?: 'default' | 'critical' | 'warning' | 'success';
}

export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtext,
  icon: Icon,
  variant = 'default',
}) => {
  const borderVariants = {
    default: 'border-graphite-800 hover:border-graphite-700',
    critical: 'border-crimson-900/60 bg-crimson-950/20',
    warning: 'border-amber-900/60 bg-amber-950/20',
    success: 'border-emerald-900/60 bg-emerald-950/20',
  };

  const iconVariants = {
    default: 'text-cyan-400 bg-graphite-800/80',
    critical: 'text-crimson-400 bg-crimson-950/80',
    warning: 'text-amber-400 bg-amber-950/80',
    success: 'text-emerald-400 bg-emerald-950/80',
  };

  return (
    <div className={`p-4 rounded-lg bg-graphite-900 border transition-all ${borderVariants[variant]}`}>
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-medium text-graphite-400 uppercase tracking-wider">{title}</span>
        <div className={`p-1.5 rounded ${iconVariants[variant]}`}>
          <Icon size={16} />
        </div>
      </div>
      <div className="text-2xl font-bold font-mono text-graphite-100">{value}</div>
      {subtext && <div className="text-xs text-graphite-400 mt-1">{subtext}</div>}
    </div>
  );
};
