<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="success" :icon="Monitor" :loading="browserLoading" @click="openBrowser">
        打开养号浏览器
      </el-button>
      <el-button :icon="Refresh" :loading="browserLoading" @click="loadBrowserStatus">检查浏览器</el-button>
      <el-button
        :type="captureStatus?.listening ? 'warning' : 'primary'"
        :icon="Connection"
        :loading="captureLoading"
        @click="toggleCapture"
      >
        {{ captureStatus?.listening ? '停止监听' : '开始监听' }}
      </el-button>
      <el-button :icon="Refresh" :loading="refreshing" @click="refreshAll">手动刷新</el-button>
      <el-button :icon="RefreshLeft" :loading="cleanupLoading" @click="previewCleanup">清理预览</el-button>
      <el-button type="danger" plain :icon="DeleteIcon" :loading="cleanupLoading" @click="cleanupBadData">
        清理不合格数据
      </el-button>
      <el-select v-model="filters.status" clearable placeholder="全部状态" class="status-filter" @change="loadTasks">
        <el-option label="待查询" value="PENDING" />
        <el-option label="查询中" value="RUNNING" />
        <el-option label="查询成功" value="SUCCESS" />
        <el-option label="查询失败" value="FAILED" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜索店铺或地址"
        clearable
        class="keyword-filter"
        @keyup.enter="loadTasks"
        @clear="loadTasks"
      />
    </div>

    <div class="status-grid">
      <div class="status-strip">
        <el-tag :type="browserStatus?.connected ? 'success' : 'warning'" size="large">
          {{ browserStatus?.connected ? '养号浏览器已连接' : '养号浏览器未连接' }}
        </el-tag>
        <span>{{ browserStatus?.message || '等待浏览器状态' }}</span>
        <code v-if="browserStatus?.debuggerAddress">{{ browserStatus.debuggerAddress }}</code>
      </div>

      <div class="status-strip">
        <el-tag :type="captureStatus?.listening ? 'success' : captureStatus?.capturedCount ? 'primary' : 'info'" size="large">
          {{ captureBadgeText }}
        </el-tag>
        <span>{{ captureStatus?.message || '等待监听状态' }}</span>
        <code v-if="captureStatus?.latestEndpoint">{{ captureStatus.latestEndpoint }}</code>
      </div>
    </div>

    <div class="stats-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <span class="stat-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <el-card class="panel" shadow="never">
      <template #header>
        <div class="card-header">
          <span>已捕获店铺</span>
          <el-button size="small" :icon="Refresh" :loading="capturesLoading" @click="loadCapturedShops">
            刷新店铺
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="capturesLoading"
        :data="capturedShops"
        row-key="captureKey"
        class="admin-table"
        highlight-current-row
        @current-change="selectCapture"
      >
        <el-table-column prop="restaurantName" label="店铺" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.restaurantName || `美团店铺-${row.meituanPoiId || row.captureKey}` }}</template>
        </el-table-column>
        <el-table-column prop="meituanPoiId" label="美团ID" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.meituanPoiId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="菜品数" width="90" />
        <el-table-column prop="missingNutritionCount" label="待补营养" width="100" />
        <el-table-column prop="endpointCount" label="响应数" width="90" />
        <el-table-column prop="importedRestaurantId" label="餐厅ID" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.importedRestaurantId" type="success" size="small">{{ row.importedRestaurantId }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="latestCapturedAt" label="最近捕获" width="170">
          <template #default="{ row }">{{ formatTime(row.latestCapturedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Download" :loading="importingKey === row.captureKey" @click.stop="importCapture(row)">
              导入
            </el-button>
            <el-button link type="danger" :icon="DeleteIcon" @click.stop="deleteCapture(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="panel" shadow="never">
      <template #header>
        <div class="card-header">
          <span>菜单预览</span>
          <div class="header-actions">
            <el-tag v-if="selectedCapture" type="primary">
              {{ selectedCapture.restaurantName || selectedCapture.meituanPoiId || selectedCapture.captureKey }}
            </el-tag>
            <el-tag v-if="lastImportResult?.restaurantId" type="success">
              已导入餐厅ID：{{ lastImportResult.restaurantId }}
            </el-tag>
            <el-button size="small" :icon="Refresh" :loading="previewLoading" @click="loadCapturePreview(selectedCaptureKey)">
              刷新预览
            </el-button>
          </div>
        </div>
      </template>
      <div class="preview-summary">
        <span>店铺：{{ capturePreview.restaurantName || '-' }}</span>
        <span>美团ID：{{ capturePreview.meituanPoiId || '-' }}</span>
        <span>菜品数：{{ previewItems.length }}</span>
        <span>待补营养：{{ previewItems.length }}</span>
      </div>
      <el-table v-loading="previewLoading" :data="previewItems" height="300" class="admin-table">
        <el-table-column prop="name" label="菜品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.category || '未分类' }}</template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="90">
          <template #default="{ row }">{{ row.price ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="portionSize" label="规格" width="140" show-overflow-tooltip />
        <el-table-column prop="available" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.available === false ? 'info' : 'success'" size="small">
              {{ row.available === false ? '不可售' : '可售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="externalId" label="菜品ID" width="150" show-overflow-tooltip />
        <el-table-column prop="skuId" label="sku" width="130" show-overflow-tooltip />
        <el-table-column prop="imageUrl" label="图片" width="90">
          <template #default="{ row }">
            <el-link v-if="row.imageUrl" :href="row.imageUrl" target="_blank" type="primary">查看</el-link>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card class="panel" shadow="never">
          <template #header>添加单个店铺</template>
          <el-form :model="singleForm" label-width="86px">
            <el-form-item label="店铺名">
              <el-input v-model="singleForm.shopName" placeholder="店铺名称" />
            </el-form-item>
            <el-form-item label="地址">
              <el-input v-model="singleForm.address" placeholder="地图 API 返回的地址" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="纬度">
                  <el-input-number v-model="singleForm.latitude" :precision="6" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="经度">
                  <el-input-number v-model="singleForm.longitude" :precision="6" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="美团ID">
              <el-input v-model="singleForm.meituanPoiId" placeholder="poi_id_str，可选" />
            </el-form-item>
            <el-form-item label="菜单URL">
              <el-input v-model="singleForm.menuUrl" placeholder="美团 H5 菜单页，可选" />
            </el-form-item>
            <el-button type="primary" :icon="Plus" :loading="saving" @click="addSingle">加入待查询</el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card class="panel" shadow="never">
          <template #header>批量导入</template>
          <el-input
            v-model="batchText"
            type="textarea"
            :rows="8"
            placeholder="每行一个店铺：店铺名,地址,纬度,经度,poi_id_str,菜单URL"
          />
          <div class="batch-actions">
            <el-button type="primary" :icon="Upload" :loading="saving" @click="addBatch">批量加入</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" shadow="never">
      <template #header>待查询列表</template>
      <el-table v-loading="loading" :data="tasks" row-key="id" class="admin-table">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="shopName" label="店铺" min-width="180" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="importedItemCount" label="菜品数" width="90" />
        <el-table-column prop="attemptCount" label="次数" width="80" />
        <el-table-column prop="meituanPoiId" label="美团ID" width="180" show-overflow-tooltip />
        <el-table-column prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'FAILED' || row.status === 'RUNNING'"
              link
              type="primary"
              :icon="RefreshLeft"
              @click="retry(row)"
            >
              {{ row.status === 'RUNNING' ? '重置' : '重试' }}
            </el-button>
            <el-button link type="danger" :icon="DeleteIcon" @click="deleteTask(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Connection,
  Delete as DeleteIcon,
  Download,
  Monitor,
  Plus,
  Refresh,
  RefreshLeft,
  Upload
} from '@element-plus/icons-vue'
import { meituanCrawlApi } from '@/api/meituanCrawl'

const tasks = ref([])
const capturedShops = ref([])
const loading = ref(false)
const capturesLoading = ref(false)
const saving = ref(false)
const importingKey = ref('')
const cleanupLoading = ref(false)
const refreshing = ref(false)
const browserLoading = ref(false)
const captureLoading = ref(false)
const previewLoading = ref(false)
const browserStatus = ref(null)
const captureStatus = ref(null)
const capturePreview = ref({ items: [] })
const selectedCaptureKey = ref('')
const lastImportResult = ref(null)
const batchText = ref('')

const filters = reactive({ status: '', keyword: '' })
const singleForm = reactive({
  shopName: '',
  address: '',
  latitude: 40.069805,
  longitude: 116.438572,
  meituanPoiId: '',
  menuUrl: ''
})

const previewItems = computed(() => capturePreview.value?.items || [])
const selectedCapture = computed(() => capturedShops.value.find((item) => item.captureKey === selectedCaptureKey.value))
const captureBadgeText = computed(() => {
  if (captureStatus.value?.listening) return '正在监听'
  if (captureStatus.value?.capturedCount) return '已有捕获'
  return '未捕获'
})
const stats = computed(() => {
  const counts = tasks.value.reduce((acc, item) => {
    acc[item.status] = (acc[item.status] || 0) + 1
    return acc
  }, {})
  return [
    { label: '待查询条目', value: tasks.value.length },
    { label: '待查询', value: counts.PENDING || 0 },
    { label: '查询中', value: counts.RUNNING || 0 },
    { label: '查询成功', value: counts.SUCCESS || 0 },
    { label: '查询失败', value: counts.FAILED || 0 },
    { label: '捕获店铺', value: capturedShops.value.length || captureStatus.value?.capturedShopCount || 0 },
    { label: '菜单响应', value: captureStatus.value?.menuResponseCount || 0 },
    { label: '预览菜品', value: previewItems.value.length }
  ]
})

onMounted(() => {
  refreshAll()
})

async function refreshAll() {
  refreshing.value = true
  try {
    await Promise.all([loadTasks(), loadBrowserStatus(), loadCaptureStatus()])
    await loadCapturedShops()
    await loadCapturePreview(selectedCaptureKey.value)
  } finally {
    refreshing.value = false
  }
}

async function openBrowser() {
  browserLoading.value = true
  try {
    const res = await meituanCrawlApi.openBrowser()
    browserStatus.value = res.data
    ElMessage.success(res.data?.message || '养号浏览器已打开')
  } finally {
    browserLoading.value = false
  }
}

async function loadBrowserStatus() {
  browserLoading.value = true
  try {
    const res = await meituanCrawlApi.browserStatus()
    browserStatus.value = res.data
  } finally {
    browserLoading.value = false
  }
}

async function loadCaptureStatus() {
  const res = await meituanCrawlApi.captureStatus()
  captureStatus.value = res.data
}

async function loadCapturedShops() {
  capturesLoading.value = true
  try {
    const res = await meituanCrawlApi.captures()
    capturedShops.value = res.data || []
    if (!selectedCaptureKey.value && capturedShops.value.length) {
      selectedCaptureKey.value = capturedShops.value[0].captureKey
    }
    if (selectedCaptureKey.value && !capturedShops.value.some((item) => item.captureKey === selectedCaptureKey.value)) {
      selectedCaptureKey.value = capturedShops.value[0]?.captureKey || ''
    }
  } finally {
    capturesLoading.value = false
  }
}

async function loadCapturePreview(captureKey) {
  previewLoading.value = true
  try {
    const res = captureKey
      ? await meituanCrawlApi.capturePreviewByKey(captureKey)
      : await meituanCrawlApi.capturePreview()
    capturePreview.value = res.data || { items: [] }
  } finally {
    previewLoading.value = false
  }
}

async function toggleCapture() {
  captureLoading.value = true
  try {
    const res = captureStatus.value?.listening
      ? await meituanCrawlApi.stopCapture()
      : await meituanCrawlApi.startCapture()
    captureStatus.value = res.data
    ElMessage.success(res.data?.message || '监听状态已更新')
    await loadCapturedShops()
    await loadCapturePreview(selectedCaptureKey.value)
  } finally {
    captureLoading.value = false
  }
}

async function selectCapture(row) {
  if (!row?.captureKey || row.captureKey === selectedCaptureKey.value) return
  selectedCaptureKey.value = row.captureKey
  await loadCapturePreview(row.captureKey)
}

async function importCapture(row) {
  importingKey.value = row.captureKey
  try {
    const res = await meituanCrawlApi.importCapture(row.captureKey)
    lastImportResult.value = res.data
    ElMessage.success(res.data?.message || `已导入 ${res.data?.itemCount || 0} 个菜品`)
    await Promise.all([loadCapturedShops(), loadCaptureStatus(), loadTasks()])
  } finally {
    importingKey.value = ''
  }
}

async function deleteCapture(row) {
  await ElMessageBox.confirm(`确定删除“${row.restaurantName || row.meituanPoiId || row.captureKey}”的捕获缓存吗？`, '删除捕获缓存', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await meituanCrawlApi.deleteCapture(row.captureKey)
  ElMessage.success('已删除捕获缓存')
  if (selectedCaptureKey.value === row.captureKey) {
    selectedCaptureKey.value = ''
    capturePreview.value = { items: [] }
  }
  await loadCapturedShops()
}

async function previewCleanup() {
  cleanupLoading.value = true
  try {
    const res = await meituanCrawlApi.cleanupPreview()
    const data = res.data || {}
    ElMessage.info(`可清理：餐厅 ${data.restaurantCount || 0}，食品 ${data.foodItemCount || 0}，菜单 ${data.menuItemCount || 0}`)
  } finally {
    cleanupLoading.value = false
  }
}

async function cleanupBadData() {
  const preview = await meituanCrawlApi.cleanupPreview()
  const data = preview.data || {}
  await ElMessageBox.confirm(
    `将清理早期不合格美团数据：餐厅 ${data.restaurantCount || 0}，食品 ${data.foodItemCount || 0}，菜单 ${data.menuItemCount || 0}。确定继续吗？`,
    '清理不合格数据',
    {
      type: 'warning',
      confirmButtonText: '清理',
      cancelButtonText: '取消'
    }
  )
  cleanupLoading.value = true
  try {
    const res = await meituanCrawlApi.cleanupBadData()
    ElMessage.success(res.data?.message || '清理完成')
    await refreshAll()
  } finally {
    cleanupLoading.value = false
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await meituanCrawlApi.list({
      status: filters.status || undefined,
      keyword: filters.keyword || undefined
    })
    tasks.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function addSingle() {
  if (!singleForm.shopName.trim()) {
    ElMessage.warning('请填写店铺名')
    return
  }
  await saveTasks([{ ...singleForm }])
  Object.assign(singleForm, {
    shopName: '',
    address: '',
    latitude: 40.069805,
    longitude: 116.438572,
    meituanPoiId: '',
    menuUrl: ''
  })
}

async function addBatch() {
  const parsed = batchText.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map(parseLine)
    .filter(Boolean)
  if (!parsed.length) {
    ElMessage.warning('没有可导入的店铺')
    return
  }
  await saveTasks(parsed)
  batchText.value = ''
}

async function saveTasks(items) {
  saving.value = true
  try {
    await meituanCrawlApi.createTasks(items)
    ElMessage.success(`已加入 ${items.length} 个店铺`)
    await loadTasks()
  } finally {
    saving.value = false
  }
}

async function retry(row) {
  await meituanCrawlApi.retry(row.id)
  ElMessage.success('已重置为待查询')
  await loadTasks()
}

async function deleteTask(row) {
  await ElMessageBox.confirm(`确定删除“${row.shopName}”这条查询任务吗？`, '删除查询任务', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await meituanCrawlApi.deleteTask(row.id)
  ElMessage.success('已删除')
  await loadTasks()
}

function parseLine(line) {
  try {
    if (line.startsWith('{')) {
      return JSON.parse(line)
    }
  } catch {
    return null
  }
  const [shopName, address, latitude, longitude, meituanPoiId, menuUrl] = line.split(/,|\t/).map((item) => item.trim())
  if (!shopName) return null
  return {
    shopName,
    address,
    latitude: latitude ? Number(latitude) : undefined,
    longitude: longitude ? Number(longitude) : undefined,
    meituanPoiId,
    menuUrl
  }
}

function statusLabel(status) {
  return {
    PENDING: '待查询',
    RUNNING: '查询中',
    SUCCESS: '查询成功',
    FAILED: '查询失败'
  }[status] || status
}

function statusType(status) {
  return {
    PENDING: 'info',
    RUNNING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger'
  }[status] || 'info'
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.status-filter { width: 150px; }
.keyword-filter { width: 240px; }
.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.status-strip {
  min-height: 48px;
  padding: 10px 14px;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  background: rgba(255,255,255,0.035);
  display: flex;
  align-items: center;
  gap: 12px;
  color: #d1d5db;
}
.status-strip span {
  flex: 1;
}
.status-strip code {
  color: #9ca3af;
  font-size: 12px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(110px, 1fr));
  gap: 12px;
}
.stat-card {
  background: #1a1d27;
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stat-label {
  color: #9ca3af;
  font-size: 13px;
}
.stat-card strong {
  color: #e5e7eb;
  font-size: 24px;
}
.panel,
.table-card {
  background: #1a1d27 !important;
  border: 1px solid rgba(255,255,255,0.06) !important;
  border-radius: 8px !important;
}
.card-header,
.header-actions,
.preview-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.card-header {
  justify-content: space-between;
}
.preview-summary {
  color: #d1d5db;
  margin-bottom: 12px;
}
:deep(.el-card__header) {
  color: #e5e7eb;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
:deep(.el-form-item__label) { color: #9ca3af; }
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  background: rgba(255,255,255,0.04) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  box-shadow: none !important;
  color: #e5e7eb !important;
}
.batch-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.admin-table { width: 100%; }
:deep(.el-table) { background: transparent !important; }
:deep(.el-table tr) { background: transparent !important; }
:deep(.el-table th) {
  background: rgba(255,255,255,0.02) !important;
  color: #6b7280 !important;
  border-bottom: 1px solid rgba(255,255,255,0.06) !important;
}
:deep(.el-table td) {
  border-bottom: 1px solid rgba(255,255,255,0.04) !important;
  color: #d1d5db;
}
@media (max-width: 1100px) {
  .status-grid,
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
