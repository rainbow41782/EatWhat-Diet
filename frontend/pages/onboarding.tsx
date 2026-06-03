'use client';
import { useState, useEffect } from 'react';
import { motion, AnimatePresence, useMotionValue, useTransform } from 'framer-motion';
import { useRouter } from 'next/router';
import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { getAuth } from '@/lib/auth';
import { updateUserBasic, updateUserProfile, fetchUser } from '@/lib/api';

type GoalKey = 'rapid_fat_loss' | 'high_intensity_fat_loss' | 'daily_fat_loss' | 'lean_bulk' | 'bulk' | 'maintain' | 'increase_strength' | 'improve_performance';
type GoalConfig = { label: string; desc: string; calorieFactor: number; proteinMode: 'lbm' | 'weight'; proteinFactor: number; fatFactor: number; durationMin: number; durationMax: number; };

const GOAL_CONFIGS: Record<GoalKey, GoalConfig> = {
  rapid_fat_loss:          { label: '极速减脂', desc: '最高强度缺口，短期冲刺（≤4 周）',    calorieFactor: 0.80, proteinMode: 'lbm',    proteinFactor: 2.6, fatFactor: 0.6,  durationMin: 1,  durationMax: 4  },
  high_intensity_fat_loss: { label: '强力减脂', desc: '稳健高效的热量缺口（8-16 周）',      calorieFactor: 0.85, proteinMode: 'lbm',    proteinFactor: 2.4, fatFactor: 0.65, durationMin: 8,  durationMax: 16 },
  daily_fat_loss:          { label: '日常减脂', desc: '温和缺口，适合长期坚持（15-20 周）', calorieFactor: 0.90, proteinMode: 'lbm',    proteinFactor: 2.2, fatFactor: 0.7,  durationMin: 15, durationMax: 20 },
  lean_bulk:               { label: '精益增肌', desc: '最小化体脂增加的增肌方案',            calorieFactor: 1.05, proteinMode: 'weight', proteinFactor: 2.0, fatFactor: 0.8,  durationMin: 8,  durationMax: 12 },
  bulk:                    { label: '增重增肌', desc: '较大盈余，加速肌肉增长',              calorieFactor: 1.12, proteinMode: 'weight', proteinFactor: 1.9, fatFactor: 0.9,  durationMin: 8,  durationMax: 12 },
  maintain:                { label: '维持体重', desc: '保持现有身体成分',                    calorieFactor: 1.00, proteinMode: 'weight', proteinFactor: 1.8, fatFactor: 0.8,  durationMin: 1,  durationMax: 52 },
  increase_strength:       { label: '提升力量', desc: '支持力量训练周期（6-8 周）',          calorieFactor: 1.03, proteinMode: 'weight', proteinFactor: 1.8, fatFactor: 0.7,  durationMin: 6,  durationMax: 8  },
  improve_performance:     { label: '提升表现', desc: '高碳水优化运动表现（8-16 周）',       calorieFactor: 1.05, proteinMode: 'weight', proteinFactor: 1.7, fatFactor: 0.65, durationMin: 8,  durationMax: 16 },
};

const ACTIVITY_LEVELS = [
  { value: 'SEDENTARY', label: '久坐', desc: '几乎不运动，办公室工作' },
  { value: 'LIGHTLY_ACTIVE', label: '轻度活跃', desc: '每周轻度运动 1-3 天' },
  { value: 'MODERATELY_ACTIVE', label: '中度活跃', desc: '每周中度运动 3-5 天' },
  { value: 'VERY_ACTIVE', label: '高度活跃', desc: '每周高强度运动 6-7 天' },
  { value: 'EXTRA_ACTIVE', label: '极度活跃', desc: '高强度运动或体力劳动工作' },
];

type ProfileForm = {
  gender?: 'MALE' | 'FEMALE';
  age?: number;
  heightCm?: number;
  weightKg?: number;
  healthGoal?: string;
  activityLevel?: string;
  dietPreference?: string;
  allergies?: string;
  dislikedFoods?: string;
  dailyCalorieTarget?: number;
  dailyProteinTarget?: number;
  dailyFatTarget?: number;
  dailyCarbTarget?: number;
};

const ACTIVITY_MULTIPLIERS: Record<string, number> = {
  SEDENTARY: 1.2,
  LIGHTLY_ACTIVE: 1.375,
  MODERATELY_ACTIVE: 1.55,
  VERY_ACTIVE: 1.725,
  EXTRA_ACTIVE: 1.9,
};

type NutritionResult = {
  bmr: number; tdee: number; lbm: number; bfPct: number;
  targetCalories: number; calorieDelta: number;
  protein: number; fat: number; carb: number;
  macroRatio: { protein: number; fat: number; carb: number };
  durationMin: number; durationMax: number; goalLabel: string;
};

function computeTargets(form: ProfileForm): NutritionResult | null {
  const { heightCm, weightKg, gender, age, activityLevel, healthGoal } = form;
  if (!heightCm || !weightKg || !gender || !age || !healthGoal) return null;
  const config = GOAL_CONFIGS[healthGoal as GoalKey];
  if (!config) return null;

  // BMI → Deurenberg BF% (Asian +5%) → LBM → Katch-McArdle BMR
  const heightM = heightCm / 100;
  const bmi = weightKg / (heightM * heightM);
  const sex = gender === 'MALE' ? 1 : 0;
  let bfPct = 1.20 * bmi + 0.23 * age - 10.8 * sex - 5.4 + 5;
  bfPct = Math.max(5, Math.min(50, bfPct));
  const lbm = weightKg * (1 - bfPct / 100);
  const bmr = 370 + 21.6 * lbm;

  // TDEE
  const tdee = bmr * (ACTIVITY_MULTIPLIERS[activityLevel ?? ''] ?? 1.2);

  // Calorie target with floor: max(bmr * 1.2, 1200)
  const targetCalories = Math.round(Math.max(tdee * config.calorieFactor, bmr * 1.2, 1200));

  // Protein: lbm mode for fat-loss goals, total weight for others
  const proteinBase = config.proteinMode === 'lbm' ? lbm : weightKg;
  const protein = Math.round(config.proteinFactor * proteinBase);

  // Fat with floor: weightKg * 0.5
  const fat = Math.round(Math.max(config.fatFactor * weightKg, weightKg * 0.5));

  // Carbs (floor 0)
  const carb = Math.max(0, Math.round((targetCalories - protein * 4 - fat * 9) / 4));

  // Macro ratio %
  const totalKcal = protein * 4 + fat * 9 + carb * 4;
  const macroRatio = {
    protein: Math.round((protein * 4 / totalKcal) * 100),
    fat:     Math.round((fat     * 9 / totalKcal) * 100),
    carb:    Math.round((carb    * 4 / totalKcal) * 100),
  };

  return {
    bmr: Math.round(bmr), tdee: Math.round(tdee),
    lbm: Math.round(lbm * 10) / 10, bfPct: Math.round(bfPct),
    targetCalories, calorieDelta: Math.round(targetCalories - tdee),
    protein, fat, carb, macroRatio,
    durationMin: config.durationMin, durationMax: config.durationMax,
    goalLabel: config.label,
  };
}

function StepIndicator({ current, total }: { current: number; total: number }) {
  return (
    <div className="flex items-center gap-2 justify-center">
      {Array.from({ length: total }).map((_, i) => (
        <div
          key={i}
          className={`h-1.5 rounded-full transition-all duration-300 ${i < current ? 'w-8 bg-emerald-400' : i === current ? 'w-8 bg-emerald-400/60' : 'w-4 bg-white/20'}`}
        />
      ))}
    </div>
  );
}

export default function OnboardingPage() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [form, setForm] = useState<ProfileForm>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  // 卡片倾斜动画 — 与登录/注册页保持一致
  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);
  const rotateX = useTransform(mouseY, [-300, 300], [10, -10]);
  const rotateY = useTransform(mouseX, [-300, 300], [-10, 10]);
  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = e.currentTarget.getBoundingClientRect();
    mouseX.set(e.clientX - rect.left - rect.width / 2);
    mouseY.set(e.clientY - rect.top - rect.height / 2);
  };
  const handleMouseLeave = () => { mouseX.set(0); mouseY.set(0); };

  // 数字字段步进
  const changeNum = (field: keyof ProfileForm, delta: number, min = 0, max = 9999) => {
    setForm((prev) => {
      const current = (prev[field] as number | undefined) ?? 0;
      const next = parseFloat(Math.max(min, Math.min(max, current + delta)).toFixed(1));
      return { ...prev, [field]: next };
    });
  };

  useEffect(() => {
    const auth = getAuth();
    if (!auth) {
      router.replace('/login');
      return;
    }
    // 从注册时已保存的用户数据预填 gender 和 age，避免重复填写
    fetchUser(auth.userId).then((res) => {
      const user = res?.data;
      setForm((prev) => ({
        ...prev,
        gender: prev.gender ?? (user?.gender === 'MALE' || user?.gender === 'FEMALE' ? user.gender : undefined),
        age: prev.age ?? (user?.age ? Number(user.age) : undefined),
      }));
    }).catch(() => {/* 获取失败时保持空表单，用户手动填写 */});
  }, [router]);

  const set = (field: keyof ProfileForm) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const numFields = ['heightCm', 'weightKg'];
    const value = numFields.includes(field) ? (e.target.value ? Number(e.target.value) : undefined) : e.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setError('');
    try {
      const auth = getAuth();
      if (!auth) { router.replace('/login'); return; }

      // Save gender & age to User entity first (needed for backend BMR calc)
      if (form.gender !== undefined || form.age !== undefined) {
        await updateUserBasic(auth.userId, { gender: form.gender, age: form.age });
      }

      // Compute macro targets from formula
      const computed = computeTargets(form);
      // 提交时 healthGoal 从小写 key 转为后端枚举所需的大写
      const profileData = {
        ...form,
        healthGoal: form.healthGoal?.toUpperCase(),
        dailyCalorieTarget: computed?.targetCalories,
        dailyProteinTarget: computed?.protein,
        dailyFatTarget:     computed?.fat,
        dailyCarbTarget:    computed?.carb,
      };

      await updateUserProfile(auth.userId, profileData as Record<string, unknown>);
      router.push('/');
    } catch {
      setError('保存失败，请重试');
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputClass = "w-full h-10 rounded-xl border border-white/10 bg-white/5 px-3 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 focus:bg-white/10 transition-all";
  const textareaClass = "w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white placeholder:text-white/30 outline-none focus:border-emerald-400/50 focus:bg-white/10 transition-all resize-none";

  const steps = [
    // Step 0: 基础身体数据（性别 + 年龄 + 身高 + 体重）
    <div key={0} className="space-y-4">
      <div className="text-center space-y-1">
        <h2 className="text-xl font-bold text-white">基础身体数据</h2>
        <p className="text-sm text-white/50">用于计算您的基础代谢和营养目标</p>
      </div>
      {/* 性别 */}
      <div className="space-y-1.5">
        <label className="text-xs text-white/60">性别</label>
        <div className="grid grid-cols-2 gap-2">
          {(['MALE', 'FEMALE'] as const).map((g) => (
            <button
              key={g}
              type="button"
              onClick={() => setForm((p) => ({ ...p, gender: g }))}
              className={`h-10 rounded-xl border text-sm font-medium transition-all ${
                form.gender === g
                  ? 'border-emerald-400 bg-emerald-500/15 text-white'
                  : 'border-white/10 bg-white/5 text-white/60 hover:bg-white/10'
              }`}
            >
              {g === 'MALE' ? '男' : '女'}
            </button>
          ))}
        </div>
      </div>
      {/* 年龄 + 身高 + 体重 */}
      <div className="grid grid-cols-3 gap-2">
        <div className="space-y-1.5">
          <label className="text-xs text-white/60">年龄</label>
          <div className="h-10 rounded-lg border border-white/10 bg-white/5 flex items-center gap-0.5 px-1">
            <button type="button" onClick={() => changeNum('age', -1, 10, 120)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">−</button>
            <input type="text" value={form.age ?? ''} onChange={(e) => { const v = e.target.value; setForm(p => ({ ...p, age: v ? Number(v) : undefined })); }} placeholder="25" className="flex-1 min-w-0 bg-transparent text-center text-sm text-white outline-none placeholder:text-white/30" />
            <button type="button" onClick={() => changeNum('age', 1, 10, 120)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">+</button>
          </div>
        </div>
        <div className="space-y-1.5">
          <label className="text-xs text-white/60">身高 (cm)</label>
          <div className="h-10 rounded-lg border border-white/10 bg-white/5 flex items-center gap-0.5 px-1">
            <button type="button" onClick={() => changeNum('heightCm', -1, 50, 300)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">−</button>
            <input type="text" value={form.heightCm ?? ''} onChange={(e) => { const v = e.target.value; setForm(p => ({ ...p, heightCm: v ? Number(v) : undefined })); }} placeholder="170" className="flex-1 min-w-0 bg-transparent text-center text-sm text-white outline-none placeholder:text-white/30" />
            <button type="button" onClick={() => changeNum('heightCm', 1, 50, 300)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">+</button>
          </div>
        </div>
        <div className="space-y-1.5">
          <label className="text-xs text-white/60">体重 (kg)</label>
          <div className="h-10 rounded-lg border border-white/10 bg-white/5 flex items-center gap-0.5 px-1">
            <button type="button" onClick={() => changeNum('weightKg', -0.5, 20, 500)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">−</button>
            <input type="text" value={form.weightKg ?? ''} onChange={(e) => { const v = e.target.value; setForm(p => ({ ...p, weightKg: v ? Number(v) : undefined })); }} placeholder="65" className="flex-1 min-w-0 bg-transparent text-center text-sm text-white outline-none placeholder:text-white/30" />
            <button type="button" onClick={() => changeNum('weightKg', 0.5, 20, 500)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">+</button>
          </div>
        </div>
      </div>
    </div>,

    // Step 1: 健康目标
    <div key={1} className="space-y-4">
      <div className="text-center space-y-1">
        <h2 className="text-xl font-bold text-white">健康目标</h2>
        <p className="text-sm text-white/50">选择您最主要的健康管理目标</p>
      </div>
      <div className="grid grid-cols-2 gap-2">
        {(Object.entries(GOAL_CONFIGS) as [GoalKey, GoalConfig][]).map(([key, goal]) => (
          <button
            key={key}
            type="button"
            onClick={() => setForm((prev) => ({ ...prev, healthGoal: key }))}
            className={`rounded-xl border p-3 text-left transition-all ${form.healthGoal === key ? 'border-emerald-400 bg-emerald-500/15' : 'border-white/10 bg-white/5 hover:bg-white/10'}`}
          >
            <p className="text-sm font-semibold text-white">{goal.label}</p>
            <p className="text-xs text-white/50 mt-0.5 leading-relaxed">{goal.desc}</p>
          </button>
        ))}
      </div>
    </div>,

    // Step 2: 活动水平
    <div key={2} className="space-y-4">
      <div className="text-center space-y-1">
        <h2 className="text-xl font-bold text-white">日常活动水平</h2>
        <p className="text-sm text-white/50">描述您的日常运动习惯</p>
      </div>
      <div className="space-y-2">
        {ACTIVITY_LEVELS.map((level) => (
          <button
            key={level.value}
            type="button"
            onClick={() => setForm((prev) => ({ ...prev, activityLevel: level.value }))}
            className={`w-full rounded-xl border p-3 text-left transition-all flex items-center gap-3 ${form.activityLevel === level.value ? 'border-emerald-400 bg-emerald-500/15' : 'border-white/10 bg-white/5 hover:bg-white/10'}`}
          >
            <div className={`w-3 h-3 rounded-full border-2 flex-shrink-0 ${form.activityLevel === level.value ? 'border-emerald-400 bg-emerald-400' : 'border-white/30'}`} />
            <div>
              <p className="text-sm font-semibold text-white">{level.label}</p>
              <p className="text-xs text-white/50">{level.desc}</p>
            </div>
          </button>
        ))}
      </div>
    </div>,

    // Step 3: 饮食偏好 + 营养目标
    <div key={3} className="space-y-4">
      <div className="text-center space-y-1">
        <h2 className="text-xl font-bold text-white">饮食偏好</h2>
        <p className="text-sm text-white/50">帮助我们为您提供个性化推荐（均可留空）</p>
      </div>
      <div className="space-y-3">
        <div className="space-y-1.5">
          <label className="text-xs text-white/60">饮食偏好（如：素食、低碳水）</label>
          <textarea placeholder="请描述您的饮食习惯..." value={form.dietPreference || ''} onChange={set('dietPreference')} rows={2} className={textareaClass} />
        </div>
        <div className="space-y-1.5">
          <label className="text-xs text-white/60">过敏信息</label>
          <input type="text" placeholder="如：花生、海鲜" value={form.allergies || ''} onChange={set('allergies')} className={inputClass} />
        </div>
        <div className="space-y-1.5">
          <label className="text-xs text-white/60">不喜欢的食物</label>
          <input type="text" placeholder="如：香菜、苦瓜" value={form.dislikedFoods || ''} onChange={set('dislikedFoods')} className={inputClass} />
        </div>
      </div>
    </div>,

    // Step 4: 营养目标展示
    <div key={4} className="space-y-4">
      <div className="text-center space-y-1">
        <h2 className="text-xl font-bold text-white">您的营养目标</h2>
        <p className="text-sm text-blue-400 cursor-pointer hover:text-blue-300 transition-colors">摄入太高/太低？</p>
      </div>
      {(() => {
        const r = computeTargets(form);
        if (!r) return <p className="text-center text-sm text-white/40 py-4">请完善前面的信息后自动计算</p>;
        const sign = r.calorieDelta >= 0 ? '+' : '';
        const dur = r.durationMin === r.durationMax ? `${r.durationMin} 周` : `${r.durationMin}–${r.durationMax} 周`;
        return (
          <div className="space-y-2">
            <div className="rounded-xl border border-white/10 bg-white/5 p-4">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-4xl font-bold text-white tracking-tight">{r.targetCalories}</p>
                  <p className="text-sm text-white/50 mt-0.5">卡路里（千卡）</p>
                </div>
                <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center text-lg">🎯</div>
              </div>
              <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-white/40">
                <span>TDEE {r.tdee} kcal</span>
                <span className={r.calorieDelta <= 0 ? 'text-emerald-400' : 'text-orange-400'}>{sign}{r.calorieDelta} kcal/天</span>
              </div>
              {/* 计划周期说明 */}
              <div className="mt-2 flex items-center gap-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 px-3 py-2">
                <span className="text-base">📅</span>
                <div>
                  <p className="text-xs text-emerald-300 font-medium">计划周期：{dur}</p>
                  <p className="text-[10px] text-white/40 mt-0.5">提交后系统将自动设置计划开始日期与到期日</p>
                </div>
              </div>
            </div>
            {([
              { label: '碳水化合物', value: r.carb,    pct: r.macroRatio.carb,    color: 'text-blue-300'   },
              { label: '蛋白质',     value: r.protein, pct: r.macroRatio.protein, color: 'text-orange-300' },
              { label: '脂肪',       value: r.fat,     pct: r.macroRatio.fat,     color: 'text-yellow-300' },
            ]).map(({ label, value, pct, color }) => (
              <div key={label} className="flex items-center justify-between rounded-xl border border-white/10 bg-white/5 px-4 py-3">
                <span className="text-sm text-white/70">{label}</span>
                <div className="flex items-baseline gap-1.5">
                  <span className={`text-lg font-semibold ${color}`}>{value}</span>
                  <span className="text-xs text-white/40">g</span>
                  <span className="text-xs text-white/30">({pct}%)</span>
                </div>
              </div>
            ))}
            <div className="grid grid-cols-2 gap-2">
              <div className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-center">
                <p className="text-xs text-white/40">瘦体重 (LBM)</p>
                <p className="text-base font-semibold text-white">{r.lbm} kg</p>
              </div>
              <div className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-center">
                <p className="text-xs text-white/40">估算体脂率</p>
                <p className="text-base font-semibold text-white">{r.bfPct}%</p>
              </div>
            </div>
          </div>
        );
      })()}
    </div>,
  ];

  return (
    <BackgroundGradientAnimation interactive>
      <div className="absolute inset-0 z-50 flex items-center justify-center px-4 py-8 overflow-y-auto">
        <motion.div className="relative w-full max-w-sm my-auto" style={{ perspective: 1500 }} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }}>
          <motion.div className="relative" style={{ rotateX, rotateY }} onMouseMove={handleMouseMove} onMouseLeave={handleMouseLeave} whileHover={{ z: 10 }}>
            <div className="relative group">
              <motion.div className="absolute -inset-[1px] rounded-2xl opacity-0 group-hover:opacity-70 transition-opacity duration-700" animate={{ boxShadow: ['0 0 10px 2px rgba(255,255,255,0.03)', '0 0 15px 5px rgba(255,255,255,0.05)', '0 0 10px 2px rgba(255,255,255,0.03)'], opacity: [0.2, 0.4, 0.2] }} transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut', repeatType: 'mirror' }} />
              <div className="absolute -inset-[1px] rounded-2xl overflow-hidden pointer-events-none">
                <motion.div className="absolute top-0 left-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-emerald-300 to-transparent opacity-70" animate={{ left: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1 }} />
                <motion.div className="absolute top-0 right-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-emerald-300 to-transparent opacity-70" animate={{ top: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 0.6 }} />
                <motion.div className="absolute bottom-0 right-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-emerald-300 to-transparent opacity-70" animate={{ right: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.2 }} />
                <motion.div className="absolute bottom-0 left-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-emerald-300 to-transparent opacity-70" animate={{ bottom: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.8 }} />
              </div>
              <div className="relative overflow-hidden rounded-2xl border border-white/[0.05] bg-black/40 p-6 shadow-2xl backdrop-blur-xl">
                <div className="absolute inset-0 opacity-[0.03]" style={{ backgroundImage: 'linear-gradient(135deg, white 0.5px, transparent 0.5px), linear-gradient(45deg, white 0.5px, transparent 0.5px)', backgroundSize: '30px 30px' }} />
                <div className="relative space-y-6">
                  <StepIndicator current={step} total={steps.length} />

              {/* 错误提示 */}
              <AnimatePresence>
                {error && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                    className="rounded-xl border border-red-400/20 bg-red-500/10 px-4 py-2 text-sm text-red-200">
                    {error}
                  </motion.div>
                )}
              </AnimatePresence>

              {/* 步骤内容 */}
              <AnimatePresence mode="wait">
                <motion.div
                  key={step}
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.25 }}
                >
                  {steps[step]}
                </motion.div>
              </AnimatePresence>

              {/* 导航按钮 */}
              <div className="flex gap-3">
                {step > 0 && (
                  <button
                    type="button"
                    onClick={() => setStep((s) => s - 1)}
                    className="flex-1 h-11 rounded-xl border border-white/15 bg-white/5 text-sm text-white/70 hover:bg-white/10 transition-all"
                  >
                    上一步
                  </button>
                )}
                {step < steps.length - 1 ? (
                  <button
                    type="button"
                    onClick={() => setStep((s) => s + 1)}
                    className="flex-1 h-11 rounded-xl bg-emerald-500 text-sm font-semibold text-white hover:bg-emerald-400 transition-all active:scale-[0.98]"
                  >
                    下一步
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={isSubmitting}
                    className="flex-1 h-11 rounded-xl bg-emerald-500 text-sm font-semibold text-white hover:bg-emerald-400 transition-all active:scale-[0.98] disabled:opacity-60"
                  >
                    {isSubmitting ? '保存中...' : '开始体验'}
                  </button>
                )}
              </div>

              {/* 跳过 */}
              <p className="text-center text-xs text-white/30">
                <button type="button" onClick={() => router.push('/')} className="hover:text-white/50 transition-colors">
                  跳过，稍后设置
                </button>
              </p>
                </div>
              </div>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </BackgroundGradientAnimation>
  );
}
