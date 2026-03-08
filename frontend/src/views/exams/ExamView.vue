<template>
  <div class="page-container" data-testid="exams-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">模拟考试</h2>
        <p style="margin:6px 0 0;color:#64748b;">创建上午卷 / 下午卷模拟，支持交卷、成绩记录和历史回看。</p>
      </div>
      <el-button type="primary" data-testid="exam-create-button" @click="createExam">创建模拟卷</el-button>
    </div>

    <el-card class="panel-card" data-testid="exam-form-card">
      <template #header><span>创建模拟卷</span></template>
      <div class="card-grid">
        <div data-testid="exam-name"><el-input v-model="examForm.name" placeholder="模拟卷名称" /></div>
        <div data-testid="exam-type"><el-select v-model="examForm.type" placeholder="类型">
          <el-option label="上午卷" value="MORNING" />
          <el-option label="下午卷" value="AFTERNOON" />
        </el-select></div>
        <div data-testid="exam-duration"><el-input-number v-model="examForm.durationMinutes" :min="30" :max="240" style="width:100%;" /></div>
      </div>
      <div data-testid="exam-description"><el-input v-model="examForm.description" type="textarea" :rows="2" style="margin-top:12px;" placeholder="模拟说明" /></div>
      <div data-testid="exam-question-ids"><el-select v-model="examForm.questionIds" multiple filterable style="width:100%;margin-top:12px;" placeholder="选择题目">
        <el-option v-for="item in questionOptions" :key="item.id" :label="`${item.type} - ${item.title}`" :value="item.id" />
      </el-select></div>
    </el-card>

    <div class="split-layout">
      <el-card class="panel-card">
        <template #header><span>模拟卷列表</span></template>
        <el-table :data="exams" stripe data-testid="exam-list-table">
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column prop="type" label="类型" width="110" />
          <el-table-column prop="durationMinutes" label="时长" width="90" />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" :data-testid="`exam-start-${row.id}`" @click="startExam(row.id)">开始</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
      <el-card class="panel-card">
        <template #header><span>历史记录</span></template>
        <el-table :data="attempts" stripe data-testid="exam-attempt-table">
          <el-table-column prop="examName" label="名称" min-width="180" />
          <el-table-column prop="examType" label="类型" width="100" />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link :data-testid="`exam-view-attempt-${row.id}`" @click="viewAttempt(row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-card v-if="currentAttempt" class="panel-card" data-testid="exam-attempt-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span data-testid="exam-attempt-title">{{ currentAttempt.examName }}</span>
          <el-tag data-testid="exam-attempt-status" :type="currentAttempt.status === 'SUBMITTED' ? 'success' : 'warning'">{{ currentAttempt.status }}</el-tag>
        </div>
      </template>
      <el-alert
        v-if="currentAttempt.systemReviewSummary"
        data-testid="exam-system-review-summary"
        type="success"
        :closable="false"
        :title="currentAttempt.systemReviewSummary"
        style="margin-bottom:16px;"
      />
      <div style="display:flex;flex-direction:column;gap:16px;">
        <el-card v-for="(answer, index) in currentAttempt.answers" :key="answer.answerId" :data-testid="`exam-answer-card-${index}`" shadow="never">
          <strong :data-testid="`exam-question-title-${index}`">第 {{ index + 1 }} 题：{{ answer.title }}</strong>
          <p style="white-space:pre-wrap;color:#334155;">{{ answer.content }}</p>
          <template v-if="answer.questionType === 'MORNING_SINGLE'">
            <div :data-testid="`exam-answer-group-${index}`"><el-radio-group v-model="attemptAnswers[answer.questionId].answerText" :disabled="currentAttempt.status === 'SUBMITTED'">
              <el-radio v-for="option in answer.options" :key="option.key" :label="option.key">{{ option.key }}. {{ option.content }}</el-radio>
            </el-radio-group></div>
          </template>
          <template v-else>
            <div :data-testid="`exam-answer-text-${index}`"><el-input v-model="attemptAnswers[answer.questionId].answerText" type="textarea" :rows="4" :disabled="currentAttempt.status === 'SUBMITTED'" /></div>
            <div :data-testid="`exam-score-input-${index}`"><el-input-number v-model="attemptAnswers[answer.questionId].subjectiveScore" :min="0" :max="Number(answer.score)" :disabled="currentAttempt.status === 'SUBMITTED'" /></div>
            <div style="font-size:12px;color:#64748b;margin-top:8px;">参考要点：{{ answer.referenceAnswer || '暂无' }}</div>
          </template>
          <div v-if="currentAttempt.status === 'SUBMITTED'" :data-testid="`exam-result-row-${index}`" style="margin-top:12px;display:flex;gap:8px;flex-wrap:wrap;">
            <el-tag>{{ answer.result || '待批阅' }}</el-tag>
            <el-tag v-if="answer.autoScore != null">自动得分 {{ answer.autoScore }}</el-tag>
            <el-tag v-if="answer.subjectiveScore != null">自评分 {{ answer.subjectiveScore }}</el-tag>
          </div>
        </el-card>
      </div>
      <div data-testid="exam-self-review"><el-input v-model="selfReviewSummary" type="textarea" :rows="3" style="margin-top:16px;" :disabled="currentAttempt.status === 'SUBMITTED'" placeholder="交卷后写下本次复盘总结" /></div>
      <div style="margin-top:16px;display:flex;justify-content:flex-end;">
        <el-button type="primary" data-testid="exam-submit-button" :disabled="currentAttempt.status === 'SUBMITTED'" @click="submitAttempt">交卷</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';

const exams = ref<any[]>([]);
const attempts = ref<any[]>([]);
const questionOptions = ref<any[]>([]);
const currentAttempt = ref<any>(null);
const selfReviewSummary = ref('');
const attemptAnswers = reactive<Record<number, any>>({});
const examForm = reactive<any>({ name: '我的模拟卷', type: 'MORNING', durationMinutes: 150, description: '', questionIds: [] as number[] });

const load = async () => {
  exams.value = await api.exams() as any[];
  attempts.value = await api.examAttempts() as any[];
  questionOptions.value = await api.questions() as any[];
};

const createExam = async () => {
  await api.createExam(examForm);
  ElMessage.success('模拟卷已创建');
  await load();
};

const initAttemptAnswers = () => {
  Object.keys(attemptAnswers).forEach((key) => delete attemptAnswers[Number(key)]);
  currentAttempt.value?.answers?.forEach((item: any) => {
    attemptAnswers[item.questionId] = { answerText: item.answerText || '', subjectiveScore: item.subjectiveScore != null ? Number(item.subjectiveScore) : 0 };
  });
  selfReviewSummary.value = currentAttempt.value?.selfReviewSummary || '';
};

const startExam = async (id: number) => {
  currentAttempt.value = await api.startExam(id);
  initAttemptAnswers();
};

const viewAttempt = async (id: number) => {
  currentAttempt.value = await api.getExamAttempt(id);
  initAttemptAnswers();
};

const submitAttempt = async () => {
  currentAttempt.value = await api.submitExam(currentAttempt.value.id, {
    durationSeconds: 3600,
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
