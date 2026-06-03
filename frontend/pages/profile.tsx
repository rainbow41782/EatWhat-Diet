'use client';
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { Camera, Lock, Plus, RefreshCw, Save, UserCircle2 } from 'lucide-react';
import { motion, useMotionValue, useTransform } from 'framer-motion';

import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select';
import { getAuth } from '@/lib/auth';
import { addBodyMeasurement, changeUserPassword, fetchBodyMeasurements, fetchUser, fetchUserProfile, updateUserBasic, updateUserProfile } from '@/lib/api';

type BasicForm = {
  nickname: string;
  email: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER' | 'UNKNOWN' | '';
  age: string;
};

// 与登录/注册/onboarding 统一的卡片样式组件
function ProfileCard({ children, className }: { children: React.ReactNode; className?: string }) {
  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);
  const rotateX = useTransform(mouseY, [-200, 200], [8, -8]);
  const rotateY = useTransform(mouseX, [-200, 200], [-8, 8]);
  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = e.currentTarget.getBoundingClientRect();
    mouseX.set(e.clientX - rect.left - rect.width / 2);
    mouseY.set(e.clientY - rect.top - rect.height / 2);
  };
  const handleMouseLeave = () => { mouseX.set(0); mouseY.set(0); };

  return (
    <motion.div style={{ perspective: 1200 }} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>
      <motion.div style={{ rotateX, rotateY }} onMouseMove={handleMouseMove} onMouseLeave={handleMouseLeave} whileHover={{ z: 8 }}>
        <div className="relative group">
          <motion.div className="absolute -inset-[1px] rounded-2xl opacity-0 group-hover:opacity-70 transition-opacity duration-700" animate={{ boxShadow: ['0 0 10px 2px rgba(255,255,255,0.03)', '0 0 15px 5px rgba(255,255,255,0.05)', '0 0 10px 2px rgba(255,255,255,0.03)'], opacity: [0.2, 0.4, 0.2] }} transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut', repeatType: 'mirror' }} />
          <div className="absolute -inset-[1px] rounded-2xl overflow-hidden pointer-events-none">
            <motion.div className="absolute top-0 left-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-emerald-300 to-transparent opacity-70" animate={{ left: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1 }} />
            <motion.div className="absolute top-0 right-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-emerald-300 to-transparent opacity-70" animate={{ top: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 0.6 }} />
            <motion.div className="absolute bottom-0 right-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-emerald-300 to-transparent opacity-70" animate={{ right: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.2 }} />
            <motion.div className="absolute bottom-0 left-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-emerald-300 to-transparent opacity-70" animate={{ bottom: ['-50%', '100%'] }} transition={{ duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.8 }} />
          </div>
          <div className={`relative overflow-hidden rounded-2xl border border-white/[0.05] bg-black/40 p-5 shadow-2xl backdrop-blur-xl ${className ?? ''}`}>
            <div className="absolute inset-0 opacity-[0.03]" style={{ backgroundImage: 'linear-gradient(135deg, white 0.5px, transparent 0.5px), linear-gradient(45deg, white 0.5px, transparent 0.5px)', backgroundSize: '30px 30px' }} />
            <div className="relative">{children}</div>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}

const HEALTH_GOAL_LABELS: Record<string, string> = {
  RAPID_FAT_LOSS: '极速减脂',
  HIGH_INTENSITY_FAT_LOSS: '强力减脂',
  DAILY_FAT_LOSS: '日常减脂',
  LEAN_BULK: '精益增肌',
  BULK: '增重增肌',
  MAINTAIN: '维持体重',
  INCREASE_STRENGTH: '提升力量',
  IMPROVE_PERFORMANCE: '提升表现',
  FAT_LOSS: '减脂',
  MUSCLE_GAIN: '增肌',
  BLOOD_SUGAR_CONTROL: '控糖',
};

const ACTIVITY_LABELS: Record<string, string> = {
  SEDENTARY: '久坐',
  LIGHTLY_ACTIVE: '轻度活跃',
  MODERATELY_ACTIVE: '中度活跃',
  VERY_ACTIVE: '高度活跃',
  EXTRA_ACTIVE: '极度活跃',
  LOW: '低', MEDIUM: '中', HIGH: '高',
};

type FullHealthProfile = {
  heightCm?: number | null;
  weightKg?: number | null;
  healthGoal?: string | null;
  activityLevel?: string | null;
  bmr?: number | null;
  dailyCalorieTarget?: number | null;
  dailyProteinTarget?: number | null;
  dailyFatTarget?: number | null;
  dailyCarbTarget?: number | null;
};

function StatItem({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="rounded-xl border border-white/[0.07] bg-white/[0.04] px-3 py-2.5">
      <p className="text-[11px] text-white/40 mb-0.5">{label}</p>
      <p className={`text-sm font-semibold ${accent ? 'text-emerald-300' : 'text-white'}`}>{value}</p>
    </div>
  );
}

type HealthForm = {
  heightCm: string;
  weightKg: string;
  healthGoal: string;
  activityLevel: string;
};

type BodyMeasurementRecord = {
  id: number;
  measureDate: string;
  weightKg?: number | null;
  bodyFatPct?: number | null;
  waistCm?: number | null;
};

export default function ProfilePage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [savingBasic, setSavingBasic] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [message, setMessage] = useState('');

  const [avatarUrl, setAvatarUrl] = useState('');
  const [basicForm, setBasicForm] = useState<BasicForm>({ nickname: '', email: '', gender: '', age: '' });
  const [healthProfile, setHealthProfile] = useState<FullHealthProfile | null>(null);
  const [passwordForm, setPasswordForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });

  // 身体数据
  const [bodyRecords, setBodyRecords] = useState<BodyMeasurementRecord[]>([]);
  const [showBodyForm, setShowBodyForm] = useState(false);
  const [savingBody, setSavingBody] = useState(false);
  const [bodyForm, setBodyForm] = useState({ weightKg: '', bodyFatPct: '', waistCm: '', measureDate: '' });

  const auth = useMemo(() => getAuth(), []);

  useEffect(() => {
    if (!auth) {
      router.replace('/login');
      return;
    }

    const avatarKey = `javadiet_avatar_${auth.userId}`;
    const cachedAvatar = localStorage.getItem(avatarKey);
    if (cachedAvatar) {
      setAvatarUrl(cachedAvatar);
    }

    fetchUser(auth.userId)
      .then((res) => {
        const user = res?.data;
        setBasicForm({
          nickname: user?.nickname || '',
          email: user?.email || '',
          gender: user?.gender || '',
          age: user?.age ? String(user.age) : '',
        });
      })
      .catch(() => {});

    fetchUserProfile(auth.userId)
      .then((res) => {
        const profile = res?.data;
        setHealthProfile(profile ?? null);
      })
      .catch(() => {})
      .finally(() => setLoading(false));

    fetchBodyMeasurements(auth.userId)
      .then((res) => setBodyRecords(res?.data ?? []))
      .catch(() => {});
  }, [auth, router]);

  const onAvatarChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (!auth) return;
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const base64 = String(reader.result || '');
      setAvatarUrl(base64);
      localStorage.setItem(`javadiet_avatar_${auth.userId}`, base64);
      setMessage('头像已更新');
    };
    reader.readAsDataURL(file);
  };

  const saveBasic = async (e: FormEvent) => {
    e.preventDefault();
    if (!auth) return;

    setSavingBasic(true);
    setMessage('');
    try {
      await updateUserBasic(auth.userId, {
        nickname: basicForm.nickname || null,
        email: basicForm.email || null,
        gender: basicForm.gender || null,
        age: basicForm.age ? Number(basicForm.age) : null,
      });
      setMessage('基础资料已保存');
    } catch {
      setMessage('基础资料保存失败');
    } finally {
      setSavingBasic(false);
    }
  };



  const savePassword = async (e: FormEvent) => {
    e.preventDefault();
    if (!auth) return;

    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
      setMessage('请输入完整的旧密码和新密码');
      return;
    }
    if (passwordForm.newPassword.length < 6) {
      setMessage('新密码至少 6 位');
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setMessage('两次输入的新密码不一致');
      return;
    }

    setSavingPassword(true);
    setMessage('');
    try {
      await changeUserPassword(auth.userId, passwordForm.oldPassword, passwordForm.newPassword);
      setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
      setMessage('密码修改成功');
    } catch {
      setMessage('密码修改失败，请确认旧密码是否正确');
    } finally {
      setSavingPassword(false);
    }
  };

  const saveBody = async (e: FormEvent) => {
    e.preventDefault();
    if (!auth) return;
    setSavingBody(true);
    try {
      const payload = {
        measureDate: bodyForm.measureDate || new Date().toISOString().slice(0, 10),
        weightKg: bodyForm.weightKg ? Number(bodyForm.weightKg) : null,
        bodyFatPct: bodyForm.bodyFatPct ? Number(bodyForm.bodyFatPct) : null,
        waistCm: bodyForm.waistCm ? Number(bodyForm.waistCm) : null,
      };
      const res = await addBodyMeasurement(auth.userId, payload);
      if (res?.data) {
        setBodyRecords((prev) => [res.data, ...prev]);
      }
      setBodyForm({ weightKg: '', bodyFatPct: '', waistCm: '', measureDate: '' });
      setShowBodyForm(false);
      setMessage('身体数据已记录');
    } catch {
      setMessage('记录失败，请稍后重试');
    } finally {
      setSavingBody(false);
    }
  };

  if (loading) {
    return <div className="min-h-screen bg-black text-white flex items-center justify-center">正在加载个人资料...</div>;
  }

  return (
    <div className="relative min-h-screen w-screen">
      <div className="fixed inset-0 z-0">
        <BackgroundGradientAnimation interactive />
      </div>
      <div className="relative z-10 mx-auto max-w-5xl px-4 py-24 grid gap-4 md:grid-cols-3">
        <div className="md:col-span-1 h-fit">
          <ProfileCard>
            <div className="flex flex-col items-center gap-3">
              <div className="relative">
                <div className="h-24 w-24 rounded-full overflow-hidden border border-white/20 bg-white/5 grid place-items-center">
                  {avatarUrl ? <img src={avatarUrl} alt="avatar" className="h-full w-full object-cover" /> : <UserCircle2 className="h-16 w-16 text-white/40" />}
                </div>
                <label className="absolute -bottom-1 -right-1 h-8 w-8 rounded-full bg-emerald-500 text-white grid place-items-center cursor-pointer hover:bg-emerald-400 transition">
                  <Camera size={14} />
                  <input type="file" accept="image/*" className="hidden" onChange={onAvatarChange} />
                </label>
              </div>
              <p className="text-sm text-white/70">上传头像后会保存在本地浏览器</p>
              <a href="/" className="mt-2 text-xs text-emerald-300 hover:text-emerald-200">返回首页</a>
            </div>
          </ProfileCard>
        </div>

        <div className="md:col-span-2 space-y-4">
          <ProfileCard>
            <form onSubmit={saveBasic} className="space-y-3">
              <h2 className="text-lg font-semibold text-white">基础信息</h2>
              <div className="grid sm:grid-cols-2 gap-2">
                <input className="h-10 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none" placeholder="昵称" value={basicForm.nickname} onChange={(e) => setBasicForm((prev) => ({ ...prev, nickname: e.target.value }))} />
                <input className="h-10 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none" placeholder="邮箱" value={basicForm.email} onChange={(e) => setBasicForm((prev) => ({ ...prev, email: e.target.value }))} />
                <Select
                  value={basicForm.gender}
                  onValueChange={(v) => setBasicForm((prev) => ({ ...prev, gender: v as BasicForm['gender'] }))}
                >
                  <SelectTrigger
                    placeholder="性别"
                    className="w-full min-w-0 text-sm"
                  />
                  <SelectContent>
                    <SelectItem index={0} value="">性别</SelectItem>
                    <SelectItem index={1} value="MALE">男</SelectItem>
                    <SelectItem index={2} value="FEMALE">女</SelectItem>
                    <SelectItem index={3} value="OTHER">其他</SelectItem>
                    <SelectItem index={4} value="UNKNOWN">不愿透露</SelectItem>
                  </SelectContent>
                </Select>
                <input className="h-10 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none" placeholder="年龄" value={basicForm.age} onChange={(e) => setBasicForm((prev) => ({ ...prev, age: e.target.value }))} />
              </div>
              <button disabled={savingBasic} className="inline-flex items-center gap-2 h-10 px-4 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-white text-sm font-semibold transition disabled:opacity-70">
                <Save size={14} />
                {savingBasic ? '保存中...' : '保存基础信息'}
              </button>
            </form>
          </ProfileCard>

          <ProfileCard>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-white">健康档案</h2>
                <button
                  type="button"
                  onClick={() => router.push('/onboarding')}
                  className="inline-flex items-center gap-1.5 h-8 px-3 rounded-lg border border-emerald-500/40 text-emerald-300 text-xs hover:bg-emerald-500/15 transition"
                >
                  <RefreshCw size={12} />
                  重新设置
                </button>
              </div>

              {healthProfile && (healthProfile.heightCm || healthProfile.healthGoal) ? (
                <div className="space-y-3">
                  <div className="grid grid-cols-2 gap-2">
                    <StatItem label="身高" value={healthProfile.heightCm ? `${healthProfile.heightCm} cm` : '未填写'} />
                    <StatItem label="体重" value={healthProfile.weightKg ? `${healthProfile.weightKg} kg` : '未填写'} />
                    <StatItem label="健康目标" value={healthProfile.healthGoal ? (HEALTH_GOAL_LABELS[healthProfile.healthGoal] ?? healthProfile.healthGoal) : '未设置'} />
                    <StatItem label="活动水平" value={healthProfile.activityLevel ? (ACTIVITY_LABELS[healthProfile.activityLevel] ?? healthProfile.activityLevel) : '未设置'} />
                    {healthProfile.bmr ? <StatItem label="基础代谢(BMR)" value={`${Math.round(healthProfile.bmr)} kcal`} /> : null}
                  </div>

                  {healthProfile.dailyCalorieTarget ? (
                    <div className="space-y-2">
                      <p className="text-[11px] text-white/40 uppercase tracking-wider">每日营养目标</p>
                      <div className="grid grid-cols-2 gap-2">
                        <StatItem label="热量" value={`${Math.round(healthProfile.dailyCalorieTarget)} kcal`} accent />
                        <StatItem label="蛋白质" value={healthProfile.dailyProteinTarget ? `${Math.round(healthProfile.dailyProteinTarget)} g` : '-'} />
                        <StatItem label="脂肪" value={healthProfile.dailyFatTarget ? `${Math.round(healthProfile.dailyFatTarget)} g` : '-'} />
                        <StatItem label="碳水" value={healthProfile.dailyCarbTarget ? `${Math.round(healthProfile.dailyCarbTarget)} g` : '-'} />
                      </div>
                    </div>
                  ) : null}
                </div>
              ) : (
                <p className="text-sm text-white/40 py-2">尚未完成健康档案设置，点击"重新设置"开始填写。</p>
              )}
            </div>
          </ProfileCard>

          <ProfileCard>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-white">身体数据</h2>
                <button
                  type="button"
                  onClick={() => setShowBodyForm((v) => !v)}
                  className="inline-flex items-center gap-1.5 h-8 px-3 rounded-lg border border-emerald-500/40 text-emerald-300 text-xs hover:bg-emerald-500/15 transition"
                >
                  <Plus size={12} />
                  记录数据
                </button>
              </div>

              {showBodyForm && (
                <form onSubmit={saveBody} className="rounded-xl border border-white/10 bg-white/5 p-3 space-y-2">
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="date"
                      className="h-9 col-span-2 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none"
                      value={bodyForm.measureDate}
                      onChange={(e) => setBodyForm((p) => ({ ...p, measureDate: e.target.value }))}
                    />
                    <input
                      type="number" step="0.1" placeholder="体重 (kg)"
                      className="h-9 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none"
                      value={bodyForm.weightKg}
                      onChange={(e) => setBodyForm((p) => ({ ...p, weightKg: e.target.value }))}
                    />
                    <input
                      type="number" step="0.1" placeholder="体脂率 (%)"
                      className="h-9 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none"
                      value={bodyForm.bodyFatPct}
                      onChange={(e) => setBodyForm((p) => ({ ...p, bodyFatPct: e.target.value }))}
                    />
                    <input
                      type="number" step="0.1" placeholder="腰围 (cm)"
                      className="h-9 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none"
                      value={bodyForm.waistCm}
                      onChange={(e) => setBodyForm((p) => ({ ...p, waistCm: e.target.value }))}
                    />
                    <button
                      type="submit" disabled={savingBody}
                      className="h-9 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-white text-sm font-semibold transition disabled:opacity-70"
                    >
                      {savingBody ? '保存中...' : '保存'}
                    </button>
                  </div>
                </form>
              )}

              {bodyRecords.length === 0 ? (
                <p className="text-sm text-white/40 py-1">暂无身体数据记录，点击"记录数据"开始追踪。</p>
              ) : (
                <div className="space-y-1.5 max-h-52 overflow-y-auto pr-1">
                  {bodyRecords.slice(0, 10).map((rec) => (
                    <div key={rec.id} className="flex items-center justify-between rounded-xl border border-white/[0.07] bg-white/[0.04] px-3 py-2 text-sm">
                      <span className="text-white/50 text-xs">{rec.measureDate}</span>
                      <div className="flex gap-3 text-white/80">
                        {rec.weightKg != null && <span>{rec.weightKg} kg</span>}
                        {rec.bodyFatPct != null && <span className="text-white/55">{rec.bodyFatPct}%脂</span>}
                        {rec.waistCm != null && <span className="text-white/55">{rec.waistCm}cm腰</span>}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </ProfileCard>

          <ProfileCard>
            <form onSubmit={savePassword} className="space-y-3">
              <h2 className="text-lg font-semibold text-white">账号密码</h2>
              <div className="grid sm:grid-cols-3 gap-2">
                <input type="password" className="h-10 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none" placeholder="旧密码" value={passwordForm.oldPassword} onChange={(e) => setPasswordForm((prev) => ({ ...prev, oldPassword: e.target.value }))} />
                <input type="password" className="h-10 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none" placeholder="新密码" value={passwordForm.newPassword} onChange={(e) => setPasswordForm((prev) => ({ ...prev, newPassword: e.target.value }))} />
                <input type="password" className="h-10 rounded-lg border border-white/10 bg-white/5 px-3 text-sm text-white outline-none" placeholder="确认新密码" value={passwordForm.confirmPassword} onChange={(e) => setPasswordForm((prev) => ({ ...prev, confirmPassword: e.target.value }))} />
              </div>
              <button disabled={savingPassword} className="inline-flex items-center gap-2 h-10 px-4 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-white text-sm font-semibold transition disabled:opacity-70">
                <Lock size={14} />
                {savingPassword ? '修改中...' : '修改密码'}
              </button>
            </form>
          </ProfileCard>

          {message ? <p className="text-sm text-emerald-200 bg-emerald-500/15 border border-emerald-500/30 rounded-lg px-3 py-2">{message}</p> : null}
        </div>
      </div>
    </div>
  );
}
