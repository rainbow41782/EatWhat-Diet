'use client';
import { useEffect, useState, FormEvent } from 'react';
import { useRouter } from 'next/router';
import { Plus, X } from 'lucide-react';
import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { getAuth, clearAuth } from '@/lib/auth';
import { fetchUser, fetchCheckIns, fetchUserProfile, fetchBodyMeasurements, addBodyMeasurement } from '@/lib/api';
import { PageCard } from '@/components/ui/page-card';

type CheckIn = {
  checkDate: string;
  totalCalories?: number;
  totalProtein?: number;
  totalFat?: number;
  totalCarb?: number;
};

type BodyRecord = {
  id: number;
  measureDate: string;
  weightKg?: number | null;
  bodyFatPct?: number | null;
  waistCm?: number | null;
  hipCm?: number | null;
  armCm?: number | null;
};

type Profile = {
  dailyCalorieTarget?: number;
  dailyProteinTarget?: number;
  dailyFatTarget?: number;
  dailyCarbTarget?: number;
  planStartDate?: string;
  planEndDate?: string;
  healthGoal?: string;
};

type Series = { label: string; color: string; values: (number | null)[] };

// 简单 SVG 柱状图组件（含 Y 轴）
function BarChart({ data, labels, color = '#10b981', targetLine }: {
  data: number[];
  labels: string[];
  color?: string;
  targetLine?: number;
}) {
  const ML = 40; // 左侧 Y 轴留白
  const W = 320;
  const H = 110;
  const max = Math.max(...data, targetLine ?? 0, 1);
  const chartW = W - ML - 8;
  const barW = Math.min(26, chartW / data.length - 4);
  const gap = chartW / data.length;

  // Y 轴刻度（4档）
  const ticks = [0, Math.round(max / 3), Math.round((max * 2) / 3), max];

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H + 18}`} className="overflow-visible">
      {/* 网格线 + Y 轴标签 */}
      {ticks.map((tick, i) => {
        const y = H - (tick / max) * H;
        return (
          <g key={i}>
            <line x1={ML} y1={y} x2={W - 4} y2={y} stroke="rgba(255,255,255,0.05)" strokeWidth={1} />
            <text x={ML - 5} y={y + 3.5} textAnchor="end" fill="rgba(255,255,255,0.28)" fontSize={8}>
              {tick >= 1000 ? `${(tick / 1000).toFixed(1)}k` : tick}
            </text>
          </g>
        );
      })}
      {/* Y 轴线 */}
      <line x1={ML} y1={0} x2={ML} y2={H} stroke="rgba(255,255,255,0.12)" strokeWidth={1} />
      {/* 目标虚线 */}
      {(targetLine ?? 0) > 0 && (
        <line
          x1={ML} y1={H - (targetLine! / max) * H}
          x2={W - 4} y2={H - (targetLine! / max) * H}
          stroke="#10b981" strokeWidth={1.2} strokeDasharray="5 3" strokeOpacity={0.55}
        />
      )}
      {/* 柱子 */}
      {data.map((v, i) => {
        const bh = (v / max) * H;
        const x = ML + i * gap + gap / 2 - barW / 2;
        const y = H - bh;
        return (
          <g key={i}>
            <rect x={x} y={y} width={barW} height={bh} rx={4} fill={color} fillOpacity={0.75} />
            <text x={x + barW / 2} y={H + 13} textAnchor="middle" fill="rgba(255,255,255,0.32)" fontSize={9}>
              {labels[i]}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

// 简单 SVG 折线图组件（含 Y 轴）
function LineChart({ data, labels, color = '#34d399' }: { data: number[]; labels: string[]; color?: string }) {
  const ML = 40;
  const W = 320;
  const H = 100;
  const rawMax = Math.max(...data);
  const rawMin = Math.min(...data);
  const pad = ((rawMax - rawMin) * 0.12) || 1;
  const yMax = rawMax + pad;
  const yMin = Math.max(0, rawMin - pad);
  const range = yMax - yMin || 1;
  const chartW = W - ML - 8;

  const toX = (i: number) => ML + (i / Math.max(data.length - 1, 1)) * (chartW - 4) + 2;
  const toY = (v: number) => H - ((v - yMin) / range) * (H - 14) - 7;

  const points = data.map((v, i) => `${toX(i)},${toY(v)}`);

  // Y 轴刻度（4档）
  const ticks = [0, 1, 2, 3].map(i => parseFloat((yMin + (range / 3) * i).toFixed(1)));

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H + 18}`} className="overflow-visible">
      {ticks.map((tick, i) => {
        const y = toY(tick);
        return (
          <g key={i}>
            <line x1={ML} y1={y} x2={W - 4} y2={y} stroke="rgba(255,255,255,0.05)" strokeWidth={1} />
            <text x={ML - 5} y={y + 3.5} textAnchor="end" fill="rgba(255,255,255,0.28)" fontSize={8}>
              {tick}
            </text>
          </g>
        );
      })}
      <line x1={ML} y1={0} x2={ML} y2={H} stroke="rgba(255,255,255,0.12)" strokeWidth={1} />
      <polyline points={points.join(' ')} fill="none" stroke={color} strokeWidth={2} strokeOpacity={0.8} />
      {data.map((v, i) => (
        <g key={i}>
          <circle cx={toX(i)} cy={toY(v)} r={3} fill={color} />
          <text x={toX(i)} y={H + 13} textAnchor="middle" fill="rgba(255,255,255,0.32)" fontSize={9}>
            {labels[i]}
          </text>
        </g>
      ))}
    </svg>
  );
}

// 多系列折线图（腰/臀/臂围，含 Y 轴）
function MultiLineChart({ series, labels }: { series: Series[]; labels: string[] }) {
  const allNums: number[] = [];
  series.forEach(s => s.values.forEach(v => { if (v !== null && v !== undefined) allNums.push(v); }));
  if (allNums.length === 0) return <p className="text-sm text-white/40">暂无数据</p>;
  const rawMax = Math.max(...allNums);
  const rawMin = Math.min(...allNums);
  const pad = ((rawMax - rawMin) * 0.12) || 1;
  const yMax = rawMax + pad;
  const yMin = Math.max(0, rawMin - pad);
  const range = yMax - yMin || 1;
  const ML = 40;
  const W = 320;
  const H = 100;
  const n = labels.length;
  const chartW = W - ML - 8;
  const toX = (i: number) => ML + (i / Math.max(n - 1, 1)) * (chartW - 4) + 2;
  const toY = (v: number) => H - ((v - yMin) / range) * (H - 14) - 7;

  const ticks = [0, 1, 2, 3].map(i => parseFloat((yMin + (range / 3) * i).toFixed(1)));

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H + 20}`} className="overflow-visible">
      {ticks.map((tick, i) => {
        const y = toY(tick);
        return (
          <g key={i}>
            <line x1={ML} y1={y} x2={W - 4} y2={y} stroke="rgba(255,255,255,0.05)" strokeWidth={1} />
            <text x={ML - 5} y={y + 3.5} textAnchor="end" fill="rgba(255,255,255,0.28)" fontSize={8}>
              {tick}
            </text>
          </g>
        );
      })}
      <line x1={ML} y1={0} x2={ML} y2={H} stroke="rgba(255,255,255,0.12)" strokeWidth={1} />
      {series.map((s, si) => {
        const pts = s.values.map((v, i) => (v == null ? null : { x: toX(i), y: toY(v) }));
        const segments: string[] = [];
        let seg = '';
        pts.forEach((pt) => {
          if (pt == null) { if (seg) segments.push(seg); seg = ''; }
          else seg += (seg ? 'L' : 'M') + pt.x + ',' + pt.y;
        });
        if (seg) segments.push(seg);
        return (
          <g key={si}>
            {segments.map((d, di) => (
              <path key={di} d={d} fill="none" stroke={s.color} strokeWidth={2} strokeOpacity={0.85} />
            ))}
            {pts.map((pt, i) => pt ? <circle key={i} cx={pt.x} cy={pt.y} r={3} fill={s.color} /> : null)}
          </g>
        );
      })}
      {labels.map((l, i) => (
        <text key={i} x={toX(i)} y={H + 14} textAnchor="middle" fill="rgba(255,255,255,0.32)" fontSize={9}>
          {l}
        </text>
      ))}
    </svg>
  );
}

export default function HealthReportPage() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [nickname, setNickname] = useState('');
  const [avatarLetter, setAvatarLetter] = useState('U');
  const [healthColor] = useState('16, 185, 129');

  const [checkIns, setCheckIns] = useState<CheckIn[]>([]);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [bodyRecords, setBodyRecords] = useState<BodyRecord[]>([]);
  const [loading, setLoading] = useState(true);

  // 记录弹窗状态
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    measureDate: new Date().toISOString().slice(0, 10),
    weightKg: '', waistCm: '', hipCm: '', armCm: '',
  });

  const handleLogout = () => { clearAuth(); router.replace('/login'); };

  const saveRecord = async (e: FormEvent) => {
    e.preventDefault();
    const auth = getAuth();
    if (!auth) return;
    setSaving(true);
    try {
      const res = await addBodyMeasurement(auth.userId, {
        measureDate: form.measureDate,
        weightKg: form.weightKg ? Number(form.weightKg) : null,
        waistCm: form.waistCm ? Number(form.waistCm) : null,
        hipCm: form.hipCm ? Number(form.hipCm) : null,
        armCm: form.armCm ? Number(form.armCm) : null,
      });
      if (res?.data) {
        setBodyRecords(prev => [res.data, ...prev]);
      }
      setShowForm(false);
      setForm({ measureDate: new Date().toISOString().slice(0, 10), weightKg: '', waistCm: '', hipCm: '', armCm: '' });
    } finally {
      setSaving(false);
    }
  };

  useEffect(() => {
    const auth = getAuth();
    if (!auth) { router.replace('/login'); return; }
    setIsLoggedIn(true);

    Promise.all([
      fetchUser(auth.userId).catch(() => null),
      fetchCheckIns(auth.userId).catch(() => null),
      fetchUserProfile(auth.userId).catch(() => null),
      fetchBodyMeasurements(auth.userId).catch(() => null),
    ]).then(([userRes, checkInsRes, profileRes, bodyRes]) => {
      const user = userRes?.data;
      setNickname(user?.nickname || user?.username || '用户');
      setAvatarLetter((user?.nickname || user?.username || 'U')[0]?.toUpperCase() || 'U');
      setCheckIns(checkInsRes?.data ?? []);
      setProfile(profileRes?.data ?? null);
      setBodyRecords(bodyRes?.data ?? []);
    }).finally(() => setLoading(false));
  }, [router]);

  // 近30天的打卡数据，计算完成率
  const recentCheckIns = checkIns.slice(0, 30);
  const pGoal = profile?.dailyProteinTarget ?? 0;
  const fGoal = profile?.dailyFatTarget ?? 0;
  const cGoal = profile?.dailyCarbTarget ?? 0;

  const daysInRange = (ci: CheckIn) => {
    if (!pGoal && !fGoal && !cGoal) return true;
    const pOk = !pGoal || (Math.abs((ci.totalProtein ?? 0) - pGoal) / pGoal <= 0.15);
    const fOk = !fGoal || (Math.abs((ci.totalFat ?? 0) - fGoal) / fGoal <= 0.15);
    const cOk = !cGoal || (Math.abs((ci.totalCarb ?? 0) - cGoal) / cGoal <= 0.15);
    return pOk && fOk && cOk;
  };

  const completedDays = recentCheckIns.filter(daysInRange).length;
  const completionRate = recentCheckIns.length > 0 ? Math.round((completedDays / recentCheckIns.length) * 100) : 0;

  // 近7天均值 vs 目标柱状图数据
  const last7 = checkIns.slice(0, 7).reverse();
  const calLabels = last7.map((ci) => ci.checkDate?.slice(5) ?? '');
  const calData = last7.map((ci) => Math.round(ci.totalCalories ?? 0));
  const calGoal = profile?.dailyCalorieTarget ?? 0;

  // 体重折线图
  const weightRecords = bodyRecords.slice(0, 14).reverse();
  const weightData = weightRecords.map((r) => r.weightKg ?? 0).filter(Boolean);
  const weightLabels = weightRecords
    .filter((r) => r.weightKg != null)
    .map((r) => r.measureDate?.slice(5) ?? '');

  // 身体维度折线图（腰/臀/臂围）
  const dimRecords = bodyRecords
    .filter(r => r.waistCm != null || r.hipCm != null || r.armCm != null)
    .slice(0, 14)
    .reverse();
  const dimLabels = dimRecords.map(r => r.measureDate?.slice(5) ?? '');
  const dimSeries = [
    { label: '腰围', color: '#f59e0b', values: dimRecords.map(r => r.waistCm ?? null) },
    { label: '臀围', color: '#a78bfa', values: dimRecords.map(r => r.hipCm ?? null) },
    { label: '臂围', color: '#38bdf8', values: dimRecords.map(r => r.armCm ?? null) },
  ];

  if (loading) {
    return <div className="min-h-screen bg-black text-white flex items-center justify-center">加载中...</div>;
  }

  return (
    <div className="relative min-h-screen w-screen">
      <div className="fixed inset-0 z-0">
        <BackgroundGradientAnimation interactive firstColor={healthColor} />
      </div>
      <div className="relative z-10">
        <MainNavbar isLoggedIn={isLoggedIn} nickname={nickname} avatarLetter={avatarLetter} onLogout={handleLogout} />
        <div className="pt-24 pb-12 px-4 mx-auto max-w-3xl space-y-4">

          {/* 计划周期 */}
          {profile?.planStartDate && profile?.planEndDate && (() => {
            const today = new Date().toISOString().slice(0, 10);
            const start = profile.planStartDate!;
            const end = profile.planEndDate!;
            const totalDays = Math.round((new Date(end).getTime() - new Date(start).getTime()) / 86400000);
            const elapsed = Math.max(0, Math.round((new Date(today).getTime() - new Date(start).getTime()) / 86400000));
            const progress = totalDays > 0 ? Math.min(100, Math.round((elapsed / totalDays) * 100)) : 100;
            const expired = today >= end;
            const goalMap: Record<string, string> = {
              RAPID_FAT_LOSS: '极速减脂', HIGH_INTENSITY_FAT_LOSS: '强力减脂', DAILY_FAT_LOSS: '日常减脂',
              LEAN_BULK: '精益增肌', BULK: '增重增肌', MAINTAIN: '维持体重',
              INCREASE_STRENGTH: '提升力量', IMPROVE_PERFORMANCE: '提升表现',
            };
            const goalLabel = profile.healthGoal ? (goalMap[profile.healthGoal] || profile.healthGoal) : null;

            if (expired) {
              // 计划已完成——成就感卡片
              return (
                <div
                  className="relative overflow-hidden rounded-2xl p-px text-white"
                  style={{ background: 'linear-gradient(135deg, rgba(52,211,153,0.55) 0%, rgba(251,191,36,0.45) 50%, rgba(16,185,129,0.35) 100%)' }}
                >
                  <div className="relative rounded-2xl bg-[#040e09]/92 backdrop-blur-xl px-5 pt-5 pb-4 overflow-hidden">
                    {/* 装饰光晕 */}
                    <div className="pointer-events-none absolute -top-10 -right-10 w-52 h-52 rounded-full"
                      style={{ background: 'radial-gradient(circle, rgba(52,211,153,0.1) 0%, transparent 70%)' }} />
                    <div className="pointer-events-none absolute -bottom-8 -left-8 w-36 h-36 rounded-full"
                      style={{ background: 'radial-gradient(circle, rgba(251,191,36,0.08) 0%, transparent 70%)' }} />

                    {/* 顶部：标题 + 目标标签 */}
                    <div className="flex items-start justify-between gap-3 mb-4">
                      <div className="flex items-center gap-2">
                        <div>
                          <p className="text-[10px] text-white/40 leading-none mb-0.5">阶段目标</p>
                          <h2 className="text-sm font-semibold tracking-wide">计划圆满完成</h2>
                        </div>
                      </div>
                      {goalLabel && (
                        <span className="shrink-0 text-[11px] px-2.5 py-0.5 rounded-full font-medium"
                          style={{ background: 'rgba(52,211,153,0.15)', color: '#34d399', border: '1px solid rgba(52,211,153,0.3)' }}>
                          {goalLabel}
                        </span>
                      )}
                    </div>

                    {/* 中间：大数字 + 周期信息 */}
                    <div className="flex items-end gap-5 mb-4">
                      <div>
                        <p
                          className="text-5xl font-bold leading-none"
                          style={{ background: 'linear-gradient(135deg, #34d399 0%, #fbbf24 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}
                        >
                          {totalDays}
                        </p>
                        <p className="text-xs text-white/40 mt-1">天 · 全程坚持</p>
                      </div>
                      <div className="mb-1 space-y-1">
                        <p className="text-xs text-white/35">开始 <span className="text-white/60">{start}</span></p>
                        <p className="text-xs text-white/35">结束 <span className="text-white/60">{end}</span></p>
                      </div>
                    </div>

                    {/* 进度条 */}
                    <div className="flex items-center gap-2 mb-4">
                      <div className="flex-1 h-1.5 rounded-full overflow-hidden bg-white/8">
                        <div
                          className="h-full rounded-full"
                          style={{ width: '100%', background: 'linear-gradient(90deg, #10b981 0%, #34d399 50%, #fbbf24 100%)' }}
                        />
                      </div>
                      <span className="text-xs font-semibold" style={{ color: '#34d399' }}>100%</span>
                    </div>

                    {/* 底部分割线 + CTA */}
                    <div className="border-t border-white/8 pt-3">
                      <a
                        href="/onboarding"
                        className="inline-flex items-center gap-1.5 text-xs text-white/45 hover:text-emerald-300 transition-colors"
                      >
                        制定下一阶段计划
                        <span style={{ color: '#34d399' }}>→</span>
                      </a>
                    </div>
                  </div>
                </div>
              );
            }

            // 计划进行中
            return (
              <PageCard className="p-5 text-white">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-base">📅</span>
                    <h2 className="text-sm font-semibold">我的计划周期</h2>
                  </div>
                  {goalLabel && (
                    <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">{goalLabel}</span>
                  )}
                </div>
                <div className="flex gap-4 text-xs text-white/50 mb-3">
                  <span>开始 <span className="text-white/80">{start}</span></span>
                  <span>到期 <span className="text-white/80">{end}</span></span>
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex-1 h-1.5 rounded-full bg-white/10 overflow-hidden">
                    <div className="h-full rounded-full bg-emerald-400 transition-all" style={{ width: `${progress}%` }} />
                  </div>
                  <span className="text-xs text-white/50 whitespace-nowrap">第 {elapsed} / {totalDays} 天</span>
                </div>
              </PageCard>
            );
          })()}

          {/* 近30天完成率 */}
          <PageCard className="p-5 text-white">
            <h2 className="text-base font-semibold mb-3">近30天营养达标率</h2>
            <div className="flex items-end gap-4">
              <div>
                <p className="text-4xl font-bold text-emerald-300">{completionRate}%</p>
                <p className="text-xs text-white/50 mt-1">
                  共 {recentCheckIns.length} 天打卡，{completedDays} 天三宏均在目标 ±15% 内
                </p>
              </div>
              <div className="flex-1 bg-white/5 rounded-full h-2.5 overflow-hidden">
                <div
                  className="h-full rounded-full bg-emerald-400 transition-all"
                  style={{ width: `${completionRate}%` }}
                />
              </div>
            </div>
          </PageCard>

          {/* 近7天热量摄入 vs 目标 */}
          <PageCard className="p-5 text-white">
            <h2 className="text-base font-semibold mb-3">近7天热量摄入</h2>
            {last7.length === 0 ? (
              <p className="text-sm text-white/40">暂无打卡数据</p>
            ) : (
              <>
                <BarChart data={calData} labels={calLabels} targetLine={calGoal > 0 ? calGoal : undefined} />
                {calGoal > 0 && (
                  <p className="text-xs text-white/40 mt-1.5 flex items-center gap-1.5">
                    <span className="inline-block w-5 border-t border-dashed border-emerald-500/60" />
                    目标 {Math.round(calGoal)} kcal/天
                  </p>
                )}
              </>
            )}
          </PageCard>

          {/* 体重趋势 */}
          <PageCard className="p-5 text-white">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-base font-semibold">体重趋势</h2>
              <button
                onClick={() => setShowForm(true)}
                className="flex items-center gap-1 text-xs text-emerald-300 hover:text-emerald-200 border border-emerald-500/40 rounded-lg px-2.5 py-1 transition-colors"
              >
                <Plus size={12} />记录数据
              </button>
            </div>
            {weightData.length < 2 ? (
              <p className="text-sm text-white/40">
                {weightData.length === 0 ? '暂无体重记录，点击右上角"记录数据"开始记录。' : '至少需要2条记录才能显示趋势图。'}
              </p>
            ) : (
              <LineChart data={weightData} labels={weightLabels} />
            )}
          </PageCard>

          {/* 身体维度 */}
          <PageCard className="p-5 text-white">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-base font-semibold">身体维度趋势</h2>
              <div className="flex gap-3 text-xs text-white/50">
                <span className="flex items-center gap-1"><span className="inline-block w-2.5 h-2.5 rounded-full bg-amber-400" />腰围</span>
                <span className="flex items-center gap-1"><span className="inline-block w-2.5 h-2.5 rounded-full bg-violet-400" />臀围</span>
                <span className="flex items-center gap-1"><span className="inline-block w-2.5 h-2.5 rounded-full bg-sky-400" />臂围</span>
              </div>
            </div>
            {dimRecords.length < 2 ? (
              <p className="text-sm text-white/40">
                {dimRecords.length === 0 ? '暂无维度记录，点击"记录数据"填写腰围/臀围/臂围。' : '至少需要2条记录才能显示趋势图。'}
              </p>
            ) : (
              <MultiLineChart series={dimSeries} labels={dimLabels} />
            )}
          </PageCard>

          <a href="/" className="inline-block text-emerald-300 hover:text-emerald-200 text-sm">← 返回首页</a>
        </div>
      </div>

      {/* 记录身体数据弹窗 */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4">
          <div className="w-full max-w-sm rounded-2xl border border-white/10 bg-[#0a1a12]/90 backdrop-blur-xl p-6 text-white shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-semibold">记录身体数据</h3>
              <button onClick={() => setShowForm(false)} className="text-white/40 hover:text-white transition-colors">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={saveRecord} className="space-y-3">
              <div>
                <label className="text-xs text-white/50 mb-1 block">日期</label>
                <input
                  type="date"
                  value={form.measureDate}
                  onChange={e => setForm(f => ({ ...f, measureDate: e.target.value }))}
                  className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm outline-none focus:border-emerald-500/60"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-white/50 mb-1 block">体重 (kg)</label>
                  <input
                    type="number" step="0.1" placeholder="例：65.5"
                    value={form.weightKg}
                    onChange={e => setForm(f => ({ ...f, weightKg: e.target.value }))}
                    className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm outline-none focus:border-emerald-500/60"
                  />
                </div>
                <div>
                  <label className="text-xs text-white/50 mb-1 block">腰围 (cm)</label>
                  <input
                    type="number" step="0.1" placeholder="例：72"
                    value={form.waistCm}
                    onChange={e => setForm(f => ({ ...f, waistCm: e.target.value }))}
                    className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm outline-none focus:border-amber-500/60"
                  />
                </div>
                <div>
                  <label className="text-xs text-white/50 mb-1 block">臀围 (cm)</label>
                  <input
                    type="number" step="0.1" placeholder="例：90"
                    value={form.hipCm}
                    onChange={e => setForm(f => ({ ...f, hipCm: e.target.value }))}
                    className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm outline-none focus:border-violet-500/60"
                  />
                </div>
                <div>
                  <label className="text-xs text-white/50 mb-1 block">臂围 (cm)</label>
                  <input
                    type="number" step="0.1" placeholder="例：28"
                    value={form.armCm}
                    onChange={e => setForm(f => ({ ...f, armCm: e.target.value }))}
                    className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm outline-none focus:border-sky-500/60"
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={saving}
                className="w-full mt-2 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-sm font-medium transition-colors"
              >
                {saving ? '保存中...' : '保存记录'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
