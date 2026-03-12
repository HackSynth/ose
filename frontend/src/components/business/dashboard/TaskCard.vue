<template>
  <PageSection title="今日学习任务">
    <template #actions>
      <div class="card-header-actions">
        <el-tag size="small" effect="plain" round class="task-count-tag">
          {{ tasks.length }} 项
        </el-tag>
      </div>
    </template>
    
    <el-empty v-if="!tasks.length" description="今天没有任务，去生成计划吧" :image-size="80" />
    
    <el-timeline v-else class="task-timeline">
      <el-timeline-item 
        v-for="task in tasks" 
        :key="task.id" 
        :timestamp="task.scheduledDate"
        :type="task.status === 'DONE' ? 'success' : 'primary'"
      >
        <div class="task-item">
          <div class="task-main">
            <span class="task-title" :class="{ 'is-done': task.status === 'DONE' }">
              {{ task.title }}
            </span>
            <p class="task-desc">{{ task.description }}</p>
          </div>
          <div class="task-meta">
            <el-tag size="small" type="info" effect="light">{{ task.phase }}</el-tag>
            <el-tag size="small" type="success" effect="light">{{ task.estimatedMinutes }} 分钟</el-tag>
            <el-tag size="small" :type="task.status === 'DONE' ? 'success' : 'warning'">
              {{ task.status === 'DONE' ? '已完成' : '待处理' }}
            </el-tag>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  tasks: any[];
}>();
</script>

<style scoped>
.card-header-actions {
  display: flex;
  align-items: center;
}

.task-timeline {
  padding-left: var(--space-2);
}

.task-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.task-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.task-title.is-done {
  text-decoration: line-through;
  color: var(--el-text-color-secondary);
}

.task-desc {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin: var(--space-1) 0 0 0;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.task-count-tag {
  font-weight: 600;
}
</style>
