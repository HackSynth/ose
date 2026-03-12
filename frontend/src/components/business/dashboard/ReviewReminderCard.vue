<template>
  <PageSection title="错题复习提醒">
    <template #actions>
      <el-button link type="primary" @click="$emit('view-all')">查看错题本</el-button>
    </template>
    
    <el-empty v-if="!reminders?.length" description="当前没有到期错题" :image-size="80" />
    
    <div v-else class="reminder-list">
      <div v-for="item in reminders" :key="item.id" class="reminder-item">
        <div class="reminder-main">
          <div class="reminder-title">{{ item.questionTitle }}</div>
          <div class="reminder-subtitle">
            {{ item.knowledgePointName }} · {{ item.reasonType }}
          </div>
        </div>
        <el-tag type="warning" size="small" effect="light">{{ item.nextReviewAt }}</el-tag>
      </div>
    </div>
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  reminders: any[];
}>();

defineEmits(['view-all']);
</script>

<style scoped>

.reminder-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.reminder-item {
  padding: var(--space-3);
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--radius-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
  transition: border-color 0.2s;
}

.reminder-item:hover {
  border-color: var(--el-color-primary-light-7);
}

.reminder-main {
  flex: 1;
  min-width: 0;
}

.reminder-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reminder-subtitle {
  color: var(--el-text-color-regular);
  font-size: 12px;
  margin-top: 4px;
}
</style>
