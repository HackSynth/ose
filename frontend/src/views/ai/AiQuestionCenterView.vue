<template>
  <div class="page-container" data-testid="ai-question-page">
    <PageHeader
      title="AI 出题中心"
      description="支持 OpenAI 与 Claude 按知识点/难度/题型生成上午题与下午题，可先预览编辑再保存。"
    />

    <el-alert
      title="AI 生成题目仅供辅助学习，内容需人工确认后使用。"
      type="warning"
      :closable="false"
      show-icon
      class="warn-alert"
    />

    <PageSection>
      <el-form label-position="top" :model="form">
        <PageFormGrid :min-item-width="200">
          <el-form-item label="AI 提供商">
            <el-select v-model="form.providerId" data-testid="ai-provider" @change="onProviderChange">
              <el-option
                v-for="item in providers"
                :key="item.providerId"
                :label="item.displayName"
                :value="item.providerId"
                :disabled="!item.configured"
              >
                <span>{{ item.displayName }}</span>
                <span style="float:right;color:var(--text-tertiary)">{{ item.configured ? '已配置' : '未配置' }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="模型">
            <el-select v-model="form.model" data-testid="ai-model">
              <el-option v-for="model in modelOptions" :key="model.model" :label="model.displayName" :value="model.model" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="form.questionType" data-testid="ai-question-type">
              <el-option label="上午题" value="MORNING_SINGLE" />
              <el-option label="下午题" value="AFTERNOON_CASE" />
            </el-select>
          </el-form-item>
          <el-form-item label="出题场景">
            <el-select v-model="form.topicType" data-testid="ai-topic-type">
              <el-option label="按知识点出题" value="KNOWLEDGE_POINT" />
              <el-option label="按难度出题" value="DIFFICULTY" />
              <el-option label="按题型出题" value="QUESTION_TYPE" />
              <el-option label="按考试阶段出题" value="EXAM_PHASE" />
              <el-option label="薄弱知识点强化" value="WEAK_KNOWLEDGE" />
              <el-option label="错题相似题强化" value="MISTAKE_SIMILAR" />
              <el-option label="生成上午选择题" value="MORNING_SET" />
              <el-option label="生成下午案例题" value="AFTERNOON_SET" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" data-testid="ai-difficulty">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="风格">
            <el-select v-model="form.styleType" data-testid="ai-style">
              <el-option label="偏考试风格" value="EXAM" />
              <el-option label="偏基础巩固" value="FOUNDATION" />
              <el-option label="偏综合应用" value="COMPREHENSIVE" />
            </el-select>
          </el-form-item>
          <el-form-item label="数量">
            <el-input-number v-model="form.count" :min="1" :max="20" data-testid="ai-count" />
          </el-form-item>
          <el-form-item label="语言">
            <el-input v-model="form.language" data-testid="ai-language" />
          </el-form-item>
        </PageFormGrid>

        <el-form-item label="知识点（多选）">
          <el-select v-model="form.knowledgePointIds" multiple filterable data-testid="ai-knowledge" style="width:100%">
            <el-option v-for="item in knowledgeOptions" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>

        <div class="switch-row">
          <el-switch v-model="form.includeExplanation" active-text="生成解析" data-testid="ai-include-explanation" />
          <el-switch v-model="form.includeAnswer" active-text="生成答案" data-testid="ai-include-answer" />
          <el-switch v-model="form.saveToBank" active-text="直接保存题库" data-testid="ai-save-to-bank" />
        </div>

        <el-form-item label="补充要求">
          <el-input v-model="form.additionalRequirement" type="textarea" :rows="2" placeholder="例如强调事务隔离、UML 建模等" />
        </el-form-item>

        <PageActionGroup>
          <el-button type="primary" :loading="generating" data-testid="ai-generate" @click="generate">提交生成</el-button>
          <el-button :disabled="!drafts.length" data-testid="ai-regenerate" @click="generate">重新生成</el-button>
          <el-button :disabled="!drafts.length" data-testid="ai-discard" @click="discard">丢弃结果</el-button>
          <el-button :disabled="!drafts.length" data-testid="ai-save" @click="saveAll">保存到题库</el-button>
        </PageActionGroup>
      </el-form>
    </PageSection>

    <PageSection>
      <template #header>
        <div class="preview-header">
          <span>结果预览（{{ drafts.length }} 题）</span>
          <el-button text :disabled="!drafts.length" @click="copyAll">复制题目</el-button>
        </div>
      </template>

      <el-empty v-if="!drafts.length" description="尚未生成题目" />
      <div v-else class="draft-list" data-testid="ai-result-list">
        <el-alert v-if="validationErrors.length" type="error" :closable="false" show-icon>
          <template #title>
            结果包含拦截项：{{ validationErrors.join('；') }}
          </template>
        </el-alert>

        <el-card v-for="(draft, idx) in drafts" :key="draft.draftId" class="business-card draft-item" shadow="never">
          <template #header>
            <div class="draft-head">
              <span>题目 {{ idx + 1 }} · <el-tag type="success" size="small">AI 生成</el-tag></span>
              <span>{{ draft.provider }} / {{ draft.model }}</span>
            </div>
          </template>

          <el-form label-position="top">
            <el-form-item label="标题">
              <el-input v-model="draft.title" />
            </el-form-item>
            <el-form-item :label="draft.questionType === 'MORNING_SINGLE' ? '题干' : '案例背景/题干'">
              <el-input v-model="draft.content" type="textarea" :rows="4" />
            </el-form-item>

            <template v-if="draft.questionType === 'MORNING_SINGLE'">
              <PageFormGrid :min-item-width="240">
                <el-form-item v-for="option in draft.options" :key="option.key" :label="`选项 ${option.key}`">
                  <el-input v-model="option.content" />
                </el-form-item>
              </PageFormGrid>
              <el-form-item label="正确答案">
                <el-select v-model="draft.correctAnswer">
                  <el-option label="A" value="A" />
                  <el-option label="B" value="B" />
                  <el-option label="C" value="C" />
                  <el-option label="D" value="D" />
                </el-select>
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="参考答案要点">
                <el-input v-model="draft.referenceAnswer" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item label="评分点/得分点">
                <el-input
                  :model-value="(draft.scoringPoints || []).join('\n')"
                  type="textarea"
                  :rows="3"
                  @input="(value: string) => draft.scoringPoints = value.split('\n').map((it) => it.trim()).filter(Boolean)"
                />
              </el-form-item>
            </template>

            <el-form-item label="解析">
              <el-input v-model="draft.explanation" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="知识点">
              <el-select v-model="draft.knowledgePointIds" multiple filterable style="width:100%">
                <el-option v-for="item in knowledgeOptions" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
              </el-select>
            </el-form-item>

            <div class="row-end">
              <el-button type="danger" link @click="removeDraft(idx)">删除题目</el-button>
            </div>
          </el-form>
        </el-card>
      </div>
    </PageSection>

    <PageSection title="生成历史">
      <el-table :data="historyRows" size="small" data-testid="ai-history-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="provider" label="Provider" width="120" />
        <el-table-column prop="model" label="Model" min-width="220" />
        <el-table-column prop="questionType" label="题型" width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PageFormGrid from '@/components/ui/form/PageFormGrid.vue';
import PageActionGroup from '@/components/ui/layout/PageActionGroup.vue';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import PageSection from '@/components/ui/layout/PageSection.vue';
import type { AiQuestionProviderOption, AiProviderType } from '@/types';

const generating = ref(false);
const generationId = ref<number | null>(null);
const providers = ref<AiQuestionProviderOption[]>([]);
const modelsByProvider = ref<Record<string, any[]>>({});
const knowledgeTree = ref<any[]>([]);
const drafts = ref<any[]>([]);
const validationErrors = ref<string[]>([]);
const historyRows = ref<any[]>([]);

const form = reactive<any>({
  providerId: '',
  provider: 'OPENAI' as AiProviderType,
  model: '',
  questionType: 'MORNING_SINGLE',
  topicType: 'KNOWLEDGE_POINT',
  knowledgePointIds: [],
  difficulty: 'MEDIUM',
  count: 3,
  includeExplanation: true,
  includeAnswer: true,
  saveToBank: false,
  language: '中文',
  styleType: 'EXAM',
  additionalRequirement: '',
});

const flatten = (items: any[], list: any[] = []) => {
  items.forEach((item) => {
    list.push(item);
    if (item.children?.length) flatten(item.children, list);
  });
  return list;
};

const knowledgeOptions = computed(() => flatten(knowledgeTree.value, []));
const activeProvider = computed(() => providers.value.find((item) => item.providerId === form.providerId) || null);
const modelOptions = computed(() => modelsByProvider.value[form.providerId] || []);

const loadProviders = async () => {
  providers.value = await api.aiProviders();
  for (const provider of providers.value) {
    modelsByProvider.value[provider.providerId] = provider.models || [];
  }
  const firstConfigured = providers.value.find((item) => item.configured);
  if (firstConfigured && !providers.value.some((item) => item.providerId === form.providerId && item.configured)) {
    form.providerId = firstConfigured.providerId;
  }
  if (activeProvider.value) {
    form.provider = activeProvider.value.provider;
  }
  form.model = (modelsByProvider.value[form.providerId] || [])[0]?.model || '';
};

const loadKnowledge = async () => {
  knowledgeTree.value = await api.knowledgeTree() as any[];
  if (!form.knowledgePointIds.length) {
    form.knowledgePointIds = knowledgeOptions.value.slice(0, 2).map((item) => item.id);
  }
};

const loadHistory = async () => {
  historyRows.value = await api.aiHistory() as any[];
};

const onProviderChange = () => {
  if (activeProvider.value) {
    form.provider = activeProvider.value.provider;
  }
  form.model = (modelsByProvider.value[form.providerId] || [])[0]?.model || '';
};

const generate = async () => {
  generating.value = true;
  try {
    const result = await api.aiGenerateQuestions(form) as any;
    generationId.value = result.generationId;
    drafts.value = result.drafts || [];
    validationErrors.value = result.validationErrors || [];
    if (form.saveToBank && drafts.value.length && validationErrors.value.length === 0) {
      await saveAll();
      return;
    }
    ElMessage.success(`生成完成，共 ${drafts.value.length} 题`);
    await loadHistory();
  } finally {
    generating.value = false;
  }
};

const saveAll = async () => {
  if (!drafts.value.length) return;
  const payload = {
    generationId: generationId.value,
    providerId: form.providerId,
    provider: form.provider,
    model: form.model,
    questionType: form.questionType,
    drafts: drafts.value.map((item) => ({
      title: item.title,
      content: item.content,
      options: item.options,
      correctAnswer: item.correctAnswer,
      explanation: item.explanation,
      referenceAnswer: item.referenceAnswer,
      scoringPoints: item.scoringPoints,
      knowledgePointIds: item.knowledgePointIds,
      difficulty: item.difficulty,
      tags: item.tags,
    })),
  };
  const result = await api.aiSaveQuestions(payload) as any;
  ElMessage.success(`已保存 ${result.savedCount} 道题，状态: ${result.status}`);
  await loadHistory();
};

const discard = () => {
  drafts.value = [];
  validationErrors.value = [];
};

const removeDraft = (index: number) => {
  drafts.value.splice(index, 1);
};

const copyAll = async () => {
  const text = drafts.value.map((item, index) => `${index + 1}. ${item.title}\n${item.content}`).join('\n\n');
  await navigator.clipboard.writeText(text);
  ElMessage.success('题目已复制');
};

onMounted(async () => {
  await loadProviders();
  await loadKnowledge();
  await loadHistory();
});
</script>

<style scoped>
.warn-alert {
  margin-bottom: var(--space-4);
}

.switch-row {
  display: flex;
  gap: var(--space-6);
  margin-bottom: var(--space-4);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.draft-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.draft-item {
  border: 1px solid var(--el-border-color-lighter);
}

.draft-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.row-end {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .switch-row,
  .page-action-group {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
