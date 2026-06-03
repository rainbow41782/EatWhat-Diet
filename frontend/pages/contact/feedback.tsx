'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ChevronLeft, MessageSquare, Send, CheckCircle2,
  Star, AlertTriangle, Smile, Frown,
} from 'lucide-react';

import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { PageCard } from '@/components/ui/page-card';
import { getAuth, clearAuth } from '@/lib/auth';
import { fetchUser, submitFeedback } from '@/lib/api';

const FEEDBACK_TYPES = [
  { value: 'COMMENT',   label: '使用建议', icon: <Smile size={15} />,        color: 'emerald' },
  { value: 'COMPLAINT', label: '问题反馈', icon: <AlertTriangle size={15} />, color: 'orange'  },
];

export default function FeedbackPage() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [nickname, setNickname] = useState('');
  const [userId, setUserId] = useState<number | null>(null);

  const [feedbackType, setFeedbackType] = useState('COMMENT');
  const [content, setContent] = useState('');
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [useful, setUseful] = useState<boolean | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const auth = getAuth();
    if (auth?.userId) {
      setIsLoggedIn(true);
      setUserId(auth.userId);
      fetchUser(auth.userId).then(d => setNickname(d.data?.nickname || d.nickname || ''));
    }
  }, []);

  function handleLogout() { clearAuth(); router.push('/login'); }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!content.trim()) { setError('请填写反馈内容'); return; }
    setSubmitting(true);
    setError('');
    try {
      await submitFeedback({
        userId: userId ?? undefined,
        feedbackType,
        content: content.trim(),
        rating: rating || undefined,
        useful: useful ?? undefined,
      });
      setDone(true);
    } catch {
      setError('提交失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  }

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

        <main className="flex-1 px-4 py-6 pt-24 max-w-xl mx-auto w-full">
          {/* 页头 */}
          <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="mb-6">
            <div className="flex items-center gap-3 mb-1">
              <button onClick={() => router.back()} className="text-white/40 hover:text-white/70 transition">
                <ChevronLeft size={18} />
              </button>
              <h1 className="text-2xl font-bold text-white">用户反馈</h1>
            </div>
            <p className="text-sm text-white/50 ml-7">你的每一条反馈都让我们变得更好</p>
          </motion.div>

          <AnimatePresence mode="wait">
            {/* 成功状态 */}
            {done ? (
              <motion.div
                key="done"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="text-center py-16"
              >
                <div className="w-16 h-16 rounded-full bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center mx-auto mb-4">
                  <CheckCircle2 size={30} className="text-emerald-400" />
                </div>
                <h2 className="text-xl font-bold text-white mb-2">感谢你的反馈！</h2>
                <p className="text-sm text-white/50 mb-6">我们会认真阅读每一条意见，持续改进产品体验。</p>
                <div className="flex gap-3 justify-center">
                  <button
                    onClick={() => { setDone(false); setContent(''); setRating(0); setUseful(null); }}
                    className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/60 hover:bg-white/10 transition"
                  >
                    再提一条
                  </button>
                  <button
                    onClick={() => router.push('/')}
                    className="rounded-xl border border-emerald-500/40 bg-emerald-500/15 px-4 py-2 text-sm text-emerald-300 hover:bg-emerald-500/25 transition"
                  >
                    返回首页
                  </button>
                </div>
              </motion.div>
            ) : (
              /* 表单 */
              <motion.div key="form" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
                <PageCard tilt={false}>
                  <form onSubmit={handleSubmit} className="p-5 space-y-6">

                    {/* 反馈类型 */}
                    <div>
                      <label className="block text-xs text-white/50 font-medium mb-2 uppercase tracking-wider">反馈类型</label>
                      <div className="grid grid-cols-2 gap-2">
                        {FEEDBACK_TYPES.map(ft => (
                          <button
                            key={ft.value}
                            type="button"
                            onClick={() => setFeedbackType(ft.value)}
                            className={`flex items-center justify-center gap-2 rounded-xl border py-2.5 text-sm font-medium transition ${
                              feedbackType === ft.value
                                ? ft.color === 'emerald'
                                  ? 'border-emerald-500/50 bg-emerald-500/15 text-emerald-300'
                                  : 'border-orange-500/50 bg-orange-500/15 text-orange-300'
                                : 'border-white/10 bg-white/5 text-white/50 hover:bg-white/10'
                            }`}
                          >
                            {ft.icon}{ft.label}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* 满意度评分 */}
                    <div>
                      <label className="block text-xs text-white/50 font-medium mb-2 uppercase tracking-wider">整体满意度（可选）</label>
                      <div className="flex items-center gap-1">
                        {[1, 2, 3, 4, 5].map(n => (
                          <button
                            key={n}
                            type="button"
                            onClick={() => setRating(n)}
                            onMouseEnter={() => setHoverRating(n)}
                            onMouseLeave={() => setHoverRating(0)}
                            className="p-1 transition"
                          >
                            <Star
                              size={22}
                              className={`transition ${
                                n <= (hoverRating || rating)
                                  ? 'text-yellow-400 fill-yellow-400'
                                  : 'text-white/20'
                              }`}
                            />
                          </button>
                        ))}
                        {rating > 0 && (
                          <button
                            type="button"
                            onClick={() => setRating(0)}
                            className="ml-1 text-xs text-white/30 hover:text-white/50 transition"
                          >
                            清除
                          </button>
                        )}
                      </div>
                    </div>

                    {/* 是否有帮助 */}
                    <div>
                      <label className="block text-xs text-white/50 font-medium mb-2 uppercase tracking-wider">本 App 对你有帮助吗？（可选）</label>
                      <div className="flex gap-2">
                        {[
                          { val: true,  label: '有帮助', icon: <Smile size={14} /> },
                          { val: false, label: '没什么帮助', icon: <Frown size={14} /> },
                        ].map(o => (
                          <button
                            key={String(o.val)}
                            type="button"
                            onClick={() => setUseful(useful === o.val ? null : o.val)}
                            className={`flex items-center gap-1.5 rounded-xl border px-3 py-2 text-xs font-medium transition ${
                              useful === o.val
                                ? o.val
                                  ? 'border-emerald-500/50 bg-emerald-500/15 text-emerald-300'
                                  : 'border-red-500/50 bg-red-500/15 text-red-300'
                                : 'border-white/10 bg-white/5 text-white/50 hover:bg-white/10'
                            }`}
                          >
                            {o.icon}{o.label}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* 反馈内容 */}
                    <div>
                      <label className="block text-xs text-white/50 font-medium mb-2 uppercase tracking-wider">
                        反馈内容 <span className="text-red-400">*</span>
                      </label>
                      <textarea
                        value={content}
                        onChange={e => { setContent(e.target.value); setError(''); }}
                        placeholder="请详细描述你遇到的问题或宝贵建议..."
                        rows={5}
                        className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder-white/25 outline-none focus:border-emerald-500/50 focus:bg-white/[0.07] transition resize-none"
                      />
                      <div className="flex justify-between mt-1">
                        {error
                          ? <p className="text-xs text-red-400">{error}</p>
                          : <span />
                        }
                        <p className={`text-xs ${content.length > 500 ? 'text-orange-400' : 'text-white/30'}`}>
                          {content.length} / 500
                        </p>
                      </div>
                    </div>

                    {/* 未登录提示 */}
                    {!isLoggedIn && (
                      <p className="text-xs text-white/40 rounded-xl border border-white/10 bg-white/5 px-3 py-2">
                        未登录用户的反馈将以匿名形式提交。
                        <a href="/login" className="text-emerald-400 hover:underline ml-1">登录</a> 后可关联账号。
                      </p>
                    )}

                    {/* 提交按钮 */}
                    <button
                      type="submit"
                      disabled={submitting || content.length > 500}
                      className="w-full flex items-center justify-center gap-2 rounded-xl border border-emerald-500/40 bg-emerald-500/15 py-3 text-sm font-semibold text-emerald-300 hover:bg-emerald-500/25 disabled:opacity-50 transition"
                    >
                      {submitting
                        ? <><span className="animate-spin border border-t-transparent border-emerald-400 rounded-full w-4 h-4" />提交中...</>
                        : <><Send size={15} />提交反馈</>
                      }
                    </button>
                  </form>
                </PageCard>
              </motion.div>
            )}
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
