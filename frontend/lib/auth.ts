export type AuthInfo = { token: string; userId: number };

export function getAuth(): AuthInfo | null {
  if (typeof window === 'undefined') return null;
  try {
    const data = localStorage.getItem('javadiet_auth');
    return data ? (JSON.parse(data) as AuthInfo) : null;
  } catch {
    return null;
  }
}

export function saveAuth(info: AuthInfo) {
  if (typeof window === 'undefined') return;
  localStorage.setItem('javadiet_auth', JSON.stringify(info));
}

export function clearAuth() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('javadiet_auth');
}
