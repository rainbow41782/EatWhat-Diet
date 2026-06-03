'use client';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useRouter } from 'next/router';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ChevronLeft, MapPin, Navigation2, RefreshCw,
  Building2, Clock, AlertCircle,
} from 'lucide-react';

import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';
import { MainNavbar } from '@/components/ui/main-navbar';
import { PageCard } from '@/components/ui/page-card';
import { getAuth, clearAuth } from '@/lib/auth';
import { fetchUser, fetchNearbyRestaurants, fetchTencentIpLocation } from '@/lib/api';

declare global {
  interface Window {
    TMap?: any;
  }
}

const TENCENT_MAP_KEY = process.env.NEXT_PUBLIC_TENCENT_MAP_KEY || '';

let tencentMapScriptPromise: Promise<void> | null = null;

function ensureTencentMapLoaded(key: string): Promise<void> {
  if (typeof window === 'undefined') return Promise.reject(new Error('window unavailable'));
  if (window.TMap) return Promise.resolve();
  if (!key) return Promise.reject(new Error('missing key'));
  if (tencentMapScriptPromise) return tencentMapScriptPromise;

  tencentMapScriptPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById('tencent-map-gl') as HTMLScriptElement | null;
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(), { once: true });
      existingScript.addEventListener('error', () => reject(new Error('load failed')), { once: true });
      return;
    }

    const script = document.createElement('script');
    script.id = 'tencent-map-gl';
    script.charset = 'utf-8';
    script.src = `https://map.qq.com/api/gljs?v=1.exp&key=${encodeURIComponent(key)}`;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('load failed'));
    document.body.appendChild(script);
  });

  return tencentMapScriptPromise;
}

function calcDistanceKm(lat1: number, lng1: number, lat2: number, lng2: number) {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const earth = 6371;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2)
    + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2))
    * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earth * c;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

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
  const [searchCenter, setSearchCenter] = useState<{ lat: number; lng: number } | null>(null);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(false);
  const [centerReloading, setCenterReloading] = useState(false);
  const [error, setError] = useState('');
  const [mapReady, setMapReady] = useState(false);
  const [mapError, setMapError] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const markerLayerRef = useRef<any>(null);
  const infoWindowRef = useRef<any>(null);

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
        setSearchCenter({ lat: latitude, lng: longitude });
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

  async function requestLocationByTencentIp() {
    setLocationState('requesting');
    setError('');
    try {
      let publicIp: string | undefined;
      try {
        const ipRes = await fetch('https://api.ipify.org?format=json');
        if (ipRes.ok) {
          const ipJson = await ipRes.json();
          if (typeof ipJson?.ip === 'string') {
            publicIp = ipJson.ip;
          }
        }
      } catch {
        // Fall back to server-side IP detection when public IP lookup is unavailable.
      }

      const res = await fetchTencentIpLocation(publicIp);
      const data = res?.data;
      if (!res?.success || !data || typeof data.latitude !== 'number' || typeof data.longitude !== 'number') {
        throw new Error('ip location failed');
      }
      setCoords({ lat: data.latitude, lng: data.longitude });
      setSearchCenter({ lat: data.latitude, lng: data.longitude });
      setLocationState('granted');
      await loadNearby(data.latitude, data.longitude);
    } catch {
      setLocationState('error');
      setError('腾讯IP定位失败，请改用浏览器定位');
    }
  }

  async function loadNearby(lat: number, lng: number) {
    setLoading(true);
    setError('');
    try {
      const d = await fetchNearbyRestaurants({ latitude: lat, longitude: lng, maxDistanceKm: 5 });
      setRestaurants(Array.isArray(d) ? d : (d.data ?? []));
      setSearchCenter({ lat, lng });
    } catch {
      setError('加载附近餐厅失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }

  function handleRefresh() {
    if (searchCenter) loadNearby(searchCenter.lat, searchCenter.lng);
    else if (coords) loadNearby(coords.lat, coords.lng);
    else requestLocation();
  }

  function getMapCenter(): { lat: number; lng: number } | null {
    const map = mapRef.current;
    if (!map || typeof map.getCenter !== 'function') {
      return null;
    }
    const center = map.getCenter();
    if (!center) {
      return null;
    }
    const lat = typeof center.getLat === 'function' ? center.getLat() : center.lat;
    const lng = typeof center.getLng === 'function' ? center.getLng() : center.lng;
    if (typeof lat !== 'number' || typeof lng !== 'number') {
      return null;
    }
    return { lat, lng };
  }

  async function reloadByMapCenter() {
    const center = getMapCenter();
    if (!center) {
      setError('无法获取地图中心点，请稍后重试');
      return;
    }
    setCenterReloading(true);
    try {
      await loadNearby(center.lat, center.lng);
    } finally {
      setCenterReloading(false);
    }
  }

  const normalizedRestaurants = useMemo(() => {
    if (!searchCenter) return restaurants;
    return restaurants
      .map((item) => {
        if (item.distanceKm != null) return item;
        if (item.latitude == null || item.longitude == null) return item;
        return {
          ...item,
          distanceKm: calcDistanceKm(searchCenter.lat, searchCenter.lng, item.latitude, item.longitude),
        };
      })
      .sort((a, b) => {
        const da = a.distanceKm ?? Number.MAX_SAFE_INTEGER;
        const db = b.distanceKm ?? Number.MAX_SAFE_INTEGER;
        return da - db;
      });
  }, [searchCenter, restaurants]);

  useEffect(() => {
    if (locationState !== 'granted' || !coords) {
      return;
    }
    if (!TENCENT_MAP_KEY) {
      setMapError('缺少前端地图 Key，请配置 NEXT_PUBLIC_TENCENT_MAP_KEY');
      return;
    }

    let cancelled = false;

    const initOrUpdateMap = async () => {
      try {
        await ensureTencentMapLoaded(TENCENT_MAP_KEY);
        if (cancelled || !mapContainerRef.current || !window.TMap) return;

        const TMap = window.TMap;
        const centerSource = searchCenter ?? coords;
        const center = new TMap.LatLng(centerSource.lat, centerSource.lng);

        if (!mapRef.current) {
          mapRef.current = new TMap.Map(mapContainerRef.current, {
            center,
            zoom: 14,
          });
        } else {
          mapRef.current.setCenter(center);
        }

        if (markerLayerRef.current?.setMap) {
          markerLayerRef.current.setMap(null);
        }

        const userIcon = 'data:image/svg+xml;utf8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 30 30"><circle cx="15" cy="15" r="8" fill="#34d399"/><circle cx="15" cy="15" r="12" fill="none" stroke="#10b981" stroke-width="2"/></svg>');
        const foodIcon = 'data:image/svg+xml;utf8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36"><path d="M14 1C7.9 1 3 5.9 3 12c0 8.3 11 22 11 22s11-13.7 11-22C25 5.9 20.1 1 14 1z" fill="#60a5fa"/><circle cx="14" cy="12" r="5" fill="#ffffff"/></svg>');

        const geometries: any[] = [
          {
            id: 'user-location',
            styleId: 'user',
            position: new TMap.LatLng(coords.lat, coords.lng),
            properties: { title: '你的位置', content: '当前位置' },
          },
        ];

        normalizedRestaurants.forEach((item, index) => {
          if (item.latitude == null || item.longitude == null) return;
          geometries.push({
            id: `restaurant-${item.id}-${index}`,
            styleId: 'restaurant',
            position: new TMap.LatLng(item.latitude, item.longitude),
            properties: {
              itemId: item.id,
              title: item.name || '未命名餐厅',
              address: item.address || '暂无地址',
              distance: item.distanceKm != null ? `${item.distanceKm.toFixed(2)} km` : '',
            },
          });
        });

        markerLayerRef.current = new TMap.MultiMarker({
          id: 'nearby-restaurant-markers',
          map: mapRef.current,
          styles: {
            user: new TMap.MarkerStyle({ width: 30, height: 30, anchor: { x: 15, y: 15 }, src: userIcon }),
            restaurant: new TMap.MarkerStyle({ width: 28, height: 36, anchor: { x: 14, y: 36 }, src: foodIcon }),
          },
          geometries,
        });

        if (!infoWindowRef.current) {
          infoWindowRef.current = new TMap.InfoWindow({
            map: mapRef.current,
            position: center,
            offset: { x: 0, y: -36 },
          });
          infoWindowRef.current.close();
        }

        markerLayerRef.current.on('click', (evt: any) => {
          const props = evt?.geometry?.properties || {};
          const title = escapeHtml(props.title || '未命名餐厅');
          const address = escapeHtml(props.address || '');
          const distance = escapeHtml(props.distance || '');
          const content = `<div style="padding:8px 10px;min-width:180px;"><div style="font-weight:600;margin-bottom:4px;">${title}</div><div style="font-size:12px;color:#6b7280;">${address}</div>${distance ? `<div style=\"font-size:12px;color:#10b981;margin-top:4px;\">距离 ${distance}</div>` : ''}</div>`;
          infoWindowRef.current.setPosition(evt.geometry.position);
          infoWindowRef.current.setContent(content);
          infoWindowRef.current.open();
          if (typeof props.itemId === 'number') {
            setSelectedId(props.itemId);
          }
        });

        setMapReady(true);
        setMapError('');
      } catch {
        if (!cancelled) {
          setMapError('腾讯地图加载失败，请检查前端 Key 的域名白名单设置');
          setMapReady(false);
        }
      }
    };

    initOrUpdateMap();

    return () => {
      cancelled = true;
    };
  }, [coords, searchCenter, locationState, normalizedRestaurants]);

  useEffect(() => {
    return () => {
      if (markerLayerRef.current?.setMap) {
        markerLayerRef.current.setMap(null);
      }
      if (mapRef.current?.destroy) {
        mapRef.current.destroy();
      }
    };
  }, []);

  const focusRestaurant = (item: Restaurant) => {
    if (!window.TMap || !mapRef.current) return;
    if (item.latitude == null || item.longitude == null) return;
    const center = new window.TMap.LatLng(item.latitude, item.longitude);
    mapRef.current.setCenter(center);
    mapRef.current.setZoom(16);
    setSelectedId(item.id);
  };

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

          {/* 地图区 */}
          <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="mb-5">
            <div className="relative w-full h-72 rounded-2xl border border-white/10 bg-white/[0.03] overflow-hidden">
              <div ref={mapContainerRef} className="absolute inset-0" />
              {!mapReady && (
                <div className="absolute inset-0 flex items-center justify-center bg-[#050b16]/70 backdrop-blur-sm">
                  <p className="text-sm text-white/55">
                    {locationState === 'granted' ? '正在加载腾讯地图...' : '定位后自动加载地图'}
                  </p>
                </div>
              )}
              {mapError && (
                <div className="absolute left-3 right-3 bottom-3 rounded-xl border border-red-500/20 bg-red-500/10 px-3 py-2 text-xs text-red-300">
                  {mapError}
                </div>
              )}
              {coords && (
                <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full border border-emerald-500/20 bg-emerald-500/15 px-3 py-1 text-xs text-emerald-200">
                  <MapPin size={11} />
                  {coords.lat.toFixed(4)}, {coords.lng.toFixed(4)}
                </div>
              )}
            </div>
          </motion.div>

          {/* 操作栏 */}
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.15 }} className="flex items-center gap-3 mb-5">
            {locationState !== 'granted' ? (
              <>
                <button
                  onClick={requestLocation}
                  disabled={locationState === 'requesting'}
                  className="flex items-center gap-2 rounded-xl border border-emerald-500/40 bg-emerald-500/15 px-4 py-2.5 text-sm font-semibold text-emerald-300 hover:bg-emerald-500/25 disabled:opacity-50 transition"
                >
                  {locationState === 'requesting'
                    ? <><RefreshCw size={14} className="animate-spin" />定位中...</>
                    : <><Navigation2 size={14} />浏览器定位</>
                  }
                </button>
                <button
                  onClick={requestLocationByTencentIp}
                  disabled={locationState === 'requesting'}
                  className="flex items-center gap-2 rounded-xl border border-cyan-400/35 bg-cyan-500/15 px-4 py-2.5 text-sm font-semibold text-cyan-200 hover:bg-cyan-500/25 disabled:opacity-50 transition"
                >
                  <MapPin size={14} />腾讯IP定位
                </button>
              </>
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
            {mapReady && locationState === 'granted' && (
              <button
                onClick={reloadByMapCenter}
                disabled={centerReloading || loading}
                className="flex items-center gap-2 rounded-xl border border-sky-400/35 bg-sky-500/15 px-4 py-2.5 text-sm font-semibold text-sky-200 hover:bg-sky-500/25 disabled:opacity-50 transition"
              >
                <RefreshCw size={14} className={centerReloading ? 'animate-spin' : ''} />
                按地图中心重搜
              </button>
            )}
          </motion.div>

          {searchCenter && (
            <p className="mb-5 text-xs text-white/35 px-1">
              当前搜索中心：{searchCenter.lat.toFixed(4)}, {searchCenter.lng.toFixed(4)}
            </p>
          )}

          {locationState !== 'granted' && (
            <p className="mb-5 text-xs text-white/30 px-1">
              提示：腾讯IP定位是城市级粗定位，精度通常低于浏览器GPS定位。
            </p>
          )}

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

              {!loading && normalizedRestaurants.map((r, i) => (
                <motion.div
                  key={r.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  whileHover={{ scale: 1.01, y: -2, transition: { duration: 0.18 } }}
                  transition={{ delay: i * 0.05 }}
                  onClick={() => focusRestaurant(r)}
                  className={`rounded-2xl border bg-white/[0.04] p-4 hover:bg-white/[0.08] transition-colors cursor-pointer ${selectedId === r.id ? 'border-emerald-400/45' : 'border-white/10 hover:border-white/15'}`}
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
