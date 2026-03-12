<template>
  <div class="page-container" data-testid="dashboard-page">
    <PageHeader 
      title="备考总览" 
      :description="`距离考试还有 ${overview?.daysUntilExam ?? '-'} 天，先做今天最重要的任务。`"
    >
      <template #actions>
        <el-tag size="large" type="danger" effect="light" data-testid="dashboard-exam-date-tag">
          目标考试日：{{ overview?.examDate || '-' }}
        </el-tag>
      </template>
    </PageHeader>

    <PageStateBlock
      :loading="loading"
      :error-message="errorMessage"
      :empty="!overview"
      empty-description="暂无概览数据"
      @retry="load"
    >
      <div class="card-grid" data-testid="dashboard-summary-cards">
        <StatCard
          v-for="item in overview?.summaryCards || []"
          :key="item.label"
          :data-testid="`dashboard-card-${item.label}`"
          :title="item.label"
          :value="item.value"
          :hint="item.hint"
        />
        <StatCard
          data-testid="dashboard-card-due-reviews"
          title="待复习错题"
          :value="overview?.dueReviewCount || 0"
          hint="到期错题复习提醒"
        />
      </div>

      <div class="split-layout">
        <TaskCard 
          data-testid="dashboard-today-tasks"
          :tasks="overview?.todayTasks || []" 
        />
        <CompletionCard 
          data-testid="dashboard-completion"
          :week="overview?.weekCompletion" 
          :month="overview?.monthCompletion" 
        />
      </div>

      <div class="split-layout">
        <ReviewReminderCard 
          data-testid="dashboard-review-reminders"
          :reminders="overview?.reviewReminders || []" 
          @view-all="router.push('/mistakes')" 
        />
        <PracticeRecommendCard 
          data-testid="dashboard-practice-recommendations"
          :recommendations="overview?.practiceRecommendations || []" 
          @practice="goToPractice" 
        />
      </div>

      <div class="split-layout">
        <KnowledgeMasteryCard 
          data-testid="dashboard-knowledge-overview"
          :knowledge-overview="overview?.knowledgeOverview || []" 
        />
        <RecentRecordsCard 
          data-testid="dashboard-recent-records"
          :recent-mistakes="overview?.recentMistakes || []" 
          :recent-exams="overview?.recentExams || []" 
          :recent-notes="overview?.recentNotes || []" 
        />
      </div>
    </PageStateBlock>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '@/api';
import { usePageState } from '@/composables/usePageState';

// UI Components
import PageStateBlock from '@/components/ui/feedback/PageStateBlock.vue';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import StatCard from '@/components/business/common/StatCard.vue';

// Business Components
import TaskCard from '@/components/business/dashboard/TaskCard.vue';
import CompletionCard from '@/components/business/dashboard/CompletionCard.vue';
import ReviewReminderCard from '@/components/business/dashboard/ReviewReminderCard.vue';
import PracticeRecommendCard from '@/components/business/dashboard/PracticeRecommendCard.vue';
import KnowledgeMasteryCard from '@/components/business/dashboard/KnowledgeMasteryCard.vue';
import RecentRecordsCard from '@/components/business/dashboard/RecentRecordsCard.vue';

const router = useRouter();
const overview = ref<any>(null);
const {
  loading,
  errorMessage,
  runWithState,
} = usePageState();

const load = async () => {
  overview.value = await runWithState(() => api.dashboard());
};

const goToPractice = (item: any) => {
  router.push({
    path: '/practice',
    query: {
      sessionType: 'KNOWLEDGE',
      questionType: 'MORNING_SINGLE',
      knowledgePointId: String(item.knowledgePointId),
    },
  });
};

onMounted(load);
</script>

<style scoped>
/* Grid and layout utilities are now in base.css */
</style>
