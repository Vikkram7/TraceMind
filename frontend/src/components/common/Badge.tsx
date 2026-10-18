import React from 'react';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'healthy' | 'critical' | 'degraded' | 'neutral' | 'demo' | 'live' | 'accent';
  className?: string;
  size?: 'sm' | 'md';
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  className = '',
  size = 'md',
}) => {
  const baseClasses = 'inline-flex items-center font-medium font-mono rounded tracking-tight transition-colors';

  const sizeClasses = size === 'sm' ? 'px-1.5 py-0.5 text-[10px]' : 'px-2 py-0.5 text-xs';

  const variantMap = {
    healthy: 'bg-emerald-950 text-emerald-400 border border-emerald-900',
    critical: 'bg-crimson-950 text-crimson-400 border border-crimson-900',
    degraded: 'bg-amber-950 text-amber-400 border border-amber-900',
    neutral: 'bg-graphite-800 text-graphite-300 border border-graphite-700',
    demo: 'bg-purple-950 text-purple-300 border border-purple-800/80',
    live: 'bg-cyan-950 text-cyan-300 border border-cyan-800',
    accent: 'bg-cyan-900/50 text-cyan-300 border border-cyan-700',
  };

  return (
    <span className={`${baseClasses} ${sizeClasses} ${variantMap[variant]} ${className}`}>
      {children}
    </span>
  );
};
