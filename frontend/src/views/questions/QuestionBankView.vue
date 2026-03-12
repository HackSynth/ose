<template>
  <div class="page-container" data-testid="questions-page">
    <PageHeader
      title="题库管理"
      description="维护软件设计师备考的上午卷（单选）与下午卷（案例），支持批量导入导出与知识点关联。"
    >
      <template #actions>
        <PageActionGroup>
          <el-dropdown trigger="click">
            <el-button>
              导入 / 导出
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="downloadTemplate('csv')">下载 CSV 模板</el-dropdown-item>
                <el-dropdown-item @click="downloadTemplate('json')">下载 JSON 模板</el-dropdown-item>
                <el-divider style="margin: 4px 0" />
                <el-dropdown-item @click="downloadExport('csv')">导出全量题库 (CSV)</el-dropdown-item>
                <el-dropdown-item @click="downloadExport('json')">导出全量题库 (JSON)</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-upload :show-file-list="false" :http-request="uploadFile">
            <el-button type="success" plain>批量导入</el-button>
          </el-upload>

          <el-button type="primary" @click="openCreate">新增单道题目</el-button>
        </PageActionGroup>
      </template>
    </PageHeader>

    <QuestionFilterForm
      :filters="filters"
      :knowledge-options="knowledgeOptions"
      :loading="loading"
      @search="load"
      @reset="onFilterReset"
    />

    <QuestionMobileList
      v-if="isMobile"
      :rows="rows"
      :loading="loading"
      @edit="openEdit"
      @remove="removeQuestion"
    />

    <QuestionTableSection
      v-else
      :rows="rows"
      :loading="loading"
      @edit="openEdit"
      @remove="removeQuestion"
    />

    <QuestionEditorDialog
      v-model="visible"
      v-model:tag-text="tagText"
      :is-mobile="isMobile"
      :editing-id="editingId"
      :form="form"
      :knowledge-options="knowledgeOptions"
      @save="save"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import { ArrowDown } from '@element-plus/icons-vue';
import { api } from '@/api';
import { useMobile } from '@/composables/useMobile';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import PageActionGroup from '@/components/ui/layout/PageActionGroup.vue';
import QuestionFilterForm from '@/components/business/questions/QuestionFilterForm.vue';
import QuestionTableSection from '@/components/business/questions/QuestionTableSection.vue';
import QuestionMobileList from '@/components/business/questions/QuestionMobileList.vue';
import QuestionEditorDialog from '@/components/business/questions/QuestionEditorDialog.vue';

const { isMobile } = useMobile();
const loading = ref(false);
const rows = ref<any[]>([]);
const tree = ref<any[]>([]);
const visible = ref(false);
const editingId = ref<number | null>(null);
const filters = reactive<any>({ keyword: '', type: '', year: undefined, knowledgePointId: undefined });
const form = reactive<any>({
  type: 'MORNING_SINGLE',
  title: '',
  content: '',
  options: [{ key: 'A', content: '' }, { key: 'B', content: '' }, { key: 'C', content: '' }, { key: 'D', content: '' }],
  correctAnswer: 'A',
  explanation: '',
  referenceAnswer: '',
  year: 2025,
  difficulty: 3,
  source: 'OSE 备考题库',
  knowledgePointIds: [] as number[],
  score: 1,
  active: true,
});
const tagText = ref('');

const flatten = (items: any[], list: any[] = []) => {
  items.forEach((item) => {
    list.push(item);
    if (item.children?.length) flatten(item.children, list);
  });
  return list;
};

const knowledgeOptions = computed(() => flatten(tree.value, []));

const load = async () => {
  loading.value = true;
  try {
    rows.value = await api.questions(filters) as any[];
    tree.value = await api.knowledgeTree() as any[];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  editingId.value = null;
  Object.assign(form, {
    type: 'MORNING_SINGLE',
    title: '',
    content: '',
    options: [{ key: 'A', content: '' }, { key: 'B', content: '' }, { key: 'C', content: '' }, { key: 'D', content: '' }],
    correctAnswer: 'A',
    explanation: '',
    referenceAnswer: '',
    year: 2025,
    difficulty: 3,
    source: 'OSE 备考题库',
    knowledgePointIds: [],
    score: 1,
    active: true,
  });
  tagText.value = '';
};

const onFilterReset = async () => {
  Object.assign(filters, { keyword: '', type: '', year: undefined, knowledgePointId: undefined });
  await load();
};

const openCreate = () => {
  resetForm();
  visible.value = true;
};

const openEdit = (row: any) => {
  editingId.value = row.id;
  Object.assign(form, {
    ...row,
    knowledgePointIds: (row.knowledgePoints || []).map((item: any) => item.id),
    options: row.options?.length
      ? row.options
      : [{ key: 'A', content: '' }, { key: 'B', content: '' }, { key: 'C', content: '' }, { key: 'D', content: '' }],
    score: Number(row.score),
  });
  tagText.value = (row.tags || []).join(',');
  visible.value = true;
};

const save = async () => {
  const payload = {
    ...form,
    tags: tagText.value.split(',').map((item) => item.trim()).filter(Boolean),
    score: Number(form.score),
  };

  if (editingId.value) {
    await api.updateQuestion(editingId.value, payload);
  } else {
    await api.createQuestion(payload);
  }

  ElMessage.success('题目已成功入库');
  visible.value = false;
  await load();
};

const removeQuestion = async (id: number) => {
  await ElMessageBox.confirm('题目删除后无法找回，确认继续？', '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  });

  await api.deleteQuestion(id);
  ElMessage.success('题目已移除');
  await load();
};

const downloadWithAuth = async (path: string, filename: string) => {
  const response = await fetch(path, {
    headers: { Authorization: `Bearer ${localStorage.getItem('ose-token') || ''}` },
  });
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
};

const downloadTemplate = (format: string) =>
  downloadWithAuth(`/api/questions/templates/${format}`, `questions-template.${format}`);

const downloadExport = (format: string) =>
  downloadWithAuth(`/api/questions/export?format=${format}`, `questions-export.${format}`);

const uploadFile = async (options: UploadRequestOptions) => {
  const formData = new FormData();
  formData.append('file', options.file);
  await api.importQuestions(formData);
  ElMessage.success('全量数据导入成功');
  await load();
};

onMounted(load);
</script>

<style scoped></style>
