'use client';
import React from 'react';
import { motion, useMotionValue, useTransform } from 'framer-motion';

interface PageCardProps {
  children: React.ReactNode;
  className?: string;
  // 是否开启鼠标倾斜效果，列表条目建议关闭
  tilt?: boolean;
  // 是否播放入场动画
  animate?: boolean;
}

export function PageCard({ children, className, tilt = true, animate = true }: PageCardProps) {
  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);
  const rotateX = useTransform(mouseY, [-200, 200], [5, -5]);
  const rotateY = useTransform(mouseX, [-200, 200], [-5, 5]);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!tilt) return;
    const rect = e.currentTarget.getBoundingClientRect();
    mouseX.set(e.clientX - rect.left - rect.width / 2);
    mouseY.set(e.clientY - rect.top - rect.height / 2);
  };
  const handleMouseLeave = () => {
    mouseX.set(0);
    mouseY.set(0);
  };

  return (
    <motion.div
      style={{ perspective: 1200 }}
      {...(animate
        ? { initial: { opacity: 0, y: 14 }, animate: { opacity: 1, y: 0 }, transition: { duration: 0.4 } }
        : {})}
    >
      <motion.div
        style={tilt ? { rotateX, rotateY } : {}}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        whileHover={tilt ? { z: 6 } : {}}
      >
        <div className="relative group">
          {/* 呼吸光晕 */}
          <motion.div
            className="absolute -inset-[1px] rounded-2xl"
            animate={{
              boxShadow: [
                '0 0 8px 2px rgba(255,255,255,0.02)',
                '0 0 16px 5px rgba(255,255,255,0.04)',
                '0 0 8px 2px rgba(255,255,255,0.02)',
              ],
              opacity: [0.12, 0.3, 0.12],
            }}
            transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut', repeatType: 'mirror' }}
          />

          {/* 四边流光 */}
          <div className="absolute -inset-[1px] rounded-2xl overflow-hidden pointer-events-none">
            <motion.div
              className="absolute top-0 left-0 h-[2px] w-[45%] bg-gradient-to-r from-transparent via-emerald-300 to-transparent opacity-55"
              animate={{ left: ['-45%', '100%'] }}
              transition={{ duration: 2.8, ease: 'easeInOut', repeat: Infinity, repeatDelay: 2 }}
            />
            <motion.div
              className="absolute top-0 right-0 h-[45%] w-[2px] bg-gradient-to-b from-transparent via-emerald-300 to-transparent opacity-55"
              animate={{ top: ['-45%', '100%'] }}
              transition={{ duration: 2.8, ease: 'easeInOut', repeat: Infinity, repeatDelay: 2, delay: 0.7 }}
            />
            <motion.div
              className="absolute bottom-0 right-0 h-[2px] w-[45%] bg-gradient-to-r from-transparent via-emerald-300 to-transparent opacity-55"
              animate={{ right: ['-45%', '100%'] }}
              transition={{ duration: 2.8, ease: 'easeInOut', repeat: Infinity, repeatDelay: 2, delay: 1.4 }}
            />
            <motion.div
              className="absolute bottom-0 left-0 h-[45%] w-[2px] bg-gradient-to-b from-transparent via-emerald-300 to-transparent opacity-55"
              animate={{ bottom: ['-45%', '100%'] }}
              transition={{ duration: 2.8, ease: 'easeInOut', repeat: Infinity, repeatDelay: 2, delay: 2.1 }}
            />
          </div>

          {/* 卡片主体 */}
          <div
            className={`relative overflow-hidden rounded-2xl border border-white/[0.06] bg-black/40 backdrop-blur-xl shadow-2xl ${className ?? ''}`}
          >
            {/* 点阵纹理 */}
            <div
              className="absolute inset-0 pointer-events-none"
              style={{
                backgroundImage:
                  'linear-gradient(135deg, rgba(255,255,255,0.03) 0.5px, transparent 0.5px), linear-gradient(45deg, rgba(255,255,255,0.03) 0.5px, transparent 0.5px)',
                backgroundSize: '30px 30px',
              }}
            />
            <div className="relative">{children}</div>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
