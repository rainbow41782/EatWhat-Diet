import { getAuth } from './auth';

const BASE = process.env.NEXT_PUBLIC_API_BASE || '';

function todayStr() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

async function apiFetch(path: string, options: RequestInit = {}) {
  const auth = getAuth();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };
  if (auth?.token) {
    headers['Authorization'] = `Bearer ${auth.token}`;
  }
  return fetch(`${BASE}${path}`, { ...options, headers });
}

export async function fetchUser(userId: number) {
  const res = await apiFetch(`/api/users/${userId}`);
  return res.json();
}

export async function fetchUserProfile(userId: number) {
  const res = await apiFetch(`/api/users/${userId}/profile`);
  return res.json();
}

export async function fetchTodayNutrition(userId: number) {
  const res = await apiFetch(`/api/diet/users/${userId}/nutrition?checkDate=${todayStr()}`);
  return res.json();
}

export async function fetchCheckIns(userId: number) {
  const res = await apiFetch(`/api/diet/users/${userId}/check-ins`);
  return res.json();
}

export async function doCheckIn(userId: number) {
  const res = await apiFetch(`/api/diet/users/${userId}/check-ins?checkDate=${todayStr()}`, {
    method: 'POST',
  });
  return res.json();
}

export async function fetchRecommendations(userId: number) {
  const res = await apiFetch(`/api/recommendations?userId=${userId}`);
  return res.json();
}

export async function updateUserProfile(userId: number, profile: Record<string, unknown>) {
  const res = await apiFetch(`/api/users/${userId}/profile`, {
    method: 'PATCH',
    body: JSON.stringify(profile),
  });
  return res.json();
}

export async function updateUserBasic(userId: number, basic: Record<string, unknown>) {
  const res = await apiFetch(`/api/users/${userId}/basic`, {
    method: 'PATCH',
    body: JSON.stringify(basic),
  });
  return res.json();
}

export async function changeUserPassword(userId: number, oldPassword: string, newPassword: string) {
  const res = await apiFetch(`/api/users/${userId}/password`, {
    method: 'PATCH',
    body: JSON.stringify({ oldPassword, newPassword }),
  });
  return res.json();
}

export async function addWaterIntake(userId: number, waterMl: number) {
  const res = await apiFetch(`/api/diet/users/${userId}/water`, {
    method: 'PATCH',
    body: JSON.stringify({ waterMl }),
  });
  return res.json();
}

export async function fetchFoods(keyword?: string, category?: string) {
  const params = new URLSearchParams();
  if (keyword) params.set('keyword', keyword);
  if (category) params.set('category', category);
  const query = params.toString();
  const res = await apiFetch(`/api/foods${query ? '?' + query : ''}`);
  return res.json();
}

export async function createMealRecord(userId: number, body: Record<string, unknown>) {
  const res = await apiFetch(`/api/diet/users/${userId}/meals`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function addFoodIntake(userId: number, body: Record<string, unknown>) {
  const res = await apiFetch(`/api/diet/users/${userId}/intakes`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function fetchRecentMeals(userId: number, limit = 5) {
  const res = await apiFetch(`/api/diet/users/${userId}/recent-meals?limit=${limit}`);
  return res.json();
}

export async function fetchMealIntakes(userId: number, mealId: number) {
  const res = await apiFetch(`/api/diet/users/${userId}/meals/${mealId}/intakes`);
  return res.json();
}

export async function updateFoodIntake(
  userId: number,
  intakeId: number,
  body: { remark?: string; calories?: number; protein?: number; fat?: number; carb?: number },
) {
  const res = await apiFetch(`/api/diet/users/${userId}/intakes/${intakeId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function deleteFoodIntake(userId: number, intakeId: number) {
  const res = await apiFetch(`/api/diet/users/${userId}/intakes/${intakeId}`, { method: 'DELETE' });
  return res.json();
}

export async function deleteMealRecord(userId: number, mealId: number) {
  const res = await apiFetch(`/api/diet/users/${userId}/meals/${mealId}`, { method: 'DELETE' });
  return res.json();
}

export async function fetchRestaurant(restaurantId: number) {
  const res = await apiFetch(`/api/restaurants/${restaurantId}`);
  return res.json();
}

export async function fetchBodyMeasurements(userId: number) {
  const res = await apiFetch(`/api/body-measurements?userId=${userId}`);
  return res.json();
}

export async function addBodyMeasurement(userId: number, body: {
  measureDate?: string;
  weightKg?: number | null;
  bodyFatPct?: number | null;
  waistCm?: number | null;
  hipCm?: number | null;
  armCm?: number | null;
}) {
  const res = await apiFetch(`/api/body-measurements/${userId}`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function generateRecommendations(body: {
  userId: number;
  targetMealType?: string;
  maxBudget?: number;
  maxDistanceKm?: number;
}) {
  const res = await apiFetch('/api/recommendations', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function acceptRecommendation(recommendationId: number) {
  const res = await apiFetch(`/api/recommendations/${recommendationId}/accept`, { method: 'PATCH' });
  return res.json();
}

export async function ignoreRecommendation(recommendationId: number) {
  const res = await apiFetch(`/api/recommendations/${recommendationId}/ignore`, { method: 'PATCH' });
  return res.json();
}

export async function submitFeedback(body: {
  userId?: number;
  recommendationId?: number;
  rating?: number;
  feedbackType: string;
  content: string;
  useful?: boolean;
}) {
  const res = await apiFetch('/api/feedback', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function fetchNearbyRestaurants(params: {
  latitude: number;
  longitude: number;
  maxDistanceKm?: number;
  onlyOpen?: boolean;
}) {
  const q = new URLSearchParams({
    latitude: String(params.latitude),
    longitude: String(params.longitude),
    ...(params.maxDistanceKm != null ? { maxDistanceKm: String(params.maxDistanceKm) } : {}),
    onlyOpen: String(params.onlyOpen ?? true),
  });
  const res = await apiFetch(`/api/restaurants/nearby?${q}`);
  return res.json();
}
