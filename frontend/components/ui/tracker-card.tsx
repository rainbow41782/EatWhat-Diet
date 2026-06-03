'use client';
import * as React from 'react';
import { motion, useSpring, useTransform } from 'framer-motion';
import { CheckCircle2, ChevronUp, ChevronDown, Flame, Droplets, Plus, Check } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Suggestion {
  name: string;
  calories: number;
}

export interface MealCheckItem {
  key: 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | string;
  label: string;
  completed: boolean;
  optional?: boolean;
  href: string;
}

export interface MacroData {
  current: number;
  goal: number;
}

export interface CalorieTrackerCardProps {
  icon?: React.ReactNode;
  title?: string;
  subtitle?: string;
  currentCalories: number;
  goalCalories: number;
  macros?: {
    protein: MacroData;
    fat: MacroData;
    carb: MacroData;
  };
  waterIntakeMl?: number;
  onAddWater?: (addedMl: number) => void;
  suggestions?: Suggestion[];
  mealChecks: MealCheckItem[];
  streak?: number;
  className?: string;
  collapseButton?: React.ReactNode;
}

// 单条宏量进度条
function MacroBar({ label, current, goal, color }: { label: string; current: number; goal: number; color: string }) {
  const pct = goal > 0 ? Math.min((current / goal) * 100, 100) : 0;
  const over = current > goal * 1.05; // 超出5%才算超标
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-[11px]">
        <span className="text-white/60">{label}</span>
        <span className={over ? 'text-red-400' : 'text-white/50'}>
          {Math.round(current)}<span className="text-white/30">/{Math.round(goal)}g</span>
        </span>
      </div>
      <div className="h-1.5 w-full rounded-full bg-white/10 overflow-hidden">
        <motion.div
          className="h-full rounded-full"
          style={{ background: over ? '#ef4444' : color }}
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 1, ease: 'easeInOut' }}
        />
      </div>
    </div>
  );
}

const CalorieTrackerCard = React.forwardRef<HTMLDivElement, CalorieTrackerCardProps>(
  (
    {
      className,
      icon,
      title = '今日热量',
      subtitle,
      currentCalories,
      goalCalories,
      macros,
      waterIntakeMl = 0,
      onAddWater,
      suggestions = [],
      mealChecks,
      streak = 0,
      collapseButton,
    },
    ref,
  ) => {
    const progressPercentage = Math.min((currentCalories / goalCalories) * 100, 100);
    // 容忍区间：超出目标 5% 以内不报警，超出才变红
    const overTolerance = goalCalories * 0.05;
    const isSlightlyOver = currentCalories > goalCalories && currentCalories <= goalCalories + overTolerance;
    const isOverGoal = currentCalories > goalCalories + overTolerance;

    const mainMeals = mealChecks.filter((item) => !item.optional);
    const completedMainCount = mainMeals.filter((item) => item.completed).length;
    const dailyCompleted = mainMeals.length > 0 && completedMainCount === mainMeals.length;

    const animatedCalories = useSpring(0, { damping: 40, stiffness: 300 });
    const displayCalories = useTransform(animatedCalories, (v) => Math.round(v).toString());

    // 饮水量内联输入状态
    const [waterInputVisible, setWaterInputVisible] = React.useState(false);
    const [waterInputValue, setWaterInputValue] = React.useState('');

    const handleConfirmWater = () => {
      const ml = parseFloat(waterInputValue);
      if (!isNaN(ml) && ml > 0 && onAddWater) {
        onAddWater(ml);
      }
      setWaterInputValue('');
      setWaterInputVisible(false);
    };

    React.useEffect(() => {
      animatedCalories.set(currentCalories);
    }, [currentCalories, animatedCalories]);

    const displaySubtitle =
      subtitle ??
      (dailyCompleted ? `今日主餐已完成，连续 ${streak} 天` : `主餐打卡 ${completedMainCount}/${mainMeals.length}`);

    return (
      <div
        ref={ref}
        className={cn(
          'w-72 rounded-3xl p-5 flex flex-col gap-4 border',
          'bg-black/50 backdrop-blur-xl border-white/10 text-white shadow-2xl',
          className,
        )}
      >
        <header className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="grid h-11 w-11 place-items-center rounded-full bg-emerald-500/15 text-emerald-400">
              {icon ?? <Flame size={20} />}
            </div>
            <div>
              <h2 className="font-bold text-base text-white">{title}</h2>
              <p className="text-xs text-white/50">{displaySubtitle}</p>
            </div>
          </div>
          {collapseButton ?? (
            <button className="text-white/40 hover:text-white/70 transition-colors">
              <ChevronUp className="h-5 w-5" />
            </button>
          )}
        </header>

        {/* 热量主显示 */}
        <div className="flex flex-col gap-2">
          <div className="flex items-end gap-2">
            <motion.p className="text-5xl font-bold tracking-tighter text-white">{displayCalories}</motion.p>
            <p className="mb-1 text-white/50 text-sm font-medium">/ {goalCalories}</p>
            <p className="mb-1 ml-auto text-white/60 text-xs">kcal</p>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-white/10">
            <motion.div
              className={cn('h-full rounded-full', isOverGoal ? 'bg-red-500' : isSlightlyOver ? 'bg-amber-400' : 'bg-gradient-to-r from-emerald-500 to-teal-400')}
              initial={{ width: 0 }}
              animate={{ width: `${progressPercentage}%` }}
              transition={{ duration: 1.2, ease: 'easeInOut' }}
            />
          </div>
          {isOverGoal ? (
            <p className="text-xs text-red-400">超出目标 {(currentCalories - goalCalories).toFixed(1)} kcal</p>
          ) : isSlightlyOver ? (
            <p className="text-xs text-amber-400/80">轻微超出 {(currentCalories - goalCalories).toFixed(1)} kcal</p>
          ) : null}
        </div>

        {/* 宏量进度条 */}
        {macros && (
          <div className="space-y-2">
            <MacroBar label="蛋白质" current={macros.protein.current} goal={macros.protein.goal} color="linear-gradient(90deg, #f97316, #fb923c)" />
            <MacroBar label="碳水化合物" current={macros.carb.current} goal={macros.carb.goal} color="linear-gradient(90deg, #3b82f6, #60a5fa)" />
            <MacroBar label="脂肪" current={macros.fat.current} goal={macros.fat.goal} color="linear-gradient(90deg, #eab308, #facc15)" />
          </div>
        )}

        {/* 饮水量 */}
        <div className="rounded-xl border border-white/10 bg-white/5 px-3 py-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Droplets size={14} className="text-blue-400" />
              <span className="text-xs text-white/70">今日饮水</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold text-blue-300">
                {waterIntakeMl >= 1000
                  ? `${(waterIntakeMl / 1000).toFixed(1)} L`
                  : `${Math.round(waterIntakeMl)} mL`}
              </span>
              {!waterInputVisible && (
                <button
                  onClick={() => { setWaterInputVisible(true); setWaterInputValue('200'); }}
                  className="w-6 h-6 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400 hover:bg-blue-500/40 transition"
                >
                  <Plus size={12} />
                </button>
              )}
            </div>
          </div>
          {waterInputVisible && (
            <div className="mt-2 flex items-center gap-1.5">
              <div className="relative flex-1">
                <input
                  type="text"
                  inputMode="numeric"
                  placeholder="ml"
                  value={waterInputValue}
                  onChange={(e) => setWaterInputValue(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleConfirmWater(); }}
                  className="w-full h-7 rounded-lg border border-white/10 bg-white/5 pl-2 pr-9 text-xs text-white outline-none focus:border-blue-400/50"
                  autoFocus
                />
                <div className="absolute right-1 top-1/2 -translate-y-1/2 flex flex-col h-6 justify-between">
                  <button
                    type="button"
                    onClick={() => setWaterInputValue((v) => String(Math.max(50, (parseFloat(v) || 200) + 50)))}
                    className="flex items-center justify-center w-7 h-3 hover:bg-white/10 rounded transition-colors"
                  >
                    <ChevronUp size={9} className="text-blue-400/70" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setWaterInputValue((v) => String(Math.max(50, (parseFloat(v) || 200) - 50)))}
                    className="flex items-center justify-center w-7 h-3 hover:bg-white/10 rounded transition-colors"
                  >
                    <ChevronDown size={9} className="text-blue-400/70" />
                  </button>
                </div>
              </div>
              <button
                onClick={handleConfirmWater}
                className="w-7 h-7 rounded-lg bg-blue-500/30 flex items-center justify-center text-blue-300 hover:bg-blue-500/50 transition"
              >
                <Check size={12} />
              </button>
              <button
                onClick={() => { setWaterInputVisible(false); setWaterInputValue(''); }}
                className="text-xs text-white/30 hover:text-white/50 transition"
              >
                取消
              </button>
            </div>
          )}
        </div>

        {/* 餐次打卡 */}
        <div className="space-y-2">
          <h3 className="font-semibold text-sm text-white/80">今日打卡进度</h3>
          <div className="grid grid-cols-2 gap-2">
            {mealChecks.map((meal) => (
              <a
                key={meal.key}
                href={meal.href}
                className={cn(
                  'rounded-xl border px-3 py-2 text-xs transition',
                  meal.completed
                    ? 'border-emerald-400/40 bg-emerald-500/15 text-emerald-200'
                    : 'border-white/10 bg-white/5 text-white/75 hover:bg-white/10',
                )}
              >
                <div className="flex items-center justify-between">
                  <span>{meal.label}</span>
                  {meal.completed ? <CheckCircle2 size={13} /> : null}
                </div>
                <p className="mt-1 text-[10px] opacity-80">{meal.optional ? '可选加餐' : meal.completed ? '已完成' : '去填写记录'}</p>
              </a>
            ))}
          </div>
          <p className="text-[11px] text-white/55">说明：早/中/晚三餐都完成才计为当日完成，加餐可按热量情况选择。</p>
        </div>

        {suggestions.length > 0 ? (
          <div className="flex flex-col gap-2">
            <h3 className="font-semibold text-sm text-white/80">推荐食物</h3>
            <ul className="flex flex-col gap-1.5">
              {suggestions.slice(0, 3).map((item) => (
                <li key={item.name} className="flex justify-between text-xs">
                  <p className="text-white/50">{item.name}</p>
                  <p className="font-medium text-white/80">{item.calories} kcal</p>
                </li>
              ))}
            </ul>
          </div>
        ) : null}
      </div>
    );
  },
);

CalorieTrackerCard.displayName = 'CalorieTrackerCard';
export { CalorieTrackerCard };
