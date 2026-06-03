'use client';
import { useMemo, useState } from 'react';
import { ChevronDown, LogOut, Menu, MessageSquare, UserCircle2, X } from 'lucide-react';

interface MainNavbarProps {
  isLoggedIn: boolean;
  nickname: string;
  avatarLetter: string;
  onLogout: () => void;
}

const primaryMenus = [
  {
    title: '膳食记录',
    items: [
      { label: '早餐', href: '/meals/breakfast' },
      { label: '午餐', href: '/meals/lunch' },
      { label: '晚餐', href: '/meals/dinner' },
      { label: '加餐', href: '/meals/snack' },
      { label: '历史打卡记录', href: '/checkin' },
    ],
  },
  {
    title: '饮食推荐',
    items: [
      { label: '推荐食谱', href: '/recipes' },
      { label: '附近餐厅', href: '/restaurants/nearby' },
    ],
  },
  {
    title: '健康',
    items: [
      { label: '健康档案', href: '/profile' },
      { label: '健康报告', href: '/reports/health' },
    ],
  },
  {
    title: 'Contact Us',
    items: [{ label: 'Feedback', href: '/contact/feedback', icon: MessageSquare }],
  },
];

export function MainNavbar({ isLoggedIn, nickname, avatarLetter, onLogout }: MainNavbarProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const displayName = useMemo(() => nickname || '用户', [nickname]);

  return (
    <nav className="absolute top-0 left-0 right-0 z-50 px-6 py-4">
      <div className="mx-auto max-w-7xl rounded-xl border border-white/10 bg-black/35 backdrop-blur-xl px-4 py-3 flex items-center justify-between gap-4">
        <a href="/" className="flex items-center gap-3">
          {/* logo：透明通道 PNG，drop-shadow 按字形轮廓做霓虹外发光 */}
          <img
            src="/logo.png"
            alt="食乜"
            style={{
              height: '48px',
              width: 'auto',
              filter:
                'drop-shadow(0 0 6px rgba(52,211,153,0.9)) ' +
                'drop-shadow(0 0 14px rgba(45,212,191,0.7)) ' +
                'drop-shadow(0 0 28px rgba(16,185,129,0.45))',
            }}
          />
          <span className="hidden md:block text-xs text-white/50">智能健康饮食助手</span>
        </a>

        <div className="hidden lg:flex items-center gap-2">
          {primaryMenus.map((menu) => (
            <div key={menu.title} className="group relative">
              <button className="inline-flex items-center gap-1 rounded-lg border border-transparent px-3 py-2 text-sm text-white/75 transition hover:border-white/10 hover:bg-white/5 hover:text-white">
                {menu.title}
                <ChevronDown size={14} className="transition group-hover:rotate-180" />
              </button>
              <div className="pointer-events-none absolute left-0 top-full pt-2 opacity-0 translate-y-2 transition duration-200 group-hover:opacity-100 group-hover:translate-y-0 group-hover:pointer-events-auto">
                <div className="min-w-48 rounded-xl border border-white/10 bg-black/80 backdrop-blur-xl p-2 shadow-xl">
                  {menu.items.map((item) => (
                    <a
                      key={item.label}
                      href={item.href}
                      className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-white/75 hover:bg-white/10 hover:text-white"
                    >
                      {item.icon ? <item.icon size={14} /> : null}
                      {item.label}
                    </a>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="hidden lg:flex items-center gap-3">
          {isLoggedIn ? (
            <>
              <a href="/profile" className="flex items-center gap-2 rounded-lg border border-white/10 px-2.5 py-1.5 hover:bg-white/5 transition">
                <div className="w-8 h-8 rounded-full bg-emerald-500/20 border border-emerald-400/30 flex items-center justify-center text-sm font-bold text-emerald-300">
                  {avatarLetter}
                </div>
                <span className="text-sm text-white/80 max-w-[120px] truncate">{displayName}</span>
              </a>
              <button onClick={onLogout} className="flex items-center gap-1.5 text-xs text-white/50 hover:text-white/80 transition-colors">
                <LogOut size={14} />
                退出
              </button>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <a href="/login" className="px-4 py-1.5 text-sm text-white/70 hover:text-white transition-colors rounded-lg border border-white/10 hover:bg-white/5">登录</a>
              <a href="/register" className="px-4 py-1.5 text-sm font-semibold text-white bg-emerald-500 hover:bg-emerald-400 rounded-lg transition-colors">注册</a>
            </div>
          )}
        </div>

        <button
          type="button"
          className="lg:hidden inline-flex items-center justify-center w-10 h-10 rounded-lg border border-white/10 text-white/75"
          onClick={() => setMobileOpen((v) => !v)}
        >
          {mobileOpen ? <X size={18} /> : <Menu size={18} />}
        </button>
      </div>

      {mobileOpen ? (
        <div className="lg:hidden mt-2 rounded-xl border border-white/10 bg-black/80 backdrop-blur-xl p-3 space-y-2 max-h-[calc(100svh-5rem)] overflow-y-auto">
          {primaryMenus.map((menu) => (
            <div key={menu.title} className="rounded-lg border border-white/10 p-2">
              <p className="text-sm text-white/90 font-semibold px-2 py-1">{menu.title}</p>
              {menu.items.map((item) => (
                <a key={item.label} href={item.href} className="flex items-center gap-2 rounded-md px-2 py-2 text-sm text-white/75 hover:bg-white/10 hover:text-white">
                  {item.icon ? <item.icon size={14} /> : null}
                  {item.label}
                </a>
              ))}
            </div>
          ))}
          {isLoggedIn ? (
            <div className="rounded-lg border border-white/10 p-2">
              <a href="/profile" className="flex items-center gap-2 rounded-md px-2 py-2 text-sm text-white/80 hover:bg-white/10">
                <UserCircle2 size={16} />
                个人资料
              </a>
              <button onClick={onLogout} className="w-full text-left flex items-center gap-2 rounded-md px-2 py-2 text-sm text-white/65 hover:bg-white/10 hover:text-white">
                <LogOut size={16} />
                退出登录
              </button>
            </div>
          ) : (
            <div className="flex gap-2">
              <a href="/login" className="flex-1 text-center rounded-lg border border-white/10 px-3 py-2 text-sm text-white/80">登录</a>
              <a href="/register" className="flex-1 text-center rounded-lg bg-emerald-500 px-3 py-2 text-sm text-white font-semibold">注册</a>
            </div>
          )}
        </div>
      ) : null}
    </nav>
  );
}
