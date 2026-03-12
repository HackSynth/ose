<template>
  <el-card class="business-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">完成情况</span>
      </div>
    </template>
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
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  week: { done: number; total: number };
  month: { done: number; total: number };
}>();

const calcPercentage = (done = 0, total = 0) => (total ? Math.round((done / total) * 100) : 0);

const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 },
];
</script>

<style scoped>
.business-card {
  height: 100%;
}

.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

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
  color: var(--text-primary);
}

.progress-stat {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
