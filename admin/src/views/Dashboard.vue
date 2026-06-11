<template>
  <div class="dashboard">
    <div class="stats-grid" v-loading="loading">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="stat-card"
        :style="{ '--card-color': card.color }"
      >
        <div class="stat-icon">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">{{ card.label }}</p>
          <p class="stat-value">{{ card.value }}</p>
        </div>
        <div class="stat-bg-icon">
          <el-icon :size="56"><component :is="card.icon" /></el-icon>
        </div>
      </div>
    </div>

    <div class="section-title">快捷管理入口</div>
    <div class="quick-grid">
      <router-link
        v-for="item in quickLinks"
        :key="item.to"
        :to="item.to"
        class="quick-card"
      >
        <el-icon :size="28" class="quick-icon"><component :is="item.icon" /></el-icon>
        <p class="quick-label">{{ item.label }}</p>
        <p class="quick-desc">{{ item.desc }}</p>
        <el-icon class="quick-arrow"><ArrowRight /></el-icon>
      </router-link>
    </div>

    <div class="section-title">系统信息</div>
    <el-card class="info-card" shadow="never">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="后端地址">
          <el-tag type="success" size="small">http://localhost:8080</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="前台地址">
          <el-tag type="info" size="small">http://localhost:3000</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="管理后台">
          <el-tag size="small">http://localhost:3001</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="技术栈 - 后端">Spring Boot 4 / MySQL 8 / JPA</el-descriptions-item>
        <el-descriptions-item label="技术栈 - 前台">Next.js 13 / TypeScript / Tailwind</el-descriptions-item>
        <el-descriptions-item label="技术栈 - 后台">Vue 3 / Element Plus / Axios</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboardApi } from '@/api/dashboard'

const loading = ref(false)

const statCards = reactive([
  { key: 'userCount', label: '注册用户', value: '-', icon: 'User', color: '#10b981' },
  { key: 'foodCount', label: '食品种类', value: '-', icon: 'Bowl', color: '#6366f1' },
  { key: 'restaurantCount', label: '收录餐厅', value: '-', icon: 'Location', color: '#f59e0b' },
  { key: 'feedbackCount', label: '用户反馈', value: '-', icon: 'ChatDotRound', color: '#ef4444' },
])

const quickLinks = [
  { to: '/users', icon: 'User', label: '用户管理', desc: '查看、搜索注册用户' },
  { to: '/foods', icon: 'Bowl', label: '食品管理', desc: '维护食品数据库' },
  { to: '/restaurants', icon: 'Location', label: '餐厅管理', desc: '管理餐厅及菜单信息' },
  { to: '/feedback', icon: 'ChatDotRound', label: '反馈管理', desc: '查看用户提交的反馈' },
]

function formatCount(value) {
  const numberValue = Number(value ?? 0)
  return Number.isFinite(numberValue) ? numberValue.toLocaleString() : '0'
}

async function loadStats() {
  loading.value = true
  try {
    const res = await dashboardApi.stats()
    const stats = res.data || {}
    statCards.forEach((card) => {
      card.value = formatCount(stats[card.key])
    })
  } catch (error) {
    statCards.forEach((card) => {
      card.value = '加载失败'
    })
    ElMessage.error('仪表盘统计加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 24px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 116px;
}
@media (max-width: 1200px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) {
  .stats-grid,
  .quick-grid {
    grid-template-columns: 1fr !important;
  }
}

.stat-card {
  background: #1a1d27;
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 16px;
  padding: 22px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
}

.stat-icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  display: grid; place-items: center;
  color: var(--card-color);
  flex-shrink: 0;
}
.stat-card:nth-child(1) .stat-icon { background: rgba(16,185,129,0.12); color: #10b981; }
.stat-card:nth-child(2) .stat-icon { background: rgba(99,102,241,0.12); color: #6366f1; }
.stat-card:nth-child(3) .stat-icon { background: rgba(245,158,11,0.12); color: #f59e0b; }
.stat-card:nth-child(4) .stat-icon { background: rgba(239,68,68,0.12); color: #ef4444; }

.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #6b7280; margin-bottom: 6px; }
.stat-value { font-size: 28px; font-weight: 700; color: #e5e7eb; }

.stat-bg-icon {
  position: absolute; right: 12px; top: 50%;
  transform: translateY(-50%);
  opacity: 0.04; color: #fff;
  pointer-events: none;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #9ca3af;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  padding-left: 2px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
@media (max-width: 1200px) { .quick-grid { grid-template-columns: repeat(2, 1fr); } }

.quick-card {
  background: #1a1d27;
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 14px;
  padding: 20px;
  text-decoration: none;
  display: block;
  position: relative;
  transition: all 0.2s;
}
.quick-card:hover {
  border-color: rgba(16,185,129,0.3);
  background: rgba(16,185,129,0.05);
  transform: translateY(-2px);
}
.quick-icon { color: #10b981; margin-bottom: 12px; display: block; }
.quick-label { font-size: 15px; font-weight: 600; color: #e5e7eb; margin-bottom: 4px; }
.quick-desc { font-size: 13px; color: #6b7280; }
.quick-arrow {
  position: absolute; right: 16px; bottom: 20px;
  color: #374151; font-size: 16px;
  transition: all 0.2s;
}
.quick-card:hover .quick-arrow { color: #10b981; transform: translateX(4px); }

.info-card {
  background: #1a1d27 !important;
  border: 1px solid rgba(255,255,255,0.05) !important;
  border-radius: 14px !important;
}
:deep(.el-card__body) { padding: 20px; }
:deep(.el-descriptions__body) { background: transparent; }
:deep(.el-descriptions__cell) {
  background: transparent !important;
  border-color: rgba(255,255,255,0.06) !important;
  color: #9ca3af;
}
:deep(.el-descriptions__label) {
  background: rgba(255,255,255,0.02) !important;
  color: #6b7280 !important;
  font-size: 13px;
}
</style>
