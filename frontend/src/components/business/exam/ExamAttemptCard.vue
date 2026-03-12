<template>
  <el-card class="business-card attempt-card" shadow="never" data-testid="exam-attempt-card">
    <template #header>
      <div class="attempt-header">
        <div class="attempt-title-info">
          <h3 class="attempt-title" data-testid="exam-attempt-title">{{ attempt.examName }}</h3>
          <el-tag 
            size="small" 
            :type="attempt.status === 'SUBMITTED' ? 'success' : 'warning'"
            data-testid="exam-attempt-status"
          >
            {{ attempt.status === 'SUBMITTED' ? '已交卷' : '进行中' }}
          </el-tag>
        </div>
        <div class="attempt-meta">
          <span v-if="attempt.status !== 'SUBMITTED'" class="timer">剩余时间: --:--</span>
          <el-button 
            type="primary" 
            size="small"
            data-testid="exam-submit-button" 
            :disabled="attempt.status === 'SUBMITTED'" 
            @click="$emit('submit')"
          >
            提交试卷
          </el-button>
        </div>
      </div>
    </template>

    <el-alert
      v-if="attempt.systemReviewSummary"
      data-testid="exam-system-review-summary"
      type="success"
      :closable="false"
      :title="attempt.systemReviewSummary"
      show-icon
      class="review-alert"
    />

    <div class="questions-list">
      <el-card 
        v-for="(answer, index) in attempt.answers" 
        :key="answer.answerId" 
        class="question-card"
        shadow="never"
        :data-testid="`exam-answer-card-${index}`"
      >
        <div class="question-header">
          <span class="question-number">第 {{ index + 1 }} 题</span>
          <span class="question-title" :data-testid="`exam-question-title-${index}`">{{ answer.title }}</span>
          <el-tag size="small" effect="plain">{{ answer.questionType === 'MORNING_SINGLE' ? '单选' : '案例' }}</el-tag>
        </div>

        <div class="question-content" style="white-space:pre-wrap;">{{ answer.content }}</div>

        <div class="answer-section">
          <!-- Morning Single Choice -->
          <template v-if="answer.questionType === 'MORNING_SINGLE'">
            <div class="options-group" :data-testid="`exam-answer-group-${index}`">
              <el-radio-group 
                v-model="answersModel[answer.questionId].answerText" 
                :disabled="attempt.status === 'SUBMITTED'"
              >
                <el-radio 
                  v-for="option in answer.options" 
                  :key="option.key" 
                  :label="option.key"
                  class="option-item"
                >
                  <span class="option-key">{{ option.key }}.</span>
                  <span class="option-text">{{ option.content }}</span>
                </el-radio>
              </el-radio-group>
            </div>
          </template>

          <!-- Afternoon Case Study -->
          <template v-else>
            <div class="case-answer" :data-testid="`exam-answer-text-${index}`">
              <label>答题区</label>
              <el-input 
                v-model="answersModel[answer.questionId].answerText" 
                type="textarea" 
                :rows="6" 
                :disabled="attempt.status === 'SUBMITTED'" 
                placeholder="请在此输入你的答案..."
              />
            </div>
            
            <div v-if="attempt.status !== 'SUBMITTED'" class="score-input" :data-testid="`exam-score-input-${index}`">
              <label>自评分 (总分: {{ answer.score }})</label>
              <el-input-number 
                v-model="answersModel[answer.questionId].subjectiveScore" 
                :min="0" 
                :max="Number(answer.score)" 
              />
            </div>

            <div v-if="attempt.status === 'SUBMITTED'" class="reference-section">
              <label>参考要点</label>
              <div class="reference-content">{{ answer.referenceAnswer || '暂无参考要点' }}</div>
            </div>
          </template>
        </div>

        <!-- Result Tags -->
        <div v-if="attempt.status === 'SUBMITTED'" class="result-footer" :data-testid="`exam-result-row-${index}`">
          <el-tag :type="answer.result === 'CORRECT' ? 'success' : 'danger'" effect="dark">
            {{ answer.result === 'CORRECT' ? '正确' : '错误' }}
          </el-tag>
          <el-tag v-if="answer.autoScore != null" type="info">系统评分: {{ answer.autoScore }}</el-tag>
          <el-tag v-if="answer.subjectiveScore != null" type="primary">自评分: {{ answer.subjectiveScore }}</el-tag>
        </div>
      </el-card>
    </div>

    <div class="attempt-footer">
      <div class="self-review-section" data-testid="exam-self-review">
        <label>复盘总结</label>
        <el-input 
          v-model="reviewModel" 
          type="textarea" 
          :rows="4" 
          :disabled="attempt.status === 'SUBMITTED'" 
          placeholder="交卷后写下本次模拟的得失、薄弱点和改进计划..." 
        />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  attempt: any;
  answersModel: Record<number, any>;
  selfReviewSummary: string;
}>();

const emit = defineEmits(['update:selfReviewSummary', 'submit']);

const reviewModel = computed({
  get: () => props.selfReviewSummary,
  set: (val) => emit('update:selfReviewSummary', val)
});
</script>

<style scoped>
.attempt-card {
  margin-top: var(--space-6);
}

.attempt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.attempt-title-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.attempt-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.attempt-meta {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.timer {
  font-family: monospace;
  font-weight: 700;
  color: var(--color-danger);
}

.review-alert {
  margin-bottom: var(--space-6);
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.question-card {
  border: 1px solid var(--border-light);
  background-color: var(--bg-surface);
}

.question-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.question-number {
  font-weight: 700;
  color: var(--color-primary);
  white-space: nowrap;
}

.question-title {
  font-weight: 600;
  font-size: 16px;
  flex: 1;
}

.question-content {
  line-height: 1.6;
  color: var(--text-primary);
  background: var(--bg-subtle);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-6);
}

.answer-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.options-group :deep(.el-radio-group) {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: flex-start;
}

.option-item {
  margin-right: 0 !important;
  height: auto !important;
  white-space: normal !important;
  display: flex !important;
  align-items: flex-start !important;
}

.option-key {
  font-weight: 700;
  margin-right: var(--space-2);
}

.case-answer, .score-input, .self-review-section, .reference-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.case-answer label, .score-input label, .self-review-section label, .reference-section label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.reference-content {
  padding: var(--space-3);
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-primary);
  white-space: pre-wrap;
}

.result-footer {
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px dashed var(--border-light);
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.attempt-footer {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border-light);
}
</style>
