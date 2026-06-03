<template>
  <div class="page">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名 / 昵称 / 邮箱"
        clearable
        :prefix-icon="Search"
        style="width: 280px"
        @input="debounceSearch"
        @clear="loadUsers"
      />
      <el-button type="primary" :icon="Refresh" @click="loadUsers">刷新</el-button>
      <span class="total-hint">共 {{ total }} 条</span>
    </div>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="users"
        row-key="id"
        class="admin-table"
        @row-click="openDetail"
      >
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <el-avatar :size="32" class="row-avatar">
              {{ row.nickname?.charAt(0)?.toUpperCase() || row.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.gender === 'MALE' ? 'primary' : row.gender === 'FEMALE' ? 'danger' : 'info'" size="small">
              {{ genderMap[row.gender] || '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'default'" size="small">
              {{ row.role || 'USER' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accountStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.accountStatus === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.accountStatus === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">
              <el-icon><View /></el-icon> 详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentUser ? `用户详情 #${currentUser.id}` : ''"
      size="400px"
      :destroy-on-close="true"
    >
      <template v-if="currentUser">
        <div class="user-detail">
          <div class="detail-avatar">
            <el-avatar :size="64" class="row-avatar">
              {{ currentUser.nickname?.charAt(0)?.toUpperCase() || '?' }}
            </el-avatar>
          </div>
          <el-descriptions :column="1" border class="detail-desc">
            <el-descriptions-item label="ID">{{ currentUser.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderMap[currentUser.gender] || '—' }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ currentUser.age || '—' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ currentUser.role }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ currentUser.accountStatus }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDate(currentUser.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'

const keyword = ref('')
const loading = ref(false)
const users = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const currentUser = ref(null)

const genderMap = { MALE: '男', FEMALE: '女', OTHER: '其他', UNKNOWN: '未知' }

function formatDate(dt) {
  if (!dt) return '—'
  const d = new Date(dt)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await userApi.list(keyword.value || undefined)
    users.value = res.data || []
    total.value = users.value.length
  } catch(e) {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

let searchTimer = null
function debounceSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadUsers, 400)
}

function openDetail(row) {
  currentUser.value = row
  drawerVisible.value = true
}

onMounted(loadUsers)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.total-hint { font-size: 13px; color: #6b7280; margin-left: 4px; }

.table-card {
  background: #1a1d27 !important;
  border: 1px solid rgba(255,255,255,0.05) !important;
  border-radius: 14px !important;
}
:deep(.el-card__body) { padding: 0; }

.admin-table { background: transparent !important; width: 100%; }
:deep(.el-table) { background: transparent !important; }
:deep(.el-table tr) { background: transparent !important; cursor: pointer; }
:deep(.el-table th) {
  background: rgba(255,255,255,0.02) !important;
  color: #6b7280 !important;
  font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,0.06) !important;
}
:deep(.el-table td) {
  border-bottom: 1px solid rgba(255,255,255,0.04) !important;
  color: #d1d5db;
}
:deep(.el-table tr:hover td) { background: rgba(16,185,129,0.04) !important; }
:deep(.el-table__empty-block) { background: transparent; }

.row-avatar { background: linear-gradient(135deg, #10b981, #059669) !important; font-weight: 700; font-size: 13px; }

/* 详情抽屉 */
:deep(.el-drawer__header) { color: #e5e7eb; font-weight: 600; border-bottom: 1px solid rgba(255,255,255,0.06); }
:deep(.el-drawer__body) { background: #13161f; padding: 20px; }

.user-detail { display: flex; flex-direction: column; gap: 20px; }
.detail-avatar { display: flex; justify-content: center; }
:deep(.detail-desc .el-descriptions__cell) { background: transparent !important; border-color: rgba(255,255,255,0.06) !important; color: #d1d5db; }
:deep(.detail-desc .el-descriptions__label) { background: rgba(255,255,255,0.02) !important; color: #6b7280 !important; font-size: 13px; }
</style>
