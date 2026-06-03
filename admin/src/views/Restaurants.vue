<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="openAdd">添加餐厅</el-button>
      <el-button :icon="Refresh" @click="resetAndLoad">刷新列表</el-button>
      <el-tooltip content="使用附近查询功能，可输入坐标检索">
        <el-button :icon="Location" @click="nearbyDrawer = true">附近查询</el-button>
      </el-tooltip>
    </div>

    <!-- 说明提示 -->
    <el-alert
      title="餐厅查询需要地理坐标，请使用「附近查询」功能检索或直接输入 ID 查询单个餐厅"
      type="info"
      :closable="false"
      show-icon
      class="info-alert"
    />

    <!-- ID 查询 -->
    <el-card class="table-card" shadow="never">
      <div class="id-query-bar">
        <span class="query-label">按 ID 查询餐厅</span>
        <el-input-number v-model="queryId" :min="1" placeholder="餐厅 ID" style="width:160px" controls-position="right" />
        <el-button type="primary" @click="queryById">查询</el-button>
      </div>

      <!-- 单个餐厅结果 -->
      <template v-if="singleRestaurant">
        <el-divider />
        <RestaurantCard :restaurant="singleRestaurant" @view-menu="openMenu" />
      </template>

      <!-- 附近查询结果 -->
      <template v-if="nearbyList.length">
        <el-divider />
        <div class="result-title">附近餐厅 ({{ nearbyList.length }} 家)</div>
        <div class="restaurant-grid">
          <RestaurantCard
            v-for="r in nearbyList"
            :key="r.id"
            :restaurant="r"
            @view-menu="openMenu"
          />
        </div>
      </template>
    </el-card>

    <!-- 添加餐厅对话框 -->
    <el-dialog v-model="addVisible" title="添加餐厅" width="520px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="餐厅名称" prop="name">
          <el-input v-model="addForm.name" placeholder="餐厅全称" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="addForm.address" placeholder="详细地址" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="addForm.latitude" :precision="6" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="addForm.longitude" :precision="6" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="addForm.status" style="width:100%">
            <el-option label="营业中" value="OPEN" />
            <el-option label="休息中" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="addForm.phone" placeholder="联系电话" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAdd">保存</el-button>
      </template>
    </el-dialog>

    <!-- 附近查询抽屉 -->
    <el-drawer v-model="nearbyDrawer" title="附近餐厅查询" size="360px">
      <el-form :model="nearbyForm" label-width="80px">
        <el-form-item label="纬度">
          <el-input-number v-model="nearbyForm.latitude" :precision="6" style="width:100%" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="nearbyForm.longitude" :precision="6" style="width:100%" />
        </el-form-item>
        <el-form-item label="最大距离">
          <el-input-number v-model="nearbyForm.maxDistanceKm" :min="1" :max="100" style="width:100%" />
          <span class="unit-hint">km</span>
        </el-form-item>
        <el-form-item label="仅营业中">
          <el-switch v-model="nearbyForm.onlyOpen" />
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="nearbyLoading" @click="queryNearby">开始查询</el-button>
      </el-form>
    </el-drawer>

    <!-- 菜单抽屉 -->
    <el-drawer v-model="menuDrawer" :title="`${menuRestaurant?.name || ''} 菜单`" size="440px">
      <div class="menu-toolbar">
        <el-button type="primary" size="small" :icon="Plus" @click="menuAddVisible = true">添加菜品</el-button>
      </div>
      <el-table v-loading="menuLoading" :data="menuItems" class="admin-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="菜品名称" min-width="120" />
        <el-table-column prop="price" label="价格(元)" width="100">
          <template #default="{ row }">
            <span class="price-val">¥{{ row.price ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="caloriesEstimate" label="热量(估)" width="100" />
      </el-table>
    </el-drawer>

    <!-- 添加菜品 -->
    <el-dialog v-model="menuAddVisible" title="添加菜品" width="400px" destroy-on-close>
      <el-form ref="menuFormRef" :model="menuForm" :rules="menuRules" label-width="90px">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="menuForm.name" />
        </el-form-item>
        <el-form-item label="价格(元)">
          <el-input-number v-model="menuForm.price" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="热量估算">
          <el-input-number v-model="menuForm.caloriesEstimate" :min="0" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuAddVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuSaving" @click="handleAddMenu">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, defineComponent } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Location } from '@element-plus/icons-vue'
import { restaurantApi } from '@/api/restaurant'

// 内联餐厅卡组件
const RestaurantCard = defineComponent({
  props: ['restaurant'],
  emits: ['view-menu'],
  template: `
    <div class="r-card">
      <div class="r-header">
        <span class="r-name">{{ restaurant.name }}</span>
        <el-tag :type="restaurant.status === 'OPEN' ? 'success' : 'info'" size="small">
          {{ restaurant.status === 'OPEN' ? '营业中' : '休息中' }}
        </el-tag>
      </div>
      <p class="r-addr"><el-icon><Location /></el-icon> {{ restaurant.address || '—' }}</p>
      <p class="r-coord">{{ restaurant.latitude }}, {{ restaurant.longitude }}</p>
      <el-button size="small" @click="$emit('view-menu', restaurant)">查看菜单</el-button>
    </div>
  `
})

const queryId = ref(1)
const singleRestaurant = ref(null)
const nearbyList = ref([])
const nearbyDrawer = ref(false)
const nearbyLoading = ref(false)
const addVisible = ref(false)
const saving = ref(false)
const addFormRef = ref()
const menuDrawer = ref(false)
const menuRestaurant = ref(null)
const menuItems = ref([])
const menuLoading = ref(false)
const menuAddVisible = ref(false)
const menuSaving = ref(false)
const menuFormRef = ref()

const addForm = reactive({ name: '', address: '', latitude: 30.0, longitude: 120.0, status: 'OPEN', phone: '' })
const addRules = {
  name: [{ required: true, message: '请输入餐厅名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

const nearbyForm = reactive({ latitude: 30.0, longitude: 120.0, maxDistanceKm: 5, onlyOpen: true })
const menuForm = reactive({ name: '', price: 0, caloriesEstimate: 0 })
const menuRules = { name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }] }

async function queryById() {
  const res = await restaurantApi.get(queryId.value)
  singleRestaurant.value = res.data
}

async function queryNearby() {
  nearbyLoading.value = true
  try {
    const res = await restaurantApi.nearby(nearbyForm)
    nearbyList.value = res.data || []
    nearbyDrawer.value = false
    ElMessage.success(`找到 ${nearbyList.value.length} 家餐厅`)
  } finally { nearbyLoading.value = false }
}

function openAdd() { Object.assign(addForm, { name: '', address: '', latitude: 30.0, longitude: 120.0, status: 'OPEN', phone: '' }); addVisible.value = true }
function resetAndLoad() { singleRestaurant.value = null; nearbyList.value = [] }

async function handleAdd() {
  await addFormRef.value.validate()
  saving.value = true
  try {
    await restaurantApi.save({ ...addForm })
    ElMessage.success('添加成功')
    addVisible.value = false
  } finally { saving.value = false }
}

async function openMenu(r) {
  menuRestaurant.value = r
  menuDrawer.value = true
  menuLoading.value = true
  try {
    const res = await restaurantApi.getMenu(r.id)
    menuItems.value = res.data || []
  } finally { menuLoading.value = false }
}

async function handleAddMenu() {
  await menuFormRef.value.validate()
  menuSaving.value = true
  try {
    await restaurantApi.addMenuItem(menuRestaurant.value.id, { ...menuForm })
    ElMessage.success('添加成功')
    menuAddVisible.value = false
    const res = await restaurantApi.getMenu(menuRestaurant.value.id)
    menuItems.value = res.data || []
  } finally { menuSaving.value = false }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.info-alert { background: rgba(16,185,129,0.06) !important; border: 1px solid rgba(16,185,129,0.2) !important; border-radius: 10px; }
:deep(.el-alert__title) { color: #9ca3af; font-size: 13px; }

.table-card {
  background: #1a1d27 !important;
  border: 1px solid rgba(255,255,255,0.05) !important;
  border-radius: 14px !important;
}

.id-query-bar { display: flex; align-items: center; gap: 12px; }
.query-label { font-size: 14px; color: #9ca3af; }

.result-title { font-size: 14px; color: #9ca3af; margin-bottom: 12px; }

.restaurant-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }

.r-card {
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 16px;
  display: flex; flex-direction: column; gap: 8px;
}
.r-header { display: flex; align-items: center; justify-content: space-between; }
.r-name { font-size: 15px; font-weight: 600; color: #e5e7eb; }
.r-addr { font-size: 13px; color: #6b7280; display: flex; align-items: center; gap: 4px; }
.r-coord { font-size: 12px; color: #4b5563; font-family: monospace; }

.menu-toolbar { margin-bottom: 16px; }

.admin-table { width: 100%; }
:deep(.el-table) { background: transparent !important; }
:deep(.el-table tr) { background: transparent !important; }
:deep(.el-table th) { background: rgba(255,255,255,0.02) !important; color: #6b7280 !important; font-size: 13px; border-bottom: 1px solid rgba(255,255,255,0.06) !important; }
:deep(.el-table td) { border-bottom: 1px solid rgba(255,255,255,0.04) !important; color: #d1d5db; }

.price-val { color: #f59e0b; font-weight: 600; }
.unit-hint { font-size: 12px; color: #6b7280; margin-left: 8px; }

/* 对话框/抽屉暗色 */
:deep(.el-dialog) { background: #1a1d27 !important; border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; }
:deep(.el-dialog__header) { border-bottom: 1px solid rgba(255,255,255,0.06); }
:deep(.el-dialog__title) { color: #e5e7eb; font-weight: 600; }
:deep(.el-form-item__label) { color: #9ca3af; }
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.04) !important; border: 1px solid rgba(255,255,255,0.08) !important; box-shadow: none !important; }
:deep(.el-input__inner) { color: #e5e7eb !important; }
:deep(.el-drawer__header) { color: #e5e7eb; font-weight: 600; border-bottom: 1px solid rgba(255,255,255,0.06); }
:deep(.el-drawer__body) { background: #13161f; padding: 20px; overflow-y: auto; }
:deep(.el-dialog__footer) { border-top: 1px solid rgba(255,255,255,0.06); }
</style>
