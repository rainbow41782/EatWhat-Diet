<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
      <div class="brand" :class="{ collapsed }">
        <span class="brand-icon">食</span>
        <transition name="fade">
          <span v-if="!collapsed" class="brand-name">JavaDiet Admin</span>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#9ca3af"
        active-text-color="#10b981"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>概览仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/foods">
          <el-icon><Bowl /></el-icon>
          <template #title>食品管理</template>
        </el-menu-item>
        <el-menu-item index="/restaurants">
          <el-icon><Location /></el-icon>
          <template #title>餐厅管理</template>
        </el-menu-item>
        <el-menu-item index="/meituan-crawl">
          <el-icon><Search /></el-icon>
          <template #title>美团菜单爬取</template>
        </el-menu-item>
        <el-menu-item index="/feedback">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>用户反馈</template>
        </el-menu-item>
      </el-menu>

      <div class="collapse-btn" @click="collapsed = !collapsed">
        <el-icon>
          <component :is="collapsed ? 'Expand' : 'Fold'" />
        </el-icon>
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="topbar">
        <div class="topbar-left">
          <span class="page-title">{{ currentTitle }}</span>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle !== '概览仪表盘'">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-tooltip content="前台主站" placement="bottom">
            <el-button :icon="Link" circle text @click="openFrontend" />
          </el-tooltip>
          <el-dropdown @command="handleCommand">
            <div class="avatar-area">
              <el-avatar :size="32" class="avatar">
                {{ auth.user?.nickname?.charAt(0)?.toUpperCase() || 'A' }}
              </el-avatar>
              <span class="username">{{ auth.user?.nickname || '管理员' }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  DataLine,
  User,
  Bowl,
  Location,
  ChatDotRound,
  Expand,
  Fold,
  ArrowDown,
  SwitchButton,
  Link,
  Search
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '管理后台')

function openFrontend() {
  window.open('http://localhost:3000', '_blank')
}

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    auth.logout()
    ElMessage.success('已退出')
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: #0f1117;
}

.sidebar {
  background: #13161f;
  border-right: 1px solid rgba(255,255,255,0.05);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  white-space: nowrap;
  overflow: hidden;
}
.brand.collapsed { justify-content: center; padding: 20px 0 16px; }
.brand-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: linear-gradient(135deg, #10b981, #059669);
  font-weight: 700;
}
.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #e5e7eb;
}

.sidebar-menu {
  flex: 1;
  border: none !important;
  padding: 8px;
  overflow-y: auto;
  overflow-x: hidden;
}

:deep(.el-menu-item) {
  border-radius: 8px;
  margin-bottom: 4px;
  height: 44px;
  line-height: 44px;
}
:deep(.el-menu-item:hover) {
  background: rgba(16,185,129,0.08) !important;
  color: #e5e7eb !important;
}
:deep(.el-menu-item.is-active) {
  background: rgba(16,185,129,0.15) !important;
  color: #10b981 !important;
  font-weight: 600;
}
:deep(.el-menu--collapse .el-menu-item) { justify-content: center; }

.collapse-btn {
  padding: 16px;
  display: flex;
  justify-content: center;
  cursor: pointer;
  color: #4b5563;
  border-top: 1px solid rgba(255,255,255,0.04);
}
.collapse-btn:hover { color: #10b981; }

.topbar {
  background: rgba(19, 22, 31, 0.95);
  border-bottom: 1px solid rgba(255,255,255,0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px !important;
}
.topbar-left { display: flex; flex-direction: column; gap: 2px; }
.page-title { font-size: 15px; font-weight: 600; color: #e5e7eb; }
.breadcrumb { font-size: 12px; }
:deep(.el-breadcrumb__inner) { color: #6b7280 !important; }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.avatar-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
}
.avatar-area:hover { background: rgba(255,255,255,0.05); }
.avatar { background: linear-gradient(135deg, #10b981, #059669) !important; font-weight: 700; }
.username { font-size: 14px; color: #d1d5db; }
.arrow { font-size: 12px; color: #6b7280; }
.main-container { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.main-content {
  background: #0f1117;
  padding: 24px;
  overflow-y: auto;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.page-fade-enter-active, .page-fade-leave-active { transition: all 0.2s ease; }
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
