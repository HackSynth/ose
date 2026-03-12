<template>
  <div class="page-container" data-testid="settings-page">
    <PageHeader 
      title="系统设置" 
      description="管理考试目标、通过阈值、学习偏好，以及整包导入导出能力。"
    >
      <template #actions>
        <el-button data-testid="settings-health-button" @click="checkHealth">健康检查</el-button>
        <el-button data-testid="settings-export-button" @click="exportAll">导出全量数据</el-button>
        <el-button 
          type="primary" 
          data-testid="settings-save-button" 
          :loading="saving" 
          @click="save"
        >
          保存所有设置
        </el-button>
      </template>
    </PageHeader>

    <el-form label-position="top" :model="form" data-testid="settings-form">
      <el-card class="business-card" shadow="never">
        <template #header>
          <span class="card-title">基本备考配置</span>
        </template>
        <div class="form-grid">
          <el-form-item label="目标考试日期" required>
            <div data-testid="settings-exam-date">
              <el-date-picker v-model="form.examDate" type="date" value-format="YYYY-MM-DD" style="width:100%;" placeholder="选择您的考试日期" />
            </div>
          </el-form-item>
          <el-form-item label="期望通过分数 (总分 75)" required>
            <div data-testid="settings-passing-score">
              <el-input-number v-model="form.passingScore" :min="1" :max="75" style="width:100%;" />
            </div>
          </el-form-item>
          <el-form-item label="每周预计学习时长 (小时)" required>
            <div data-testid="settings-weekly-hours">
              <el-input-number v-model="form.weeklyStudyHours" :min="1" :max="168" style="width:100%;" />
            </div>
          </el-form-item>
          <el-form-item label="单次学习时长 (分钟)" required>
            <div data-testid="settings-session-minutes">
              <el-input-number v-model="form.dailySessionMinutes" :min="15" :step="15" :max="480" style="width:100%;" />
            </div>
          </el-form-item>
        </div>
        
        <el-form-item label="学习背景与偏好">
          <div data-testid="settings-learning-preference">
            <el-input 
              v-model="form.learningPreference" 
              type="textarea" 
              :rows="3" 
              placeholder="描述您的专业背景、薄弱环节或特定的学习习惯，系统将据此优化计划建议..." 
            />
          </div>
        </el-form-item>
        
        <el-form-item label="错题复习周期 (艾宾浩斯天数，逗号分隔)">
          <div data-testid="settings-review-intervals">
            <el-input v-model="intervalText" placeholder="默认推荐: 1,3,7,14" />
          </div>
        </el-form-item>
      </el-card>
    </el-form>

    <el-card class="business-card import-export-card" shadow="never" data-testid="settings-import-panel">
      <template #header>
        <span class="card-title">整包数据迁移 (JSON)</span>
      </template>
      <div class="import-layout">
        <div class="import-controls">
          <el-form-item label="重复数据处理策略" class="strategy-item">
            <div data-testid="settings-import-strategy">
              <el-select v-model="importStrategy" class="import-strategy-select" style="width:100%;">
                <el-option label="覆盖已有记录 (推荐)" value="OVERWRITE" />
                <el-option label="仅导入新数据 (跳过重复)" value="SKIP" />
              </el-select>
            </div>
          </el-form-item>
          <div class="action-group">
            <el-button data-testid="settings-import-template-button" @click="downloadTemplate">下载标准模板</el-button>
            <el-upload
              :show-file-list="false"
              :http-request="uploadBundle"
              accept="application/json,.json"
            >
              <el-button type="primary" plain data-testid="settings-import-upload-button">
                上传 JSON 文件并执行导入
              </el-button>
            </el-upload>
          </div>
        </div>
        <el-alert
          title="数据迁移说明"
          type="info"
          :closable="false"
          show-icon
          description="该功能允许您备份或恢复整个备考环境，包括知识点、题库、学习计划和笔记。导入前建议先执行“导出数据”进行备份。"
        />
      </div>
    </el-card>

    <div class="summary-grid">
      <el-card class="business-card" shadow="never" data-testid="settings-summary">
        <template #header><span class="card-title">计划核心参数摘要</span></template>
        <div class="card-grid">
          <StatCard data-testid="settings-summary-exam-date" title="最终考试日" :value="form.examDate || '未设置'" hint="倒计时基准" />
          <StatCard data-testid="settings-summary-weekly-hours" title="周投入时间" :value="`${form.weeklyStudyHours || 0} 小时`" hint="任务强度依据" />
          <StatCard data-testid="settings-summary-intervals" title="复习节奏" :value="intervalText || '-'" hint="艾宾浩斯曲线" />
        </div>
      </el-card>

      <el-card v-if="importResult" class="business-card" shadow="never" data-testid="settings-import-result">
        <template #header><span class="card-title">最近导入概览</span></template>
        <div class="card-grid">
          <StatCard title="知识点变动" :value="summaryText(importResult.knowledgePoints)" hint="新增 / 更新 / 跳过" />
          <StatCard title="题库变动" :value="summaryText(importResult.questions)" hint="新增 / 更新 / 跳过" />
        </div>
        
        <div v-if="importResult.warnings?.length || importResult.errors?.length" class="result-feedback">
          <el-alert v-for="warning in importResult.warnings" :key="warning" :title="warning" type="warning" :closable="false" class="feedback-item" />
          <el-alert
            v-for="error in importResult.errors"
            :key="`${error.scope}-${error.identifier}-${error.message}`"
            :title="`${error.scope}: ${error.identifier}`"
            :description="error.message"
            type="error"
            :closable="false"
            class="feedback-item"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, type UploadRequestOptions } from 'element-plus';
import { api } from '@/api';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import StatCard from '@/components/business/common/StatCard.vue';
import { downloadJson, normalizeIntervals } from '@/utils';

const saving = ref(false);
const importStrategy = ref<'OVERWRITE' | 'SKIP'>('OVERWRITE');
const importResult = ref<any>(null);
const form = reactive<any>({
  examDate: '',
  passingScore: 45,
  weeklyStudyHours: 12,
  learningPreference: '',
  dailySessionMinutes: 90,
});
const intervalText = ref('1,3,7,14');

const load = async () => {
  const data = await api.settings() as any;
  Object.assign(form, data);
  intervalText.value = (data.reviewIntervals || []).join(',');
};

const save = async () => {
  saving.value = true;
  try {
    await api.updateSettings({
      ...form,
      reviewIntervals: normalizeIntervals(intervalText.value),
    });
    ElMessage.success('系统设置已更新，核心学习计划已同步');
    await load();
  } finally {
    saving.value = false;
  }
};

const exportAll = async () => {
  const data = await api.exportAll();
  downloadJson('ose-full-backup.json', data);
  ElMessage.success('全量数据备份成功');
};

const downloadWithAuth = async (path: string, filename: string) => {
  const response = await fetch(path, { headers: { Authorization: `Bearer ${localStorage.getItem('ose-token') || ''}` } });
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
};

const downloadTemplate = async () => {
  await downloadWithAuth('/api/import/template', 'ose-import-template.json');
};

const uploadBundle = async (options: UploadRequestOptions) => {
  const formData = new FormData();
  formData.append('file', options.file);
  importResult.value = await api.importAllFile(formData, importStrategy.value);
  ElMessage.success('数据包已成功解析并导入');
  await load();
};

const checkHealth = async () => {
  const data = await api.health() as any;
  ElMessage.success(`系统运行状态：${data.status}`);
};

const summaryText = (section?: { created?: number; updated?: number; skipped?: number }) => {
  if (!section) return '0 / 0 / 0';
  return `${section.created || 0} / ${section.updated || 0} / ${section.skipped || 0}`;
};

onMounted(load);
</script>

<style scoped>
.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.import-layout {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.import-controls {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--space-3);
}

.strategy-item {
  margin-bottom: 0;
  flex: 1;
  min-width: 240px;
}

.strategy-item :deep(.el-form-item__label) {
  padding-bottom: 8px;
  line-height: 1.4;
}

.strategy-item :deep(.el-form-item__content) {
  display: block;
  line-height: normal;
}

.import-strategy-select :deep(.el-select__wrapper) {
  min-height: 40px;
  align-items: center;
}

.import-strategy-select :deep(.el-select__selected-item) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.action-group :deep(.el-button) {
  min-height: 40px;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-6);
  margin-top: var(--space-2);
}

.result-feedback {
  margin-top: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.feedback-item {
  border-radius: var(--radius-md);
}

@media (max-width: 768px) {
  .action-group {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
