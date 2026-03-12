<template>
  <el-card class="business-card stat-card" shadow="never">
    <div class="stat-content">
      <span class="stat-title">{{ title }}</span>
      <div class="stat-value">{{ value }}</div>
      <span v-if="hint" class="stat-hint">{{ hint }}</span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  value: string | number;
  hint?: string;
}>();
</script>

<style scoped>
.stat-card {
  min-height: 108px;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-title {
  color: var(--el-text-color-regular);
  font-size: 14px;
  font-weight: 500;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.stat-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
