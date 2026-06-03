<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索食品名称"
        clearable
        :prefix-icon="Search"
        style="width: 220px"
        @input="debounceSearch"
        @clear="loadFoods"
      />
      <el-select v-model="category" placeholder="分类筛选" clearable style="width: 160px" @change="loadFoods">
        <el-option label="谷物" value="谷物" />
        <el-option label="蔬菜" value="蔬菜" />
        <el-option label="水果" value="水果" />
        <el-option label="肉类" value="肉类" />
        <el-option label="奶制品" value="奶制品" />
        <el-option label="豆制品" value="豆制品" />
        <el-option label="坚果" value="坚果" />
        <el-option label="饮品" value="饮品" />
        <el-option label="未分类" value="未分类" />
        <el-option label="其他" value="其他" />
      </el-select>
      <el-select v-model="nutritionStatus" placeholder="营养状态" clearable style="width: 150px" @change="loadFoods">
        <el-option label="待补全" value="PENDING" />
        <el-option label="已完整" value="COMPLETE" />
      </el-select>
      <el-button
        type="success"
        :icon="MagicStick"
        :loading="previewLoading"
        @click="handlePreviewNutrition"
      >
        AI 生成补全预览
      </el-button>
      <el-button type="primary" :icon="Plus" @click="openForm(null)">添加食品</el-button>
      <el-button :icon="Refresh" @click="loadFoods">刷新</el-button>
      <span class="total-hint">共 {{ foods.length }} 条</span>
    </div>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="foods" row-key="id" class="admin-table">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="食品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nutritionStatus" label="营养状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.nutritionStatus === 'COMPLETE' ? 'success' : 'warning'" size="small">
              {{ row.nutritionStatus === 'COMPLETE' ? '已完整' : '待补全' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="caloriesPer100g" label="热量(kcal/100g)" width="150">
          <template #default="{ row }">
            <span class="calorie-val">{{ row.caloriesPer100g ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="proteinPer100g" label="蛋白质(g)" width="110" />
        <el-table-column prop="fatPer100g" label="脂肪(g)" width="100" />
        <el-table-column prop="carbPer100g" label="碳水(g)" width="100" />
        <el-table-column prop="isRecommended" label="推荐" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isRecommended ? 'success' : 'info'" size="small">
              {{ row.isRecommended ? '启用' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑食品' : '添加食品'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="食品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入食品名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
            <el-option label="谷物" value="谷物" />
            <el-option label="蔬菜" value="蔬菜" />
            <el-option label="水果" value="水果" />
            <el-option label="肉类" value="肉类" />
            <el-option label="奶制品" value="奶制品" />
            <el-option label="豆制品" value="豆制品" />
            <el-option label="坚果" value="坚果" />
            <el-option label="饮品" value="饮品" />
            <el-option label="未分类" value="未分类" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="热量(kcal)" prop="caloriesPer100g">
              <el-input-number v-model="form.caloriesPer100g" :min="0" :max="9999" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="蛋白质(g)" prop="proteinPer100g">
              <el-input-number v-model="form.proteinPer100g" :min="0" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="脂肪(g)" prop="fatPer100g">
              <el-input-number v-model="form.fatPer100g" :min="0" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="碳水(g)" prop="carbPer100g">
              <el-input-number v-model="form.carbPer100g" :min="0" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="进入推荐">
          <el-switch v-model="form.isRecommended" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="食品描述，可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确认保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="AI 营养补全预览" size="82%">
      <div class="preview-head">
        <el-tag :type="nutritionConfig?.llmConfigured ? 'success' : 'warning'">
          LLM：{{ nutritionConfig?.llmConfigured ? nutritionConfig.llmModel || '已配置' : '未配置' }}
        </el-tag>
        <el-tag :type="nutritionConfig?.fdcConfigured ? 'success' : 'warning'">
          USDA FDC：{{ nutritionConfig?.fdcConfigured ? '已配置' : '未配置' }}
        </el-tag>
        <span class="preview-hint">结果为 AI 估算，勾选后才会写入食品表。</span>
      </div>

      <div v-if="nutritionJob" class="preview-progress">
        <div class="progress-line">
          <el-progress
            :percentage="nutritionProgressPercent"
            :stroke-width="10"
            :status="nutritionJob.status === 'FAILED' ? 'exception' : undefined"
          />
          <span class="progress-count">
            {{ nutritionJob.completedCount || 0 }}/{{ nutritionJob.totalCount || 0 }}
          </span>
          <el-tag size="small" :type="jobStatusTagType(nutritionJob.status)">
            {{ jobStatusText(nutritionJob.status) }}
          </el-tag>
          <el-button
            v-if="isPreviewJobActive(nutritionJob)"
            type="warning"
            size="small"
            :loading="cancelLoading"
            @click="handleCancelPreviewJob"
          >
            中断
          </el-button>
        </div>
        <div v-if="nutritionJob.currentFoodName" class="preview-hint">
          正在补全：{{ nutritionJob.currentFoodName }}
        </div>
        <div v-else-if="nutritionJob.cancelRequested && nutritionJob.status === 'CANCELLING'" class="preview-hint">
          已收到中断请求，当前条目完成后会停止。
        </div>
      </div>

      <el-table
        :data="nutritionSuggestions"
        class="admin-table"
        height="560"
        row-key="foodItemId"
        @selection-change="selectedSuggestions = $event"
      >
        <el-table-column type="expand" width="46">
          <template #default="{ row }">
            <div class="nutrition-debug">
              <div><strong>USDA 检索词：</strong>{{ searchQuerySummary(row.searchQueries) }}</div>
              <div v-if="row.parseWarning"><strong>解析警告：</strong>{{ row.parseWarning }}</div>
              <div v-if="row.rawModelResponse">
                <strong>模型原文：</strong>
                <pre>{{ row.rawModelResponse }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column type="selection" width="48" :selectable="isSuggestionValid" />
        <el-table-column prop="foodName" label="食品" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="suggestionStatusType(row)" size="small">
              {{ suggestionStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column prop="caloriesPer100g" label="热量" width="90" />
        <el-table-column prop="proteinPer100g" label="蛋白质" width="90" />
        <el-table-column prop="fatPer100g" label="脂肪" width="80" />
        <el-table-column prop="carbPer100g" label="碳水" width="80" />
        <el-table-column prop="confidence" label="置信度" width="90">
          <template #default="{ row }">
            <span>{{ row.confidence == null ? '—' : `${Math.round(row.confidence * 100)}%` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="USDA 检索词" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ searchQuerySummary(row.searchQueries) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="USDA 参考" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ referenceSummary(row.references) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="依据/失败原因" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.failureReason || row.parseWarning || row.basis || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="preview-footer">
        <span class="preview-hint">已选择 {{ selectedSuggestions.length }} 条</span>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="applyLoading"
          :disabled="selectedSuggestions.length === 0 || isPreviewJobActive(nutritionJob)"
          @click="handleApplyNutrition"
        >
          应用选中建议
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh, Edit, Delete, MagicStick } from '@element-plus/icons-vue'
import { foodApi } from '@/api/food'

const keyword = ref('')
const category = ref('')
const nutritionStatus = ref('')
const loading = ref(false)
const foods = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()
const nutritionConfig = ref(null)
const previewVisible = ref(false)
const previewLoading = ref(false)
const applyLoading = ref(false)
const cancelLoading = ref(false)
const nutritionSuggestions = ref([])
const selectedSuggestions = ref([])
const nutritionJob = ref(null)
let nutritionJobTimer = null

const nutritionProgressPercent = computed(() => {
  const total = nutritionJob.value?.totalCount || 0
  if (!total) return 0
  return Math.round(((nutritionJob.value?.completedCount || 0) / total) * 100)
})

const defaultForm = () => ({
  name: '',
  category: '',
  caloriesPer100g: 0,
  proteinPer100g: 0,
  fatPer100g: 0,
  carbPer100g: 0,
  isRecommended: true,
  description: ''
})
const form = reactive(defaultForm())
const rules = {
  name: [{ required: true, message: '请输入食品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  caloriesPer100g: [{ required: true, message: '请填写热量', trigger: 'blur' }],
  proteinPer100g: [{ required: true, message: '请填写蛋白质', trigger: 'blur' }],
  fatPer100g: [{ required: true, message: '请填写脂肪', trigger: 'blur' }],
  carbPer100g: [{ required: true, message: '请填写碳水', trigger: 'blur' }]
}

async function loadFoods() {
  loading.value = true
  try {
    const res = await foodApi.list(keyword.value || undefined, category.value || undefined, nutritionStatus.value || undefined)
    foods.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadNutritionConfig() {
  const res = await foodApi.nutritionConfigStatus()
  nutritionConfig.value = res.data
}

let timer = null
function debounceSearch() {
  clearTimeout(timer)
  timer = setTimeout(loadFoods, 400)
}

function openForm(row) {
  Object.assign(form, defaultForm())
  if (row) {
    editingId.value = row.id
    Object.assign(form, row)
    form.isRecommended = Boolean(row.isRecommended)
  } else {
    editingId.value = null
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await foodApi.update(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await foodApi.create({ ...form })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadFoods()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除食品“${row.name}”？`, '警告', { type: 'warning' })
  await foodApi.remove(row.id)
  ElMessage.success('已删除')
  loadFoods()
}

async function handlePreviewNutrition() {
  previewLoading.value = true
  try {
    if (!nutritionConfig.value) {
      await loadNutritionConfig()
    }
    if (!nutritionConfig.value?.llmConfigured) {
      ElMessage.warning('LLM Gateway 未配置，预览可能全部失败')
    }
    const pendingRes = await foodApi.pendingNutrition({
      keyword: keyword.value || undefined
    })
    const ids = (pendingRes.data || []).map(item => item.foodItemId).filter(Boolean)
    if (!ids.length) {
      ElMessage.info('没有待补全食品')
      return
    }
    const res = await foodApi.startNutritionPreviewJob(ids)
    nutritionJob.value = res.data
    nutritionSuggestions.value = res.data?.items || []
    selectedSuggestions.value = []
    previewVisible.value = true
    startNutritionJobPolling(res.data?.jobId)
  } finally {
    if (!isPreviewJobActive(nutritionJob.value)) {
      previewLoading.value = false
    }
  }
}

function startNutritionJobPolling(jobId) {
  clearNutritionJobPolling()
  if (!jobId) {
    previewLoading.value = false
    return
  }
  nutritionJobTimer = setInterval(async () => {
    try {
      const res = await foodApi.nutritionPreviewJobStatus(jobId)
      updateNutritionJob(res.data)
      if (!isPreviewJobActive(res.data)) {
        clearNutritionJobPolling()
        previewLoading.value = false
      }
    } catch {
      clearNutritionJobPolling()
      previewLoading.value = false
    }
  }, 1500)
}

function updateNutritionJob(job) {
  nutritionJob.value = job
  nutritionSuggestions.value = job?.items || []
}

function clearNutritionJobPolling() {
  if (nutritionJobTimer) {
    clearInterval(nutritionJobTimer)
    nutritionJobTimer = null
  }
}

async function handleCancelPreviewJob() {
  if (!nutritionJob.value?.jobId) return
  cancelLoading.value = true
  try {
    const res = await foodApi.cancelNutritionPreviewJob(nutritionJob.value.jobId)
    updateNutritionJob(res.data)
    ElMessage.info('已请求中断，当前条目会先完成')
  } finally {
    cancelLoading.value = false
  }
}

async function handleApplyNutrition() {
  const items = selectedSuggestions.value
    .filter(isSuggestionValid)
    .map(item => ({
      foodItemId: item.foodItemId,
      category: item.category,
      caloriesPer100g: item.caloriesPer100g,
      proteinPer100g: item.proteinPer100g,
      fatPer100g: item.fatPer100g,
      carbPer100g: item.carbPer100g
    }))
  if (!items.length) {
    ElMessage.warning('请选择可应用的补全建议')
    return
  }
  await ElMessageBox.confirm(`确认应用 ${items.length} 条 AI 营养补全建议？`, '确认写入', { type: 'warning' })
  applyLoading.value = true
  try {
    const res = await foodApi.applyNutritionSuggestions(items)
    ElMessage.success(`已更新 ${res.data?.updatedCount || 0} 条食品`)
    previewVisible.value = false
    await loadFoods()
  } finally {
    applyLoading.value = false
  }
}

function isSuggestionValid(row) {
  return Boolean(row && !row.failureReason && row.category && row.caloriesPer100g != null
    && row.proteinPer100g != null && row.fatPer100g != null && row.carbPer100g != null)
}

function suggestionStatusText(row) {
  if (!isSuggestionValid(row)) return '失败'
  return row.parseWarning ? '可应用·警告' : '可应用'
}

function suggestionStatusType(row) {
  if (!isSuggestionValid(row)) return 'danger'
  return row.parseWarning ? 'warning' : 'success'
}

function isPreviewJobActive(job) {
  return job && ['PENDING', 'RUNNING', 'CANCELLING'].includes(job.status)
}

function jobStatusText(status) {
  const map = {
    PENDING: '等待中',
    RUNNING: '补全中',
    CANCELLING: '中断中',
    CANCELLED: '已中断',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status || '未开始'
}

function jobStatusTagType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'CANCELLED' || status === 'CANCELLING') return 'warning'
  return 'info'
}

function referenceSummary(references = []) {
  if (!references.length) return '无候选，模型按菜名估算'
  return references.slice(0, 2).map(item => `${item.description || '未命名'}#${item.fdcId}`).join('；')
}

function searchQuerySummary(searchQueries = []) {
  if (!searchQueries.length) return '无英文检索词'
  return searchQueries.join('；')
}

onMounted(() => {
  loadFoods()
  loadNutritionConfig()
})

onBeforeUnmount(clearNutritionJobPolling)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.total-hint { font-size: 13px; color: #6b7280; }

.table-card {
  background: #1a1d27 !important;
  border: 1px solid rgba(255,255,255,0.05) !important;
  border-radius: 8px !important;
}
:deep(.el-card__body) { padding: 0; }

.admin-table { background: transparent !important; width: 100%; }
:deep(.el-table) { background: transparent !important; }
:deep(.el-table tr) { background: transparent !important; }
:deep(.el-table th) {
  background: rgba(255,255,255,0.02) !important;
  color: #6b7280 !important; font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,0.06) !important;
}
:deep(.el-table td) { border-bottom: 1px solid rgba(255,255,255,0.04) !important; color: #d1d5db; }
:deep(.el-table tr:hover td) { background: rgba(16,185,129,0.04) !important; }

.calorie-val { color: #f59e0b; font-weight: 600; }

.preview-head { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.preview-hint { font-size: 13px; color: #9ca3af; }
.preview-progress {
  margin-bottom: 14px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}
.progress-line {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto auto auto;
  align-items: center;
  gap: 12px;
}
.progress-count {
  min-width: 58px;
  color: #e5e7eb;
  font-weight: 600;
  text-align: right;
}
.preview-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
}
.nutrition-debug {
  padding: 12px 16px;
  display: grid;
  gap: 8px;
  color: #d1d5db;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 8px;
}
.nutrition-debug pre {
  margin: 6px 0 0;
  padding: 10px;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: #e5e7eb;
  background: rgba(0,0,0,0.24);
  border-radius: 6px;
}

:deep(.el-dialog) { background: #1a1d27 !important; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; }
:deep(.el-drawer) { background: #1a1d27 !important; color: #e5e7eb; }
:deep(.el-drawer__header) { color: #e5e7eb; margin-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,0.06); padding-bottom: 12px; }
:deep(.el-dialog__header) { border-bottom: 1px solid rgba(255,255,255,0.06); }
:deep(.el-dialog__title) { color: #e5e7eb; font-weight: 600; }
:deep(.el-form-item__label) { color: #9ca3af; }
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.04) !important; border: 1px solid rgba(255,255,255,0.08) !important; box-shadow: none !important; }
:deep(.el-input__inner) { color: #e5e7eb !important; }
:deep(.el-textarea__inner) { background: rgba(255,255,255,0.04) !important; border: 1px solid rgba(255,255,255,0.08) !important; color: #e5e7eb !important; box-shadow: none !important; }
:deep(.el-select .el-input__wrapper) { background: rgba(255,255,255,0.04) !important; }
:deep(.el-dialog__footer) { border-top: 1px solid rgba(255,255,255,0.06); }
</style>
