<template>
  <PageSection title="可用的模拟卷">
    <el-table :data="exams" stripe data-testid="exam-list-table">
      <el-table-column prop="name" label="模拟卷名称" min-width="180">
        <template #default="{ row }">
          <div class="exam-name-cell">
            <span class="exam-name">{{ row.name }}</span>
            <el-tag size="small" :type="row.type === 'MORNING' ? 'primary' : 'warning'" effect="plain">
              {{ row.type === 'MORNING' ? '上午卷' : '下午卷' }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="考试时长" width="100" align="center">
        <template #default="{ row }">{{ row.durationMinutes }} 分钟</template>
      </el-table-column>
      <el-table-column prop="totalScore" label="总分" width="80" align="center" />
      <el-table-column label="操作" width="100" align="right">
        <template #default="{ row }">
          <el-button 
            type="primary" 
            size="small"
            plain
            :data-testid="`exam-start-${row.id}`" 
            @click="$emit('start', row.id)"
          >
            开始
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  exams: any[];
}>();

defineEmits(['start']);
</script>

<style scoped>
.exam-name-cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.exam-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
