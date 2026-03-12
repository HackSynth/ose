<template>
  <div class="page-container" data-testid="exams-page">
    <PageHeader 
      title="模拟考试" 
      description="创建上午卷 / 下午卷模拟，支持交卷、成绩记录和历史回看。"
    >
      <template #actions>
        <el-button 
          type="primary" 
          data-testid="exam-create-button" 
          @click="showCreateCard = !showCreateCard"
        >
          {{ showCreateCard ? '取消创建' : '创建模拟卷' }}
        </el-button>
      </template>
    </PageHeader>

    <transition name="el-zoom-in-top">
      <ExamCreateCard 
        v-if="showCreateCard"
        :form="examForm"
        :question-options="questionOptions"
        :loading="creating"
        @create="createExam"
      />
    </transition>

    <div class="split-layout">
      <ExamListCard 
        :exams="exams" 
        @start="startExam" 
      />
      <ExamHistoryCard 
        :attempts="attempts" 
        @view="viewAttempt" 
      />
    </div>

    <ExamAttemptCard 
      v-if="currentAttempt"
      :attempt="currentAttempt"
      :answers-model="attemptAnswers"
      v-model:self-review-summary="selfReviewSummary"
      @submit="submitAttempt"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';

// UI Components
import PageHeader from '@/components/ui/layout/PageHeader.vue';

// Business Components
import ExamCreateCard from '@/components/business/exam/ExamCreateCard.vue';
import ExamListCard from '@/components/business/exam/ExamListCard.vue';
import ExamHistoryCard from '@/components/business/exam/ExamHistoryCard.vue';
import ExamAttemptCard from '@/components/business/exam/ExamAttemptCard.vue';

const exams = ref<any[]>([]);
const attempts = ref<any[]>([]);
const questionOptions = ref<any[]>([]);
const currentAttempt = ref<any>(null);
const selfReviewSummary = ref('');
const attemptAnswers = reactive<Record<number, any>>({});
const examForm = reactive<any>({ 
  name: '我的模拟卷', 
  type: 'MORNING', 
  durationMinutes: 150, 
  description: '', 
  questionIds: [] as number[] 
});

const showCreateCard = ref(false);
const creating = ref(false);

const load = async () => {
  exams.value = await api.exams() as any[];
  attempts.value = await api.examAttempts() as any[];
  questionOptions.value = await api.questions() as any[];
};

const createExam = async () => {
  creating.value = true;
  try {
    await api.createExam(examForm);
    ElMessage.success('模拟卷已创建');
    showCreateCard.value = false;
    await load();
  } finally {
    creating.value = false;
  }
};

const initAttemptAnswers = () => {
  Object.keys(attemptAnswers).forEach((key) => delete attemptAnswers[Number(key)]);
  currentAttempt.value?.answers?.forEach((item: any) => {
    attemptAnswers[item.questionId] = { 
      answerText: item.answerText || '', 
      subjectiveScore: item.subjectiveScore != null ? Number(item.subjectiveScore) : 0 
    };
  });
  selfReviewSummary.value = currentAttempt.value?.selfReviewSummary || '';
};

const startExam = async (id: number) => {
  currentAttempt.value = await api.startExam(id);
  initAttemptAnswers();
  // Scroll to attempt card
  setTimeout(() => {
    document.querySelector('[data-testid="exam-attempt-card"]')?.scrollIntoView({ behavior: 'smooth' });
  }, 100);
};

const viewAttempt = async (id: number) => {
  currentAttempt.value = await api.getExamAttempt(id);
  initAttemptAnswers();
  setTimeout(() => {
    document.querySelector('[data-testid="exam-attempt-card"]')?.scrollIntoView({ behavior: 'smooth' });
  }, 100);
};

const submitAttempt = async () => {
  currentAttempt.value = await api.submitExam(currentAttempt.value.id, {
    durationSeconds: 3600, // TODO: Calc from start time
    selfReviewSummary: selfReviewSummary.value,
    answers: currentAttempt.value.answers.map((item: any) => ({
      questionId: item.questionId,
      answerText: attemptAnswers[item.questionId].answerText,
      subjectiveScore: item.questionType === 'AFTERNOON_CASE' ? Number(attemptAnswers[item.questionId].subjectiveScore) : null,
      feedback: item.questionType === 'AFTERNOON_CASE' ? '自评完成' : null,
    })),
  });
  ElMessage.success('已交卷');
  initAttemptAnswers();
  await load();
};

onMounted(load);
</script>

<style scoped>
/* Page layout handled by base.css */
</style>
