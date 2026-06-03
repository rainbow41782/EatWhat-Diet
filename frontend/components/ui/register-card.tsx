'use client';
import React, { useState } from 'react';
import { AnimatePresence, motion, useMotionValue, useTransform } from 'framer-motion';
import { ArrowRight, Eye, EyeOff, Lock, Mail, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { saveAuth } from '@/lib/auth';
import { useRouter } from 'next/router';

function Input({ className, type, ...props }: React.ComponentProps<'input'>) {
  return (
    <input
      type={type}
      className={cn(
        'w-full border-transparent bg-white/5 text-white placeholder:text-white/30 transition-all duration-300',
        'rounded-lg px-3 py-2 text-sm outline-none focus:border-white/20 focus:bg-white/10',
        className,
      )}
      {...props}
    />
  );
}

type RegisterRequest = {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  gender?: string;
  age?: number;
};

type ApiResponse = {
  success: boolean;
  message?: string;
  data?: { id?: number; username?: string } | null;
};

const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
  { label: '其他', value: 'OTHER' },
  { label: '不透露', value: 'UNKNOWN' },
];

export function RegisterCard() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [form, setForm] = useState<RegisterRequest>({ username: '', password: '' });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [focusedInput, setFocusedInput] = useState<string | null>(null);

  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);
  const rotateX = useTransform(mouseY, [-300, 300], [10, -10]);
  const rotateY = useTransform(mouseX, [-300, 300], [-10, 10]);

  const set = (field: keyof RegisterRequest) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = field === 'age' ? (e.target.value ? Number(e.target.value) : undefined) : e.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const changeAge = (delta: number) => {
    setForm((prev) => {
      const current = prev.age ?? 18;
      const next = Math.max(1, Math.min(120, current + delta));
      return { ...prev, age: next };
    });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = e.currentTarget.getBoundingClientRect();
    mouseX.set(e.clientX - rect.left - rect.width / 2);
    mouseY.set(e.clientY - rect.top - rect.height / 2);
  };

  const handleMouseLeave = () => {
    mouseX.set(0);
    mouseY.set(0);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!form.username || !form.password) {
      setError('用户名和密码为必填项');
      return;
    }
    if (form.password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }
    if (form.password.length < 6) {
      setError('密码至少需要 6 位字符');
      return;
    }

    setIsLoading(true);
    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      });
      const result: ApiResponse = await res.json();

      if (result.success && result.data?.id) {
        const loginRes = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: form.username, password: form.password }),
        });
        const loginResult = await loginRes.json();
        if (loginResult.success && loginResult.data?.token) {
          saveAuth({ token: loginResult.data.token, userId: loginResult.data.user?.id ?? result.data.id! });
        }
        router.push('/onboarding');
      } else {
        setError(result.message || '注册失败，请稍后重试');
      }
    } catch {
      setError('网络错误，请检查连接后重试');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <motion.div className="relative w-full max-w-sm" style={{ perspective: 1500 }} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }}>
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

            <div className="relative space-y-4">
              <div className="text-center space-y-1">
                <motion.div initial={{ scale: 0.5, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ type: 'spring', duration: 0.8 }} className="mx-auto flex h-10 w-10 items-center justify-center rounded-full border border-white/10 overflow-hidden relative">
                  <span className="bg-clip-text text-lg font-bold text-transparent bg-gradient-to-b from-white to-white/70">J</span>
                </motion.div>
                <h1 className="bg-clip-text text-xl font-bold text-transparent bg-gradient-to-b from-white to-white/80">创建账号</h1>
                <p className="text-xs text-white/60">加入 食乜，开始健康饮食管理</p>
              </div>

              <AnimatePresence>
                {error ? (
                  <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="rounded-xl border border-red-400/20 bg-red-500/10 px-4 py-3 text-sm text-red-200">
                    {error}
                  </motion.div>
                ) : null}
              </AnimatePresence>

              <form onSubmit={handleSubmit} className="space-y-3">
                <div className={`relative ${focusedInput === 'username' ? 'z-10' : ''}`}>
                  <div className="relative flex items-center overflow-hidden rounded-lg">
                    <User className={`absolute left-3 h-4 w-4 transition-all duration-300 ${focusedInput === 'username' ? 'text-white' : 'text-white/40'}`} />
                    <Input
                      type="text"
                      placeholder="用户名 *"
                      value={form.username}
                      onChange={set('username')}
                      onFocus={() => setFocusedInput('username')}
                      onBlur={() => setFocusedInput(null)}
                      className="pl-10"
                    />
                  </div>
                </div>

                <div className={`relative ${focusedInput === 'password' ? 'z-10' : ''}`}>
                  <div className="relative flex items-center overflow-hidden rounded-lg">
                    <Lock className={`absolute left-3 h-4 w-4 transition-all duration-300 ${focusedInput === 'password' ? 'text-white' : 'text-white/40'}`} />
                    <Input
                      type={showPassword ? 'text' : 'password'}
                      placeholder="密码 * (至少 6 位)"
                      value={form.password}
                      onChange={set('password')}
                      onFocus={() => setFocusedInput('password')}
                      onBlur={() => setFocusedInput(null)}
                      className="pl-10 pr-10"
                    />
                    <button type="button" onClick={() => setShowPassword((v) => !v)} className="absolute right-3 text-white/40 hover:text-white/70">
                      {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                <div className="relative flex items-center overflow-hidden rounded-lg">
                  <Lock className="absolute left-3 h-4 w-4 text-white/40" />
                  <Input type={showPassword ? 'text' : 'password'} placeholder="确认密码 *" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} className="pl-10" />
                </div>

                <div className="relative flex items-center overflow-hidden rounded-lg">
                  <Mail className="absolute left-3 h-4 w-4 text-white/40" />
                  <Input type="text" placeholder="昵称（可选）" value={form.nickname || ''} onChange={set('nickname')} className="pl-10" />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <Input type="email" placeholder="邮箱（可选）" value={form.email || ''} onChange={set('email')} />
                  <div className="h-10 rounded-lg border border-white/10 bg-white/5 flex items-center gap-0.5 px-1">
                    <button type="button" onClick={() => changeAge(-1)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">−</button>
                    <input
                      type="text"
                      value={form.age ?? ''}
                      onChange={(e) => {
                        const raw = e.target.value.trim();
                        if (!raw) {
                          setForm((prev) => ({ ...prev, age: undefined }));
                          return;
                        }
                        const next = Number(raw);
                        if (!Number.isNaN(next)) {
                          setForm((prev) => ({ ...prev, age: Math.max(1, Math.min(120, next)) }));
                        }
                      }}
                      placeholder="年龄"
                      className="flex-1 bg-transparent text-center text-sm text-white placeholder:text-white/40 outline-none min-w-0"
                    />
                    <button type="button" onClick={() => changeAge(1)} className="w-7 h-7 rounded flex items-center justify-center text-lg leading-none text-white/70 hover:text-white hover:bg-white/10 transition select-none">+</button>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  {genderOptions.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setForm((prev) => ({ ...prev, gender: option.value }))}
                      className={cn(
                        'h-10 rounded-lg border text-sm transition',
                        form.gender === option.value
                          ? 'border-emerald-300/70 bg-emerald-500/20 text-emerald-100'
                          : 'border-white/10 bg-white/5 text-white/70 hover:bg-white/10 hover:text-white',
                      )}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>

                <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }} type="submit" disabled={isLoading} className="group/button relative mt-4 w-full">
                  <div className="absolute inset-0 rounded-lg bg-white/10 blur-lg opacity-0 transition-opacity duration-300 group-hover/button:opacity-70" />
                  <div className="relative flex h-11 items-center justify-center overflow-hidden rounded-lg bg-white font-medium text-black transition-all duration-300">
                    <span className="flex items-center justify-center gap-1 text-sm font-medium">
                      {isLoading ? '注册中...' : '注册'}
                      {!isLoading ? <ArrowRight className="h-3 w-3 transition-transform duration-300 group-hover/button:translate-x-1" /> : null}
                    </span>
                  </div>
                </motion.button>
              </form>

              <p className="text-center text-xs text-white/50">
                已有账号？{' '}
                <a href="/login" className="text-emerald-400 hover:text-emerald-300 underline-offset-2 hover:underline">
                  立即登录
                </a>
              </p>
            </div>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
