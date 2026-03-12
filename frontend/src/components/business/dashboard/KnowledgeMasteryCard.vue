<template>
  <PageSection title="知识域掌握度">
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
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  knowledgeOverview: any[];
}>();
</script>

<style scoped>
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
  color: var(--el-text-color-primary);
}

.mastery-value {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
