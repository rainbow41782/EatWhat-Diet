'use client';
import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowRight, Link, Lock, Zap } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export interface TimelineMealEntry {
  key: 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | string;
  label: string;
  status: 'completed' | 'pending';
  summary: string;
  href: string;
}

export interface TimelineItem {
  id: number;
  title: string;
  date: string;
  description: string;
  category: '膳食' | '功能' | '打卡' | '健康档案' | string;
  icon: React.ElementType;
  relatedIds: number[];
  status: 'completed' | 'in-progress' | 'pending';
  energy: number;
  details?: { label: string; value: string }[];
  entryHref?: string;
  mealEntries?: TimelineMealEntry[];
  checkInHistory?: string[];
  // 展示控制
  hideStatusBadge?: boolean;
  hideCategoryBadge?: boolean;
  hideEnergy?: boolean;
  customStatusLabel?: string;
}

interface RadialOrbitalTimelineProps {
  timelineData: TimelineItem[] | null;
}

function getCategoryAccent(category: string): string {
  switch (category) {
    case '膳食':
      return 'rgba(16, 185, 129, 0.7)';
    case '功能':
      return 'rgba(59, 130, 246, 0.7)';
    case '打卡':
      return 'rgba(168, 85, 247, 0.7)';
    case '健康档案':
      return 'rgba(249, 115, 22, 0.7)';
    default:
      return 'rgba(255, 255, 255, 0.3)';
  }
}

function getCategoryBorderClass(category: string, isExpanded: boolean, isRelated: boolean): string {
  if (isExpanded) return 'border-white shadow-lg shadow-white/30';
  if (isRelated) return 'border-white animate-pulse';
  switch (category) {
    case '膳食':
      return 'border-emerald-400/60';
    case '功能':
      return 'border-blue-400/60';
    case '打卡':
      return 'border-purple-400/60';
    case '健康档案':
      return 'border-orange-400/60';
    default:
      return 'border-white/40';
  }
}

function getStatusStyles(status: TimelineItem['status']): string {
  switch (status) {
    case 'completed':
      return 'text-white bg-black border-white';
    case 'in-progress':
      return 'text-black bg-white border-black';
    case 'pending':
      return 'text-white bg-black/40 border-white/50';
    default:
      return 'text-white bg-black/40 border-white/50';
  }
}

function getStatusLabel(status: TimelineItem['status']): string {
  switch (status) {
    case 'completed':
      return '已完成';
    case 'in-progress':
      return '进行中';
    case 'pending':
      return '待开始';
    default:
      return '待开始';
  }
}

export default function RadialOrbitalTimeline({ timelineData }: RadialOrbitalTimelineProps) {
  const safeData = timelineData ?? [];
  const [expandedItems, setExpandedItems] = useState<Record<number, boolean>>({});
  const [rotationAngle, setRotationAngle] = useState<number>(0);
  const [autoRotate, setAutoRotate] = useState<boolean>(true);
  const [pulseEffect, setPulseEffect] = useState<Record<number, boolean>>({});
  const [activeNodeId, setActiveNodeId] = useState<number | null>(null);
  const [activeMealKeyByNode, setActiveMealKeyByNode] = useState<Record<number, string>>({});
  const containerRef = useRef<HTMLDivElement>(null);
  const orbitRef = useRef<HTMLDivElement>(null);

  const handleContainerClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === containerRef.current || e.target === orbitRef.current) {
      setExpandedItems({});
      setActiveNodeId(null);
      setPulseEffect({});
      setAutoRotate(true);
    }
  };

  const getRelatedItems = (itemId: number): number[] => {
    const item = safeData.find((i) => i.id === itemId);
    return item ? item.relatedIds : [];
  };

  const isRelatedToActive = (itemId: number): boolean => {
    if (!activeNodeId) return false;
    return getRelatedItems(activeNodeId).includes(itemId);
  };

  const centerViewOnNode = (nodeId: number) => {
    const idx = safeData.findIndex((item) => item.id === nodeId);
    if (idx < 0 || safeData.length === 0) return;
    const targetAngle = (idx / safeData.length) * 360;
    setRotationAngle(270 - targetAngle);
  };

  const toggleItem = (id: number) => {
    setExpandedItems((prev) => {
      const next = { ...prev };
      Object.keys(next).forEach((key) => {
        if (Number(key) !== id) next[Number(key)] = false;
      });
      next[id] = !prev[id];

      if (!prev[id]) {
        setActiveNodeId(id);
        setAutoRotate(false);
        const pulse: Record<number, boolean> = {};
        getRelatedItems(id).forEach((rid) => {
          pulse[rid] = true;
        });
        setPulseEffect(pulse);
        centerViewOnNode(id);
      } else {
        setActiveNodeId(null);
        setAutoRotate(true);
        setPulseEffect({});
      }
      return next;
    });
  };

  useEffect(() => {
    let timer: ReturnType<typeof setInterval>;
    if (autoRotate) {
      timer = setInterval(() => {
        setRotationAngle((prev) => Number(((prev + 0.3) % 360).toFixed(3)));
      }, 50);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [autoRotate]);

  const calculateNodePosition = (index: number, total: number) => {
    const angle = ((index / total) * 360 + rotationAngle) % 360;
    const radius = 220;
    const radian = (angle * Math.PI) / 180;
    const x = radius * Math.cos(radian);
    const y = radius * Math.sin(radian);
    const zIndex = Math.round(100 + 50 * Math.cos(radian));
    const opacity = Math.max(0.4, Math.min(1, 0.4 + 0.6 * ((1 + Math.sin(radian)) / 2)));
    return { x, y, zIndex, opacity };
  };

  if (safeData.length === 0) {
    return (
      <div className="w-full h-full flex flex-col items-center justify-center" ref={containerRef}>
        <div className="relative w-full max-w-4xl h-full flex items-center justify-center">
          <div className="absolute w-[440px] h-[440px] rounded-full border border-white/10" />
          <div className="flex flex-col items-center gap-3 text-center">
            <div className="w-16 h-16 rounded-full bg-white/5 border border-white/20 flex items-center justify-center">
              <Lock size={24} className="text-white/40" />
            </div>
            <p className="text-white/40 text-sm">登录后查看健康数据</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      className="w-full h-full flex flex-col items-center justify-center"
      ref={containerRef}
      onClick={handleContainerClick}
    >
      <div className="relative w-full max-w-4xl h-full flex items-center justify-center">
        {/* 轨道装饰层：overflow-hidden 防止轨道圆圈横向溢出 */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none flex items-center justify-center">
          <div className="absolute w-[440px] h-[440px] rounded-full border border-white/10" />
        </div>

        {/* 交互层：中心点 + 节点 + 弹出卡片（不裁剪，允许卡片溢出显示） */}
        <div className="absolute w-full h-full flex items-center justify-center" ref={orbitRef} style={{ perspective: '1000px' }}>
          <div className="absolute w-16 h-16 rounded-full bg-gradient-to-br from-emerald-500 via-teal-500 to-emerald-700 animate-pulse flex items-center justify-center z-10">
            <div className="absolute w-20 h-20 rounded-full border border-white/20 animate-ping opacity-70" />
            <div className="absolute w-24 h-24 rounded-full border border-white/10 animate-ping opacity-50" style={{ animationDelay: '0.5s' }} />
            <div className="w-8 h-8 rounded-full bg-white/80 backdrop-blur-md" />
          </div>


          {safeData.map((item, index) => {
            const pos = calculateNodePosition(index, safeData.length);
            const isExpanded = expandedItems[item.id];
            const isRelated = isRelatedToActive(item.id);
            const isPulsing = pulseEffect[item.id];
            const Icon = item.icon;
            const accent = getCategoryAccent(item.category);

            return (
              <div
                key={item.id}
                className="absolute transition-all duration-700 cursor-pointer"
                style={{
                  transform: `translate(${pos.x}px, ${pos.y}px)`,
                  zIndex: isExpanded ? 200 : pos.zIndex,
                  opacity: isExpanded ? 1 : pos.opacity,
                }}
                onClick={(e) => {
                  e.stopPropagation();
                  toggleItem(item.id);
                }}
              >
                <div
                  className={`absolute rounded-full -inset-1 ${isPulsing ? 'animate-pulse' : ''}`}
                  style={{
                    background: `radial-gradient(circle, ${accent} 0%, rgba(255,255,255,0) 70%)`,
                    width: `${Math.min(120, item.energy * 0.35 + 48)}px`,
                    height: `${Math.min(120, item.energy * 0.35 + 48)}px`,
                    left: `-${(Math.min(120, item.energy * 0.35 + 48) - 48) / 2}px`,
                    top: `-${(Math.min(120, item.energy * 0.35 + 48) - 48) / 2}px`,
                  }}
                />

                <div
                  className={`
                  w-12 h-12 rounded-full flex items-center justify-center
                  ${isExpanded ? 'bg-white text-black' : isRelated ? 'bg-white/50 text-black' : 'bg-black/60 text-white'}
                  border-2 ${getCategoryBorderClass(item.category, isExpanded, isRelated)}
                  transition-all duration-300 transform
                  ${isExpanded ? 'scale-150' : ''}
                `}
                >
                  <Icon size={20} />
                </div>

                <div
                  className={`absolute top-14 whitespace-nowrap text-xs font-semibold tracking-wider transition-all duration-300 ${isExpanded ? 'text-white scale-125' : 'text-white/70'}`}
                >
                  {item.title}
                </div>
              </div>
            );
          })}
        </div>

        {/* ── 详情卡：挂在轨道容器根节点，脱离节点 transform 堆叠 ── *
         *  centerViewOnNode 总是把激活节点带到轨道顶部 (y = -220)。
         *  节点图标顶部 = 容器中心 - 220；卡片顶部 = 节点顶部 + 76 = 中心 - 144。
         *  用 top: calc(50% - 144px) 精确对准，maxHeight: calc(50% + 144px) 恰好撑到视口底部。 */}
        <AnimatePresence>
        {activeNodeId !== null && expandedItems[activeNodeId] && (() => {
          const item = safeData.find((i) => i.id === activeNodeId);
          if (!item) return null;
          const mealEntries = item.mealEntries ?? [];
          const activeMealKey = activeMealKeyByNode[activeNodeId] ?? mealEntries[0]?.key;
          const activeMeal = mealEntries.find((entry) => entry.key === activeMealKey);
          const detailHref = activeMeal?.href || item.entryHref;

          return (
            <motion.div
              key={activeNodeId}
              initial={{ opacity: 0, scale: 0.92, y: -8 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.92, y: -8, transition: { duration: 0.15, ease: 'easeIn' } }}
              transition={{ delay: 0.7, duration: 0.2, ease: 'easeOut' }}
              className="absolute left-1/2 -translate-x-1/2 w-72 flex flex-col"
              style={{ top: 'calc(50% - 180px)', maxHeight: 'calc(50% + 168px)', zIndex: 500 }}
              onClick={(e) => e.stopPropagation()}
            >
              {/* 连接线（节点图标底部 → 卡片顶部） */}
              <div className="flex justify-center flex-shrink-0">
                <div className="w-px h-3 bg-white/50" />
              </div>
              <Card className="bg-black/90 backdrop-blur-lg border-white/30 shadow-xl shadow-white/10 overflow-hidden flex flex-col flex-1">
                <CardHeader className="pb-2 flex-shrink-0">
                  <div className="flex justify-between items-center">
                    {!item.hideStatusBadge && (
                      <Badge className={`px-2 text-xs ${getStatusStyles(item.status)}`}>
                        {item.customStatusLabel ?? getStatusLabel(item.status)}
                      </Badge>
                    )}
                    <span className="text-xs font-mono text-white/50 ml-auto">{item.date}</span>
                  </div>
                  {!item.hideCategoryBadge && (
                    <div className="flex items-center gap-2 mt-2">
                      <span className="text-xs text-white/40 border border-white/20 rounded-full px-2 py-0.5">{item.category}</span>
                    </div>
                  )}
                  <CardTitle className="text-sm mt-1 text-white">{item.title}</CardTitle>
                </CardHeader>

                <CardContent className="text-xs text-white/80 overflow-y-auto flex-1">
                  <p>{item.description}</p>

                  {mealEntries.length > 0 && (
                    <div className="mt-4 pt-3 border-t border-white/10 space-y-2">
                      <h4 className="text-xs uppercase tracking-wider text-white/70">餐种入口</h4>
                      <div className="grid grid-cols-2 gap-2">
                        {mealEntries.map((entry) => (
                          <button
                            key={entry.key}
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              setActiveMealKeyByNode((prev) => ({ ...prev, [activeNodeId]: entry.key }));
                            }}
                            className={`rounded-md border px-2 py-1 text-left transition ${
                              activeMealKey === entry.key
                                ? 'border-emerald-300/70 bg-emerald-500/20 text-white'
                                : 'border-white/20 bg-white/5 text-white/70 hover:bg-white/10'
                            }`}
                          >
                            <div className="font-semibold">{entry.label}</div>
                            <div className="text-[10px] opacity-80">{entry.status === 'completed' ? '已记录' : '未记录'}</div>
                          </button>
                        ))}
                      </div>
                      {activeMeal ? <p className="text-white/70">{activeMeal.summary}</p> : null}
                    </div>
                  )}

                  {(item.checkInHistory ?? []).length >= 0 && item.category === '打卡' && (
                    <div className="mt-4 pt-3 border-t border-white/10 space-y-2">
                      <h4 className="text-xs uppercase tracking-wider text-white/70">最近三条打卡</h4>
                      {(item.checkInHistory ?? []).length === 0 ? (
                        <p className="text-white/50">暂无记录</p>
                      ) : (
                        <ul className="space-y-1 text-white/80">
                          {(item.checkInHistory ?? []).slice(0, 3).map((date) => (
                            <li key={date}>• {date}</li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}

                  {item.details && item.details.length > 0 && (
                    <div className="mt-4 pt-3 border-t border-white/10 space-y-1">
                      {item.details.slice(0, 4).map((detail) => (
                        <div key={detail.label} className="flex items-center justify-between gap-2">
                          <span className="text-white/50">{detail.label}</span>
                          <span className="text-white/90">{detail.value}</span>
                        </div>
                      ))}
                    </div>
                  )}

                  {!item.hideEnergy && (
                    <div className="mt-4 pt-3 border-t border-white/10">
                      <div className="flex justify-between items-center text-xs mb-1">
                        <span className="flex items-center gap-1">
                          <Zap size={10} />
                          能量等级
                        </span>
                        <span className="font-mono">{item.energy}%</span>
                      </div>
                      <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                        <div className="h-full bg-gradient-to-r from-emerald-500 to-teal-400" style={{ width: `${Math.min(100, item.energy)}%` }} />
                      </div>
                    </div>
                  )}

                  {detailHref ? (
                    <a
                      href={detailHref}
                      onClick={(e) => e.stopPropagation()}
                      className="mt-4 inline-flex items-center gap-1 text-emerald-300 hover:text-emerald-200"
                    >
                      进入详情页
                      <ArrowRight size={12} />
                    </a>
                  ) : null}

                  {item.relatedIds.length > 0 && (
                    <div className="mt-4 pt-3 border-t border-white/10">
                      <div className="flex items-center mb-2 gap-1">
                        <Link size={10} className="text-white/70" />
                        <h4 className="text-xs uppercase tracking-wider font-medium text-white/70">关联节点</h4>
                      </div>
                      <div className="flex flex-wrap gap-1">
                        {item.relatedIds.map((relatedId) => {
                          const related = safeData.find((i) => i.id === relatedId);
                          return (
                            <Button
                              key={relatedId}
                              variant="outline"
                              size="sm"
                              className="h-6 px-2 text-xs border-white/20 bg-white/5 text-white/70 hover:bg-white/10"
                              onClick={(e) => {
                                e.stopPropagation();
                                toggleItem(relatedId);
                              }}
                            >
                              {related?.title || `节点 ${relatedId}`}
                              <ArrowRight size={8} className="ml-1" />
                            </Button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </motion.div>
          );
        })()}
        </AnimatePresence>
      </div>
    </div>
  );
}
