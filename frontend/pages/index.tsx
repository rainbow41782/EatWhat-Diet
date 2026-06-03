'use client';
import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { motion } from 'framer-motion';
import { Activity, Calendar, CheckSquare, FileText, Flame, MapPin, Sparkles, TrendingUp, UtensilsCrossed } from 'lucide-react';

import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { RecommendationPanel } from '@/components/ui/recommendation-panel';
import RadialOrbitalTimeline, { TimelineItem } from '@/components/ui/radial-orbital-timeline';
import { CalorieTrackerCard, MealCheckItem } from '@/components/ui/tracker-card';
import { CollapsiblePanelDesktop, CollapsiblePanelMobile } from '@/components/ui/collapsible-panel';

import { getAuth, clearAuth } from '@/lib/auth';
import { fetchUser, fetchUserProfile, fetchTodayNutrition, fetchCheckIns, fetchRecommendations, fetchRecentMeals, addWaterIntake, fetchRestaurant } from '@/lib/api';
import { computeHealthScore } from '@/lib/healthScore';

type Recommendation = {
  mealType?: 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | string;
  foodItemName?: string;
  totalCalories?: number;
  restaurantId?: number;
};

type RestaurantInfo = {
  id: number;
  name?: string;
  address?: string;
};

type CheckIn = {
  checkDate?: string;
  streakCount?: number;
};

type UserProfile = {
  nickname?: string;
  heightCm?: number;
  weightKg?: number;
  healthGoal?: string;
  activityLevel?: string;
  dailyCalorieTarget?: number;
  dailyProteinTarget?: number;
  dailyFatTarget?: number;
  dailyCarbTarget?: number;
  planStartDate?: string;
  planEndDate?: string;
};

type RecentMeal = {
  id?: number;
  mealType?: string;
  mealTime?: string;
  createdAt?: string;
  totalCalories?: number;
};

function getMealLabel(mealType: string) {
  const map: Record<string, string> = {
    BREAKFAST: '早餐',
    LUNCH: '午餐',
    DINNER: '晚餐',
    SNACK: '加餐',
  };
  return map[mealType] || mealType;
}

function getMealHref(mealType: string) {
  const map: Record<string, string> = {
    BREAKFAST: '/meals/breakfast',
    LUNCH: '/meals/lunch',
    DINNER: '/meals/dinner',
    SNACK: '/meals/snack',
  };
  return map[mealType] || '/meals/breakfast';
}

function getNextMealLabel(now = new Date()) {
  const hour = now.getHours();
  if (hour < 10) return '早餐';
  if (hour < 15) return '午餐';
  if (hour < 20) return '晚餐';
  return '加餐';
}

function buildMealChecks(recentMeals: RecentMeal[]): MealCheckItem[] {
  const today = new Date();
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  const todayTypes = new Set(
    recentMeals
      .filter((m) => (m.createdAt ?? m.mealTime ?? '').startsWith(todayStr))
      .map((m) => m.mealType)
  );
  const types: Array<'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'> = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
  return types.map((type) => ({
    key: type,
    label: getMealLabel(type),
    completed: todayTypes.has(type),
    optional: type === 'SNACK',
    href: getMealHref(type),
  }));
}

const HEALTH_GOAL_LABELS: Record<string, string> = {
  DAILY_FAT_LOSS: '减脂',
  DAILY_MUSCLE_GAIN: '增肌',
  MAINTAIN: '维持体重',
  LOSE_WEIGHT: '减重',
  GAIN_WEIGHT: '增重',
  BALANCED: '均衡饮食',
};

const ACTIVITY_LABELS: Record<string, string> = {
  SEDENTARY: '久坐少动',
  LIGHT: '轻度活动',
  MODERATE: '中度活动',
  ACTIVE: '积极活动',
  VERY_ACTIVE: '高度活动',
};

function buildTimelineData(
  recommendations: Recommendation[],
  checkIns: CheckIn[],
  userProfile: UserProfile | null,
  recentMeals: RecentMeal[],
  todayCalories: number,
  goalCalories: number,
): TimelineItem[] {
  const mealTypes: Array<'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'> = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];

  // 用本地日期（非 UTC）判断今日餐食，基于服务端 createdAt（CST）
  const nowLocal = new Date();
  const today = `${nowLocal.getFullYear()}-${String(nowLocal.getMonth() + 1).padStart(2, '0')}-${String(nowLocal.getDate()).padStart(2, '0')}`;
  const todayMeals = recentMeals.filter((m) => (m.createdAt ?? m.mealTime ?? '').startsWith(today));
  const todayMealTypes = new Set(todayMeals.map((m) => m.mealType));

  // 用实际记录的餐食判断状态，而非推荐系统
  const mealEntries = mealTypes.map((type) => {
    const recorded = todayMealTypes.has(type);
    return {
      key: type,
      label: getMealLabel(type),
      status: (recorded ? 'completed' : 'pending') as 'completed' | 'pending',
      summary: recorded ? `已记录${getMealLabel(type)}` : `还未记录${getMealLabel(type)}，点击进入填写`,
      href: getMealHref(type),
    };
  });

  // 当日热量贡献度（用于"记录膳食"节点的能量等级）
  const calorieContrib = goalCalories > 0 ? Math.min(100, Math.round((todayCalories / goalCalories) * 100)) : 0;

  // 今日已记录的餐种数量（用于"记录膳食"节点状态）
  const recordedMealCount = mealEntries.filter((e) => e.status === 'completed').length;
  const mealStatus = recordedMealCount > 0
    ? (recordedMealCount >= 3 ? 'completed' : 'in-progress')
    : 'pending';
  const mealTypeSet = todayMealTypes;
  let checkInStatus: 'completed' | 'in-progress' | 'pending' = 'pending';
  if (mealTypeSet.has('BREAKFAST') && mealTypeSet.has('LUNCH') && mealTypeSet.has('DINNER')) {
    checkInStatus = 'completed';
  } else if (todayMeals.length > 0) {
    checkInStatus = 'in-progress';
  }
  const checkInStatusLabel = checkInStatus === 'completed' ? '已完成' : checkInStatus === 'in-progress' ? '未完成' : '待开始';

  // 最近三条餐食记录（用于"打卡"节点详情）
  const recentMealLines = recentMeals.slice(0, 3).map((m) => {
    const label = getMealLabel(m.mealType || '');
    const kcal = m.totalCalories != null ? `${Math.round(m.totalCalories)} kcal` : '';
    const date = m.mealTime ? m.mealTime.split('T')[0] : '';
    return `${date} ${label}${kcal ? ' - ' + kcal : ''}`;
  });

  // 健康档案状态
  const profileFilled = !!(userProfile?.heightCm);
  const profileStatusLabel = profileFilled ? '已完成' : '未填写';
  const profileStatus: 'completed' | 'pending' = profileFilled ? 'completed' : 'pending';

  const nodes: TimelineItem[] = [
    {
      id: 1,
      title: '记录膳食',
      date: new Date().toLocaleDateString('zh-CN'),
      description: '早/中/晚/加餐统一入口，点击切换到对应餐种详情。',
      category: '膳食',
      icon: UtensilsCrossed,
      relatedIds: [2, 3, 4],
      status: mealStatus,
      energy: calorieContrib,
      mealEntries,
      entryHref: '/meals/breakfast',
      hideCategoryBadge: true,
    },
    {
      id: 2,
      title: '打卡',
      date: new Date().toLocaleDateString('zh-CN'),
      description: '记录三餐即完成今日打卡，可在详情页查看历史记录。',
      category: '打卡',
      icon: CheckSquare,
      relatedIds: [1],
      status: checkInStatus,
      customStatusLabel: checkInStatusLabel,
      energy: 0,
      checkInHistory: recentMealLines,
      entryHref: '/checkin',
      hideCategoryBadge: true,
      hideEnergy: true,
    },
    {
      id: 3,
      title: '健康报告',
      date: '-',
      description: '查看每日营养趋势与健康分析。',
      category: '功能',
      icon: FileText,
      relatedIds: [4],
      status: 'pending',
      energy: 0,
      entryHref: '/reports/health',
      hideCategoryBadge: true,
      hideStatusBadge: true,
      hideEnergy: true,
    },
    {
      id: 4,
      title: '健康档案',
      date: '-',
      description: profileFilled ? '您的基础健康信息已同步。' : '尚未完善健康档案，请前往填写。',
      category: '健康档案',
      icon: Activity,
      relatedIds: [3],
      status: profileStatus,
      customStatusLabel: profileStatusLabel,
      energy: 0,
      details: userProfile
        ? [
            { label: '身高', value: `${userProfile.heightCm ?? '-'} cm` },
            { label: '体重', value: `${userProfile.weightKg ?? '-'} kg` },
            { label: '目标', value: HEALTH_GOAL_LABELS[userProfile.healthGoal ?? ''] ?? userProfile.healthGoal ?? '-' },
            { label: '活动', value: ACTIVITY_LABELS[userProfile.activityLevel ?? ''] ?? userProfile.activityLevel ?? '-' },
          ]
        : [],
      entryHref: '/profile',
      hideCategoryBadge: true,
      hideEnergy: true,
    },
    {
      id: 5,
      title: '附近餐厅',
      date: '-',
      description: '查看附近健康餐厅与推荐菜品。',
      category: '功能',
      icon: MapPin,
      relatedIds: [1],
      status: 'pending',
      energy: 0,
      entryHref: '/restaurants/nearby',
      hideCategoryBadge: true,
      hideStatusBadge: true,
      hideEnergy: true,
    },
  ];

  return nodes;
}

export default function HomePage() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [nickname, setNickname] = useState('');
  const [healthColor, setHealthColor] = useState('16, 185, 129');

  const [timelineData, setTimelineData] = useState<TimelineItem[]>([]);
  const [calories, setCalories] = useState(0);
  const [goalCalories, setGoalCalories] = useState(2000);
  const [macros, setMacros] = useState<{ protein: { current: number; goal: number }; fat: { current: number; goal: number }; carb: { current: number; goal: number } } | undefined>(undefined);
  const [waterIntakeMl, setWaterIntakeMl] = useState(0);
  const [suggestions, setSuggestions] = useState<{ name: string; calories: number; restaurantName?: string; restaurantAddress?: string }[]>([]);
  const [mealChecks, setMealChecks] = useState<MealCheckItem[]>([]);
  const [planExpired, setPlanExpired] = useState(false);
  const [planSummary, setPlanSummary] = useState<{ checkInDays: number; completionRate: number } | null>(null);
  const [planGoalLabel, setPlanGoalLabel] = useState('');

  const nextMealLabel = useMemo(() => getNextMealLabel(), []);

  useEffect(() => {
    const auth = getAuth();
    if (!auth) {
      setIsLoggedIn(false);
      return;
    }

    setIsLoggedIn(true);

    Promise.all([
      fetchUser(auth.userId).catch(() => null),
      fetchUserProfile(auth.userId).catch(() => null),
      fetchTodayNutrition(auth.userId).catch(() => null),
      fetchCheckIns(auth.userId).catch(() => null),
      fetchRecommendations(auth.userId).catch(() => null),
      fetchRecentMeals(auth.userId, 50).catch(() => null),
    ]).then(async ([userRes, profileRes, nutritionRes, checkInsRes, recRes, recentMealsRes]) => {
      const user = userRes?.data;
      const nutrition = nutritionRes?.data;
      const checkIns: CheckIn[] = checkInsRes?.data ?? [];
      const recommendations: Recommendation[] = recRes?.data ?? [];
      const recentMeals: RecentMeal[] = recentMealsRes?.data ?? [];

      const profile: UserProfile | null = profileRes?.data ?? null;
      setNickname(user?.nickname || user?.username || '用户');

      const todayCalories = nutrition
        ? Math.round((nutrition.protein ?? 0) * 4 + (nutrition.fat ?? 0) * 9 + (nutrition.carb ?? 0) * 4)
        : 0;
      // 用宏量目标推算热量目标，确保三项营养素达标时热量也达标
      const pGoal = profile?.dailyProteinTarget ?? 0;
      const fGoal = profile?.dailyFatTarget ?? 0;
      const cGoal = profile?.dailyCarbTarget ?? 0;
      const macroDerived = Math.round(pGoal * 4 + fGoal * 9 + cGoal * 4);
      const targetCalories = macroDerived > 0 ? macroDerived : (profile?.dailyCalorieTarget ?? 2000);
      setCalories(todayCalories);
      setGoalCalories(targetCalories);

      // 宏量数据
      setMacros({
        protein: { current: nutrition?.protein ?? 0, goal: profile?.dailyProteinTarget ?? 60 },
        fat:     { current: nutrition?.fat     ?? 0, goal: profile?.dailyFatTarget     ?? 60 },
        carb:    { current: nutrition?.carb    ?? 0, goal: profile?.dailyCarbTarget    ?? 250 },
      });
      setWaterIntakeMl(nutrition?.waterIntakeMl ?? 0);

      // 获取推荐中涉及的餐厅信息
      const uniqueRestaurantIds = [...new Set(
        recommendations.slice(0, 4).map((r) => r.restaurantId).filter((id): id is number => !!id)
      )];
      const restaurantMap: Record<number, RestaurantInfo> = {};
      if (uniqueRestaurantIds.length > 0) {
        await Promise.all(
          uniqueRestaurantIds.map((id) =>
            fetchRestaurant(id).then((r) => {
              if (r?.data) restaurantMap[id] = r.data;
            }).catch(() => {})
          )
        );
      }

      const recSuggestions = recommendations.slice(0, 4).map((item) => {
        const rest = item.restaurantId ? restaurantMap[item.restaurantId] : undefined;
        return {
          name: item.foodItemName ?? '推荐菜品',
          calories: item.totalCalories ?? 0,
          restaurantName: rest?.name,
          restaurantAddress: rest?.address,
        };
      });
      setSuggestions(recSuggestions);

      const timeline = buildTimelineData(recommendations, checkIns, profile, recentMeals, todayCalories, targetCalories);
      setTimelineData(timeline);
      setMealChecks(buildMealChecks(recentMeals));

      const score = computeHealthScore(
        { calories: todayCalories, protein: nutrition?.protein ?? 0, fat: nutrition?.fat ?? 0, carb: nutrition?.carb ?? 0 },
        { dailyCalorieTarget: targetCalories, dailyProteinTarget: profile?.dailyProteinTarget, dailyFatTarget: profile?.dailyFatTarget, dailyCarbTarget: profile?.dailyCarbTarget },
      );
      setHealthColor(score.color);

      // 检测计划是否到期
      if (profile?.planEndDate) {
        const today = new Date().toISOString().slice(0, 10);
        if (today >= profile.planEndDate) {
          setPlanExpired(true);
          // 计算上一个周期摘要
          const totalCheckIns = checkIns.length;
          const pG = profile.dailyProteinTarget ?? 0;
          const fG = profile.dailyFatTarget ?? 0;
          const cG = profile.dailyCarbTarget ?? 0;
          const goodDays = checkIns.filter((ci: { totalProtein?: number; totalFat?: number; totalCarb?: number }) => {
            const pOk = !pG || Math.abs((ci.totalProtein ?? 0) - pG) / pG <= 0.15;
            const fOk = !fG || Math.abs((ci.totalFat ?? 0) - fG) / fG <= 0.15;
            const cOk = !cG || Math.abs((ci.totalCarb ?? 0) - cG) / cG <= 0.15;
            return pOk && fOk && cOk;
          }).length;
          setPlanSummary({
            checkInDays: totalCheckIns,
            completionRate: totalCheckIns > 0 ? Math.round((goodDays / totalCheckIns) * 100) : 0,
          });
          const _goalMap: Record<string, string> = {
            RAPID_FAT_LOSS: '极速减脂', HIGH_INTENSITY_FAT_LOSS: '强力减脂', DAILY_FAT_LOSS: '日常减脂',
            LEAN_BULK: '精益增肌', BULK: '增重增肌', MAINTAIN: '维持体重',
            INCREASE_STRENGTH: '提升力量', IMPROVE_PERFORMANCE: '提升表现',
          };
          setPlanGoalLabel(_goalMap[profile?.healthGoal || ''] || '');
        }
      }
    });
  }, []);

  const handleLogout = () => {
    clearAuth();
    setIsLoggedIn(false);
    router.push('/login');
  };

  const handleAddWater = async (addedMl: number) => {
    const auth = getAuth();
    if (!auth) return;
    setWaterIntakeMl((prev) => prev + addedMl);
    try {
      await addWaterIntake(auth.userId, addedMl);
    } catch {
      // 乐观更新回滚
      setWaterIntakeMl((prev) => prev - addedMl);
    }
  };

  const avatarLetter = (nickname.charAt(0) || 'U').toUpperCase();

  // 计划到期卡片内容（桌面/移动复用）
  const PlanExpiredCard = () => (
    <div className="rounded-2xl overflow-hidden text-white"
      style={{
        background: 'rgba(4, 14, 9, 0.55)',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
      }}>
      <div className="p-5">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="flex-shrink-0 w-11 h-11 rounded-xl flex items-center justify-center text-white font-semibold text-sm"
              style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}>
              {planGoalLabel ? planGoalLabel.slice(0, 2) : '饮食'}
            </div>
            <div>
              <h3 className="text-white font-medium text-base">计划周期已结束</h3>
              <p className="text-sm mt-0.5" style={{ color: 'rgba(255,255,255,0.45)' }}>
                {planGoalLabel || '健康计划'}
              </p>
            </div>
          </div>
          <span className="text-xs px-2.5 py-0.5 rounded-md font-medium"
            style={{ background: 'rgba(52,211,153,0.15)', color: '#34d399', border: '1px solid rgba(52,211,153,0.3)' }}>
            已完成
          </span>
        </div>
        {planSummary && (
          <p className="leading-relaxed text-sm mb-4" style={{ color: 'rgba(255,255,255,0.75)' }}>
            &ldquo;本周期共坚持打卡 <span className="text-white font-semibold">{planSummary.checkInDays}</span> 天，
            三宏平均达标率 <span className="font-semibold" style={{ color: '#34d399' }}>{planSummary.completionRate}%</span>，
            继续保持，开启下一阶段！&rdquo;
          </p>
        )}
        <div className="flex flex-col gap-3 border-t pt-3" style={{ borderColor: 'rgba(255,255,255,0.08)' }}>
          <div className="flex items-center justify-between">
            <div className="flex gap-1.5">
              <span className="text-xs px-2 py-0.5 rounded-md"
                style={{ background: 'rgba(52,211,153,0.15)', color: '#34d399', border: '1px solid rgba(52,211,153,0.25)' }}>
                计划完成
              </span>
              {planGoalLabel && (
                <span className="text-xs px-2 py-0.5 rounded-md"
                  style={{ background: 'rgba(255,255,255,0.06)', color: 'rgba(255,255,255,0.45)' }}>
                  {planGoalLabel}
                </span>
              )}
            </div>
            {planSummary && (
              <div className="flex items-center gap-3 text-xs" style={{ color: 'rgba(255,255,255,0.4)' }}>
                <span className="flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5" />
                  {planSummary.checkInDays}天
                </span>
                <span className="flex items-center gap-1">
                  <TrendingUp className="w-3.5 h-3.5" />
                  {planSummary.completionRate}%
                </span>
              </div>
            )}
          </div>
          <a href="/onboarding"
            className="block w-full text-center rounded-xl py-2 text-sm font-semibold text-white transition"
            style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}>
            制定新计划
          </a>
        </div>
      </div>
    </div>
  );

  return (
    <BackgroundGradientAnimation healthColor={healthColor} interactive>
      <div className="absolute inset-0 z-10">
        <MainNavbar isLoggedIn={isLoggedIn} nickname={nickname} avatarLetter={avatarLetter} onLogout={handleLogout} />

        {/* ── 大屏布局 (xl+): 居中时间轴 + 两侧面板 ── */}
        <div className="absolute inset-0 z-20 hidden xl:flex items-center justify-center pointer-events-none">
          <div className="w-full h-full pt-20 pointer-events-auto">
            <RadialOrbitalTimeline timelineData={isLoggedIn ? timelineData : null} />
          </div>
        </div>

        {isLoggedIn ? (
          <>
            {/* 计划到期提示卡片 - 仅大屏 */}
            {planExpired && (
              <div className="hidden xl:block absolute top-24 left-1/2 -translate-x-1/2 z-40 w-full max-w-sm px-4">
                <PlanExpiredCard />
              </div>
            )}
            {/* 左侧推荐面板 - 可折叠，仅大屏绝对定位 */}
            <CollapsiblePanelDesktop
              side="left"
              label="饮食推荐"
              icon={<Sparkles size={16} />}
            >
              {(collapseBtn) => (
                <RecommendationPanel
                  nextMealLabel={nextMealLabel}
                  items={suggestions}
                  collapseButton={collapseBtn}
                />
              )}
            </CollapsiblePanelDesktop>
            {/* 右侧热量追踪 - 可折叠，仅大屏绝对定位 */}
            <CollapsiblePanelDesktop
              side="right"
              label="今日热量"
              icon={<Flame size={16} />}
            >
              {(collapseBtn) => (
                <CalorieTrackerCard
                  currentCalories={calories}
                  goalCalories={goalCalories}
                  macros={macros}
                  waterIntakeMl={waterIntakeMl}
                  onAddWater={handleAddWater}
                  suggestions={suggestions}
                  mealChecks={mealChecks}
                  streak={0}
                  collapseButton={collapseBtn}
                />
              )}
            </CollapsiblePanelDesktop>
          </>
        ) : null}

        {!isLoggedIn ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="absolute inset-0 z-40 flex items-center justify-center"
            style={{ background: 'rgba(0,0,0,0.35)', backdropFilter: 'blur(2px)' }}
          >
            <div className="text-center space-y-4 px-4">
              <p className="text-2xl font-bold text-white/90">欢迎来到 食乜</p>
              <p className="text-white/50 text-sm">登录后查看您的个人健康数据与饮食记录</p>
              <div className="flex items-center gap-3 justify-center mt-6">
                <a href="/login" className="px-6 py-2.5 rounded-xl bg-emerald-500 text-white text-sm font-semibold hover:bg-emerald-400 transition-all">
                  立即登录
                </a>
                <a href="/register" className="px-6 py-2.5 rounded-xl border border-white/20 text-white/70 text-sm hover:bg-white/5 transition-all">
                  免费注册
                </a>
              </div>
            </div>
          </motion.div>
        ) : null}

        {/* ── 小屏/平板布局 (<xl): 可滚动竖排 ── */}
        <div className="xl:hidden absolute inset-x-0 top-20 bottom-0 overflow-y-auto z-20">
          {isLoggedIn && (
            <div className="flex flex-col gap-4 pb-8">
              {/* 时间轴 */}
              <div className="w-full h-[520px] flex-shrink-0 pointer-events-auto relative z-10">
                <RadialOrbitalTimeline timelineData={timelineData} />
              </div>
              {/* 内容卡片区 */}
              <div className="px-4 flex flex-col gap-4">
                {planExpired && <PlanExpiredCard />}
                {/* 可折叠推荐面板 */}
                <CollapsiblePanelMobile
                  label="饮食推荐"
                  icon={<Sparkles size={16} className="text-emerald-400" />}
                >
                  <RecommendationPanel
                    nextMealLabel={nextMealLabel}
                    items={suggestions}
                    className="w-full rounded-none border-0 bg-transparent backdrop-blur-none shadow-none"
                  />
                </CollapsiblePanelMobile>
                {/* 可折叠热量追踪卡片 */}
                <CollapsiblePanelMobile
                  label="今日热量"
                  icon={<Flame size={16} className="text-orange-400" />}
                >
                  <CalorieTrackerCard
                    currentCalories={calories}
                    goalCalories={goalCalories}
                    macros={macros}
                    waterIntakeMl={waterIntakeMl}
                    onAddWater={handleAddWater}
                    suggestions={suggestions}
                    mealChecks={mealChecks}
                    streak={0}
                    className="w-full rounded-none border-0 bg-transparent backdrop-blur-none shadow-none"
                  />
                </CollapsiblePanelMobile>
              </div>
            </div>
          )}
        </div>
      </div>
    </BackgroundGradientAnimation>
  );
}
