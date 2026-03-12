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
          :loading="loading"
          data-testid="analytics-refresh-button" 
          @click="load"
        >
          刷新数据
        </el-button>
      </template>
    </PageHeader>

    <PageStateBlock
      :loading="loading"
      :error-message="errorMessage"
      :empty="!summary && !trends"
      empty-description="暂无统计数据"
      @retry="load"
    >
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
        <PageSection title="知识点练习表现" class="chart-section" data-testid="analytics-knowledge-chart-card">
          <div class="chart-wrapper">
            <VChart v-if="knowledgeOption" data-testid="analytics-knowledge-chart" :option="knowledgeOption" autoresize />
          </div>
        </PageSection>
        <PageSection title="错题分布" class="chart-section" data-testid="analytics-mistake-chart-card">
          <div class="chart-wrapper">
            <VChart v-if="mistakeOption" data-testid="analytics-mistake-chart" :option="mistakeOption" autoresize />
          </div>
        </PageSection>
      </div>

      <div class="split-layout">
        <PageSection title="模拟考成绩趋势" class="chart-section" data-testid="analytics-exam-chart-card">
          <div class="chart-wrapper">
            <VChart v-if="examOption" data-testid="analytics-exam-chart" :option="examOption" autoresize />
          </div>
        </PageSection>
        <PageSection title="计划 / 练习趋势" class="chart-section" data-testid="analytics-activity-chart-card">
          <div class="chart-wrapper">
            <VChart v-if="activityOption" data-testid="analytics-activity-chart" :option="activityOption" autoresize />
          </div>
        </PageSection>
      </div>
    </PageStateBlock>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '@/api';
import { usePageState } from '@/composables/usePageState';
import PageStateBlock from '@/components/ui/feedback/PageStateBlock.vue';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import PageSection from '@/components/ui/layout/PageSection.vue';
import StatCard from '@/components/business/common/StatCard.vue';

const summary = ref<any>(null);
const trends = ref<any>(null);
const {
  loading,
  errorMessage,
  runWithState,
} = usePageState();

const load = async () => {
  const result = await runWithState(async () => {
    const [summaryData, trendsData] = await Promise.all([
      api.analyticsSummary(),
      api.analyticsTrends(),
    ]);
    return { summaryData, trendsData };
  });

  if (!result) {
    summary.value = null;
    trends.value = null;
    return;
  }

  summary.value = result.summaryData;
  trends.value = result.trendsData;
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
    itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
    data: (summary.value?.knowledgeStats || []).map((item: any) => Number(item.correctRate)) 
  }],
}));

const mistakeOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%', left: 'center' },
  color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'],
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
    lineStyle: { width: 3, color: '#67C23A' },
    itemStyle: { color: '#67C23A' },
    areaStyle: { color: 'rgba(103, 194, 58, 0.12)' },
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
      itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
      data: (trends.value?.planCompletionTrend || []).map((item: any) => Number(item.value)) 
    },
    { 
      name: '练习次数', 
      type: 'line', 
      smooth: true, 
      lineStyle: { width: 3, color: '#E6A23C' },
      itemStyle: { color: '#E6A23C' },
      data: (trends.value?.practiceTrend || []).map((item: any) => Number(item.value)) 
    },
  ],
}));

onMounted(load);
</script>

<style scoped>
.chart-section {
  height: 100%;
}

.chart-wrapper {
  height: 320px;
  width: 100%;
}

@media (max-width: 767px) {
  .chart-wrapper {
    height: 260px;
  }
}
</style>
