<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
html, body, #app {
  height: 100%;
  background: #0f1117;
  color: #e5e7eb;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
/* 整体暗色覆盖 */
:root {
  --el-bg-color: #1a1d27;
  --el-bg-color-page: #0f1117;
  --el-bg-color-overlay: #1e2130;
  --el-text-color-primary: #e5e7eb;
  --el-text-color-regular: #9ca3af;
  --el-border-color: #2d3141;
  --el-border-color-light: #252838;
  --el-color-primary: #10b981;
  --el-color-primary-light-3: #059669;
  --el-color-primary-light-5: #047857;
  --el-color-primary-light-7: #065f46;
  --el-color-primary-light-8: #064e3b;
  --el-color-primary-light-9: #022c22;
  --el-color-primary-dark-2: #34d399;
  --el-fill-color: #1e2130;
  --el-fill-color-light: #252838;
  --el-fill-color-blank: #1a1d27;
}
</style>
