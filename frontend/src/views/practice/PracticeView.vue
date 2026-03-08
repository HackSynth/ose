<template>
  <div class="page-container" data-testid="practice-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">练习系统</h2>
        <p style="margin:6px 0 0;color:#64748b;">支持按知识点、随机、错题三种模式，上午题自动判分，下午题手动自评。</p>
      </div>
      <el-button type="primary" data-testid="practice-create-button" @click="createSession">开始练习</el-button>
    </div>

    <el-card class="panel-card">
      <div class="card-grid" data-testid="practice-form">
        <div data-testid="practice-session-type"><el-select v-model="sessionForm.sessionType" placeholder="练习模式">
          <el-option label="按知识点" value="KNOWLEDGE" />
          <el-option label="随机练习" value="RANDOM" />
          <el-option label="错题复习" value="MISTAKE" />
        </el-select></div>
        <div data-testid="practice-question-type"><el-select v-model="sessionForm.questionType" placeholder="题型">
          <el-option label="上午题" value="MORNING_SINGLE" />
          <el-option label="下午题" value="AFTERNOON_CASE" />
        </el-select></div>
        <div data-testid="practice-knowledge-point"><el-select v-model="sessionForm.knowledgePointId" clearable placeholder="知识点">
          <el-option v-for="item in knowledgeOptions" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
        </el-select></div>
        <div data-testid="practice-count"><el-input-number v-model="sessionForm.count" :min="1" :max="20" style="width:100%;" /></div>
      </div>
    </el-card>

    <el-empty v-if="!session" data-testid="practice-empty" description="还没有创建练习会话" />
    <template v-else>
      <el-card class="panel-card" data-testid="practice-session-card">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span data-testid="practice-session-title">当前会话 #{{ session.id }}</span>
            <el-tag data-testid="practice-session-status" :type="session.status === 'SUBMITTED' ? 'success' : 'warning'">{{ session.status }}</el-tag>
          </div>
        </template>
        <div style="display:flex;flex-direction:column;gap:16px;">
          <el-card v-for="(record, index) in session.records" :key="record.recordId" :data-testid="`practice-record-${index}`" shadow="never">
            <div style="display:flex;flex-direction:column;gap:12px;">
              <div>
                <strong :data-testid="`practice-question-title-${index}`">第 {{ index + 1 }} 题：{{ record.question.title }}</strong>
                <p style="white-space:pre-wrap;color:#334155;">{{ record.question.content }}</p>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                  <el-tag v-for="item in record.question.knowledgePoints" :key="item.id" size="small">{{ item.name }}</el-tag>
                </div>
              </div>
              <template v-if="record.question.type === 'MORNING_SINGLE'">
                <div :data-testid="`practice-answer-group-${index}`"><el-radio-group v-model="answers[record.recordId].answer" :disabled="session.status === 'SUBMITTED'">
                  <el-radio v-for="option in record.question.options" :key="option.key" :label="option.key">{{ option.key }}. {{ option.content }}</el-radio>
                </el-radio-group></div>
              </template>
              <template v-else>
                <div :data-testid="`practice-answer-text-${index}`"><el-input v-model="answers[record.recordId].answer" type="textarea" :rows="4" :disabled="session.status === 'SUBMITTED'" placeholder="请输入下午题作答内容" /></div>
                <div :data-testid="`practice-score-input-${index}`"><el-input-number v-model="answers[record.recordId].subjectiveScore" :min="0" :max="Number(record.question.score)" :step="1" :disabled="session.status === 'SUBMITTED'" /></div>
                <div style="font-size:12px;color:#64748b;">参考要点：{{ record.question.referenceAnswer || '暂无' }}</div>
              </template>
              <div v-if="session.status === 'SUBMITTED'" :data-testid="`practice-result-row-${index}`" style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                <el-tag :type="resultType(record.result)" :data-testid="`practice-result-${index}`">{{ record.result || '待评阅' }}</el-tag>
                <el-tag v-if="record.autoScore != null" :data-testid="`practice-auto-score-${index}`">自动得分 {{ record.autoScore }}</el-tag>
                <el-tag v-if="record.subjectiveScore != null" :data-testid="`practice-subjective-score-${index}`">自评分 {{ record.subjectiveScore }}</el-tag>
                <el-button size="small" @click="toggleFlag(record, 'favorite')">{{ record.favorite ? '取消收藏' : '收藏' }}</el-button>
                <el-button size="small" @click="toggleFlag(record, 'markedUnknown')">{{ record.markedUnknown ? '取消不会' : '标记不会' }}</el-button>
                <el-button size="small" @click="toggleFlag(record, 'addedToReview')">{{ record.addedToReview ? '取消复习' : '加入复习' }}</el-button>
              </div>
            </div>
          </el-card>
        </div>
        <div style="margin-top:16px;display:flex;justify-content:flex-end;">
          <el-button type="primary" data-testid="practice-submit-button" :disabled="session.status === 'SUBMITTED'" @click="submitSession">提交练习</el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '@/api';

const route = useRoute();
const tree = ref<any[]>([]);
const session = ref<any>(null);
const answers = reactive<Record<number, any>>({});
const sessionForm = reactive<any>({ sessionType: 'RANDOM', questionType: 'MORNING_SINGLE', knowledgePointId: undefined, count: 5 });

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
  session.value = await api.createPracticeSession(sessionForm);
  initAnswers();
};

const submitSession = async () => {
  const payload = {
    answers: session.value.records.map((record: any) => ({
      recordId: record.recordId,
      answer: answers[record.recordId].answer,
      durationSeconds: 60,
      subjectiveScore: record.question.type === 'AFTERNOON_CASE' ? Number(answers[record.recordId].subjectiveScore) : null,
      reasonType: 'CONCEPT',
    })),
  };
  session.value = await api.submitPractice(session.value.id, payload);
  initAnswers();
  ElMessage.success('练习已提交');
};

const toggleFlag = async (record: any, field: 'favorite' | 'markedUnknown' | 'addedToReview') => {
  session.value = await api.getPracticeSession(session.value.id);
  const payload = { favorite: record.favorite, markedUnknown: record.markedUnknown, addedToReview: record.addedToReview };
  payload[field] = !record[field];
  await api.updatePracticeFlags(record.recordId, payload);
  session.value = await api.getPracticeSession(session.value.id);
  initAnswers();
};

const resultType = (result?: string) => {
  if (result === 'CORRECT') return 'success';
  if (result === 'WRONG') return 'danger';
  return 'warning';
};

watch(() => route.query, applyRoutePreset);
onMounted(loadKnowledge);
</script>
