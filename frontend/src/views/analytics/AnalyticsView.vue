<template>
  <div class="page-container" data-testid="analytics-page">
    <PageHeader 
      title="统计分析" 
      description="查看计划完成率、知识点表现、模拟考趋势与错题分布。"
    >
      <template #actions>
        <el-button 
          type="primary" 
          plain 
          data-testid="analytics-refresh-button" 
          @click="load"
        >
          刷新数据
        </el-button>
      </template>
    </PageHeader>

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
      <el-card class="chart-card" data-testid="analytics-knowledge-chart-card">
        <template #header><span class="chart-title">知识点练习表现</span></template>
        <div class="chart-wrapper">
          <VChart v-if="knowledgeOption" data-testid="analytics-knowledge-chart" :option="knowledgeOption" autoresize />
        </div>
      </el-card>
      <el-card class="chart-card" data-testid="analytics-mistake-chart-card">
        <template #header><span class="chart-title">错题分布</span></template>
        <div class="chart-wrapper">
          <VChart v-if="mistakeOption" data-testid="analytics-mistake-chart" :option="mistakeOption" autoresize />
        </div>
      </el-card>
    </div>

    <div class="split-layout">
      <el-card class="chart-card" data-testid="analytics-exam-chart-card">
        <template #header><span class="chart-title">模拟考成绩趋势</span></template>
        <div class="chart-wrapper">
          <VChart v-if="examOption" data-testid="analytics-exam-chart" :option="examOption" autoresize />
        </div>
      </el-card>
      <el-card class="chart-card" data-testid="analytics-activity-chart-card">
        <template #header><span class="chart-title">计划 / 练习趋势</span></template>
        <div class="chart-wrapper">
          <VChart v-if="activityOption" data-testid="analytics-activity-chart" :option="activityOption" autoresize />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '@/api';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import StatCard from '@/components/business/common/StatCard.vue';

const summary = ref<any>(null);
const trends = ref<any>(null);

const load = async () => {
  summary.value = await api.analyticsSummary();
  trends.value = await api.analyticsTrends();
};

const knowledgeOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { 
    type: 'category', 
    data: (summary.value?.knowledgeStats || []).map((item: any) => item.name),
    axisLabel: { interval: 0, rotate: 30 }
  },
  yAxis: { type: 'value', name: '正确率 %' },
  series: [{ 
    type: 'bar', 
    barWidth: '40%',
    itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] },
    data: (summary.value?.knowledgeStats || []).map((item: any) => Number(item.correctRate)) 
  }],
}));

const mistakeOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%', left: 'center' },
  series: [{ 
    type: 'pie', 
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
    label: { show: false, position: 'center' },
    emphasis: { label: { show: true, fontSize: '18', fontWeight: 'bold' } },
    labelLine: { show: false },
    data: (summary.value?.mistakeDistribution || []).map((item: any) => ({ name: item.name, value: item.value })) 
  }],
}));

const examOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: (trends.value?.examTrend || []).map((item: any) => item.label) },
  yAxis: { type: 'value' },
  series: [{ 
    type: 'line', 
    smooth: true, 
    symbol: 'circle',
    symbolSize: 8,
    lineStyle: { width: 3, color: '#10b981' },
    itemStyle: { color: '#10b981' },
    areaStyle: { color: 'rgba(16, 185, 129, 0.1)' },
    data: (trends.value?.examTrend || []).map((item: any) => Number(item.value)) 
  }],
}));

const activityOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: '0%', right: '0%' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: (trends.value?.planCompletionTrend || []).map((item: any, index: number) => item.label || trends.value?.practiceTrend?.[index]?.label) },
  yAxis: { type: 'value' },
  series: [
    { 
      name: '计划完成', 
      type: 'bar', 
      itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] },
      data: (trends.value?.planCompletionTrend || []).map((item: any) => Number(item.value)) 
    },
    { 
      name: '练习次数', 
      type: 'line', 
      smooth: true, 
      lineStyle: { width: 3, color: '#f59e0b' },
      itemStyle: { color: '#f59e0b' },
      data: (trends.value?.practiceTrend || []).map((item: any) => Number(item.value)) 
    },
  ],
}));

onMounted(load);
</script>

<style scoped>
.chart-card {
  height: 100%;
}

.chart-title {
  font-weight: 700;
  color: var(--text-primary);
}

.chart-wrapper {
  height: 320px;
  width: 100%;
}
</style>
