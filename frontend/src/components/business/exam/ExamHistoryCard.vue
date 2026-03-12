<template>
  <PageSection title="历史记录">
    <el-table :data="attempts" stripe data-testid="exam-attempt-table">
      <el-table-column prop="examName" label="模拟卷名称" min-width="180">
        <template #default="{ row }">
          <div class="exam-name-cell">
            <span class="exam-name">{{ row.examName }}</span>
            <el-tag size="small" :type="row.examType === 'MORNING' ? 'primary' : 'warning'" effect="plain">
              {{ row.examType === 'MORNING' ? '上午卷' : '下午卷' }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="totalScore" label="实得分" width="80" align="center">
        <template #default="{ row }">
          <span class="score-value" :class="{ 'is-high': row.totalScore >= 45 }">
            {{ row.totalScore }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'SUBMITTED' ? 'success' : 'info'">
            {{ row.status === 'SUBMITTED' ? '已提交' : '进行中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="right">
        <template #default="{ row }">
          <el-button 
            link 
            type="primary"
            :data-testid="`exam-view-attempt-${row.id}`" 
            @click="$emit('view', row.id)"
          >
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  attempts: any[];
}>();

defineEmits(['view']);
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

.score-value {
  font-weight: 700;
}

.score-value.is-high {
  color: var(--el-color-success);
}
</style>
