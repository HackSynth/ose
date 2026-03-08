<template>
  <div class="page-container" data-testid="settings-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">系统设置</h2>
        <p style="margin:6px 0 0;color:#64748b;">管理考试目标、通过阈值、学习偏好，以及整包导入导出能力。</p>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end;">
        <el-button data-testid="settings-health-button" @click="checkHealth">健康检查</el-button>
        <el-button data-testid="settings-import-template-button" @click="downloadTemplate">下载导入模板</el-button>
        <el-button data-testid="settings-export-button" @click="exportAll">导出数据</el-button>
        <el-button type="primary" data-testid="settings-save-button" :loading="saving" @click="save">保存设置</el-button>
      </div>
    </div>

    <el-card class="panel-card">
      <el-form label-position="top" :model="form" data-testid="settings-form">
        <div class="card-grid">
          <el-form-item label="目标考试日期">
            <div data-testid="settings-exam-date"><el-date-picker v-model="form.examDate" type="date" value-format="YYYY-MM-DD" style="width:100%;" /></div>
          </el-form-item>
          <el-form-item label="通过分数阈值">
            <div data-testid="settings-passing-score"><el-input-number v-model="form.passingScore" :min="1" :max="100" style="width:100%;" /></div>
          </el-form-item>
          <el-form-item label="每周学习时长（小时）">
            <div data-testid="settings-weekly-hours"><el-input-number v-model="form.weeklyStudyHours" :min="1" :max="60" style="width:100%;" /></div>
          </el-form-item>
          <el-form-item label="单次学习时长（分钟）">
            <div data-testid="settings-session-minutes"><el-input-number v-model="form.dailySessionMinutes" :min="15" :step="15" :max="300" style="width:100%;" /></div>
          </el-form-item>
        </div>
        <el-form-item label="学习偏好">
          <div data-testid="settings-learning-preference"><el-input v-model="form.learningPreference" type="textarea" :rows="3" /></div>
        </el-form-item>
        <el-form-item label="默认复习周期（用逗号分隔）">
          <div data-testid="settings-review-intervals"><el-input v-model="intervalText" placeholder="例如 1,3,7,14" /></div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="panel-card" data-testid="settings-import-panel">
      <template #header><span>整包导入 / 导出</span></template>
      <div class="card-grid">
        <el-form-item label="重复数据处理策略" style="margin-bottom:0;">
          <div data-testid="settings-import-strategy"><el-select v-model="importStrategy" style="width:100%;">
            <el-option label="覆盖已有数据" value="OVERWRITE" />
            <el-option label="跳过重复数据" value="SKIP" />
          </el-select></div>
        </el-form-item>
        <div style="display:flex;align-items:flex-end;">
          <el-upload
            :show-file-list="false"
            :http-request="uploadBundle"
            accept="application/json,.json"
          >
            <el-button type="primary" data-testid="settings-import-upload-button">上传整包 JSON 导入</el-button>
          </el-upload>
        </div>
      </div>
      <el-alert
        title="导入说明"
        type="info"
        :closable="false"
        style="margin-top:12px;"
        description="支持导入设置、知识点、题库、学习计划和笔记。推荐先从“导出数据”导出当前结构，再按模板补充或修改。"
      />
    </el-card>

    <el-card class="panel-card" data-testid="settings-summary">
      <template #header><span>当前状态</span></template>
      <div class="card-grid">
        <StatCard data-testid="settings-summary-exam-date" title="考试日期" :value="form.examDate || '-'" hint="可在此页面修改" />
        <StatCard data-testid="settings-summary-weekly-hours" title="每周学习时长" :value="`${form.weeklyStudyHours || 0} 小时`" hint="用于计划生成" />
        <StatCard data-testid="settings-summary-intervals" title="复习周期" :value="intervalText || '-'" hint="错题复习默认节奏" />
      </div>
    </el-card>

    <el-card v-if="importResult" class="panel-card" data-testid="settings-import-result">
      <template #header><span>最近一次导入结果</span></template>
      <div class="card-grid">
        <StatCard title="知识点" :value="summaryText(importResult.knowledgePoints)" hint="创建 / 更新 / 跳过" />
        <StatCard title="题库" :value="summaryText(importResult.questions)" hint="创建 / 更新 / 跳过" />
        <StatCard title="笔记" :value="summaryText(importResult.notes)" hint="创建 / 更新 / 跳过" />
        <StatCard title="计划" :value="summaryText(importResult.studyPlans)" hint="创建 / 更新 / 跳过" />
      </div>
      <div v-if="importResult.warnings?.length" style="margin-top:16px;display:flex;flex-direction:column;gap:8px;">
        <el-alert v-for="warning in importResult.warnings" :key="warning" :title="warning" type="warning" :closable="false" />
      </div>
      <div v-if="importResult.errors?.length" style="margin-top:16px;display:flex;flex-direction:column;gap:8px;">
        <el-alert
          v-for="error in importResult.errors"
          :key="`${error.scope}-${error.identifier}-${error.message}`"
          :title="`${error.scope} / ${error.identifier}`"
          :description="error.message"
          type="error"
          :closable="false"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, type UploadRequestOptions } from 'element-plus';
import { api } from '@/api';
import StatCard from '@/components/StatCard.vue';
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
    ElMessage.success('设置已保存，计划已按最新考试日期自动重排');
    await load();
  } finally {
    saving.value = false;
  }
};

const exportAll = async () => {
  const data = await api.exportAll();
  downloadJson('ose-export.json', data);
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
  await downloadWithAuth('/api/import/template', 'full-import-template.json');
};

const uploadBundle = async (options: UploadRequestOptions) => {
  const formData = new FormData();
  formData.append('file', options.file);
  importResult.value = await api.importAllFile(formData, importStrategy.value);
  ElMessage.success('整包导入完成');
  await load();
};

const checkHealth = async () => {
  const data = await api.health() as any;
  ElMessage.success(`服务状态：${data.status}`);
};

const summaryText = (section?: { created?: number; updated?: number; skipped?: number }) => {
  if (!section) return '0 / 0 / 0';
  return `${section.created || 0} / ${section.updated || 0} / ${section.skipped || 0}`;
};

onMounted(load);
</script>
