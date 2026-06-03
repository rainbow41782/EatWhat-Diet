'use client';
import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import RadialOrbitalTimeline from '@/components/ui/radial-orbital-timeline';
import { motion } from 'framer-motion';
import {
  UtensilsCrossed,
  Star,
  MapPin,
  Activity,
  CheckSquare,
} from 'lucide-react';

// 演示用静态节点数据
const demoTimeline = [
  {
    id: 1,
    title: '智能膳食推荐',
    date: '每日更新',
    description: '根据您的健康目标和饮食偏好，提供个性化的一日三餐推荐',
    icon: UtensilsCrossed,
    category: '膳食' as const,
    status: 'completed' as const,
    energy: 1800,
    relatedIds: [3, 4],
    details: [
      { label: '早餐推荐', value: '燕麦粥 + 水煮蛋' },
      { label: '午餐推荐', value: '鸡胸肉沙拉' },
      { label: '晚餐推荐', value: '清蒸鱼 + 蔬菜' },
    ],
  },
  {
    id: 2,
    title: '附近健康餐厅',
    date: '实时定位',
    description: '基于 GPS 定位，发现您周边评分最高的健康餐饮门店',
    icon: MapPin,
    category: '功能' as const,
    status: 'in-progress' as const,
    energy: 0,
    relatedIds: [1, 5],
    details: [{ label: '搜索范围', value: '5 公里以内' }],
  },
  {
    id: 3,
    title: '营养数据追踪',
    date: '实时同步',
    description: '精确记录每日卡路里、蛋白质、脂肪、碳水化合物摄入',
    icon: Activity,
    category: '健康档案' as const,
    status: 'in-progress' as const,
    energy: 2000,
    relatedIds: [1, 4],
    details: [
      { label: '今日卡路里', value: '1,340 kcal' },
      { label: '蛋白质', value: '80g / 120g' },
    ],
  },
  {
    id: 4,
    title: '每日健康打卡',
    date: '今日',
    description: '坚持每日打卡，养成健康饮食习惯，查看连续打卡记录',
    icon: CheckSquare,
    category: '打卡' as const,
    status: 'completed' as const,
    energy: 1,
    relatedIds: [3],
    details: [{ label: '连续打卡', value: '已坚持 7 天' }],
  },
  {
    id: 5,
    title: '个性化推荐引擎',
    date: '持续学习',
    description: '算法根据饮食记录不断优化，推荐更适合您的健康食物',
    icon: Star,
    category: '功能' as const,
    status: 'pending' as const,
    energy: 0,
    relatedIds: [1, 2],
    details: [{ label: '推荐准确率', value: '持续提升中' }],
  },
];

export default function TestPage() {
  return (
    <div className="relative w-screen h-screen overflow-hidden">
      {/* 彩色渐变背景（演示用，使用默认彩色主题） */}
      <BackgroundGradientAnimation
        gradientBackgroundStart="rgb(108, 0, 162)"
        gradientBackgroundEnd="rgb(0, 17, 82)"
        firstColor="18, 113, 255"
        secondColor="221, 74, 255"
        thirdColor="100, 220, 255"
        fourthColor="200, 50, 50"
        fifthColor="180, 180, 50"
        interactive
      />

      {/* 顶部标题区 */}
      <div className="absolute top-0 left-0 right-0 z-50 flex items-center justify-between px-8 py-5">
        <motion.span
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          className="text-xl font-bold text-white/90"
        >
          食乜
        </motion.span>
        <div className="flex gap-3">
          <a href="/login" className="px-4 py-1.5 text-sm text-white/70 border border-white/20 rounded-lg hover:bg-white/10 transition-all">
            登录
          </a>
          <a href="/register" className="px-4 py-1.5 text-sm font-semibold bg-white/20 text-white rounded-lg hover:bg-white/30 transition-all backdrop-blur-sm">
            免费注册
          </a>
        </div>
      </div>

      {/* 中央文案 */}
      <div className="absolute top-20 left-0 right-0 z-30 flex flex-col items-center text-center px-4 pt-8 pointer-events-none">
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="text-4xl sm:text-5xl font-extrabold text-transparent bg-clip-text bg-gradient-to-b from-white to-white/60 leading-tight"
        >
          智能饮食管理
          <br />
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-300 to-teal-200">
            让健康更简单
          </span>
        </motion.h1>
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="mt-4 text-white/50 text-sm max-w-sm"
        >
          基于 AI 推荐 · 营养精准追踪 · 健康目标管理
        </motion.p>
      </div>

      {/* 演示时间轴 */}
      <div className="absolute inset-0 z-20 flex items-center justify-center">
        <div className="w-full h-full pt-52">
          <RadialOrbitalTimeline timelineData={demoTimeline} />
        </div>
      </div>

      {/* 底部 CTA */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
        className="absolute bottom-10 left-0 right-0 z-50 flex flex-col items-center gap-3"
      >
        <div className="flex items-center gap-4">
          <a
            href="/register"
            className="px-8 py-3 rounded-2xl bg-emerald-500 text-white font-semibold text-sm hover:bg-emerald-400 transition-all active:scale-95 shadow-lg shadow-emerald-500/30"
          >
            立即开始，免费注册
          </a>
          <a
            href="/login"
            className="px-6 py-3 rounded-2xl border border-white/20 text-white/70 text-sm hover:bg-white/5 transition-all backdrop-blur-sm"
          >
            已有账号 登录
          </a>
        </div>
        <p className="text-xs text-white/30">这是设计预览页，后端数据均为演示</p>
      </motion.div>
    </div>
  );
}
