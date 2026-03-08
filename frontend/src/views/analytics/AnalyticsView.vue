<template>
  <div class="page-container" data-testid="analytics-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">统计分析</h2>
        <p style="margin:6px 0 0;color:#64748b;">查看计划完成率、知识点表现、模拟考趋势与错题分布。</p>
      </div>
      <el-button data-testid="analytics-refresh-button" @click="load">刷新</el-button>
    </div>

    <div class="card-grid" data-testid="analytics-cards">
      <StatCard
        v-for="item in summary?.cards || []"
        :key="item.label"
        :data-testid="`analytics-card-${item.label}`"
        :title="item.label"
        :value="item.value"
        :hint="item.hint"
      />
    </div>

    <div class="split-layout">
      <el-card class="panel-card" data-testid="analytics-knowledge-chart-card">
        <template #header><span>知识点练习表现</span></template>
        <VChart v-if="knowledgeOption" data-testid="analytics-knowledge-chart" :option="knowledgeOption" autoresize style="height:320px;" />
      </el-card>
      <el-card class="panel-card" data-testid="analytics-mistake-chart-card">
        <template #header><span>错题分布</span></template>
        <VChart v-if="mistakeOption" data-testid="analytics-mistake-chart" :option="mistakeOption" autoresize style="height:320px;" />
      </el-card>
    </div>

    <div class="split-layout">
      <el-card class="panel-card" data-testid="analytics-exam-chart-card">
        <template #header><span>模拟考成绩趋势</span></template>
        <VChart v-if="examOption" data-testid="analytics-exam-chart" :option="examOption" autoresize style="height:320px;" />
      </el-card>
      <el-card class="panel-card" data-testid="analytics-activity-chart-card">
        <template #header><span>计划 / 练习趋势</span></template>
        <VChart v-if="activityOption" data-testid="analytics-activity-chart" :option="activityOption" autoresize style="height:320px;" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '@/api';
import StatCard from '@/components/StatCard.vue';

const summary = ref<any>(null);
const trends = ref<any>(null);

const load = async () => {
  summary.value = await api.analyticsSummary();
  trends.value = await api.analyticsTrends();
};

const knowledgeOption = computed(() => ({
  tooltip: {},
  xAxis: { type: 'category', data: (summary.value?.knowledgeStats || []).map((item: any) => item.name) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: (summary.value?.knowledgeStats || []).map((item: any) => Number(item.correctRate)) }],
}));

const mistakeOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{ type: 'pie', radius: '65%', data: (summary.value?.mistakeDistribution || []).map((item: any) => ({ name: item.name, value: item.value })) }],
}));

const examOption = computed(() => ({
  tooltip: {},
  xAxis: { type: 'category', data: (trends.value?.examTrend || []).map((item: any) => item.label) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', smooth: true, data: (trends.value?.examTrend || []).map((item: any) => Number(item.value)) }],
}));

const activityOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['计划完成', '练习次数'] },
  xAxis: { type: 'category', data: (trends.value?.planCompletionTrend || []).map((item: any, index: number) => item.label || trends.value?.practiceTrend?.[index]?.label) },
  yAxis: { type: 'value' },
  series: [
    { name: '计划完成', type: 'bar', data: (trends.value?.planCompletionTrend || []).map((item: any) => Number(item.value)) },
    { name: '练习次数', type: 'line', smooth: true, data: (trends.value?.practiceTrend || []).map((item: any) => Number(item.value)) },
  ],
}));

onMounted(load);
</script>
