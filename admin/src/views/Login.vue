<template>
  <div class="login-wrapper">
    <!-- 背景装饰 -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <div class="login-card">
      <!-- Logo -->
      <div class="logo-area">
        <span class="logo-icon">🥗</span>
        <div>
          <h1 class="logo-title">食乜</h1>
          <p class="logo-sub">管理后台</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="login-btn"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>

      <p class="login-hint">
        <el-icon><InfoFilled /></el-icon>
        请使用拥有 ADMIN 角色的账号登录
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post('/auth/login', {
      username: form.username,
      password: form.password
    })
    const data = res.data
    // 检查 ADMIN 权限
    if (data.user?.role !== 'ADMIN') {
      ElMessage.error('权限不足，请使用管理员账号登录')
      return
    }
    auth.login(data.token, data.user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f1117;
  position: relative;
  overflow: hidden;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}
.bg-glow-1 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(16,185,129,0.15) 0%, transparent 70%);
  top: -100px; left: -100px;
}
.bg-glow-2 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(99,102,241,0.1) 0%, transparent 70%);
  bottom: -50px; right: -50px;
}

.login-card {
  width: 400px;
  background: rgba(26, 29, 39, 0.9);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 20px;
  padding: 40px 36px;
  backdrop-filter: blur(20px);
  box-shadow: 0 24px 64px rgba(0,0,0,0.4);
  position: relative;
  z-index: 1;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 36px;
}
.logo-icon { font-size: 40px; }
.logo-title {
  font-size: 22px;
  font-weight: 700;
  color: #e5e7eb;
  line-height: 1.2;
}
.logo-sub {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

:deep(.el-form-item__label) {
  color: #9ca3af;
  font-size: 13px;
}
:deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.04) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  box-shadow: none !important;
  border-radius: 10px;
}
:deep(.el-input__wrapper:hover) {
  border-color: rgba(16,185,129,0.4) !important;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: #10b981 !important;
}
:deep(.el-input__inner) { color: #e5e7eb; }

.login-btn {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
  letter-spacing: 2px;
}
.login-btn:hover {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.login-hint {
  margin-top: 20px;
  font-size: 12px;
  color: #4b5563;
  display: flex;
  align-items: center;
  gap: 5px;
  justify-content: center;
}
</style>
