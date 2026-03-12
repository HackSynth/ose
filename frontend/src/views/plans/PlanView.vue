<template>
  <div class="page-container" data-testid="plan-page">
    <PageHeader 
      title="学习计划" 
      description="基于考试大纲与您的目标日期，自动生成阶段性备考计划，并支持动态重排与任务追踪。"
    >
      <template #actions>
        <PageActionGroup>
          <el-button 
            data-testid="plan-rebalance-button" 
            :loading="loading" 
            @click="rebalance"
          >
            重排延期任务
          </el-button>
          <el-button 
            type="primary" 
            data-testid="plan-generate-button" 
            :loading="loading" 
            @click="generate"
          >
            重新生成全量计划
          </el-button>
        </PageActionGroup>
      </template>
    </PageHeader>

    <div class="card-grid" v-if="plan" data-testid="plan-summary">
      <StatCard data-testid="plan-name-card" title="计划名称" :value="plan.name" hint="当前生效的备考路线" />
      <StatCard data-testid="plan-period-card" title="计划周期" :value="`${plan.startDate} ~ ${plan.endDate}`" hint="自动按阶段分解时长" />
      <StatCard data-testid="plan-total-hours-card" title="累计学习时长" :value="`${plan.totalHours} 小时`" hint="基于每周投入时间估算" />
    </div>

    <PageSection title="备考路线图任务列表">
      <template #actions>
        <div class="card-header-actions">
          <el-tag data-testid="plan-task-count" effect="plain" round>{{ plan?.tasks?.length || 0 }} 项</el-tag>
        </div>
      </template>
      <el-table v-if="!isMobile" :data="plan?.tasks || []" stripe data-testid="plan-task-table" style="width: 100%">
        <el-table-column prop="scheduledDate" label="计划日期" width="120" sortable />
        <el-table-column prop="phase" label="所属阶段" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="phaseType(row.phase)">{{ row.phase }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务标题" min-width="200">
          <template #default="{ row }">
            <div class="task-title-cell">
              <span class="task-title">{{ row.title }}</span>
              <span class="task-type">{{ row.taskType }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgePointName" label="核心知识点" min-width="150" />
        <el-table-column label="当前进度" width="160">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="10" :status="row.progress === 100 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="updateTask(row, 'IN_PROGRESS', 50)">开始</el-button>
            <el-button link type="success" size="small" @click="updateTask(row, 'DONE', 100)">完成</el-button>
            <el-button link type="warning" size="small" @click="delayTask(row)">延期一天</el-button>
          </template>
        </el-table-column>
      </el-table>
      <MobileCardList
        v-else
        data-testid="plan-task-mobile-list"
        :items="plan?.tasks || []"
        item-key="id"
        empty-description="暂无计划任务"
      >
        <template #item="{ item: row }">
          <div class="task-mobile-title-wrap">
            <div class="task-mobile-title">{{ row.title }}</div>
            <el-tag size="small" :type="phaseType(row.phase)" effect="plain">{{ row.phase }}</el-tag>
          </div>
          <div class="task-mobile-meta">
            <span>计划日期：{{ row.scheduledDate }}</span>
            <span>知识点：{{ row.knowledgePointName || '-' }}</span>
            <span>状态：{{ row.status }}</span>
          </div>
          <el-progress :percentage="row.progress" :stroke-width="10" :status="row.progress === 100 ? 'success' : ''" />
          <PageActionGroup>
            <el-button link type="primary" size="small" @click="updateTask(row, 'IN_PROGRESS', 50)">开始</el-button>
            <el-button link type="success" size="small" @click="updateTask(row, 'DONE', 100)">完成</el-button>
            <el-button link type="warning" size="small" @click="delayTask(row)">延期一天</el-button>
          </PageActionGroup>
        </template>
      </MobileCardList>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { api } from '@/api';
import { useMobile } from '@/composables/useMobile';
import MobileCardList from '@/components/ui/data/MobileCardList.vue';
import PageActionGroup from '@/components/ui/layout/PageActionGroup.vue';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import PageSection from '@/components/ui/layout/PageSection.vue';
import StatCard from '@/components/business/common/StatCard.vue';

const { isMobile } = useMobile();
const loading = ref(false);
const plan = ref<any>(null);

const load = async () => {
  plan.value = await api.currentPlan();
};

const generate = async () => {
  loading.value = true;
  try {
    plan.value = await api.generatePlan();
    ElMessage.success('全新计划已根据最新数据生成');
  } finally {
    loading.value = false;
  }
};

const rebalance = async () => {
  loading.value = true;
  try {
    plan.value = await api.rebalanceTasks({ fromDate: dayjs().add(1, 'day').format('YYYY-MM-DD') });
    ElMessage.success('延期任务已成功重排至后续日期');
  } finally {
    loading.value = false;
  }
};

const updateTask = async (task: any, status: string, progress: number) => {
  await api.updateTask(task.id, { status, progress });
  ElMessage.success('任务状态已更新');
  await load();
};

const delayTask = async (task: any) => {
  await api.updateTask(task.id, { 
    status: 'DELAYED', 
    postponedTo: dayjs(task.scheduledDate).add(1, 'day').format('YYYY-MM-DD') 
  });
  ElMessage.success('任务已延期至明天');
  await load();
};

const phaseType = (phase: string) => {
  if (phase.includes('基础')) return 'primary';
  if (phase.includes('强化')) return 'warning';
  if (phase.includes('冲刺')) return 'danger';
  return 'info';
};

const statusType = (status: string) => {
  if (status === 'DONE') return 'success';
  if (status === 'IN_PROGRESS') return 'primary';
  if (status === 'DELAYED') return 'danger';
  return 'info';
};

onMounted(load);
</script>

<style scoped>
.card-header-actions {
  display: flex;
  align-items: center;
}

.task-mobile-title-wrap {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-2);
}

.task-mobile-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.task-mobile-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.task-title-cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.task-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.task-type {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
</style>
