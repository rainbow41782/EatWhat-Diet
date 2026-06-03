'use client';

import * as React from 'react';
import { motion } from 'framer-motion';
import { ChevronLeft, ChevronRight, Flame, MapPin } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface RecommendationStackItem {
  name: string;
  description?: string;
  calories: number;
  protein?: number;
  fat?: number;
  carb?: number;
  restaurantName?: string;
  restaurantAddress?: string;
  price?: number;
  portionSize?: string;
  imageUrl?: string;
}

function macroText(value?: number, suffix = 'g') {
  return value == null ? '-' : `${Math.round(value * 10) / 10}${suffix}`;
}

export function RecommendationCardStack({
  items,
  className,
}: {
  items: RecommendationStackItem[];
  className?: string;
}) {
  const [active, setActive] = React.useState(0);
  const safeItems = items.slice(0, 4);
  const current = safeItems[active];

  React.useEffect(() => {
    if (active >= safeItems.length) {
      setActive(0);
    }
  }, [active, safeItems.length]);

  if (!current) return null;

  const move = (direction: 1 | -1) => {
    setActive((value) => (value + direction + safeItems.length) % safeItems.length);
  };

  return (
    <div className={cn('relative', className)}>
      <div className="relative h-[292px] overflow-hidden rounded-2xl">
        {safeItems.map((item, index) => {
          const offset = (index - active + safeItems.length) % safeItems.length;
          const visible = offset < 3;
          return (
            <motion.article
              key={`${item.name}-${index}`}
              initial={false}
              animate={{
                x: visible ? offset * 10 : 32,
                y: visible ? offset * 12 : 28,
                scale: visible ? 1 - offset * 0.045 : 0.86,
                opacity: visible ? 1 - offset * 0.22 : 0,
                zIndex: safeItems.length - offset,
              }}
              transition={{ type: 'spring', stiffness: 260, damping: 28 }}
              className={cn(
                'absolute inset-0 rounded-2xl border border-white/10 bg-zinc-950/88 p-4 shadow-2xl backdrop-blur-xl',
                offset === 0 ? 'pointer-events-auto' : 'pointer-events-none',
              )}
              style={{
                background:
                  'linear-gradient(145deg, rgba(16,185,129,0.16), rgba(14,165,233,0.10) 42%, rgba(24,24,27,0.92) 100%)',
              }}
            >
              {item.imageUrl ? (
                <div
                  className="mb-3 h-24 rounded-xl bg-cover bg-center ring-1 ring-white/10"
                  style={{ backgroundImage: `url(${item.imageUrl})` }}
                />
              ) : (
                <div className="mb-3 grid h-24 place-items-center rounded-xl border border-white/10 bg-white/[0.04]">
                  <Flame size={22} className="text-emerald-300/80" />
                </div>
              )}

              <div className="min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <h4 className="line-clamp-2 text-sm font-semibold leading-snug text-white">{item.name}</h4>
                  {item.price != null ? (
                    <span className="shrink-0 rounded-full border border-emerald-400/25 bg-emerald-500/10 px-2 py-0.5 text-xs text-emerald-200">
                      ¥{item.price}
                    </span>
                  ) : null}
                </div>
                {item.restaurantName ? (
                  <div className="mt-1 flex items-center gap-1 text-[11px] text-white/45">
                    <MapPin size={11} className="shrink-0" />
                    <span className="truncate">{item.restaurantName}</span>
                  </div>
                ) : null}
                {item.description ? (
                  <p className="mt-2 line-clamp-2 text-xs leading-relaxed text-white/56">{item.description}</p>
                ) : null}
              </div>

              <div className="mt-3 grid grid-cols-2 gap-1.5 text-[11px]">
                <span className="rounded-lg border border-orange-400/20 bg-orange-500/10 px-2 py-1 text-orange-200">
                  {macroText(item.calories, ' kcal')}
                </span>
                <span className="rounded-lg border border-sky-400/20 bg-sky-500/10 px-2 py-1 text-sky-200">
                  蛋白 {macroText(item.protein)}
                </span>
                <span className="rounded-lg border border-violet-400/20 bg-violet-500/10 px-2 py-1 text-violet-200">
                  脂肪 {macroText(item.fat)}
                </span>
                <span className="rounded-lg border border-cyan-400/20 bg-cyan-500/10 px-2 py-1 text-cyan-200">
                  碳水 {macroText(item.carb)}
                </span>
              </div>
            </motion.article>
          );
        })}
      </div>

      {safeItems.length > 1 ? (
        <div className="mt-3 flex items-center justify-between">
          <button
            type="button"
            onClick={() => move(-1)}
            className="grid h-8 w-8 place-items-center rounded-full border border-white/10 bg-white/5 text-white/70 hover:bg-white/10"
            aria-label="上一道推荐"
          >
            <ChevronLeft size={15} />
          </button>
          <div className="flex items-center gap-1.5">
            {safeItems.map((_, index) => (
              <button
                key={index}
                type="button"
                onClick={() => setActive(index)}
                className={cn(
                  'h-1.5 rounded-full transition-all',
                  index === active ? 'w-5 bg-emerald-300' : 'w-1.5 bg-white/25',
                )}
                aria-label={`查看第 ${index + 1} 道推荐`}
              />
            ))}
          </div>
          <button
            type="button"
            onClick={() => move(1)}
            className="grid h-8 w-8 place-items-center rounded-full border border-white/10 bg-white/5 text-white/70 hover:bg-white/10"
            aria-label="下一道推荐"
          >
            <ChevronRight size={15} />
          </button>
        </div>
      ) : null}
    </div>
  );
}
