// 健康评分工具 — 根据今日营养摄入状态计算评分和背景光效颜色

export interface NutritionData {
  calories?: number;
  protein?: number;
  fat?: number;
  carb?: number;
}

export interface ProfileTargets {
  dailyCalorieTarget?: number;
  dailyProteinTarget?: number;
  dailyFatTarget?: number;
  dailyCarbTarget?: number;
}

export interface HealthScore {
  score: number;   // 0 (不健康/红) → 1 (健康/绿)
  color: string;   // RGB 字符串，传入 BackgroundGradientAnimation 的 firstColor
}

// 把 ratio 偏差映射到 0~1 分数：完美 ratio=1 → score=1，偏差越大分越低
function ratioScore(consumed: number, goal: number): number | null {
  if (!goal || goal <= 0) return null;
  const ratio = consumed / goal;
  // 线性衰减：|ratio-1| 超过 0.5 时分数为 0
  return Math.max(0, 1 - Math.abs(ratio - 1) * 2);
}

// 根据加权分数返回 RGB 颜色字符串
function scoreToColor(score: number): string {
  if (score >= 0.7) return '16, 185, 129';   // 绿 — emerald-500
  if (score >= 0.4) return '249, 115, 22';    // 橙 — orange-500
  return '239, 68, 68';                        // 红 — red-500
}

export function computeHealthScore(
  nutrition: NutritionData | null,
  profile: ProfileTargets | null,
): HealthScore {
  if (!nutrition || !profile) {
    // 数据未加载时显示中性绿
    return { score: 1, color: '16, 185, 129' };
  }

  const scores: number[] = [];

  // 热量贡献权重最高（占 60%）
  const calScore = ratioScore(nutrition.calories ?? 0, profile.dailyCalorieTarget ?? 0);
  if (calScore !== null) {
    scores.push(calScore * 0.6);
    scores.push(0); // 占位，保证权重比例
  }

  // 蛋白质 20%
  const pScore = ratioScore(nutrition.protein ?? 0, profile.dailyProteinTarget ?? 0);
  if (pScore !== null) scores.push(pScore * 0.2);

  // 脂肪 10%
  const fScore = ratioScore(nutrition.fat ?? 0, profile.dailyFatTarget ?? 0);
  if (fScore !== null) scores.push(fScore * 0.1);

  // 碳水 10%
  const cScore = ratioScore(nutrition.carb ?? 0, profile.dailyCarbTarget ?? 0);
  if (cScore !== null) scores.push(cScore * 0.1);

  // 如果没有任何有效数据（目标全为 0），返回中性
  if (scores.length === 0) return { score: 1, color: '16, 185, 129' };

  // 目标全 0 但热量已填时，只用热量
  const finalScore = calScore !== null
    ? calScore  // 直接用热量得分来决定颜色（最直观）
    : scores.reduce((a, b) => a + b, 0);

  return { score: finalScore, color: scoreToColor(finalScore) };
}

