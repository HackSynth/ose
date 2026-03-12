<template>
  <PageSection title="完成情况">
    <div class="completion-container">
      <div class="progress-item">
        <div class="progress-info">
          <span class="progress-label">本周</span>
          <span class="progress-stat">{{ week?.done || 0 }}/{{ week?.total || 0 }}</span>
        </div>
        <el-progress 
          :percentage="calcPercentage(week?.done, week?.total)" 
          stroke-width="12"
          :color="customColors"
        />
      </div>
      <div class="progress-item">
        <div class="progress-info">
          <span class="progress-label">本月</span>
          <span class="progress-stat">{{ month?.done || 0 }}/{{ month?.total || 0 }}</span>
        </div>
        <el-progress 
          status="success" 
          :percentage="calcPercentage(month?.done, month?.total)" 
          stroke-width="12"
        />
      </div>
    </div>
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  week: { done: number; total: number };
  month: { done: number; total: number };
}>();

const calcPercentage = (done = 0, total = 0) => (total ? Math.round((done / total) * 100) : 0);

const customColors = [
  { color: 'var(--el-color-danger)', percentage: 20 },
  { color: 'var(--el-color-warning)', percentage: 40 },
  { color: 'var(--el-color-success)', percentage: 60 },
  { color: 'var(--el-color-primary-light-3)', percentage: 80 },
  { color: 'var(--el-color-primary)', percentage: 100 },
];
</script>

<style scoped>

.completion-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-2);
}

.progress-label {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.progress-stat {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
