'use client';
import { Clock3, Sparkles, MapPin, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface SuggestionItem {
  name: string;
  calories: number;
  restaurantName?: string;
  restaurantAddress?: string;
}

export interface RecommendationPanelProps {
  nextMealLabel: string;
  items: SuggestionItem[];
  className?: string;
  collapseButton?: React.ReactNode;
}

export function RecommendationPanel({ nextMealLabel, items, className, collapseButton }: RecommendationPanelProps) {
  return (
    <aside className={cn("w-72 rounded-3xl p-5 border bg-black/50 backdrop-blur-xl border-white/10 text-white shadow-2xl", className)}>
      <header className="flex items-start justify-between mb-4">
        <div>
          <p className="text-xs text-white/50">下一餐推荐</p>
          <h3 className="text-lg font-bold text-white mt-1">{nextMealLabel}</h3>
        </div>
        {collapseButton ?? (
          <div className="grid h-11 w-11 place-items-center rounded-full bg-emerald-500/15 text-emerald-400">
            <Sparkles size={18} />
          </div>
        )}
      </header>

      {items.length === 0 ? (
        <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-5 text-sm text-white/55">
          推荐系统即将上线，先为你预留位置。
        </div>
      ) : (
        <ul className="space-y-2">
          {items.slice(0, 4).map((item, idx) => (
            <li key={idx} className="rounded-xl border border-white/10 bg-white/5 px-3 py-2">
              <div className="flex items-center justify-between">
                <span className="text-sm text-white/85 flex-1 min-w-0 truncate">{item.name}</span>
                <span className="text-xs text-emerald-300 ml-2 shrink-0">{item.calories > 0 ? `${item.calories} kcal` : ''}</span>
              </div>
              {item.restaurantName && (
                <div className="flex items-center gap-1 mt-1 text-xs text-white/45">
                  <MapPin size={11} className="shrink-0" />
                  <span className="truncate">{item.restaurantName}</span>
                  {item.restaurantAddress && (
                    <span className="truncate text-white/30">· {item.restaurantAddress}</span>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      <div className="mt-4 flex items-center gap-2 text-xs text-white/50">
        <Clock3 size={13} />
        根据当前时段自动切换推荐餐种
      </div>

      <a
        href="/restaurants/nearby"
        className="mt-3 flex items-center justify-between rounded-xl border border-white/10 bg-emerald-500/10 px-3 py-2 text-xs text-emerald-300 hover:bg-emerald-500/20 transition-colors"
      >
        <span>查看更多附近餐厅</span>
        <ChevronRight size={14} />
      </a>
    </aside>
  );
}
