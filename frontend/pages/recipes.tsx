'use client';
import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/router';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ChevronLeft, ChevronUp, ChevronDown, Sparkles, Search, Plus, Trash2,
  UtensilsCrossed, CheckCircle2, XCircle, RefreshCw,
  Flame, Beef, Wheat, Droplets, BookOpen, Save,
} from 'lucide-react';

import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { PageCard } from '@/components/ui/page-card';
import type { RecommendationStackItem } from '@/components/ui/recommendation-card-stack';
import { getAuth, clearAuth } from '@/lib/auth';
import {
  fetchUser, fetchFoods,
  generateDailyRecommendations, acceptRecommendation, ignoreRecommendation,
  fetchCurrentRecommendations,
  unwrapApiData,
  type RecommendationResponse,
} from '@/lib/api';
import { sortRecommendationsForDisplay } from '@/lib/recommendation-utils';

// ── 类型定义 ──────────────────────────────────────────────
type Rec = RecommendationResponse;

type FoodItem = {
  id: number;
  name: string;
  category?: string;
  caloriesPer100g?: number;
  proteinPer100g?: number;
  fatPer100g?: number;
  carbPer100g?: number;
};

type PlanEntry = {
  key: string;
  food: FoodItem;
  grams: number;
};

// ── 常量 ──────────────────────────────────────────────────
const MEAL_TYPES = [
  { key: 'BREAKFAST', label: '早餐', emoji: '🌅' },
  { key: 'LUNCH',     label: '午餐', emoji: '☀️' },
  { key: 'DINNER',    label: '晚餐', emoji: '🌙' },
  { key: 'SNACK',     label: '加餐', emoji: '🍎' },
];

const MY_FOODS_KEY = 'javadiet_my_foods';
function loadMyFoods(): FoodItem[] {
  if (typeof window === 'undefined') return [];
  try { return JSON.parse(localStorage.getItem(MY_FOODS_KEY) ?? '[]'); } catch { return []; }
}

function calcNutr(food: FoodItem, grams: number) {
  const r = grams / 100;
  return {
    cal:     Math.round((food.caloriesPer100g ?? 0) * r * 10) / 10,
    protein: Math.round((food.proteinPer100g  ?? 0) * r * 10) / 10,
    fat:     Math.round((food.fatPer100g      ?? 0) * r * 10) / 10,
    carb:    Math.round((food.carbPer100g     ?? 0) * r * 10) / 10,
  };
}

function MacroBadge({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: string; color: string }) {
  return (
    <div className={`flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs ${color}`}>
      {icon}
      <span className="text-white/60">{label}</span>
      <span className="font-mono font-semibold">{value}</span>
    </div>
  );
}

function formatMacro(value?: number, suffix = 'g') {
  return value == null ? '-' : `${Math.round(value * 10) / 10}${suffix}`;
}

// ── 克重步进输入（chevron 风格，与 correct-number-input 同款）──
function GramsInput({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  const step = 5;
  return (
    <div className="relative inline-flex items-center">
      <input
        type="text"
        inputMode="numeric"
        pattern="[0-9]*"
        value={value === 0 ? '' : value}
        onChange={e => {
          const v = e.target.value;
          onChange(v === '' ? 0 : Math.max(1, parseInt(v) || 0));
        }}
        className="w-20 h-9 pl-3 pr-8 text-sm text-white bg-white/5 border border-white/10 rounded-lg focus:outline-none focus:ring-1 focus:ring-white/20 transition-all text-center"
        placeholder="100"
      />
      <span className="absolute right-9 top-1/2 -translate-y-1/2 text-[10px] text-white/30 pointer-events-none select-none">g</span>
      <div className="absolute right-1 top-1/2 -translate-y-1/2 flex flex-col h-7 justify-between">
        <button type="button" onClick={() => onChange(value + step)}
          className="flex items-center justify-center w-6 h-3.5 hover:bg-white/10 transition-colors rounded">
          <ChevronUp size={10} className="text-white/50" />
        </button>
        <button type="button" onClick={() => onChange(Math.max(1, value - step))}
          className="flex items-center justify-center w-6 h-3.5 hover:bg-white/10 transition-colors rounded">
          <ChevronDown size={10} className="text-white/50" />
        </button>
      </div>
    </div>
  );
}

// ── 营养数值步进输入 ───────────────────────────────────────
function MacroInput({ label, value, onChange, accent }: {
  label: string; value: string; onChange: (v: string) => void; accent: string;
}) {
  const numVal = parseFloat(value) || 0;
  return (
    <div>
      <p className={`text-[10px] mb-1 font-medium ${accent}`}>{label}</p>
      <div className="relative">
        <input
          type="text" inputMode="numeric"
          value={value === '0' || value === '' ? '' : value}
          onChange={e => onChange(e.target.value)}
          placeholder="0"
          className="w-full h-9 pl-3 pr-8 text-xs text-white bg-white/5 border border-white/10 rounded-lg focus:outline-none focus:ring-1 focus:ring-white/20 placeholder-white/20 transition-all"
        />
        <div className="absolute right-1 top-1/2 -translate-y-1/2 flex flex-col h-7 justify-between">
          <button type="button" onClick={() => onChange(String(Math.round((numVal + 1) * 10) / 10))}
            className="flex items-center justify-center w-6 h-3.5 hover:bg-white/10 transition-colors rounded">
            <ChevronUp size={9} className="text-white/40" />
          </button>
          <button type="button" onClick={() => onChange(String(Math.max(0, Math.round((numVal - 1) * 10) / 10)))}
            className="flex items-center justify-center w-6 h-3.5 hover:bg-white/10 transition-colors rounded">
            <ChevronDown size={9} className="text-white/40" />
          </button>
        </div>
      </div>
    </div>
  );
}

function foodMacroBadges(food: FoodItem, small = false) {
  const s = small ? 9 : 10;
  return (
    <>
      <MacroBadge icon={<Flame size={s} />}    label="热量" value={`${food.caloriesPer100g ?? 0}`}  color="border-orange-500/30 text-orange-300" />
      <MacroBadge icon={<Beef size={s} />}     label="蛋白" value={`${food.proteinPer100g ?? 0}g`} color="border-blue-500/30 text-blue-300" />
      <MacroBadge icon={<Wheat size={s} />}    label="碳水" value={`${food.carbPer100g ?? 0}g`}    color="border-yellow-500/30 text-yellow-300" />
      <MacroBadge icon={<Droplets size={s} />} label="脂肪" value={`${food.fatPer100g ?? 0}g`}     color="border-purple-500/30 text-purple-300" />
    </>
  );
}

// ── 推荐食谱卡 ────────────────────────────────────────────
function recToStoryItem(rec: Rec): RecommendationStackItem {
  const item = rec.items?.[0];
  return {
    name: item?.name ?? rec.foodItemName ?? '推荐菜品',
    description: item?.description ?? rec.recommendedReason,
    calories: Math.round(item?.totalCalories ?? rec.totalCalories ?? 0),
    protein: item?.totalProtein,
    fat: item?.totalFat,
    carb: item?.totalCarb,
    restaurantName: item?.restaurantName ?? rec.restaurant?.name,
    restaurantAddress: item?.restaurantAddress ?? rec.restaurant?.address,
    price: item?.price,
    portionSize: item?.portionSize,
    imageUrl: item?.imageUrl,
  };
}

function RecCard({ rec, onAccept, onIgnore }: { rec: Rec; onAccept: () => void; onIgnore: () => void }) {
  const meal = MEAL_TYPES.find(m => m.key === rec.mealType);
  const isAccepted = rec.status === 'ACCEPTED';
  const isIgnored  = rec.status === 'IGNORED';
  const storyItem = recToStoryItem(rec);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ scale: 1.01, y: -2, transition: { duration: 0.18 } }}
      className={`rounded-2xl border p-4 transition-colors cursor-default ${
        isAccepted ? 'border-emerald-500/40 bg-emerald-500/10 hover:border-emerald-500/60' :
        isIgnored  ? 'border-white/5 bg-white/[0.02] opacity-50' :
        'border-white/10 bg-white/[0.04] hover:bg-white/[0.06] hover:border-white/15'
      }`}
    >
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="text-xl">{meal?.emoji ?? '🍽️'}</span>
          <div>
            <p className="text-sm font-semibold text-white">{storyItem.name}</p>
            <p className="text-xs text-white/40">建议摄入</p>
          </div>
        </div>
        {!isIgnored && (
          <div className="flex gap-1.5">
            {!isAccepted && (
              <button
                onClick={onAccept}
                className="flex items-center gap-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-xs text-emerald-300 hover:bg-emerald-500/20 transition"
              >
                <CheckCircle2 size={12} />采纳
              </button>
            )}
            <button
              onClick={onIgnore}
              className="flex items-center gap-1 rounded-lg border border-white/10 bg-white/5 px-2.5 py-1 text-xs text-white/50 hover:bg-white/10 transition"
            >
              <XCircle size={12} />{isAccepted ? '取消' : '忽略'}
            </button>
          </div>
        )}
      </div>

      <div className="mt-3 flex flex-wrap gap-2">
        <MacroBadge icon={<Flame size={10} />} label="热量" value={formatMacro(storyItem.calories, ' kcal')} color="border-orange-500/30 text-orange-300" />
        <MacroBadge icon={<Beef size={10} />}   label="蛋白" value={formatMacro(storyItem.protein)} color="border-blue-500/30 text-blue-300" />
        <MacroBadge icon={<Wheat size={10} />}  label="碳水" value={formatMacro(storyItem.carb)} color="border-yellow-500/30 text-yellow-300" />
        <MacroBadge icon={<Droplets size={10} />} label="脂肪" value={formatMacro(storyItem.fat)} color="border-purple-500/30 text-purple-300" />
      </div>

      {rec.recommendedReason ? (
        <p className="mt-3 text-xs text-white/55 leading-relaxed border-t border-white/10 pt-3">
          {rec.recommendedReason}
        </p>
      ) : null}
    </motion.div>
  );
}

// ── 单个餐次规划（搜索数据库 + 我的食物库）────────────────
function MealPlanner({
  mealKey, label, emoji,
  entries, onAdd, onRemove,
  myFoods,
}: {
  mealKey: string; label: string; emoji: string;
  entries: PlanEntry[];
  onAdd: (food: FoodItem, grams: number) => void;
  onRemove: (key: string) => void;
  myFoods: FoodItem[];
}) {
  const [keyword, setKeyword] = useState('');
  const [results, setResults]  = useState<FoodItem[]>([]);
  const [searching, setSearching] = useState(false);
  const [selectedFood, setSelectedFood] = useState<FoodItem | null>(null);
  const [grams, setGrams]     = useState(100);
  const [open, setOpen]       = useState(false);
  const [addMode, setAddMode] = useState<'search' | 'my-foods'>('search');

  async function doSearch() {
    if (!keyword.trim()) return;
    setSearching(true);
    try {
      const data = await fetchFoods(keyword);
      setResults(Array.isArray(data) ? data : (data.data ?? []));
    } finally {
      setSearching(false);
    }
  }

  function selectFood(food: FoodItem) {
    setSelectedFood(food); setResults([]); setKeyword(food.name); setGrams(100);
  }

  function confirmAdd() {
    if (!selectedFood || grams < 1) return;
    onAdd(selectedFood, grams);
    setSelectedFood(null); setKeyword(''); setGrams(100);
  }

  const totals = entries.reduce((acc, e) => {
    const n = calcNutr(e.food, e.grams);
    return { cal: acc.cal + n.cal, protein: acc.protein + n.protein, fat: acc.fat + n.fat, carb: acc.carb + n.carb };
  }, { cal: 0, protein: 0, fat: 0, carb: 0 });

  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.03] overflow-hidden">
      {/* 标题行 */}
      <button
        className="w-full flex items-center justify-between px-4 py-3 hover:bg-white/5 transition"
        onClick={() => setOpen(v => !v)}
      >
        <div className="flex items-center gap-2">
          <span className="text-lg">{emoji}</span>
          <span className="font-semibold text-white">{label}</span>
          {entries.length > 0 && (
            <span className="text-xs text-white/40 font-mono">{totals.cal.toFixed(0)} kcal</span>
          )}
        </div>
        <div className={`text-white/40 transition-transform duration-200 ${open ? 'rotate-180' : ''}`}>
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M2 5l5 5 5-5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
        </div>
      </button>

      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            initial={{ height: 0 }}
            animate={{ height: 'auto' }}
            exit={{ height: 0 }}
            transition={{ duration: 0.22, ease: 'easeInOut' }}
            className="overflow-hidden"
          >
            <div className="px-4 pb-4 space-y-3">
              {/* 已添加条目 */}
              {entries.map(e => {
                const n = calcNutr(e.food, e.grams);
                return (
                  <div key={e.key} className="flex items-center justify-between gap-2 rounded-xl border border-white/10 bg-white/5 px-3 py-2">
                    <div>
                      <p className="text-xs font-medium text-white">{e.food.name}</p>
                      <p className="text-[10px] text-white/40">{e.grams}g · {n.cal} kcal · 蛋白 {n.protein}g · 脂 {n.fat}g · 碳 {n.carb}g</p>
                    </div>
                    <button onClick={() => onRemove(e.key)} className="text-white/30 hover:text-red-400 transition"><Trash2 size={13} /></button>
                  </div>
                );
              })}

              {/* 添加食物 */}
              <div className="space-y-2">
                {/* 模式切换 */}
                <div className="flex rounded-xl border border-white/10 bg-white/5 p-0.5">
                  <button
                    onClick={() => { setAddMode('search'); setSelectedFood(null); }}
                    className={`flex-1 flex items-center justify-center gap-1.5 rounded-lg py-1.5 text-xs font-medium transition ${
                      addMode === 'search' ? 'bg-white/10 text-white shadow' : 'text-white/40 hover:text-white/60'
                    }`}
                  >
                    <Search size={11} />搜索数据库
                  </button>
                  <button
                    onClick={() => { setAddMode('my-foods'); setSelectedFood(null); setResults([]); setKeyword(''); }}
                    className={`flex-1 flex items-center justify-center gap-1.5 rounded-lg py-1.5 text-xs font-medium transition ${
                      addMode === 'my-foods' ? 'bg-white/10 text-white shadow' : 'text-white/40 hover:text-white/60'
                    }`}
                  >
                    <BookOpen size={11} />我的食物
                  </button>
                </div>

                {/* 搜索模式 */}
                {addMode === 'search' && (
                  <div className="space-y-2">
                    <div className="flex gap-2">
                      <input
                        value={keyword}
                        onChange={e => setKeyword(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && doSearch()}
                        placeholder="搜索食物..."
                        className="flex-1 rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-white placeholder-white/30 outline-none focus:border-emerald-500/50"
                      />
                      <button onClick={doSearch} disabled={searching} className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-white/60 hover:bg-white/10 transition">
                        {searching ? '...' : <Search size={13} />}
                      </button>
                    </div>
                    {results.length > 0 && (
                      <div className="max-h-36 overflow-y-auto rounded-xl border border-white/10 bg-black/40 backdrop-blur divide-y divide-white/5">
                        {results.map(food => (
                          <button key={food.id} onClick={() => selectFood(food)} className="w-full flex justify-between items-center px-3 py-2 text-xs hover:bg-white/10 transition text-left">
                            <span className="text-white">{food.name}</span>
                            <span className="text-white/40 font-mono">{food.caloriesPer100g ?? 0} kcal/100g</span>
                          </button>
                        ))}
                      </div>
                    )}
                    {selectedFood && (
                      <div className="flex items-center gap-2 rounded-xl border border-emerald-500/20 bg-emerald-500/5 px-3 py-2">
                        <span className="text-xs text-emerald-300 flex-1 truncate">{selectedFood.name}</span>
                        <GramsInput value={grams} onChange={setGrams} />
                        <button onClick={confirmAdd} className="flex-shrink-0 flex items-center gap-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1.5 text-xs text-emerald-300 hover:bg-emerald-500/20 transition">
                          <Plus size={11} />添加
                        </button>
                      </div>
                    )}
                  </div>
                )}

                {/* 我的食物库模式 */}
                {addMode === 'my-foods' && (
                  <div className="space-y-1.5">
                    {myFoods.length === 0 ? (
                      <p className="text-center text-xs text-white/30 py-4">食物库为空，请先在上方"我的食物库"中创建</p>
                    ) : (
                      myFoods.map(food => {
                        const isSelected = selectedFood?.id === food.id;
                        return (
                          <div key={food.id} className="rounded-xl border border-white/10 overflow-hidden">
                            <button
                              onClick={() => { setSelectedFood(isSelected ? null : food); setGrams(100); }}
                              className={`w-full flex items-center justify-between px-3 py-2 text-left transition ${isSelected ? 'bg-emerald-500/10' : 'hover:bg-white/5'}`}
                            >
                              <div className="flex-1 min-w-0">
                                <p className="text-xs font-medium text-white truncate">{food.name}</p>
                                <p className="text-[10px] text-white/35">{food.caloriesPer100g ?? 0} kcal · 蛋白 {food.proteinPer100g ?? 0}g/100g</p>
                              </div>
                              <div className={`text-white/40 transition-transform duration-150 ml-2 ${isSelected ? 'rotate-180' : ''}`}>
                                <svg width="12" height="12" viewBox="0 0 14 14" fill="none"><path d="M2 5l5 5 5-5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                              </div>
                            </button>
                            <AnimatePresence initial={false}>
                              {isSelected && (
                                <motion.div initial={{ height: 0 }} animate={{ height: 'auto' }} exit={{ height: 0 }} transition={{ duration: 0.18 }} className="overflow-hidden">
                                  <div className="flex items-center gap-2 px-3 py-2 border-t border-white/5">
                                    <span className="text-xs text-white/50 flex-1">份量</span>
                                    <GramsInput value={grams} onChange={setGrams} />
                                    <button onClick={confirmAdd} className="flex-shrink-0 flex items-center gap-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1.5 text-xs text-emerald-300 hover:bg-emerald-500/20 transition">
                                      <Plus size={11} />添加
                                    </button>
                                  </div>
                                </motion.div>
                              )}
                            </AnimatePresence>
                          </div>
                        );
                      })
                    )}
                  </div>
                )}
              </div>

              {/* 小计 */}
              {entries.length > 0 && (
                <div className="flex flex-wrap gap-2 pt-2 border-t border-white/10">
                  <MacroBadge icon={<Flame size={9} />}    label="热量" value={`${totals.cal.toFixed(0)} kcal`} color="border-orange-500/30 text-orange-300" />
                  <MacroBadge icon={<Beef size={9} />}     label="蛋白" value={`${totals.protein.toFixed(1)}g`} color="border-blue-500/30 text-blue-300" />
                  <MacroBadge icon={<Wheat size={9} />}    label="碳水" value={`${totals.carb.toFixed(1)}g`}    color="border-yellow-500/30 text-yellow-300" />
                  <MacroBadge icon={<Droplets size={9} />} label="脂肪" value={`${totals.fat.toFixed(1)}g`}     color="border-purple-500/30 text-purple-300" />
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ── 我的食物库 ─────────────────────────────────────────────
function MyFoodsLibrary({ myFoods, onAdd, onRemove }: {
  myFoods: FoodItem[];
  onAdd: (food: FoodItem) => void;
  onRemove: (id: number) => void;
}) {
  const [showForm, setShowForm] = useState(false);
  const [name, setName]       = useState('');
  const [cal, setCal]         = useState('');
  const [protein, setProtein] = useState('');
  const [carb, setCarb]       = useState('');
  const [fat, setFat]         = useState('');

  function handleCreate() {
    const trimmed = name.trim();
    if (!trimmed) return;
    onAdd({
      id: -Date.now(),
      name: trimmed,
      caloriesPer100g: parseFloat(cal) || 0,
      proteinPer100g:  parseFloat(protein) || 0,
      carbPer100g:     parseFloat(carb) || 0,
      fatPer100g:      parseFloat(fat) || 0,
    });
    setName(''); setCal(''); setProtein(''); setCarb(''); setFat('');
    setShowForm(false);
  }

  return (
    <PageCard tilt={false} animate={false}>
      <div className="p-5 space-y-4">
        {/* 标题行 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BookOpen size={15} className="text-emerald-400" />
            <p className="text-sm font-semibold text-white">我的食物库</p>
            {myFoods.length > 0 && (
              <span className="rounded-full bg-white/10 px-1.5 py-0.5 text-[10px] text-white/50">{myFoods.length}</span>
            )}
          </div>
          <button
            onClick={() => setShowForm(v => !v)}
            className="flex items-center gap-1 rounded-lg border border-white/10 bg-white/5 px-2.5 py-1.5 text-xs text-white/60 hover:bg-white/10 hover:text-white transition"
          >
            <Plus size={11} />{showForm ? '取消' : '新建食物'}
          </button>
        </div>

        {/* 创建表单 */}
        <AnimatePresence initial={false}>
          {showForm && (
            <motion.div
              initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.2 }}
              className="overflow-hidden"
            >
              <div className="rounded-xl border border-white/10 bg-white/[0.03] p-4 space-y-3">
                <p className="text-[11px] text-white/40">营养数值以每 100g 计</p>
                <input
                  value={name}
                  onChange={e => setName(e.target.value)}
                  placeholder="食物名称（必填）"
                  className="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-white placeholder-white/25 outline-none focus:border-emerald-500/50"
                />
                <div className="grid grid-cols-2 gap-3">
                  <MacroInput label="热量 (kcal)" value={cal}     onChange={setCal}     accent="text-orange-300/80" />
                  <MacroInput label="蛋白质 (g)"  value={protein} onChange={setProtein} accent="text-blue-300/80" />
                  <MacroInput label="碳水 (g)"    value={carb}    onChange={setCarb}    accent="text-yellow-300/80" />
                  <MacroInput label="脂肪 (g)"    value={fat}     onChange={setFat}     accent="text-purple-300/80" />
                </div>
                <button
                  onClick={handleCreate}
                  disabled={!name.trim()}
                  className="w-full flex items-center justify-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 py-2 text-sm font-medium text-emerald-300 hover:bg-emerald-500/20 disabled:opacity-40 transition"
                >
                  <Save size={13} />保存到食物库
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* 食物列表 */}
        {myFoods.length === 0 ? (
          <p className="text-center text-xs text-white/25 py-4">暂无自定义食物，点击"新建食物"开始录入</p>
        ) : (
          <div className="space-y-2">
            {myFoods.map(food => (
              <div key={food.id} className="flex items-start gap-3 rounded-xl border border-white/8 bg-white/[0.02] px-3 py-2.5">
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-medium text-white mb-1 truncate">{food.name}</p>
                  <div className="flex flex-wrap gap-1">
                    {foodMacroBadges(food, true)}
                  </div>
                </div>
                <button onClick={() => onRemove(food.id)} className="flex-shrink-0 text-white/20 hover:text-red-400 transition mt-0.5">
                  <Trash2 size={13} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </PageCard>
  );
}

// ── 主页面 ────────────────────────────────────────────────
export default function RecipesPage() {
  const router = useRouter();
  const [tab, setTab] = useState<'recommend' | 'custom'>('recommend');
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [nickname, setNickname] = useState('');
  const [userId, setUserId] = useState<number | null>(null);

  // 今日推荐 state
  const [recs, setRecs] = useState<Rec[]>([]);
  const [loadingRecs, setLoadingRecs] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [genError, setGenError] = useState('');

  // 自定义食谱 state：mealKey → PlanEntry[]
  const [plan, setPlan] = useState<Record<string, PlanEntry[]>>({
    BREAKFAST: [], LUNCH: [], DINNER: [], SNACK: [],
  });
  const [myFoods, setMyFoods] = useState<FoodItem[]>(() => loadMyFoods());

  useEffect(() => {
    try { localStorage.setItem(MY_FOODS_KEY, JSON.stringify(myFoods)); } catch {}
  }, [myFoods]);

  const addToLibrary      = useCallback((food: FoodItem) => setMyFoods(prev => [...prev, food]), []);
  const removeFromLibrary = useCallback((id: number) => setMyFoods(prev => prev.filter(f => f.id !== id)), []);

  useEffect(() => {
    const auth = getAuth();
    if (auth?.userId) {
      setIsLoggedIn(true);
      setUserId(auth.userId);
      fetchUser(auth.userId).then(d => setNickname(d.data?.nickname || d.nickname || ''));
      setLoadingRecs(true);
      fetchCurrentRecommendations(auth.userId).then(d => {
        const list = unwrapApiData<Rec[]>(d, []);
        setRecs(sortRecommendationsForDisplay(list));
      }).catch(() => {
        setRecs([]);
      }).finally(() => {
        setLoadingRecs(false);
      });
    } else {
      setLoadingRecs(false);
    }
  }, []);

  function handleLogout() { clearAuth(); router.push('/login'); }

  async function handleGenerate() {
    if (!userId) { router.push('/login'); return; }
    setGenerating(true);
    setGenError('');
    try {
      const d = await generateDailyRecommendations({ userId });
      const list = unwrapApiData<Rec[]>(d, []);
      setRecs(sortRecommendationsForDisplay(list));
    } catch {
      setGenError('生成失败，请稍后重试');
    } finally {
      setGenerating(false);
    }
  }

  async function handleAccept(id: number) {
    await acceptRecommendation(id);
    setRecs(prev => prev.map(r => r.id === id ? { ...r, status: 'ACCEPTED' } : r));
  }

  async function handleIgnore(id: number) {
    await ignoreRecommendation(id);
    setRecs(prev => prev.map(r => r.id === id ? { ...r, status: 'IGNORED' } : r));
  }

  function addToMeal(mealKey: string, food: FoodItem, grams: number) {
    setPlan(prev => ({
      ...prev,
      [mealKey]: [...prev[mealKey], { key: `${food.id}-${Date.now()}`, food, grams }],
    }));
  }

  function removeFromMeal(mealKey: string, entryKey: string) {
    setPlan(prev => ({ ...prev, [mealKey]: prev[mealKey].filter(e => e.key !== entryKey) }));
  }

  const dailyTotals = Object.values(plan).flat().reduce((acc, e) => {
    const n = calcNutr(e.food, e.grams);
    return { cal: acc.cal + n.cal, protein: acc.protein + n.protein, fat: acc.fat + n.fat, carb: acc.carb + n.carb };
  }, { cal: 0, protein: 0, fat: 0, carb: 0 });

  // 按餐次分组推荐
  const recByMeal = MEAL_TYPES.map(m => ({
    ...m,
    recs: recs.filter(r => r.mealType === m.key),
  }));
  const hasTodayRecommendations = recs.length > 0;

  return (
    <div className="relative min-h-screen w-screen">
      <div className="fixed inset-0 z-0">
        <BackgroundGradientAnimation interactive />
      </div>

      <div className="relative z-10 flex flex-col min-h-screen">
        <MainNavbar
          isLoggedIn={isLoggedIn}
          nickname={nickname}
          avatarLetter={(nickname || '用')[0].toUpperCase()}
          onLogout={handleLogout}
        />

        <main className="flex-1 px-4 py-6 pt-24 max-w-3xl mx-auto w-full">
          {/* 页头 */}
          <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="mb-6">
            <div className="flex items-center gap-3 mb-1">
              <button onClick={() => router.back()} className="text-white/40 hover:text-white/70 transition">
                <ChevronLeft size={18} />
              </button>
              <h1 className="text-2xl font-bold text-white">推荐食谱</h1>
            </div>
            <p className="text-sm text-white/50 ml-7">根据你的健康目标生成个性化一日食谱，也可自行规划</p>
          </motion.div>

          {/* Tab 切换 */}
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.1 }}
            className="flex gap-2 mb-6 rounded-xl border border-white/10 bg-white/5 p-1"
          >
            {[
              { key: 'recommend', label: '今日推荐', icon: <Sparkles size={14} /> },
              { key: 'custom',    label: '自定义食谱', icon: <UtensilsCrossed size={14} /> },
            ].map(t => (
              <button
                key={t.key}
                onClick={() => setTab(t.key as 'recommend' | 'custom')}
                className={`flex-1 flex items-center justify-center gap-1.5 rounded-lg py-2 text-sm font-medium transition ${
                  tab === t.key
                    ? 'bg-emerald-500 text-white shadow'
                    : 'text-white/50 hover:text-white/80'
                }`}
              >
                {t.icon}{t.label}
              </button>
            ))}
          </motion.div>

          <AnimatePresence mode="wait">
            {/* ── 今日推荐 ── */}
            {tab === 'recommend' && (
              <motion.div key="recommend" initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -10 }} className="space-y-4">
                <PageCard tilt={false} animate={false}>
                  <div className="p-5">
                    <div className="flex items-center justify-between mb-4">
                      <div>
                        <p className="text-sm font-semibold text-white">AI 生成今日食谱</p>
                        <p className="text-xs text-white/45">基于你的健康档案与热量目标自动生成</p>
                      </div>
                      {loadingRecs ? (
                        <div className="flex items-center gap-1.5 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-white/50">
                          <RefreshCw size={14} className="animate-spin" />加载中...
                        </div>
                      ) : hasTodayRecommendations ? (
                        <button
                          onClick={handleGenerate}
                          disabled={generating || !isLoggedIn}
                          className="flex items-center gap-1.5 rounded-xl border border-cyan-500/35 bg-cyan-500/12 px-4 py-2 text-sm font-semibold text-cyan-200 hover:bg-cyan-500/20 disabled:opacity-50 transition"
                        >
                          {generating
                            ? <><RefreshCw size={14} className="animate-spin" />生成中...</>
                            : <><Sparkles size={14} />换一换</>
                          }
                        </button>
                      ) : (
                      <button
                        onClick={handleGenerate}
                        disabled={generating || !isLoggedIn}
                        className="flex items-center gap-1.5 rounded-xl border border-emerald-500/40 bg-emerald-500/15 px-4 py-2 text-sm font-semibold text-emerald-300 hover:bg-emerald-500/25 disabled:opacity-50 transition"
                      >
                        {generating
                          ? <><RefreshCw size={14} className="animate-spin" />生成中...</>
                          : <><Sparkles size={14} />生成食谱</>
                        }
                      </button>
                      )}
                    </div>
                    {!isLoggedIn && (
                      <p className="text-xs text-white/40 text-center py-2">
                        <a href="/login" className="text-emerald-400 hover:underline">登录</a> 后可生成个性化食谱
                      </p>
                    )}
                    {genError && <p className="text-xs text-red-400 mt-1">{genError}</p>}
                  </div>
                </PageCard>

                {loadingRecs && !generating && (
                  <div className="text-center py-12 text-white/30">
                    <RefreshCw size={30} className="mx-auto mb-3 animate-spin opacity-50" />
                    <p className="text-sm">正在加载今日推荐...</p>
                  </div>
                )}

                {recs.length === 0 && !generating && !loadingRecs && (
                  <div className="text-center py-12 text-white/30">
                    <UtensilsCrossed size={32} className="mx-auto mb-3 opacity-40" />
                    <p className="text-sm">暂无推荐，点击"生成食谱"获取今日建议</p>
                  </div>
                )}

                {recs.length > 0 && recByMeal.map(({ key, label, emoji, recs: mealRecs }) => (
                    <div key={key}>
                      <p className="text-xs text-white/40 uppercase tracking-wider mb-2 px-1">
                        {emoji} {label}
                      </p>
                      {mealRecs.length > 0 ? (
                        <div className="space-y-2">
                          {mealRecs.map(rec => (
                            <RecCard
                              key={rec.id}
                              rec={rec}
                              onAccept={() => handleAccept(rec.id)}
                              onIgnore={() => handleIgnore(rec.id)}
                            />
                          ))}
                        </div>
                      ) : (
                        <div className="rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-5 text-sm text-white/35">
                          该餐暂无可推荐菜品
                        </div>
                      )}
                    </div>
                ))}

              </motion.div>
            )}

            {tab === 'custom' && (
              <motion.div key="custom" initial={{ opacity: 0, x: 10 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 10 }} className="space-y-3">
                {/* 我的食物库 */}
                <MyFoodsLibrary myFoods={myFoods} onAdd={addToLibrary} onRemove={removeFromLibrary} />

                {/* 全天小计 */}
                {Object.values(plan).flat().length > 0 && (
                  <PageCard tilt={false} animate={false}>
                    <div className="p-5">
                      <p className="text-xs text-white/50 mb-2 font-medium">全天摄入合计</p>
                      <div className="flex flex-wrap gap-2">
                        <MacroBadge icon={<Flame size={10} />}    label="热量" value={`${dailyTotals.cal.toFixed(0)} kcal`} color="border-orange-500/30 text-orange-300" />
                        <MacroBadge icon={<Beef size={10} />}     label="蛋白" value={`${dailyTotals.protein.toFixed(1)}g`} color="border-blue-500/30 text-blue-300" />
                        <MacroBadge icon={<Wheat size={10} />}    label="碳水" value={`${dailyTotals.carb.toFixed(1)}g`}    color="border-yellow-500/30 text-yellow-300" />
                        <MacroBadge icon={<Droplets size={10} />} label="脂肪" value={`${dailyTotals.fat.toFixed(1)}g`}     color="border-purple-500/30 text-purple-300" />
                      </div>
                    </div>
                  </PageCard>
                )}

                {MEAL_TYPES.map(m => (
                  <MealPlanner
                    key={m.key}
                    mealKey={m.key}
                    label={m.label}
                    emoji={m.emoji}
                    entries={plan[m.key]}
                    onAdd={(food, grams) => addToMeal(m.key, food, grams)}
                    onRemove={key => removeFromMeal(m.key, key)}
                    myFoods={myFoods}
                  />
                ))}
              </motion.div>
            )}
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
