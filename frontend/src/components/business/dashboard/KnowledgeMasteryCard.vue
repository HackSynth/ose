<template>
  <el-card class="business-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">知识域掌握度</span>
      </div>
    </template>
    <div class="mastery-list">
      <div v-for="item in knowledgeOverview" :key="item.name" class="mastery-item">
        <div class="mastery-info">
          <span class="mastery-name">{{ item.name }}</span>
          <span class="mastery-value">{{ item.masteryLevel }}%</span>
        </div>
        <el-progress 
          :percentage="item.masteryLevel" 
          :stroke-width="8"
          :show-text="false"
        />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  knowledgeOverview: any[];
}>();
</script>

<style scoped>
.business-card {
  height: 100%;
}

.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

.mastery-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.mastery-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.mastery-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.mastery-value {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
