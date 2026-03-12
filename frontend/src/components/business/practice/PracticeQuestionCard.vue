<template>
  <el-card 
    class="business-card question-card" 
    shadow="never" 
    :data-testid="`practice-record-${index}`"
  >
    <div class="question-header">
      <div class="question-meta">
        <span class="question-number">第 {{ index + 1 }} 题</span>
        <div class="knowledge-tags">
          <el-tag 
            v-for="kp in record.question.knowledgePoints" 
            :key="kp.id" 
            size="small" 
            effect="plain"
          >
            {{ kp.name }}
          </el-tag>
        </div>
      </div>
      <div class="question-actions" v-if="sessionStatus === 'SUBMITTED'">
        <el-button-group>
          <el-button 
            size="small" 
            :type="record.favorite ? 'warning' : 'default'" 
            @click="$emit('toggle-flag', 'favorite')"
          >
            {{ record.favorite ? '取消收藏' : '收藏' }}
          </el-button>
          <el-button 
            size="small" 
            :type="record.markedUnknown ? 'danger' : 'default'" 
            @click="$emit('toggle-flag', 'markedUnknown')"
          >
            {{ record.markedUnknown ? '取消不会' : '标记不会' }}
          </el-button>
          <el-button 
            size="small" 
            :type="record.addedToReview ? 'primary' : 'default'" 
            @click="$emit('toggle-flag', 'addedToReview')"
          >
            {{ record.addedToReview ? '取消复习' : '加入复习' }}
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div class="question-main">
      <h4 class="question-title" :data-testid="`practice-question-title-${index}`">
        {{ record.question.title }}
      </h4>
      <div class="question-content" style="white-space:pre-wrap;">{{ record.question.content }}</div>
    </div>

    <div class="answer-section">
      <!-- Morning Single Choice -->
      <template v-if="record.question.type === 'MORNING_SINGLE'">
        <div class="options-group" :data-testid="`practice-answer-group-${index}`">
          <el-radio-group 
            v-model="answerModel.answer" 
            :disabled="sessionStatus === 'SUBMITTED'"
          >
            <el-radio 
              v-for="option in record.question.options" 
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
        <div class="case-answer" :data-testid="`practice-answer-text-${index}`">
          <label>作答区域</label>
          <el-input 
            v-model="answerModel.answer" 
            type="textarea" 
            :rows="6" 
            :disabled="sessionStatus === 'SUBMITTED'" 
            placeholder="请输入您的作答内容..." 
          />
        </div>
        
        <div class="score-input" v-if="sessionStatus !== 'SUBMITTED'" :data-testid="`practice-score-input-${index}`">
          <label>自评分 (最高 {{ record.question.score }} 分)</label>
          <el-input-number 
            v-model="answerModel.subjectiveScore" 
            :min="0" 
            :max="Number(record.question.score)" 
            :step="1" 
          />
        </div>

        <div v-if="sessionStatus === 'SUBMITTED'" class="reference-section">
          <label>参考答案 / 评分要点</label>
          <div class="reference-content">{{ record.question.referenceAnswer || '暂无参考要点' }}</div>
        </div>
      </template>
    </div>

    <!-- Results Section -->
    <div v-if="sessionStatus === 'SUBMITTED'" class="result-footer" :data-testid="`practice-result-row-${index}`">
      <el-tag :type="resultType(record.result)" :data-testid="`practice-result-${index}`" effect="dark">
        {{ record.result === 'CORRECT' ? '正确' : (record.result === 'WRONG' ? '错误' : '待自评') }}
      </el-tag>
      <el-tag v-if="record.autoScore != null" :data-testid="`practice-auto-score-${index}`" type="info">
        系统得分 {{ record.autoScore }}
      </el-tag>
      <el-tag v-if="record.subjectiveScore != null" :data-testid="`practice-subjective-score-${index}`" type="primary">
        自评分 {{ record.subjectiveScore }}
      </el-tag>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  record: any;
  index: number;
  answerModel: any;
  sessionStatus: string;
}>();

defineEmits(['toggle-flag']);

const resultType = (result?: string) => {
  if (result === 'CORRECT') return 'success';
  if (result === 'WRONG') return 'danger';
  return 'warning';
};
</script>

<style scoped>
.question-card {
  margin-bottom: var(--space-6);
  border: 1px solid var(--border-light);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
  gap: var(--space-3);
}

.question-meta {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.question-number {
  font-weight: 700;
  color: var(--color-primary);
  font-size: 15px;
}

.knowledge-tags {
  display: flex;
  gap: var(--space-2);
}

.question-main {
  margin-bottom: var(--space-6);
}

.question-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 var(--space-3) 0;
  color: var(--text-primary);
  line-height: 1.4;
}

.question-content {
  line-height: 1.6;
  color: var(--text-primary);
  background: var(--bg-subtle);
  padding: var(--space-4);
  border-radius: var(--radius-md);
}

.answer-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
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

.case-answer, .score-input, .reference-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.case-answer label, .score-input label, .reference-section label {
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
  align-items: center;
}
</style>
