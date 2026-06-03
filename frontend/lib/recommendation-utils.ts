import type { RecommendationResponse } from './api';

export const RECOMMENDATION_MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'] as const;

function localDateKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function recommendationTime(rec: RecommendationResponse) {
  return rec.createdAt ?? rec.recommendationTime ?? '';
}

function mealSortIndex(mealType?: string) {
  const index = RECOMMENDATION_MEAL_TYPES.indexOf(mealType as (typeof RECOMMENDATION_MEAL_TYPES)[number]);
  return index < 0 ? RECOMMENDATION_MEAL_TYPES.length : index;
}

export function isTodayRecommendation(rec: RecommendationResponse) {
  const time = recommendationTime(rec);
  return time.startsWith(localDateKey());
}

export function isActiveRecommendation(rec: RecommendationResponse) {
  return rec.status !== 'IGNORED';
}

export function latestRecommendationsByMeal(recommendations: RecommendationResponse[], perMeal = 4) {
  const sorted = recommendations
    .filter((rec) => isTodayRecommendation(rec) && isActiveRecommendation(rec))
    .slice()
    .sort((a, b) => {
      const bt = recommendationTime(b);
      const at = recommendationTime(a);
      if (bt !== at) return bt.localeCompare(at);
      return (b.id ?? 0) - (a.id ?? 0);
    });

  const grouped: Record<string, RecommendationResponse[]> = {};
  for (const rec of sorted) {
    const mealType = rec.mealType ?? 'UNKNOWN';
    grouped[mealType] = grouped[mealType] ?? [];
    if (grouped[mealType].length < perMeal) {
      grouped[mealType].push(rec);
    }
  }

  return RECOMMENDATION_MEAL_TYPES.flatMap((meal) => grouped[meal] ?? []);
}

export function sortRecommendationsForDisplay(recommendations: RecommendationResponse[]) {
  return recommendations
    .filter(isActiveRecommendation)
    .slice()
    .sort((a, b) => {
      const mealDelta = mealSortIndex(a.mealType) - mealSortIndex(b.mealType);
      if (mealDelta !== 0) return mealDelta;
      const at = recommendationTime(a);
      const bt = recommendationTime(b);
      if (at !== bt) return bt.localeCompare(at);
      return (a.id ?? 0) - (b.id ?? 0);
    });
}

export function groupRecommendationsByMeal(recommendations: RecommendationResponse[]) {
  const grouped: Record<string, RecommendationResponse[]> = {};
  for (const mealType of RECOMMENDATION_MEAL_TYPES) {
    grouped[mealType] = [];
  }
  for (const recommendation of sortRecommendationsForDisplay(recommendations)) {
    const mealType = recommendation.mealType ?? 'UNKNOWN';
    grouped[mealType] = grouped[mealType] ?? [];
    grouped[mealType].push(recommendation);
  }
  return grouped;
}
