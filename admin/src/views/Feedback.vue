<template>
  <div class="page">
    <div class="toolbar">
      <span class="query-label">用户反馈</span>
      <el-input-number
        v-model="userId"
        :min="1"
        placeholder="用户 ID"
        style="width: 160px"
        controls-position="right"
      />
      <el-button type="primary" :icon="Search" :loading="loading" @click="loadFeedback">
        按用户查询
      </el-button>
      <el-button :icon="Refresh" :loading="loading" @click="loadAllFeedback">查看全部</el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <span class="result-count">{{ listTitle }}</span>
      </div>

      <el-empty v-if="!loading && feedbacks.length === 0" description="暂无反馈记录" class="empty" />

      <div v-else v-loading="loading" class="feedback-list">
        <div
          v-for="fb in feedbacks"
          :key="fb.id"
          class="feedback-item"
          :class="`type-${String(fb.feedbackType || '').toLowerCase()}`"
        >
          <div class="fb-header">
            <el-tag :type="typeColor(fb.feedbackType)" size="small" class="fb-type">
              {{ typeLabel(fb.feedbackType) }}
            </el-tag>
            <span class="fb-id">#{{ fb.id }}</span>
            <span class="fb-time">{{ formatDate(fb.createdAt) }}</span>
          </div>

          <p class="fb-content">{{ fb.content || '-' }}</p>

          <div class="fb-meta">
            <span>{{ fb.userId ? `用户 #${fb.userId}` : '匿名用户' }}</span>
            <span v-if="fb.recommendationId">推荐 #{{ fb.recommendationId }}</span>
            <span v-if="fb.rating">评分 {{ fb.rating }}/5</span>
            <span v-if="fb.useful !== null && fb.useful !== undefined">
              {{ fb.useful ? '认为有帮助' : '认为帮助不大' }}
            </span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { feedbackApi } from '@/api/feedback'

const userId = ref(null)
const loading = ref(false)
const feedbacks = ref([])
const filterUserId = ref(null)

const typeMap = {
  ACCEPTED: '已采纳',
  REJECTED: '已忽略',
  COMMENT: '使用建议',
  COMPLAINT: '问题反馈'
}
const typeColorMap = {
  ACCEPTED: 'success',
  REJECTED: 'info',
  COMMENT: 'success',
  COMPLAINT: 'warning'
}

const listTitle = computed(() => {
  if (filterUserId.value) {
    return `用户 #${filterUserId.value} 的反馈共 ${feedbacks.value.length} 条`
  }
  return `全部反馈共 ${feedbacks.value.length} 条`
})

function typeLabel(type) {
  return typeMap[type] || type || '-'
}

function typeColor(type) {
  return typeColorMap[type] || 'info'
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

async function loadAllFeedback() {
  loading.value = true
  try {
    const res = await feedbackApi.listAll()
    feedbacks.value = res.data || []
    filterUserId.value = null
  } finally {
    loading.value = false
  }
}

async function loadFeedback() {
  if (!userId.value) {
    await loadAllFeedback()
    return
  }

  loading.value = true
  try {
    const res = await feedbackApi.listByUser(userId.value)
    feedbacks.value = res.data || []
    filterUserId.value = userId.value
  } finally {
    loading.value = false
  }
}

onMounted(loadAllFeedback)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.query-label { font-size: 14px; color: #9ca3af; }

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

.feedback-list { display: flex; flex-direction: column; gap: 12px; min-height: 80px; }

.feedback-item {
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  border-left: 3px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 14px 16px;
  transition: background 0.2s;
}
.feedback-item:hover { background: rgba(255,255,255,0.04); }
.feedback-item.type-complaint { border-left-color: #f59e0b; }
.feedback-item.type-comment { border-left-color: #10b981; }
.feedback-item.type-accepted { border-left-color: #22c55e; }
.feedback-item.type-rejected { border-left-color: #6b7280; }

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

.fb-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 10px;
  font-size: 12px;
  color: #6b7280;
}
</style>
