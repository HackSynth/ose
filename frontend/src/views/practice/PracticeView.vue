<template>
  <div class="page-container" data-testid="practice-page">
    <PageHeader 
      title="练习系统" 
      description="支持按知识点、随机、错题三种模式，上午题自动判分，下午题手动自评。"
    >
      <template #actions>
        <el-button 
          type="primary" 
          data-testid="practice-create-button" 
          @click="createSession"
        >
          {{ session ? '重新开始' : '开始练习' }}
        </el-button>
      </template>
    </PageHeader>

    <PracticeFormCard 
      :form="sessionForm" 
      :knowledge-options="knowledgeOptions" 
      :loading="loading"
      @create="createSession"
    />

    <el-empty 
      v-if="!session" 
      data-testid="practice-empty" 
      description="配置上方的参数并点击「进入练习会话」开始学习" 
      :image-size="120"
    />

    <template v-else>
      <div class="session-container" data-testid="practice-session-card">
        <div class="session-header-sticky">
          <div class="session-info">
            <h3 class="session-title" data-testid="practice-session-title">
              练习会话 #{{ session.id }}
            </h3>
            <el-tag 
              data-testid="practice-session-status" 
              :type="session.status === 'SUBMITTED' ? 'success' : 'warning'"
              effect="dark"
            >
              {{ session.status === 'SUBMITTED' ? '已提交' : '进行中' }}
            </el-tag>
          </div>
          <div class="session-actions">
            <el-button 
              type="primary" 
              data-testid="practice-submit-button" 
              :disabled="session.status === 'SUBMITTED'" 
              @click="submitSession"
            >
              提交本次练习
            </el-button>
          </div>
        </div>

        <div class="records-list">
          <PracticeQuestionCard 
            v-for="(record, index) in session.records" 
            :key="record.recordId"
            :record="record"
            :index="index"
            :answer-model="answers[record.recordId]"
            :session-status="session.status"
            @toggle-flag="(field) => toggleFlag(record, field)"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '@/api';

// UI Components
import PageHeader from '@/components/ui/layout/PageHeader.vue';

// Business Components
import PracticeFormCard from '@/components/business/practice/PracticeFormCard.vue';
import PracticeQuestionCard from '@/components/business/practice/PracticeQuestionCard.vue';

const route = useRoute();
const tree = ref<any[]>([]);
const session = ref<any>(null);
const loading = ref(false);
const answers = reactive<Record<number, any>>({});
const sessionForm = reactive<any>({ 
  sessionType: 'RANDOM', 
  questionType: 'MORNING_SINGLE', 
  knowledgePointId: undefined, 
  count: 5 
});

const flatten = (items: any[], list: any[] = []) => {
  items.forEach((item) => {
    list.push(item);
    if (item.children?.length) flatten(item.children, list);
  });
  return list;
};
const knowledgeOptions = computed(() => flatten(tree.value, []));

const applyRoutePreset = () => {
  if (route.query.sessionType) sessionForm.sessionType = String(route.query.sessionType);
  if (route.query.questionType) sessionForm.questionType = String(route.query.questionType);
  if (route.query.knowledgePointId) sessionForm.knowledgePointId = Number(route.query.knowledgePointId);
};

const loadKnowledge = async () => {
  tree.value = await api.knowledgeTree() as any[];
  applyRoutePreset();
};

const initAnswers = () => {
  Object.keys(answers).forEach((key) => delete answers[Number(key)]);
  session.value?.records?.forEach((record: any) => {
    answers[record.recordId] = {
      answer: record.userAnswer || '',
      subjectiveScore: record.subjectiveScore != null ? Number(record.subjectiveScore) : 0,
    };
  });
};

const createSession = async () => {
  loading.value = true;
  try {
    session.value = await api.createPracticeSession(sessionForm);
    initAnswers();
    setTimeout(() => {
      document.querySelector('.session-container')?.scrollIntoView({ behavior: 'smooth' });
    }, 100);
  } finally {
    loading.value = false;
  }
};

const submitSession = async () => {
  loading.value = true;
  try {
    const payload = {
      answers: session.value.records.map((record: any) => ({
        recordId: record.recordId,
        answer: answers[record.recordId].answer,
        durationSeconds: 60, // Simplified for now
        subjectiveScore: record.question.type === 'AFTERNOON_CASE' ? Number(answers[record.recordId].subjectiveScore) : null,
        reasonType: 'CONCEPT',
      })),
    };
    session.value = await api.submitPractice(session.value.id, payload);
    initAnswers();
    ElMessage.success('练习已提交');
  } finally {
    loading.value = false;
  }
};

const toggleFlag = async (record: any, field: 'favorite' | 'markedUnknown' | 'addedToReview') => {
  const payload = { 
    favorite: record.favorite, 
    markedUnknown: record.markedUnknown, 
    addedToReview: record.addedToReview 
  };
  payload[field] = !record[field];
  await api.updatePracticeFlags(record.recordId, payload);
  // Refresh session to get updated flags
  session.value = await api.getPracticeSession(session.value.id);
  initAnswers();
};

watch(() => route.query, applyRoutePreset);
onMounted(loadKnowledge);
</script>

<style scoped>
.session-container {
  margin-top: var(--space-8);
}

.session-header-sticky {
  position: sticky;
  top: var(--header-height);
  z-index: 5;
  background: var(--bg-app);
  padding: var(--space-4) 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: var(--space-6);
}

.session-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.session-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.records-list {
  display: flex;
  flex-direction: column;
}
</style>
