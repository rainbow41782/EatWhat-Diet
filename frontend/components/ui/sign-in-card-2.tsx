'use client'

import React, { useMemo, useState } from 'react';
import { AnimatePresence, motion, useMotionValue, useTransform } from 'framer-motion';
import { ArrowRight, CheckCircle2, Copy, Eye, EyeOff, Lock, Mail, RotateCcw, ShieldCheck } from 'lucide-react';
import { useRouter } from 'next/router';

import { cn } from '@/lib/utils';
import { saveAuth } from '@/lib/auth';

type LoginSuccess = {
  user?: {
    id?: number;
    username?: string;
    nickname?: string;
    role?: string;
    email?: string;
  };
  token?: string;
};

type LoginResponse = {
  success: boolean;
  message?: string;
  data?: LoginSuccess | null;
};

function Input({ className, type, ...props }: React.ComponentProps<'input'>) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        'file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input flex h-10 w-full min-w-0 rounded-md border bg-transparent px-3 py-1 text-base shadow-xs transition-[color,box-shadow] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm',
        'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
        'aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive',
        className
      )}
      {...props}
    />
  );
}

function parseUserDisplay(user?: LoginSuccess['user']) {
  if (!user) return { title: 'User', subtitle: 'Login completed' };
  return {
    title: user.nickname || user.username || 'User',
    subtitle: user.role ? `Role: ${user.role}` : 'Login completed',
  };
}

function shortenToken(token?: string) {
  if (!token) return 'No token returned';
  if (token.length <= 24) return token;
  return `${token.slice(0, 12)}...${token.slice(-8)}`;
}

async function readLoginResponse(response: Response): Promise<LoginResponse> {
  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json')
    ? ((await response.json()) as LoginResponse)
    : null;

  if (!response.ok) {
    return {
      success: false,
      message: payload?.message || `Login failed (${response.status})`,
      data: null,
    };
  }

  return payload || {
    success: false,
    message: 'Server returned an empty response',
    data: null,
  };
}

function LoginSuccessCard({
  payload,
  onReset,
}: {
  payload?: LoginSuccess | null;
  onReset: () => void;
}) {
  const display = parseUserDisplay(payload?.user);
  const tokenText = useMemo(() => shortenToken(payload?.token), [payload?.token]);

  const copyToken = async () => {
    if (!payload?.token) return;
    try {
      await navigator.clipboard.writeText(payload.token);
    } catch {
      // ignore clipboard failures silently
    }
  };

  return (
    <div className="relative min-h-screen w-screen overflow-hidden bg-black flex items-center justify-center">
      <div className="absolute inset-0 bg-gradient-to-b from-emerald-500/40 via-emerald-700/50 to-black" />
      <div className="absolute inset-0 opacity-[0.03] mix-blend-soft-light" style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E")`, backgroundSize: '200px 200px' }} />

      <div className="absolute top-0 left-1/2 h-[60vh] w-[120vh] -translate-x-1/2 rounded-b-[50%] bg-emerald-400/20 blur-[80px]" />
      <motion.div className="absolute top-0 left-1/2 h-[60vh] w-[100vh] -translate-x-1/2 rounded-b-full bg-emerald-300/20 blur-[60px]" animate={{ opacity: [0.15, 0.3, 0.15], scale: [0.98, 1.02, 0.98] }} transition={{ duration: 8, repeat: Infinity, repeatType: 'mirror' }} />
      <motion.div className="absolute bottom-0 left-1/2 h-[90vh] w-[90vh] -translate-x-1/2 rounded-t-full bg-emerald-400/20 blur-[60px]" animate={{ opacity: [0.3, 0.5, 0.3], scale: [1, 1.1, 1] }} transition={{ duration: 6, repeat: Infinity, repeatType: 'mirror', delay: 1 }} />

      <div className="absolute left-1/4 top-1/4 h-96 w-96 rounded-full bg-white/5 blur-[100px] opacity-40 animate-pulse" />
      <div className="absolute right-1/4 bottom-1/4 h-96 w-96 rounded-full bg-white/5 blur-[100px] opacity-40 animate-pulse delay-1000" />

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }} className="relative z-10 w-full max-w-sm" style={{ perspective: 1500 }}>
        <div className="relative group">
          <div className="absolute -inset-[1px] rounded-2xl bg-gradient-to-r from-white/3 via-white/7 to-white/3 opacity-70 transition-opacity duration-500" />
          <div className="absolute -inset-[1px] rounded-2xl overflow-hidden">
            <motion.div className="absolute top-0 left-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ left: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ left: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror' }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror' } }} />
            <motion.div className="absolute top-0 right-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ top: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ top: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 0.6 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror', delay: 0.6 }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror', delay: 0.6 } }} />
            <motion.div className="absolute bottom-0 right-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ right: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ right: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.2 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror', delay: 1.2 }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror', delay: 1.2 } }} />
            <motion.div className="absolute bottom-0 left-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ bottom: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ bottom: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.8 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror', delay: 1.8 }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror', delay: 1.8 } }} />
          </div>

          <div className="relative overflow-hidden rounded-2xl border border-white/[0.05] bg-black/40 p-6 shadow-2xl backdrop-blur-xl">
            <div className="absolute inset-0 opacity-[0.03]" style={{ backgroundImage: `linear-gradient(135deg, white 0.5px, transparent 0.5px), linear-gradient(45deg, white 0.5px, transparent 0.5px)`, backgroundSize: '30px 30px' }} />

            <AnimatePresence mode="wait">
              <motion.div
                key="success-card"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.35 }}
                className="relative space-y-5 text-center"
              >
                <motion.div initial={{ scale: 0.5, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ type: 'spring', duration: 0.8 }} className="mx-auto flex h-12 w-12 items-center justify-center rounded-full border border-emerald-300/30 bg-emerald-400/10 shadow-[0_0_30px_rgba(16,185,129,0.2)]">
                  <CheckCircle2 className="h-6 w-6 text-emerald-300" />
                </motion.div>

                <div className="space-y-1">
                  <h1 className="bg-clip-text text-2xl font-bold text-transparent bg-gradient-to-b from-white to-white/80">Login Successful</h1>
                  <p className="text-xs text-white/60">Same visual language, now showing the authenticated state.</p>
                </div>

                <div className="rounded-xl border border-white/10 bg-white/5 p-4 text-left">
                  <div className="mb-3 flex items-center gap-2 text-xs font-medium text-white/70">
                    <ShieldCheck className="h-4 w-4 text-emerald-300" />
                    Session Details
                  </div>
                  <div className="space-y-3 text-sm">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-white/45">User</span>
                      <span className="truncate text-white">{display.title}</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-white/45">Status</span>
                      <span className="text-emerald-300">{display.subtitle}</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-white/45">Token</span>
                      <span className="truncate text-white/80">{tokenText}</span>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <button type="button" onClick={copyToken} className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 text-sm text-white/85 transition-colors hover:bg-white/10">
                    <Copy className="h-4 w-4" />
                    Copy Token
                  </button>
                  <button type="button" onClick={onReset} className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-white text-sm font-medium text-black transition-transform hover:scale-[1.01]">
                    <RotateCcw className="h-4 w-4" />
                    Back
                  </button>
                </div>

                <p className="text-center text-xs text-white/50">
                  You can now let backend colleagues hook this response into the real session / dashboard flow.
                </p>
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

export function Component() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [focusedInput, setFocusedInput] = useState<string | null>(null);
  const [rememberMe, setRememberMe] = useState(false);
  const [loginResult, setLoginResult] = useState<LoginSuccess | null>(null);
  const [errorMessage, setErrorMessage] = useState('');

  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);
  const rotateX = useTransform(mouseY, [-300, 300], [10, -10]);
  const rotateY = useTransform(mouseX, [-300, 300], [-10, 10]);

  const authEndpoint = process.env.NEXT_PUBLIC_AUTH_ENDPOINT || '/api/auth/login';

  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = e.currentTarget.getBoundingClientRect();
    mouseX.set(e.clientX - rect.left - rect.width / 2);
    mouseY.set(e.clientY - rect.top - rect.height / 2);
  };

  const handleMouseLeave = () => {
    mouseX.set(0);
    mouseY.set(0);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setErrorMessage('');

    if (!username || !password) {
      setErrorMessage('Please enter both username and password');
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(authEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      const result = await readLoginResponse(response);
      if (result?.success && result.data) {
        // 保存认证信息到 localStorage，跳转首页
        if (result.data.token && result.data.user?.id) {
          saveAuth({ token: result.data.token, userId: result.data.user.id });
        }
        router.push('/');
        return;
      }

      setErrorMessage(result?.message || 'Login failed, please try again');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Network error, please try again later');
    } finally {
      setIsLoading(false);
    }
  };

  const resetLogin = () => {
    setLoginResult(null);
    setPassword('');
    setErrorMessage('');
    setIsLoading(false);
  };

  if (loginResult) {
    return <LoginSuccessCard payload={loginResult} onReset={resetLogin} />;
  }

  return (
    <div className="relative min-h-screen w-screen overflow-hidden bg-black flex items-center justify-center">
      <div className="absolute inset-0 bg-gradient-to-b from-emerald-500/40 via-emerald-700/50 to-black" />
      <div className="absolute inset-0 opacity-[0.03] mix-blend-soft-light" style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E")`, backgroundSize: '200px 200px' }} />

      <div className="absolute top-0 left-1/2 h-[60vh] w-[120vh] -translate-x-1/2 rounded-b-[50%] bg-emerald-400/20 blur-[80px]" />
      <motion.div className="absolute top-0 left-1/2 h-[60vh] w-[100vh] -translate-x-1/2 rounded-b-full bg-emerald-300/20 blur-[60px]" animate={{ opacity: [0.15, 0.3, 0.15], scale: [0.98, 1.02, 0.98] }} transition={{ duration: 8, repeat: Infinity, repeatType: 'mirror' }} />
      <motion.div className="absolute bottom-0 left-1/2 h-[90vh] w-[90vh] -translate-x-1/2 rounded-t-full bg-emerald-400/20 blur-[60px]" animate={{ opacity: [0.3, 0.5, 0.3], scale: [1, 1.1, 1] }} transition={{ duration: 6, repeat: Infinity, repeatType: 'mirror', delay: 1 }} />
      <div className="absolute left-1/4 top-1/4 h-96 w-96 rounded-full bg-white/5 blur-[100px] opacity-40 animate-pulse" />
      <div className="absolute right-1/4 bottom-1/4 h-96 w-96 rounded-full bg-white/5 blur-[100px] opacity-40 animate-pulse delay-1000" />

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }} className="relative z-10 w-full max-w-sm" style={{ perspective: 1500 }}>
        <motion.div className="relative" style={{ rotateX, rotateY }} onMouseMove={handleMouseMove} onMouseLeave={handleMouseLeave} whileHover={{ z: 10 }}>
          <div className="relative group">
            <motion.div className="absolute -inset-[1px] rounded-2xl opacity-0 group-hover:opacity-70 transition-opacity duration-700" animate={{ boxShadow: ['0 0 10px 2px rgba(255,255,255,0.03)', '0 0 15px 5px rgba(255,255,255,0.05)', '0 0 10px 2px rgba(255,255,255,0.03)'], opacity: [0.2, 0.4, 0.2] }} transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut', repeatType: 'mirror' }} />

            <div className="absolute -inset-[1px] rounded-2xl overflow-hidden">
              <motion.div className="absolute top-0 left-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ left: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ left: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror' }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror' } }} />
              <motion.div className="absolute top-0 right-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ top: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ top: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 0.6 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror', delay: 0.6 }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror', delay: 0.6 } }} />
              <motion.div className="absolute bottom-0 right-0 h-[3px] w-[50%] bg-gradient-to-r from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ right: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ right: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.2 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror', delay: 1.2 }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror', delay: 1.2 } }} />
              <motion.div className="absolute bottom-0 left-0 h-[50%] w-[3px] bg-gradient-to-b from-transparent via-white to-transparent opacity-70" initial={{ filter: 'blur(2px)' }} animate={{ bottom: ['-50%', '100%'], opacity: [0.3, 0.7, 0.3], filter: ['blur(1px)', 'blur(2.5px)', 'blur(1px)'] }} transition={{ bottom: { duration: 2.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1, delay: 1.8 }, opacity: { duration: 1.2, repeat: Infinity, repeatType: 'mirror', delay: 1.8 }, filter: { duration: 1.5, repeat: Infinity, repeatType: 'mirror', delay: 1.8 } }} />
            </div>

            <div className="absolute -inset-[0.5px] rounded-2xl bg-gradient-to-r from-white/3 via-white/7 to-white/3 opacity-0 group-hover:opacity-70 transition-opacity duration-500" />

            <div className="relative overflow-hidden rounded-2xl border border-white/[0.05] bg-black/40 p-6 shadow-2xl backdrop-blur-xl">
              <div className="absolute inset-0 opacity-[0.03]" style={{ backgroundImage: `linear-gradient(135deg, white 0.5px, transparent 0.5px), linear-gradient(45deg, white 0.5px, transparent 0.5px)`, backgroundSize: '30px 30px' }} />

              <div className="relative space-y-5">
                <div className="text-center space-y-1">
                  <motion.div initial={{ scale: 0.5, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ type: 'spring', duration: 0.8 }} className="mx-auto flex h-10 w-10 items-center justify-center rounded-full border border-white/10 overflow-hidden relative">
                    <span className="bg-clip-text text-lg font-bold text-transparent bg-gradient-to-b from-white to-white/70">S</span>
                    <div className="absolute inset-0 bg-gradient-to-br from-white/10 to-transparent opacity-50" />
                  </motion.div>

                  <motion.h1 initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className="bg-clip-text text-xl font-bold text-transparent bg-gradient-to-b from-white to-white/80">Welcome Back</motion.h1>
                  <motion.p initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.3 }} className="text-xs text-white/60">Sign in to continue</motion.p>
                </div>

                {errorMessage ? (
                  <div className="rounded-xl border border-red-400/20 bg-red-500/10 px-4 py-3 text-sm text-red-200">{errorMessage}</div>
                ) : null}

                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="space-y-3">
                    <motion.div className={`relative ${focusedInput === 'username' ? 'z-10' : ''}`} whileFocus={{ scale: 1.02 }} whileHover={{ scale: 1.01 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }}>
                      <div className="absolute -inset-[0.5px] rounded-lg bg-gradient-to-r from-white/10 via-white/5 to-white/10 opacity-0 transition-all duration-300 group-hover:opacity-100" />
                      <div className="relative flex items-center overflow-hidden rounded-lg">
                        <Mail className={`absolute left-3 h-4 w-4 transition-all duration-300 ${focusedInput === 'username' ? 'text-white' : 'text-white/40'}`} />
                        <Input
                          type="text"
                          placeholder="Username"
                          value={username}
                          onChange={(e) => setUsername(e.target.value)}
                          onFocus={() => setFocusedInput('username')}
                          onBlur={() => setFocusedInput(null)}
                          className="w-full border-transparent bg-white/5 pl-10 pr-3 text-white placeholder:text-white/30 transition-all duration-300 focus:border-white/20 focus:bg-white/10"
                        />
                        {focusedInput === 'username' ? <motion.div layoutId="input-highlight" className="absolute inset-0 -z-10 bg-white/5" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.2 }} /> : null}
                      </div>
                    </motion.div>

                    <motion.div className={`relative ${focusedInput === 'password' ? 'z-10' : ''}`} whileFocus={{ scale: 1.02 }} whileHover={{ scale: 1.01 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }}>
                      <div className="absolute -inset-[0.5px] rounded-lg bg-gradient-to-r from-white/10 via-white/5 to-white/10 opacity-0 transition-all duration-300 group-hover:opacity-100" />
                      <div className="relative flex items-center overflow-hidden rounded-lg">
                        <Lock className={`absolute left-3 h-4 w-4 transition-all duration-300 ${focusedInput === 'password' ? 'text-white' : 'text-white/40'}`} />
                        <Input
                          type={showPassword ? 'text' : 'password'}
                          placeholder="Password"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          onFocus={() => setFocusedInput('password')}
                          onBlur={() => setFocusedInput(null)}
                          className="w-full border-transparent bg-white/5 pl-10 pr-10 text-white placeholder:text-white/30 transition-all duration-300 focus:border-white/20 focus:bg-white/10"
                        />
                        <button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-3 cursor-pointer text-white/40 transition-colors duration-300 hover:text-white">
                          {showPassword ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
                        </button>
                        {focusedInput === 'password' ? <motion.div layoutId="input-highlight" className="absolute inset-0 -z-10 bg-white/5" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.2 }} /> : null}
                      </div>
                    </motion.div>
                  </div>

                  <div className="flex items-center justify-between pt-1">
                    <label className="flex items-center gap-2 text-xs text-white/60 transition-colors duration-200 hover:text-white/80">
                      <span className="relative">
                        <input id="remember-me" name="remember-me" type="checkbox" checked={rememberMe} onChange={() => setRememberMe(!rememberMe)} className="h-4 w-4 appearance-none rounded border border-white/20 bg-white/5 transition-all duration-200 checked:border-white checked:bg-white focus:outline-none focus:ring-1 focus:ring-white/30" />
                        {rememberMe ? (
                          <motion.span initial={{ opacity: 0, scale: 0.5 }} animate={{ opacity: 1, scale: 1 }} className="pointer-events-none absolute inset-0 flex items-center justify-center text-black">
                            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                          </motion.span>
                        ) : null}
                      </span>
                      Remember me
                    </label>

                    <button type="button" onClick={() => setErrorMessage('Password recovery is not available yet')} className="text-xs text-white/60 transition-colors duration-200 hover:text-white">
                      Forgot password?
                    </button>
                  </div>

                  <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }} type="submit" disabled={isLoading} className="group/button relative mt-5 w-full">
                    <div className="absolute inset-0 rounded-lg bg-white/10 blur-lg opacity-0 transition-opacity duration-300 group-hover/button:opacity-70" />
                    <div className="relative flex h-11 items-center justify-center overflow-hidden rounded-lg bg-white font-medium text-black transition-all duration-300">
                      <motion.div className="-z-10 absolute inset-0 bg-gradient-to-r from-white/0 via-white/30 to-white/0" animate={{ x: ['-100%', '100%'] }} transition={{ duration: 1.5, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1 }} style={{ opacity: isLoading ? 1 : 0, transition: 'opacity 0.3s ease' }} />
                      <AnimatePresence mode="wait">
                        {isLoading ? (
                          <motion.div key="loading" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex items-center justify-center">
                            <div className="h-4 w-4 rounded-full border-2 border-black/70 border-t-transparent animate-spin" />
                          </motion.div>
                        ) : (
                          <motion.span key="button-text" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex items-center justify-center gap-1 text-sm font-medium">
                            Sign In
                            <ArrowRight className="h-3 w-3 transition-transform duration-300 group-hover/button:translate-x-1" />
                          </motion.span>
                        )}
                      </AnimatePresence>
                    </div>
                  </motion.button>

                  <div className="relative mt-2 mb-5 flex items-center">
                    <div className="flex-grow border-t border-white/5" />
                    <motion.span className="mx-3 text-xs text-white/40" initial={{ opacity: 0.7 }} animate={{ opacity: [0.7, 0.9, 0.7] }} transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}>or</motion.span>
                    <div className="flex-grow border-t border-white/5" />
                  </div>

                  <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }} type="button" className="group/google relative w-full">
                    <div className="absolute inset-0 rounded-lg bg-white/5 blur opacity-0 transition-opacity duration-300 group-hover/google:opacity-70" />
                    <div className="relative flex h-11 items-center justify-center gap-2 overflow-hidden rounded-lg border border-white/10 bg-white/5 font-medium text-white transition-all duration-300 hover:border-white/20">
                      <div className="flex h-4 w-4 items-center justify-center text-white/80 transition-colors duration-300 group-hover/google:text-white">G</div>
                      <span className="text-xs text-white/80 transition-colors group-hover/google:text-white">Sign in with Google</span>
                      <motion.div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/5 to-white/0" initial={{ x: '-100%' }} whileHover={{ x: '100%' }} transition={{ duration: 1, ease: 'easeInOut' }} />
                    </div>
                  </motion.button>

                  <motion.p className="mt-4 text-center text-xs text-white/60" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5 }}>
                    还没有账号？{' '}
                    <a href="/register" className="group/signup relative inline-block font-medium text-emerald-400 transition-colors duration-300 hover:text-emerald-300">
                      立即注册
                      <span className="absolute bottom-0 left-0 h-px w-0 bg-emerald-400 transition-all duration-300 group-hover/signup:w-full" />
                    </a>
                  </motion.p>
                </form>
              </div>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
}
