<template>
  <el-card class="stat-card" shadow="hover">
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
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-title {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.stat-hint {
  color: var(--text-tertiary);
  font-size: 12px;
}
</style>
