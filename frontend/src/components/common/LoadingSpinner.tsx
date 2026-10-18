import React from 'react';
import { Loader2 } from 'lucide-react';

export const LoadingSpinner: React.FC<{ message?: string }> = ({ message = 'Loading telemetry data...' }) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-graphite-400">
      <Loader2 className="animate-spin text-cyan-400 mb-3" size={28} />
      <span className="text-sm font-mono">{message}</span>
    </div>
  );
};
