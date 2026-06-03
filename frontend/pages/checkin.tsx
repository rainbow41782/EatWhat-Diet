'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { ChevronLeft, ChevronUp, ChevronDown, Edit2, Trash2, Plus, X, Check } from 'lucide-react';
import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { getAuth, clearAuth } from '@/lib/auth';
import { PageCard } from '@/components/ui/page-card';
import {
  fetchUser, fetchRecentMeals, fetchMealIntakes,
  deleteFoodIntake, deleteMealRecord, addFoodIntake, updateFoodIntake,
} from '@/lib/api';

type MealRecord = {
  id?: number;
  mealType?: string;
  mealTime?: string;
  createdAt?: string;
  totalCalories?: number;
  totalProtein?: number;
  totalFat?: number;
  totalCarb?: number;
};

type FoodIntake = {
  id: number;
  remark?: string;
  calories?: number;
  protein?: number;
  fat?: number;
  carb?: number;
};

const MEAL_LABELS: Record<string, string> = {
  BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐',
};

export default function CheckInPage() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [nickname, setNickname] = useState('');
  const [meals, setMeals] = useState<MealRecord[]>([]);
  const [loading, setLoading] = useState(true);

  // 编辑弹窗状态
  const [editMeal, setEditMeal] = useState<MealRecord | null>(null);
  const [editIntakes, setEditIntakes] = useState<FoodIntake[]>([]);
  const [editLoading, setEditLoading] = useState(false);
  const [addName, setAddName] = useState('');
  const [addProtein, setAddProtein] = useState('');
  const [addFat, setAddFat] = useState('');
  const [addCarb, setAddCarb] = useState('');

  // 行内编辑已有食物条目
  const [editingIntake, setEditingIntake] = useState<{
    id: number; remark: string; protein: string; fat: string; carb: string;
  } | null>(null);

  const handleLogout = () => { clearAuth(); setIsLoggedIn(false); router.push('/login'); };
  const avatarLetter = (nickname.charAt(0) || 'U').toUpperCase();

  const loadMeals = (userId: number) => {
    fetchRecentMeals(userId, 30).then((res) => {
      setMeals(res?.data ?? []);
    }).catch(() => {});
  };

  useEffect(() => {
    const auth = getAuth();
    if (!auth) { router.replace('/login'); return; }
    setIsLoggedIn(true);
    Promise.all([
      fetchUser(auth.userId).catch(() => null),
      fetchRecentMeals(auth.userId, 30).catch(() => null),
    ]).then(([userRes, mealsRes]) => {
      setNickname(userRes?.data?.nickname || userRes?.data?.username || '用户');
      setMeals(mealsRes?.data ?? []);
    }).finally(() => setLoading(false));
  }, [router]);

  const openEdit = async (meal: MealRecord) => {
    const auth = getAuth();
    if (!auth || !meal.id) return;
    setEditMeal(meal);
    setEditIntakes([]);
    setEditLoading(true);
    try {
      const res = await fetchMealIntakes(auth.userId, meal.id);
      setEditIntakes(res?.data ?? []);
    } catch { setEditIntakes([]); }
    finally { setEditLoading(false); }
  };

  const closeEdit = () => {
    setEditMeal(null);
    setEditIntakes([]);
    setEditingIntake(null);
    setAddName(''); setAddProtein(''); setAddFat(''); setAddCarb('');
    const auth = getAuth();
    if (auth) loadMeals(auth.userId);
  };

  const handleDeleteIntake = async (intakeId: number) => {
    const auth = getAuth();
    if (!auth) return;
    await deleteFoodIntake(auth.userId, intakeId);
    setEditIntakes((prev) => prev.filter((i) => i.id !== intakeId));
  };

  const handleSaveIntake = async () => {
    if (!editingIntake) return;
    const auth = getAuth();
    if (!auth) return;
    const p = parseFloat(editingIntake.protein) || 0;
    const f = parseFloat(editingIntake.fat) || 0;
    const c = parseFloat(editingIntake.carb) || 0;
    const calories = Math.round(p * 4 + f * 9 + c * 4);
    const res = await updateFoodIntake(auth.userId, editingIntake.id, {
      remark: editingIntake.remark.trim() || '手动食物',
      calories, protein: p, fat: f, carb: c,
    });
    if (res?.data) {
      setEditIntakes((prev) => prev.map((i) => i.id === editingIntake.id ? res.data as FoodIntake : i));
    }
    setEditingIntake(null);
  };

  const handleDeleteMeal = async () => {
    const auth = getAuth();
    if (!auth || !editMeal?.id) return;
    if (!window.confirm('确定删除这整条餐食记录吗？')) return;
    await deleteMealRecord(auth.userId, editMeal.id);
    closeEdit();
  };

  const handleAddItem = async () => {
    const p = parseFloat(addProtein) || 0;
    const f = parseFloat(addFat) || 0;
    const c = parseFloat(addCarb) || 0;
    if (p === 0 && f === 0 && c === 0) return;
    const auth = getAuth();
    if (!auth || !editMeal?.id) return;
    const calories = Math.round(p * 4 + f * 9 + c * 4);
    const res = await addFoodIntake(auth.userId, {
      mealRecordId: editMeal.id,
      name: addName.trim() || '手动食物',
      calories, protein: p, fat: f, carb: c, unit: '份',
    });
    if (res?.data) setEditIntakes((prev) => [...prev, res.data as FoodIntake]);
    setAddName(''); setAddProtein(''); setAddFat(''); setAddCarb('');
  };

  // 按日期分组（优先用 createdAt，再用 mealTime）
  const grouped = meals.reduce<Record<string, MealRecord[]>>((acc, m) => {
    const dateStr = m.createdAt ?? m.mealTime ?? '';
    const date = dateStr ? dateStr.split('T')[0] : '未知日期';
    if (!acc[date]) acc[date] = [];
    acc[date].push(m);
    return acc;
  }, {});
  const dates = Object.keys(grouped).sort((a, b) => b.localeCompare(a));

  const inputCls = 'w-full h-8 rounded-lg border border-white/10 bg-white/5 px-2 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 transition-all';

  return (
    <div className="relative min-h-screen w-screen">
      {/* 固定背景 */}
      <div className="fixed inset-0 z-0">
        <BackgroundGradientAnimation interactive />
      </div>

      {/* 可滚动内容层 */}
      <div className="relative z-10">
        <MainNavbar isLoggedIn={isLoggedIn} nickname={nickname} avatarLetter={avatarLetter} onLogout={handleLogout} />
        <div className="pt-20 pb-12 flex justify-center px-4">
          <div className="w-full max-w-xl space-y-4">
            <div className="flex items-center gap-3 pt-4">
              <button onClick={() => router.back()}
                className="w-9 h-9 rounded-xl border border-white/10 bg-black/30 flex items-center justify-center text-white/60 hover:text-white hover:bg-white/10 transition">
                <ChevronLeft size={18} />
              </button>
              <h1 className="text-xl font-bold text-white">打卡历史记录</h1>
            </div>

            {loading && <p className="text-white/40 text-sm text-center py-8">加载中...</p>}

            {!loading && dates.length === 0 && (
              <PageCard tilt={false} className="p-6 text-center text-white/50 text-sm">
                暂无打卡记录，前往
                <a href="/meals/breakfast" className="text-emerald-300 hover:text-emerald-200 mx-1">记录膳食</a>
                开始打卡吧！
              </PageCard>
            )}

            {dates.map((date) => (
              <PageCard key={date} tilt={false} className="overflow-hidden">
                <div className="px-4 py-3 border-b border-white/[0.06] bg-white/[0.03]">
                  <span className="text-sm font-semibold text-white">{date}</span>
                </div>
                <div className="divide-y divide-white/5">
                  {grouped[date].map((meal) => (
                    <div key={meal.id ?? meal.mealTime} className="flex items-center justify-between px-4 py-3 gap-3">
                      <div className="flex-1 min-w-0">
                        <span className="text-sm text-white font-medium">
                          {MEAL_LABELS[meal.mealType ?? ''] ?? meal.mealType ?? '-'}
                        </span>
                        <div className="text-xs text-white/40 mt-0.5">
                          蛋白 {meal.totalProtein?.toFixed(1) ?? 0}g &nbsp;|&nbsp;
                          脂肪 {meal.totalFat?.toFixed(1) ?? 0}g &nbsp;|&nbsp;
                          碳水 {meal.totalCarb?.toFixed(1) ?? 0}g
                        </div>
                      </div>
                      <span className="text-sm font-semibold text-emerald-300 whitespace-nowrap">
                        {meal.totalCalories != null ? Math.round(meal.totalCalories) : 0} kcal
                      </span>
                      <button onClick={() => openEdit(meal)}
                        className="w-8 h-8 rounded-lg border border-white/10 bg-white/5 flex items-center justify-center text-white/50 hover:text-white hover:bg-white/10 transition">
                        <Edit2 size={14} />
                      </button>
                    </div>
                  ))}
                </div>
              </PageCard>
            ))}
          </div>
        </div>
      </div>

      {/* 编辑弹窗 */}
      {editMeal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-white/10 bg-[#0a0f1a] shadow-2xl overflow-hidden">
            {/* 弹窗标题栏 */}
            <div className="flex items-center justify-between px-5 py-4 border-b border-white/10">
              <div>
                <h2 className="text-base font-semibold text-white">
                  编辑{MEAL_LABELS[editMeal.mealType ?? ''] ?? editMeal.mealType}
                </h2>
                <p className="text-xs text-white/40 mt-0.5">
                  {(editMeal.createdAt ?? editMeal.mealTime ?? '').split('T')[0]}
                </p>
              </div>
              <button onClick={closeEdit}
                className="w-8 h-8 rounded-lg border border-white/10 flex items-center justify-center text-white/50 hover:text-white hover:bg-white/10 transition">
                <X size={16} />
              </button>
            </div>

            <div className="p-5 space-y-4 max-h-[65vh] overflow-y-auto">
              {/* 食物条目列表 */}
              {editLoading && <p className="text-white/40 text-sm text-center py-4">加载中...</p>}

              {!editLoading && editIntakes.length === 0 && (
                <p className="text-white/40 text-sm text-center py-2">暂无食物记录</p>
              )}

              {editIntakes.length > 0 && (
                <div className="space-y-2">
                  <p className="text-xs text-white/50 font-medium">已记录食物</p>
                  {editIntakes.map((intake) => (
                <div key={intake.id} className="rounded-xl border border-white/5 bg-white/5 overflow-hidden">
                  {editingIntake?.id === intake.id ? (
                    /* 编辑模式 */
                    <div className="px-3 py-2 space-y-2">
                      <input
                        type="text"
                        placeholder="食物名称"
                        value={editingIntake.remark}
                        onChange={(e) => setEditingIntake((prev) => prev && ({ ...prev, remark: e.target.value }))}
                        className={inputCls}
                      />
                      <div className="grid grid-cols-3 gap-2">
                        {([
                          { label: '蛋白质 (g)', key: 'protein' as const },
                          { label: '脂肪 (g)',   key: 'fat'     as const },
                          { label: '碳水 (g)',   key: 'carb'    as const },
                        ] as const).map(({ label, key }) => (
                          <div key={key}>
                            <label className="text-[10px] text-white/40 block mb-1">{label}</label>
                            <div className="relative">
                              <input
                                type="text" inputMode="decimal" placeholder="0"
                                value={editingIntake[key]}
                                onChange={(e) => setEditingIntake((prev) => prev && ({ ...prev, [key]: e.target.value }))}
                                className="w-full h-8 rounded-lg border border-white/10 bg-white/5 pl-2 pr-8 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 transition-all"
                              />
                              <div className="absolute right-0.5 top-1/2 -translate-y-1/2 flex flex-col h-7 justify-between">
                                <button type="button"
                                  onClick={() => setEditingIntake((prev) => prev && ({ ...prev, [key]: String(Math.max(0, (parseFloat(prev[key]) || 0) + 1)) }))}
                                  className="flex items-center justify-center w-7 h-3.5 hover:bg-white/10 rounded transition-colors">
                                  <ChevronUp size={9} className="text-white/40" />
                                </button>
                                <button type="button"
                                  onClick={() => setEditingIntake((prev) => prev && ({ ...prev, [key]: String(Math.max(0, (parseFloat(prev[key]) || 0) - 1)) }))}
                                  className="flex items-center justify-center w-7 h-3.5 hover:bg-white/10 rounded transition-colors">
                                  <ChevronDown size={9} className="text-white/40" />
                                </button>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                      <div className="flex gap-2">
                        <button onClick={() => setEditingIntake(null)}
                          className="flex-1 h-7 rounded-lg border border-white/10 text-xs text-white/50 hover:text-white hover:bg-white/10 transition">
                          取消
                        </button>
                        <button onClick={handleSaveIntake}
                          className="flex-1 h-7 rounded-lg bg-emerald-500/20 border border-emerald-500/30 text-xs text-emerald-300 hover:bg-emerald-500/30 transition flex items-center justify-center gap-1">
                          <Check size={12} /> 保存
                        </button>
                      </div>
                    </div>
                  ) : (
                    /* 显示模式 */
                    <div className="flex items-center justify-between px-3 py-2 gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-white truncate">{intake.remark || '未命名食物'}</p>
                        <div className="text-xs text-white/40 mt-0.5">
                          {Math.round(intake.calories ?? 0)} kcal &nbsp;·&nbsp;
                          蛋白 {(intake.protein ?? 0).toFixed(1)}g &nbsp;·&nbsp;
                          脂肪 {(intake.fat ?? 0).toFixed(1)}g &nbsp;·&nbsp;
                          碳水 {(intake.carb ?? 0).toFixed(1)}g
                        </div>
                      </div>
                      <button
                        onClick={() => setEditingIntake({
                          id: intake.id,
                          remark: intake.remark || '',
                          protein: String(intake.protein ?? ''),
                          fat: String(intake.fat ?? ''),
                          carb: String(intake.carb ?? ''),
                        })}
                        className="w-7 h-7 rounded-lg flex items-center justify-center text-white/30 hover:text-emerald-300 hover:bg-emerald-500/10 transition">
                        <Edit2 size={13} />
                      </button>
                      <button onClick={() => handleDeleteIntake(intake.id)}
                        className="w-7 h-7 rounded-lg flex items-center justify-center text-red-400/60 hover:text-red-400 hover:bg-red-400/10 transition">
                        <Trash2 size={13} />
                      </button>
                    </div>
                  )}
                </div>
              ))}
                </div>
              )}

              {/* 添加新食物 */}
              <div className="rounded-xl border border-white/10 bg-white/5 p-3 space-y-3">
                <p className="text-xs text-white/50 font-medium">添加食物</p>
                <div>
                  <label className="text-xs text-white/40 block mb-1">食物名称（可选）</label>
                  <input type="text" placeholder="如：米饭" value={addName}
                    onChange={(e) => setAddName(e.target.value)} className={inputCls} />
                </div>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { label: '蛋白质 (g)', value: addProtein, setter: setAddProtein },
                    { label: '脂肪 (g)',   value: addFat,     setter: setAddFat     },
                    { label: '碳水 (g)',   value: addCarb,    setter: setAddCarb    },
                  ].map(({ label, value, setter }) => (
                    <div key={label}>
                      <label className="text-xs text-white/40 block mb-1">{label}</label>
                      <div className="relative">
                        <input type="text" inputMode="decimal" placeholder="0" value={value}
                          onChange={(e) => setter(e.target.value)}
                          className="w-full h-8 rounded-lg border border-white/10 bg-white/5 pl-2 pr-8 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 transition-all" />
                        <div className="absolute right-0.5 top-1/2 -translate-y-1/2 flex flex-col h-7 justify-between">
                          <button type="button"
                            onClick={() => setter((v) => String(Math.max(0, (parseFloat(v) || 0) + 1)))}
                            className="flex items-center justify-center w-7 h-3.5 hover:bg-white/10 rounded transition-colors">
                            <ChevronUp size={9} className="text-white/40" />
                          </button>
                          <button type="button"
                            onClick={() => setter((v) => String(Math.max(0, (parseFloat(v) || 0) - 1)))}
                            className="flex items-center justify-center w-7 h-3.5 hover:bg-white/10 rounded transition-colors">
                            <ChevronDown size={9} className="text-white/40" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                <button onClick={handleAddItem}
                  className="w-full h-8 rounded-lg bg-emerald-500/20 border border-emerald-500/30 text-sm text-emerald-300 hover:bg-emerald-500/30 transition flex items-center justify-center gap-1.5">
                  <Plus size={14} /> 加入
                </button>
              </div>
            </div>

            {/* 底部操作栏 */}
            <div className="flex gap-3 px-5 py-4 border-t border-white/10">
              <button onClick={handleDeleteMeal}
                className="flex items-center gap-1.5 px-3 h-9 rounded-xl border border-red-500/30 text-sm text-red-400 hover:bg-red-500/10 transition">
                <Trash2 size={14} /> 删除整条记录
              </button>
              <button onClick={closeEdit}
                className="flex-1 h-9 rounded-xl bg-emerald-500/20 border border-emerald-500/30 text-sm text-emerald-300 hover:bg-emerald-500/30 transition">
                完成
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

