'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ChevronLeft, MapPin, Navigation2, RefreshCw,
  Building2, Clock, Map, AlertCircle,
} from 'lucide-react';

import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { PageCard } from '@/components/ui/page-card';
import { getAuth, clearAuth } from '@/lib/auth';
import { fetchUser, fetchNearbyRestaurants } from '@/lib/api';

type Restaurant = {
  id: number;
  name?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  openTime?: string;
  closeTime?: string;
  isOpen?: boolean;
  distanceKm?: number;
};

type LocationState = 'idle' | 'requesting' | 'granted' | 'denied' | 'error';

export default function NearbyRestaurantsPage() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [nickname, setNickname] = useState('');

  const [locationState, setLocationState] = useState<LocationState>('idle');
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const auth = getAuth();
    if (auth?.userId) {
      setIsLoggedIn(true);
      fetchUser(auth.userId).then(d => setNickname(d.data?.nickname || d.nickname || ''));
    }
  }, []);

  function handleLogout() { clearAuth(); router.push('/login'); }

  function requestLocation() {
    if (!navigator.geolocation) {
      setLocationState('error');
      setError('当前浏览器不支持地理定位');
      return;
    }
    setLocationState('requesting');
    navigator.geolocation.getCurrentPosition(
      pos => {
        const { latitude, longitude } = pos.coords;
        setCoords({ lat: latitude, lng: longitude });
        setLocationState('granted');
        loadNearby(latitude, longitude);
      },
      () => {
        setLocationState('denied');
        setError('未获取到位置权限，请在浏览器中允许定位');
      },
      { timeout: 10000 },
    );
  }

  async function loadNearby(lat: number, lng: number) {
    setLoading(true);
    setError('');
    try {
      const d = await fetchNearbyRestaurants({ latitude: lat, longitude: lng, maxDistanceKm: 5 });
      setRestaurants(Array.isArray(d) ? d : (d.data ?? []));
    } catch {
      setError('加载附近餐厅失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }

  function handleRefresh() {
    if (coords) loadNearby(coords.lat, coords.lng);
    else requestLocation();
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

        <main className="flex-1 px-4 py-6 pt-24 max-w-3xl mx-auto w-full">
          {/* 页头 */}
          <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="mb-6">
            <div className="flex items-center gap-3 mb-1">
              <button onClick={() => router.back()} className="text-white/40 hover:text-white/70 transition">
                <ChevronLeft size={18} />
              </button>
              <h1 className="text-2xl font-bold text-white">附近餐厅</h1>
            </div>
            <p className="text-sm text-white/50 ml-7">发现你周边的健康饮食选择</p>
          </motion.div>

          {/* 地图占位区 */}
          <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="mb-5">
            <div className="relative w-full h-52 rounded-2xl border border-white/10 bg-white/[0.03] overflow-hidden flex flex-col items-center justify-center gap-3">
              {/* 网格背景 */}
              <div className="absolute inset-0 opacity-[0.04]" style={{
                backgroundImage: 'linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.5) 1px, transparent 1px)',
                backgroundSize: '32px 32px',
              }} />
              {/* 装饰圆 */}
              <div className="absolute w-32 h-32 rounded-full border border-emerald-500/10 animate-ping" style={{ animationDuration: '3s' }} />
              <div className="absolute w-20 h-20 rounded-full border border-emerald-500/15" />
              <div className="absolute w-3 h-3 rounded-full bg-emerald-400/60" />

              <Map size={28} className="relative text-white/20" />
              <div className="relative text-center">
                <p className="text-sm font-medium text-white/50">地图服务接入中</p>
                <p className="text-xs text-white/30 mt-0.5">后续将集成高德地图 / Google Maps API</p>
              </div>

              {coords && (
                <div className="relative flex items-center gap-1.5 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs text-emerald-300">
                  <MapPin size={11} />
                  {coords.lat.toFixed(4)}, {coords.lng.toFixed(4)}
                </div>
              )}
            </div>
          </motion.div>

          {/* 操作栏 */}
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.15 }} className="flex items-center gap-3 mb-5">
            {locationState !== 'granted' ? (
              <button
                onClick={requestLocation}
                disabled={locationState === 'requesting'}
                className="flex items-center gap-2 rounded-xl border border-emerald-500/40 bg-emerald-500/15 px-4 py-2.5 text-sm font-semibold text-emerald-300 hover:bg-emerald-500/25 disabled:opacity-50 transition"
              >
                {locationState === 'requesting'
                  ? <><RefreshCw size={14} className="animate-spin" />定位中...</>
                  : <><Navigation2 size={14} />获取我的位置</>
                }
              </button>
            ) : (
              <button
                onClick={handleRefresh}
                disabled={loading}
                className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-white/60 hover:bg-white/10 disabled:opacity-50 transition"
              >
                <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
                刷新
              </button>
            )}
            {locationState === 'granted' && (
              <span className="flex items-center gap-1 text-xs text-emerald-400">
                <MapPin size={11} />已定位 · 显示5km内
              </span>
            )}
          </motion.div>

          {/* 错误提示 */}
          <AnimatePresence>
            {(locationState === 'denied' || locationState === 'error' || error) && (
              <motion.div
                initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}
                className="flex items-start gap-2 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 mb-5 text-sm text-red-300"
              >
                <AlertCircle size={16} className="mt-0.5 flex-shrink-0" />
                <p>{error || '位置权限被拒绝，请在浏览器设置中允许定位后刷新页面'}</p>
              </motion.div>
            )}
          </AnimatePresence>

          {/* 餐厅列表 */}
          {locationState === 'granted' && (
            <div className="space-y-3">
              <p className="text-xs text-white/40 uppercase tracking-wider px-1">
                {loading ? '搜索中...' : `附近餐厅 · ${restaurants.length} 家`}
              </p>

              {loading && (
                <div className="flex justify-center py-10">
                  <RefreshCw size={20} className="animate-spin text-white/30" />
                </div>
              )}

              {!loading && restaurants.length === 0 && (
                <div className="text-center py-12 text-white/30">
                  <Building2 size={32} className="mx-auto mb-3 opacity-40" />
                  <p className="text-sm">附近暂无餐厅数据</p>
                  <p className="text-xs mt-1 text-white/20">餐厅数据由管理员录入或通过抓取工具导入</p>
                </div>
              )}

              {!loading && restaurants.map((r, i) => (
                <motion.div
                  key={r.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  whileHover={{ scale: 1.01, y: -2, transition: { duration: 0.18 } }}
                  transition={{ delay: i * 0.05 }}
                  className="rounded-2xl border border-white/10 bg-white/[0.04] p-4 hover:bg-white/[0.08] hover:border-white/15 transition-colors cursor-default"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center flex-shrink-0">
                        <Building2 size={18} className="text-emerald-400" />
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-white">{r.name ?? '未命名餐厅'}</p>
                        {r.address && (
                          <p className="text-xs text-white/45 flex items-center gap-1 mt-0.5">
                            <MapPin size={10} />{r.address}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="text-right flex-shrink-0">
                      {r.distanceKm != null && (
                        <p className="text-xs font-mono text-emerald-300">{r.distanceKm.toFixed(1)} km</p>
                      )}
                      {r.isOpen != null && (
                        <span className={`text-[10px] rounded-full px-2 py-0.5 ${r.isOpen ? 'bg-emerald-500/15 text-emerald-300' : 'bg-white/10 text-white/40'}`}>
                          {r.isOpen ? '营业中' : '已关闭'}
                        </span>
                      )}
                    </div>
                  </div>
                  {(r.openTime || r.closeTime) && (
                    <p className="mt-2 text-xs text-white/35 flex items-center gap-1">
                      <Clock size={10} />{r.openTime ?? '--'} ~ {r.closeTime ?? '--'}
                    </p>
                  )}
                </motion.div>
              ))}
            </div>
          )}

          {/* 未定位时的引导 */}
          {locationState === 'idle' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }}
              className="text-center py-16 text-white/25"
            >
              <Navigation2 size={36} className="mx-auto mb-4 opacity-40" />
              <p className="text-sm">点击"获取我的位置"开始搜索附近餐厅</p>
            </motion.div>
          )}
        </main>
      </div>
    </div>
  );
}
