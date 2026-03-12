<template>
  <PageSection title="历史记录">
    <el-table v-if="!isMobile" :data="attempts" stripe data-testid="exam-attempt-table">
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
    <MobileCardList
      v-else
      data-testid="exam-attempt-mobile"
      :items="attempts"
      item-key="id"
      empty-description="暂无历史记录"
    >
      <template #item="{ item: row }">
        <div class="exam-name-cell">
          <span class="exam-name">{{ row.examName }}</span>
          <el-tag size="small" :type="row.examType === 'MORNING' ? 'primary' : 'warning'" effect="plain">
            {{ row.examType === 'MORNING' ? '上午卷' : '下午卷' }}
          </el-tag>
        </div>
        <div class="history-mobile-meta">
          <span>实得分：<span class="score-value" :class="{ 'is-high': row.totalScore >= 45 }">{{ row.totalScore }}</span></span>
          <span>
            状态：
            <el-tag size="small" :type="row.status === 'SUBMITTED' ? 'success' : 'info'">
              {{ row.status === 'SUBMITTED' ? '已提交' : '进行中' }}
            </el-tag>
          </span>
        </div>
        <PageActionGroup>
          <el-button link type="primary" :data-testid="`exam-view-attempt-${row.id}`" @click="$emit('view', row.id)">
            查看详情
          </el-button>
        </PageActionGroup>
      </template>
    </MobileCardList>
  </PageSection>
</template>

<script setup lang="ts">
import { useMobile } from '@/composables/useMobile';
import MobileCardList from '@/components/ui/data/MobileCardList.vue';
import PageActionGroup from '@/components/ui/layout/PageActionGroup.vue';
import PageSection from '@/components/ui/layout/PageSection.vue';

const { isMobile } = useMobile();

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

.history-mobile-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
