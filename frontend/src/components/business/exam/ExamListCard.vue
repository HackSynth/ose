<template>
  <PageSection title="可用的模拟卷">
    <el-table v-if="!isMobile" :data="exams" stripe data-testid="exam-list-table">
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
    <MobileCardList
      v-else
      data-testid="exam-list-mobile"
      :items="exams"
      item-key="id"
      empty-description="暂无可用模拟卷"
    >
      <template #item="{ item: row }">
        <div class="exam-name-cell">
          <span class="exam-name">{{ row.name }}</span>
          <el-tag size="small" :type="row.type === 'MORNING' ? 'primary' : 'warning'" effect="plain">
            {{ row.type === 'MORNING' ? '上午卷' : '下午卷' }}
          </el-tag>
        </div>
        <div class="exam-mobile-meta">
          <span>考试时长：{{ row.durationMinutes }} 分钟</span>
          <span>总分：{{ row.totalScore }}</span>
        </div>
        <PageActionGroup>
          <el-button type="primary" size="small" plain :data-testid="`exam-start-${row.id}`" @click="$emit('start', row.id)">
            开始
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

.exam-mobile-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
