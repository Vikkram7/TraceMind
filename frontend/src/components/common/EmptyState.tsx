import React from 'react';
import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  title: string;
  description: string;
  icon: LucideIcon;
  actionText?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  icon: Icon,
  actionText,
  onAction,
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center border border-dashed border-graphite-800 rounded-lg bg-graphite-900/40 my-6">
      <div className="p-3 bg-graphite-800 rounded-full text-graphite-400 mb-3">
        <Icon size={24} />
      </div>
      <h3 className="text-base font-semibold text-graphite-200 mb-1">{title}</h3>
      <p className="text-xs text-graphite-400 max-w-sm mb-4">{description}</p>
      {actionText && onAction && (
        <button
          onClick={onAction}
          className="px-3 py-1.5 text-xs font-medium text-cyan-300 bg-cyan-950 border border-cyan-800 hover:bg-cyan-900/60 rounded transition-colors"
        >
          {actionText}
        </button>
      )}
    </div>
  );
};
