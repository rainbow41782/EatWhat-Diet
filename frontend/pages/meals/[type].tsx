'use client';
import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/router';
import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { getAuth, clearAuth } from '@/lib/auth';
import { fetchUser, fetchFoods, createMealRecord, addFoodIntake as apiAddFoodIntake } from '@/lib/api';
import { Search, Trash2, ChevronLeft, ChevronUp, ChevronDown, Plus, CheckCircle2 } from 'lucide-react';
import { PageCard } from '@/components/ui/page-card';

type FoodItem = {
  id: number;
  name: string;
  category?: string;
  caloriesPer100g?: number;
  proteinPer100g?: number;
  fatPer100g?: number;
  carbPer100g?: number;
};

type SelectedEntry = {
  key: string;
  foodItemId?: number;
  name: string;
  grams?: number;
  calories: number;
  protein: number;
  fat: number;
  carb: number;
  isManual: boolean;
};

const MEAL_LABELS: Record<string, string> = {
  breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐',
};
const MEAL_TYPES: Record<string, string> = {
  breakfast: 'BREAKFAST', lunch: 'LUNCH', dinner: 'DINNER', snack: 'SNACK',
};

function calcNutrition(food: FoodItem, grams: number) {
  const r = grams / 100;
  return {
    calories: Math.round((food.caloriesPer100g ?? 0) * r * 10) / 10,
    protein:  Math.round((food.proteinPer100g  ?? 0) * r * 10) / 10,
    fat:      Math.round((food.fatPer100g      ?? 0) * r * 10) / 10,
    carb:     Math.round((food.carbPer100g     ?? 0) * r * 10) / 10,
  };
}

export default function MealTypePage() {
  const router = useRouter();
  const type = String(router.query.type || 'breakfast');
  const mealLabel = MEAL_LABELS[type] || type;
  const mealType = MEAL_TYPES[type] || 'BREAKFAST';

  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [nickname, setNickname] = useState('');

  // 食物搜索
  const [keyword, setKeyword] = useState('');
  const [searchResults, setSearchResults] = useState<FoodItem[]>([]);
  const [searching, setSearching] = useState(false);
  const [selectedFood, setSelectedFood] = useState<FoodItem | null>(null);
  const [portionGrams, setPortionGrams] = useState('100');

  // 手动填写
  const [mode, setMode] = useState<'search' | 'manual'>('search');
  const [manualName, setManualName] = useState('');
  const [manualProtein, setManualProtein] = useState('');
  const [manualFat, setManualFat] = useState('');
  const [manualCarb, setManualCarb] = useState('');

  const [entries, setEntries] = useState<SelectedEntry[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitDone, setSubmitDone] = useState(false);
  const [error, setError] = useState('');

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const auth = getAuth();
    if (!auth) { router.replace('/login'); return; }
    setIsLoggedIn(true);
    fetchUser(auth.userId).then((res) => {
      const u = res?.data;
      setNickname(u?.nickname || u?.username || '用户');
    }).catch(() => {});
  }, [router]);

  // 防抖搜索
  useEffect(() => {
    if (!keyword.trim()) { setSearchResults([]); return; }
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setSearching(true);
      try {
        const res = await fetchFoods(keyword.trim());
        setSearchResults((res?.data ?? []).slice(0, 10));
      } catch { setSearchResults([]); }
      finally { setSearching(false); }
    }, 300);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [keyword]);

  const handleAddPortioned = () => {
    if (!selectedFood) return;
    const grams = parseFloat(portionGrams);
    if (isNaN(grams) || grams <= 0) return;
    const nutr = calcNutrition(selectedFood, grams);
    setEntries((prev) => [...prev, {
      key: `${selectedFood.id}-${Date.now()}`,
      foodItemId: selectedFood.id, name: selectedFood.name,
      grams, ...nutr, isManual: false,
    }]);
    setSelectedFood(null); setKeyword(''); setSearchResults([]);
  };

  const calcManualCalories = () => {
    const p = parseFloat(manualProtein) || 0;
    const f = parseFloat(manualFat) || 0;
    const c = parseFloat(manualCarb) || 0;
    return Math.round(p * 4 + f * 9 + c * 4);
  };

  const handleAddManual = () => {
    const p = parseFloat(manualProtein) || 0;
    const f = parseFloat(manualFat) || 0;
    const c = parseFloat(manualCarb) || 0;
    if (p === 0 && f === 0 && c === 0) return;
    setEntries((prev) => [...prev, {
      key: `manual-${Date.now()}`,
      name: manualName.trim() || '手动食物',
      calories: calcManualCalories(), protein: p, fat: f, carb: c, isManual: true,
    }]);
    setManualName(''); setManualProtein(''); setManualFat(''); setManualCarb('');
  };

  const removeEntry = (key: string) => setEntries((prev) => prev.filter((e) => e.key !== key));

  const totalCalories = entries.reduce((s, e) => s + e.calories, 0);
  const totalProtein  = entries.reduce((s, e) => s + e.protein, 0);
  const totalFat      = entries.reduce((s, e) => s + e.fat, 0);
  const totalCarb     = entries.reduce((s, e) => s + e.carb, 0);

  const handleSubmit = async () => {
    if (entries.length === 0) return;
    setSubmitting(true); setError('');
    const auth = getAuth();
    if (!auth) { router.replace('/login'); return; }
    try {
      const mealRes = await createMealRecord(auth.userId, {
        mealType,
        mealTime: (() => {
          const n = new Date();
          return `${n.getFullYear()}-${String(n.getMonth()+1).padStart(2,'0')}-${String(n.getDate()).padStart(2,'0')}T${String(n.getHours()).padStart(2,'0')}:${String(n.getMinutes()).padStart(2,'0')}:${String(n.getSeconds()).padStart(2,'0')}`;
        })(),
      });
      const meal = mealRes?.data;
      if (!meal?.id) throw new Error('创建餐食记录失败');

      for (const entry of entries) {
        if (!entry.isManual && entry.foodItemId) {
          await apiAddFoodIntake(auth.userId, {
            mealRecordId: meal.id,
            foodItemId: entry.foodItemId,
            quantity: entry.grams ?? 100,
            unit: '克',
          });
        } else if (entry.isManual) {
          // 手动录入：直接传营养值，不带 foodItemId
          await apiAddFoodIntake(auth.userId, {
            mealRecordId: meal.id,
            name: entry.name,
            calories: entry.calories,
            protein: entry.protein,
            fat: entry.fat,
            carb: entry.carb,
            unit: '份',
          });
        }
      }
      setSubmitDone(true);
      setTimeout(() => router.push('/'), 1500);
    } catch (e: unknown) {
      setError((e as Error)?.message || '提交失败，请重试');
    } finally { setSubmitting(false); }
  };

  const handleLogout = () => { clearAuth(); setIsLoggedIn(false); router.push('/login'); };
  const avatarLetter = (nickname.charAt(0) || 'U').toUpperCase();
  const inputCls = 'w-full h-9 rounded-xl border border-white/10 bg-white/5 px-3 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 transition-all';

  return (
    <div className="relative min-h-screen w-screen">
      <div className="fixed inset-0 z-0">
        <BackgroundGradientAnimation interactive />
      </div>
      <div className="relative z-10">
        <MainNavbar isLoggedIn={isLoggedIn} nickname={nickname} avatarLetter={avatarLetter} onLogout={handleLogout} />

        <div className="pt-20 pb-12 flex justify-center px-4">
          <div className="w-full max-w-xl space-y-4">

            {/* 标题 */}
            <div className="flex items-center gap-3 pt-4">
              <button onClick={() => router.back()}
                className="w-9 h-9 rounded-xl border border-white/10 bg-black/30 flex items-center justify-center text-white/60 hover:text-white hover:bg-white/10 transition">
                <ChevronLeft size={18} />
              </button>
              <h1 className="text-xl font-bold text-white">{mealLabel}记录</h1>
            </div>

            {/* 模式切换 */}
            <div className="flex rounded-xl border border-white/10 bg-black/30 p-1 gap-1">
              {(['search', 'manual'] as const).map((m) => (
                <button key={m} onClick={() => setMode(m)}
                  className={`flex-1 h-9 rounded-lg text-sm font-medium transition-all ${
                    mode === m
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                      : 'text-white/50 hover:text-white/70'
                  }`}>
                  {m === 'search' ? '搜索食物数据库' : '手动填写营养'}
                </button>
              ))}
            </div>

            {/* 搜索模式 */}
            {mode === 'search' && (
              <PageCard className="p-4">
                <div className="space-y-3">
                <div className="relative">
                  <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/30" />
                  <input type="text" placeholder="输入食物名称搜索..." value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    className="w-full h-9 rounded-xl border border-white/10 bg-white/5 pl-9 pr-3 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 transition-all" />
                </div>

                {searching && <p className="text-xs text-white/40 text-center py-2">搜索中...</p>}

                {searchResults.length > 0 && !selectedFood && (
                  <ul className="space-y-1 max-h-48 overflow-y-auto pr-1">
                    {searchResults.map((food) => (
                      <li key={food.id}>
                        <button onClick={() => { setSelectedFood(food); setPortionGrams('100'); }}
                          className="w-full text-left rounded-xl border border-white/5 bg-white/5 hover:bg-white/10 px-3 py-2 transition">
                          <div className="flex items-center justify-between">
                            <span className="text-sm text-white">{food.name}</span>
                            <span className="text-xs text-white/40">{food.caloriesPer100g ?? '?'} kcal/100g</span>
                          </div>
                          {food.category && <span className="text-[10px] text-white/30">{food.category}</span>}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}

                {selectedFood && (
                  <div className="rounded-xl border border-emerald-400/30 bg-emerald-500/10 p-3 space-y-2">
                    <p className="text-sm font-semibold text-white">{selectedFood.name}</p>
                    <div className="flex items-center gap-2">
                      <label className="text-xs text-white/60 whitespace-nowrap">重量 (g)</label>
                      <div className="relative flex-1">
                        <input type="text" inputMode="decimal" value={portionGrams}
                          onChange={(e) => setPortionGrams(e.target.value)}
                          className="w-full h-9 rounded-xl border border-white/10 bg-white/5 pl-3 pr-10 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 transition-all" />
                        <div className="absolute right-1 top-1/2 -translate-y-1/2 flex flex-col h-8 justify-between">
                          <button type="button"
                            onClick={() => setPortionGrams((v) => String(Math.max(1, (parseFloat(v) || 100) + 10)))}
                            className="flex items-center justify-center w-8 h-4 hover:bg-white/10 rounded transition-colors">
                            <ChevronUp size={10} className="text-white/50" />
                          </button>
                          <button type="button"
                            onClick={() => setPortionGrams((v) => String(Math.max(1, (parseFloat(v) || 100) - 10)))}
                            className="flex items-center justify-center w-8 h-4 hover:bg-white/10 rounded transition-colors">
                            <ChevronDown size={10} className="text-white/50" />
                          </button>
                        </div>
                      </div>
                    </div>
                    {(() => {
                      const g = parseFloat(portionGrams);
                      if (isNaN(g) || g <= 0) return null;
                      const n = calcNutrition(selectedFood, g);
                      return (
                        <div className="flex gap-3 text-xs text-white/50 flex-wrap">
                          <span>热量 <span className="text-white/80">{n.calories}</span> kcal</span>
                          <span>蛋白 <span className="text-orange-300">{n.protein}g</span></span>
                          <span>碳水 <span className="text-blue-300">{n.carb}g</span></span>
                          <span>脂肪 <span className="text-yellow-300">{n.fat}g</span></span>
                        </div>
                      );
                    })()}
                    <div className="flex gap-2">
                      <button onClick={handleAddPortioned}
                        className="flex-1 h-9 rounded-xl bg-emerald-500 text-sm font-medium text-white hover:bg-emerald-400 transition">
                        加入餐食
                      </button>
                      <button onClick={() => setSelectedFood(null)}
                        className="h-9 px-3 rounded-xl border border-white/10 text-sm text-white/50 hover:bg-white/5 transition">
                        取消
                      </button>
                    </div>
                  </div>
                )}

                {keyword.trim() && !searching && searchResults.length === 0 && !selectedFood && (
                  <p className="text-xs text-white/40 text-center py-3">未找到相关食物，可切换到手动填写模式</p>
                )}
                </div>
              </PageCard>
            )}

            {/* 手动填写模式 */}
            {mode === 'manual' && (
              <PageCard className="p-4">
                <div className="space-y-3">
                <div>
                  <label className="text-xs text-white/60 block mb-1">食物名称（可选）</label>
                  <input type="text" placeholder="如：家常炒菜" value={manualName}
                    onChange={(e) => setManualName(e.target.value)} className={inputCls} />
                </div>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { label: '蛋白质 (g)', value: manualProtein, setter: setManualProtein },
                    { label: '脂肪 (g)',   value: manualFat,     setter: setManualFat     },
                    { label: '碳水 (g)',   value: manualCarb,    setter: setManualCarb    },
                  ].map(({ label, value, setter }) => (
                    <div key={label}>
                      <label className="text-xs text-white/60 block mb-1">{label}</label>
                      <div className="relative">
                        <input type="text" inputMode="decimal" placeholder="0" value={value}
                          onChange={(e) => setter(e.target.value)}
                          className="w-full h-9 rounded-xl border border-white/10 bg-white/5 pl-3 pr-9 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 transition-all" />
                        <div className="absolute right-1 top-1/2 -translate-y-1/2 flex flex-col h-8 justify-between">
                          <button type="button"
                            onClick={() => setter((v) => String(Math.max(0, (parseFloat(v) || 0) + 1)))}
                            className="flex items-center justify-center w-7 h-4 hover:bg-white/10 rounded transition-colors">
                            <ChevronUp size={10} className="text-white/50" />
                          </button>
                          <button type="button"
                            onClick={() => setter((v) => String(Math.max(0, (parseFloat(v) || 0) - 1)))}
                            className="flex items-center justify-center w-7 h-4 hover:bg-white/10 rounded transition-colors">
                            <ChevronDown size={10} className="text-white/50" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                <p className="text-xs text-white/40">
                  预估热量：<span className="text-white/70 font-medium">{calcManualCalories()} kcal</span>
                </p>
                <button onClick={handleAddManual}
                  className="w-full h-9 rounded-xl bg-emerald-500/20 border border-emerald-500/30 text-sm text-emerald-300 hover:bg-emerald-500/30 transition flex items-center justify-center gap-2">
                  <Plus size={14} /> 加入餐食
                </button>
                </div>
              </PageCard>
            )}

            {/* 已添加食物列表 */}
            {entries.length > 0 && (
              <PageCard className="p-4">
                <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-white/80">已添加食物</h2>
                  <span className="text-xs text-white/40">{entries.length} 项</span>
                </div>
                <ul className="space-y-2">
                  {entries.map((entry) => (
                    <li key={entry.key} className="flex items-start justify-between rounded-xl border border-white/5 bg-white/5 px-3 py-2 gap-3">
                      <div className="min-w-0">
                        <p className="text-sm text-white truncate">{entry.name}</p>
                        <div className="flex flex-wrap gap-x-3 text-[11px] text-white/40 mt-0.5">
                          <span className="text-white/60 font-medium">{entry.calories} kcal</span>
                          {entry.grams && <span>{entry.grams}g</span>}
                          <span>蛋白 {entry.protein}g</span>
                          <span>碳水 {entry.carb}g</span>
                          <span>脂肪 {entry.fat}g</span>
                        </div>
                      </div>
                      <button onClick={() => removeEntry(entry.key)} className="text-white/30 hover:text-red-400 transition mt-0.5 shrink-0">
                        <Trash2 size={14} />
                      </button>
                    </li>
                  ))}
                </ul>
                {/* 合计 */}
                <div className="rounded-xl border border-emerald-400/20 bg-emerald-500/5 px-3 py-2">
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-white/60">本餐合计</span>
                    <span className="text-sm font-bold text-white">{Math.round(totalCalories)} kcal</span>
                  </div>
                  <div className="flex gap-4 mt-1 text-[11px]">
                    <span className="text-orange-300">蛋白 {Math.round(totalProtein)}g</span>
                    <span className="text-blue-300">碳水 {Math.round(totalCarb)}g</span>
                    <span className="text-yellow-300">脂肪 {Math.round(totalFat)}g</span>
                  </div>
                </div>
                </div>
              </PageCard>
            )}

            {error && (
              <div className="rounded-xl border border-red-400/20 bg-red-500/10 px-4 py-2 text-sm text-red-200">{error}</div>
            )}

            {/* 提交按钮 */}
            <button onClick={handleSubmit}
              disabled={entries.length === 0 || submitting || submitDone}
              className="w-full h-12 rounded-2xl bg-emerald-500 text-white font-semibold text-sm hover:bg-emerald-400 transition active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2">
              {submitDone ? <><CheckCircle2 size={16} /> 保存成功，正在跳转...</>
               : submitting ? '保存中...'
               : `保存${mealLabel}记录`}
            </button>
            {entries.length === 0 && (
              <p className="text-center text-xs text-white/30 pb-4">请先添加至少一项食物</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

