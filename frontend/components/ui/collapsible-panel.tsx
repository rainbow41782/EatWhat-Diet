'use client';
import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft, ChevronRight, ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

// ── 桌面端 (xl+): 绝对定位可折叠侧边面板 ─────────────────────────────────────
type DesktopChildren =
  | React.ReactNode
  | ((collapseBtn: React.ReactNode) => React.ReactNode);

interface CollapsiblePanelDesktopProps {
  side: 'left' | 'right';
  label: string;
  icon: React.ReactNode;
  children: DesktopChildren;
}

export function CollapsiblePanelDesktop({
  side,
  label,
  icon,
  children,
}: CollapsiblePanelDesktopProps) {
  const [isExpanded, setIsExpanded] = useState(true);

  // 折叠按钮元素，通过 render prop 注入子卡片的合适位置
  const collapseBtn = (
    <button
      onClick={() => setIsExpanded(false)}
      title="收起"
      aria-label="收起面板"
      className="flex h-9 w-9 items-center justify-center rounded-full bg-white/10 text-white/40 hover:bg-white/20 hover:text-white transition-all"
    >
      {side === 'left' ? <ChevronLeft size={15} /> : <ChevronRight size={15} />}
    </button>
  );

  const renderedChildren =
    typeof children === 'function' ? children(collapseBtn) : children;

  return (
    <>
      {/* 展开状态：面板主体 */}
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            key="panel"
            initial={{ opacity: 0, x: side === 'left' ? -24 : 24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: side === 'left' ? -24 : 24 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
            className={cn(
              // hidden on <xl, flex column on xl+
              'hidden xl:flex flex-col justify-center absolute z-30',
              side === 'left' ? 'left-8' : 'right-8',
              // 固定在导航栏下方到页面底部，避免被导航栏遮挡
              'top-20 bottom-4',
            )}
          >
            <div
              className="relative max-h-full overflow-y-auto"
            >
              {renderedChildren}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* 折叠状态：浮动展开按钮 */}
      <AnimatePresence>
        {!isExpanded && (
          <motion.button
            key="toggle"
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            transition={{ duration: 0.2 }}
            onClick={() => setIsExpanded(true)}
            title={`展开${label}`}
            aria-label={`展开${label}`}
            className={cn(
              'hidden xl:flex absolute top-1/2 -translate-y-1/2 z-30 flex-col items-center gap-1.5 rounded-2xl',
              'border border-white/10 bg-black/60 backdrop-blur-xl px-2.5 py-3',
              'text-white/50 hover:text-white hover:border-white/20 transition-all shadow-lg',
              side === 'left' ? 'left-3' : 'right-3',
            )}
          >
            <span className="text-emerald-400/80">{icon}</span>
            <span
              className="text-[10px] font-medium text-white/50"
              style={{ writingMode: 'vertical-lr' }}
            >
              {label}
            </span>
            {side === 'left' ? (
              <ChevronRight size={12} className="text-white/40" />
            ) : (
              <ChevronLeft size={12} className="text-white/40" />
            )}
          </motion.button>
        )}
      </AnimatePresence>
    </>
  );
}

// ── 移动端 (<xl): 行内可折叠手风琴 ───────────────────────────────────────────
interface CollapsiblePanelMobileProps {
  label: string;
  icon: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

export function CollapsiblePanelMobile({
  label,
  icon,
  children,
  className,
}: CollapsiblePanelMobileProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div
      className={cn(
        'w-full rounded-3xl border border-white/10 bg-black/40 backdrop-blur-xl overflow-hidden shadow-xl',
        className,
      )}
    >
      {/* 标题栏 / 展开触发器 */}
      <button
        onClick={() => setIsExpanded((v) => !v)}
        className="w-full flex items-center justify-between px-5 py-3.5 text-white"
      >
        <div className="flex items-center gap-2.5">
          {icon}
          <span className="text-sm font-semibold">{label}</span>
        </div>
        <motion.div
          animate={{ rotate: isExpanded ? 180 : 0 }}
          transition={{ duration: 0.2 }}
        >
          <ChevronDown size={16} className="text-white/50" />
        </motion.div>
      </button>

      {/* 内容区（高度动画） */}
      <AnimatePresence initial={false}>
        {isExpanded && (
          <motion.div
            key="content"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: [0.04, 0.62, 0.23, 0.98] }}
            className="overflow-hidden"
          >
            {children}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
