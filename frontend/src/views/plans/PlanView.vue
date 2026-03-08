<template>
  <div class="page-container" data-testid="plan-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">学习计划</h2>
        <p style="margin:6px 0 0;color:#64748b;">自动生成阶段计划，并支持手动流转、延期和重排。</p>
      </div>
      <div style="display:flex;gap:8px;">
        <el-button data-testid="plan-rebalance-button" :loading="loading" @click="rebalance">重排延期任务</el-button>
        <el-button type="primary" data-testid="plan-generate-button" :loading="loading" @click="generate">重新生成计划</el-button>
      </div>
    </div>

    <div class="card-grid" v-if="plan" data-testid="plan-summary">
      <StatCard data-testid="plan-name-card" title="计划名称" :value="plan.name" hint="当前激活计划" />
      <StatCard data-testid="plan-period-card" title="计划周期" :value="`${plan.startDate} ~ ${plan.endDate}`" hint="自动按考试日期拆分" />
      <StatCard data-testid="plan-total-hours-card" title="总学习时长" :value="`${plan.totalHours} 小时`" hint="基于每周学习时长估算" />
    </div>

    <el-card class="panel-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>任务列表</span>
          <el-tag data-testid="plan-task-count">{{ plan?.tasks?.length || 0 }} 项</el-tag>
        </div>
      </template>
      <el-table :data="plan?.tasks || []" stripe data-testid="plan-task-table">
        <el-table-column prop="scheduledDate" label="日期" width="120" />
        <el-table-column prop="phase" label="阶段" width="120" />
        <el-table-column prop="taskType" label="类型" width="120" />
        <el-table-column prop="title" label="任务" min-width="260" />
        <el-table-column prop="knowledgePointName" label="知识点" min-width="150" />
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="12" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="updateTask(row, 'IN_PROGRESS', 50)">进行中</el-button>
            <el-button link type="success" @click="updateTask(row, 'DONE', 100)">完成</el-button>
            <el-button link type="warning" @click="delayTask(row)">延期</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { api } from '@/api';
import StatCard from '@/components/StatCard.vue';

const loading = ref(false);
const plan = ref<any>(null);

const load = async () => {
  plan.value = await api.currentPlan();
};

const generate = async () => {
  loading.value = true;
  try {
    plan.value = await api.generatePlan();
    ElMessage.success('计划已重新生成');
  } finally {
    loading.value = false;
  }
};

const rebalance = async () => {
  loading.value = true;
  try {
    plan.value = await api.rebalanceTasks({ fromDate: dayjs().add(1, 'day').format('YYYY-MM-DD') });
    ElMessage.success('延期任务已重排');
  } finally {
    loading.value = false;
  }
};

const updateTask = async (task: any, status: string, progress: number) => {
  await api.updateTask(task.id, { status, progress });
  ElMessage.success('任务已更新');
  await load();
};

const delayTask = async (task: any) => {
  await api.updateTask(task.id, { status: 'DELAYED', postponedTo: dayjs(task.scheduledDate).add(1, 'day').format('YYYY-MM-DD') });
  ElMessage.success('任务已延期');
  await load();
};

onMounted(load);
</script>
