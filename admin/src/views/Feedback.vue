<template>
  <div class="page">
    <!-- 查询栏 -->
    <div class="toolbar">
      <span class="query-label">按用户 ID 查询反馈</span>
      <el-input-number
        v-model="userId"
        :min="1"
        placeholder="用户 ID"
        style="width: 160px"
        controls-position="right"
      />
      <el-button type="primary" :icon="Search" :loading="loading" @click="loadFeedback">
        查询
      </el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <el-alert
      v-if="!queried"
      title="请输入用户 ID 后点击「查询」以查看该用户的全部反馈记录"
      type="info"
      :closable="false"
      show-icon
      class="info-alert"
    />

    <!-- 反馈列表 -->
    <template v-if="queried">
      <el-card class="table-card" shadow="never">
        <div class="table-header">
          <span class="result-count">用户 #{{ queriedId }} 的反馈共 {{ feedbacks.length }} 条</span>
        </div>

        <el-empty v-if="!loading && feedbacks.length === 0" description="该用户暂无反馈记录" class="empty" />

        <div v-else class="feedback-list">
          <div
            v-for="fb in feedbacks"
            :key="fb.id"
            class="feedback-item"
            :class="{ 'type-bug': fb.feedbackType === 'BUG', 'type-suggestion': fb.feedbackType === 'SUGGESTION' }"
          >
            <div class="fb-header">
              <el-tag :type="typeColor(fb.feedbackType)" size="small" class="fb-type">
                {{ typeLabel(fb.feedbackType) }}
              </el-tag>
              <span class="fb-id">#{{ fb.id }}</span>
              <span class="fb-time">{{ formatDate(fb.createdAt) }}</span>
            </div>
            <p class="fb-content">{{ fb.content }}</p>
            <div class="fb-footer" v-if="fb.contact">
              <span class="fb-contact">联系方式：{{ fb.contact }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { feedbackApi } from '@/api/feedback'

const userId = ref(1)
const loading = ref(false)
const feedbacks = ref([])
const queried = ref(false)
const queriedId = ref(null)

const typeMap = {
  BUG: '问题反馈',
  SUGGESTION: '功能建议',
  PRAISE: '使用好评',
  OTHER: '其他'
}
const typeColorMap = {
  BUG: 'danger',
  SUGGESTION: 'warning',
  PRAISE: 'success',
  OTHER: 'info'
}

function typeLabel(t) { return typeMap[t] || t || '—' }
function typeColor(t) { return typeColorMap[t] || 'info' }

function formatDate(dt) {
  if (!dt) return '—'
  const d = new Date(dt)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function loadFeedback() {
  loading.value = true
  queried.value = false
  try {
    const res = await feedbackApi.listByUser(userId.value)
    feedbacks.value = res.data || []
    queriedId.value = userId.value
    queried.value = true
  } finally {
    loading.value = false
  }
}

function reset() {
  feedbacks.value = []
  queried.value = false
  queriedId.value = null
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.query-label { font-size: 14px; color: #9ca3af; }

.info-alert {
  background: rgba(16,185,129,0.06) !important;
  border: 1px solid rgba(16,185,129,0.2) !important;
  border-radius: 10px;
}
:deep(.el-alert__title) { color: #9ca3af; font-size: 13px; }

.table-card {
  background: #1a1d27 !important;
  border: 1px solid rgba(255,255,255,0.05) !important;
  border-radius: 14px !important;
}
:deep(.el-card__body) { padding: 20px; }

.table-header { margin-bottom: 16px; }
.result-count { font-size: 14px; color: #9ca3af; }

.empty { padding: 40px 0; }
:deep(.el-empty__description p) { color: #4b5563; }

/* 反馈卡片列表 */
.feedback-list { display: flex; flex-direction: column; gap: 12px; }

.feedback-item {
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  border-left: 3px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 14px 16px;
  transition: background 0.2s;
}
.feedback-item:hover { background: rgba(255,255,255,0.04); }
.feedback-item.type-bug { border-left-color: #ef4444; }
.feedback-item.type-suggestion { border-left-color: #f59e0b; }

.fb-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.fb-type { flex-shrink: 0; }
.fb-id { font-size: 12px; color: #4b5563; font-family: monospace; }
.fb-time { font-size: 12px; color: #4b5563; margin-left: auto; }

.fb-content {
  font-size: 14px;
  color: #d1d5db;
  line-height: 1.6;
  word-break: break-word;
}

.fb-footer { margin-top: 8px; }
.fb-contact { font-size: 12px; color: #6b7280; }
</style>
