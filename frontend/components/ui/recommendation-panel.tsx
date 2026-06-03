'use client';

import type React from 'react';
import { Clock3, Sparkles, ChevronRight, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';
import { RecommendationCardStack, type RecommendationStackItem } from './recommendation-card-stack';

const TEXT = {
  title: '\u4e0b\u4e00\u9910\u63a8\u8350',
  empty: '\u8fd8\u6ca1\u6709\u4e0b\u4e00\u9910\u63a8\u8350\u3002',
  loading: '\u6b63\u5728\u52a0\u8f7d\u63a8\u8350...',
  generating: '\u751f\u6210\u4e2d...',
  generate: 'AI \u751f\u6210\u4e0b\u4e00\u9910\u63a8\u8350',
  refresh: '\u6362\u4e00\u6362',
  autoMeal: '\u6839\u636e\u5f53\u524d\u65f6\u6bb5\u81ea\u52a8\u5207\u6362\u63a8\u8350\u9910\u6b21',
  moreRestaurants: '\u67e5\u770b\u66f4\u591a\u9644\u8fd1\u9910\u5385',
};

export interface SuggestionItem extends RecommendationStackItem {
  name: string;
  calories: number;
}

export interface RecommendationPanelProps {
  nextMealLabel: string;
  items: SuggestionItem[];
  className?: string;
  collapseButton?: React.ReactNode;
  generating?: boolean;
  loading?: boolean;
  generateError?: string;
  onGenerate?: () => void;
  onRefresh?: () => void;
}

export function RecommendationPanel({
  nextMealLabel,
  items,
  className,
  collapseButton,
  generating = false,
  loading = false,
  generateError,
  onGenerate,
  onRefresh,
}: RecommendationPanelProps) {
  return (
    <aside className={cn('w-72 rounded-3xl border border-white/10 bg-black/50 p-5 text-white shadow-2xl backdrop-blur-xl', className)}>
      <header className="mb-4 flex items-start justify-between">
        <div>
          <p className="text-xs text-white/50">{TEXT.title}</p>
          <h3 className="mt-1 text-lg font-bold text-white">{nextMealLabel}</h3>
        </div>
        {collapseButton ?? (
          <div className="grid h-11 w-11 place-items-center rounded-full bg-emerald-500/15 text-emerald-400">
            <Sparkles size={18} />
          </div>
        )}
      </header>

      {loading ? (
        <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-5 text-sm text-white/55">
          <div className="flex items-center gap-2">
            <RefreshCw size={14} className="animate-spin text-emerald-300" />
            <span>{TEXT.loading}</span>
          </div>
        </div>
      ) : items.length === 0 ? (
        <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-5 text-sm text-white/55">
          <p className="leading-relaxed">{TEXT.empty}</p>
          {onGenerate ? (
            <button
              type="button"
              onClick={onGenerate}
              disabled={generating}
              className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl border border-emerald-400/30 bg-emerald-500/15 px-3 py-2 text-xs font-semibold text-emerald-200 transition hover:bg-emerald-500/25 disabled:opacity-55"
            >
              {generating ? <RefreshCw size={13} className="animate-spin" /> : <Sparkles size={13} />}
              {generating ? TEXT.generating : TEXT.generate}
            </button>
          ) : null}
          {generateError ? <p className="mt-2 text-xs text-red-300">{generateError}</p> : null}
        </div>
      ) : (
        <div>
          <RecommendationCardStack items={items} />
          {onRefresh ? (
            <button
              type="button"
              onClick={onRefresh}
              disabled={generating}
              className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl border border-cyan-400/25 bg-cyan-500/10 px-3 py-2 text-xs font-semibold text-cyan-100 transition hover:bg-cyan-500/18 disabled:opacity-55"
            >
              {generating ? <RefreshCw size={13} className="animate-spin" /> : <Sparkles size={13} />}
              {generating ? TEXT.generating : TEXT.refresh}
            </button>
          ) : null}
          {generateError ? <p className="mt-2 text-xs text-red-300">{generateError}</p> : null}
        </div>
      )}

      <div className="mt-4 flex items-center gap-2 text-xs text-white/50">
        <Clock3 size={13} />
        {TEXT.autoMeal}
      </div>

      <a
        href="/restaurants/nearby"
        className="mt-3 flex items-center justify-between rounded-xl border border-white/10 bg-emerald-500/10 px-3 py-2 text-xs text-emerald-300 transition-colors hover:bg-emerald-500/20"
      >
        <span>{TEXT.moreRestaurants}</span>
        <ChevronRight size={14} />
      </a>
    </aside>
  );
}
