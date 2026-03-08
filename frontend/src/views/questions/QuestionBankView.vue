<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">题库管理</h2>
        <p style="margin:6px 0 0;color:#64748b;">维护上午题 / 下午题，支持筛选、导入模板与导出。</p>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;">
        <el-button @click="downloadTemplate('csv')">CSV 模板</el-button>
        <el-button @click="downloadTemplate('json')">JSON 模板</el-button>
        <el-button @click="downloadExport('csv')">导出 CSV</el-button>
        <el-button @click="downloadExport('json')">导出 JSON</el-button>
        <el-upload :show-file-list="false" :http-request="uploadFile">
          <el-button>导入题库</el-button>
        </el-upload>
        <el-button type="primary" @click="openCreate">新增题目</el-button>
      </div>
    </div>

    <el-card class="panel-card">
      <div class="card-grid" style="margin-bottom:16px;">
        <el-input v-model="filters.keyword" placeholder="搜索题干/标题" clearable @change="load" />
        <el-select v-model="filters.type" clearable placeholder="题型" @change="load">
          <el-option label="上午题" value="MORNING_SINGLE" />
          <el-option label="下午题" value="AFTERNOON_CASE" />
        </el-select>
        <el-select v-model="filters.knowledgePointId" clearable placeholder="知识点" @change="load">
          <el-option v-for="item in knowledgeOptions" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
        </el-select>
        <el-input-number v-model="filters.year" :min="2020" :max="2030" controls-position="right" style="width:100%;" @change="load" />
      </div>
      <el-table :data="rows" stripe>
        <el-table-column prop="title" label="标题" min-width="260" />
        <el-table-column prop="type" label="题型" width="130" />
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="difficulty" label="难度" width="90" />
        <el-table-column label="知识点" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="item in row.knowledgePoints" :key="item.id" size="small" style="margin-right:6px;">{{ item.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeQuestion(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="editingId ? '编辑题目' : '新增题目'" width="880px">
      <el-form label-position="top" :model="form">
        <div class="card-grid">
          <el-form-item label="题型">
            <el-select v-model="form.type" style="width:100%;">
              <el-option label="上午题" value="MORNING_SINGLE" />
              <el-option label="下午题" value="AFTERNOON_CASE" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="年份"><el-input-number v-model="form.year" :min="2020" :max="2030" style="width:100%;" /></el-form-item>
          <el-form-item label="难度"><el-input-number v-model="form.difficulty" :min="1" :max="5" style="width:100%;" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="form.source" /></el-form-item>
          <el-form-item label="分值"><el-input-number v-model="form.score" :min="1" :max="50" style="width:100%;" /></el-form-item>
        </div>
        <el-form-item label="题干"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="标签（逗号分隔）"><el-input v-model="tagText" /></el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointIds" multiple filterable style="width:100%;">
            <el-option v-for="item in knowledgeOptions" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <template v-if="form.type === 'MORNING_SINGLE'">
          <div class="card-grid">
            <el-form-item v-for="option in form.options" :key="option.key" :label="`选项 ${option.key}`">
              <el-input v-model="option.content" />
            </el-form-item>
          </div>
          <el-form-item label="正确答案">
            <el-select v-model="form.correctAnswer" style="width:100%;">
              <el-option label="A" value="A" /><el-option label="B" value="B" /><el-option label="C" value="C" /><el-option label="D" value="D" />
            </el-select>
          </el-form-item>
          <el-form-item label="解析"><el-input v-model="form.explanation" type="textarea" :rows="3" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="参考答案要点"><el-input v-model="form.referenceAnswer" type="textarea" :rows="4" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { UploadRequestOptions } from 'element-plus';
import { api } from '@/api';

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
  year: 2026,
  difficulty: 2,
  source: 'OSE 示例题',
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
  rows.value = await api.questions(filters) as any[];
  tree.value = await api.knowledgeTree() as any[];
};

const reset = () => {
  editingId.value = null;
  Object.assign(form, {
    type: 'MORNING_SINGLE', title: '', content: '', options: [{ key: 'A', content: '' }, { key: 'B', content: '' }, { key: 'C', content: '' }, { key: 'D', content: '' }],
    correctAnswer: 'A', explanation: '', referenceAnswer: '', year: 2026, difficulty: 2, source: 'OSE 示例题', knowledgePointIds: [], score: 1, active: true,
  });
  tagText.value = '';
};

const openCreate = () => {
  reset();
  visible.value = true;
};

const openEdit = (row: any) => {
  editingId.value = row.id;
  Object.assign(form, {
    ...row,
    knowledgePointIds: (row.knowledgePoints || []).map((item: any) => item.id),
    options: row.options?.length ? row.options : [{ key: 'A', content: '' }, { key: 'B', content: '' }, { key: 'C', content: '' }, { key: 'D', content: '' }],
    score: Number(row.score),
  });
  tagText.value = (row.tags || []).join(',');
  visible.value = true;
};

const save = async () => {
  const payload = { ...form, tags: tagText.value.split(',').map((item) => item.trim()).filter(Boolean), score: Number(form.score) };
  if (editingId.value) await api.updateQuestion(editingId.value, payload);
  else await api.createQuestion(payload);
  ElMessage.success('题目已保存');
  visible.value = false;
  await load();
};

const removeQuestion = async (id: number) => {
  await ElMessageBox.confirm('确认删除该题目吗？', '删除确认', { type: 'warning' });
  await api.deleteQuestion(id);
  ElMessage.success('已删除');
  await load();
};

const downloadWithAuth = async (path: string, filename: string) => {
  const response = await fetch(path, { headers: { Authorization: `Bearer ${localStorage.getItem('ose-token') || ''}` } });
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

const downloadTemplate = (format: string) => downloadWithAuth(`/api/questions/templates/${format}`, `questions-template.${format}`);
const downloadExport = (format: string) => downloadWithAuth(`/api/questions/export?format=${format}`, `questions-export.${format}`);

const uploadFile = async (options: UploadRequestOptions) => {
  const formData = new FormData();
  formData.append('file', options.file);
  await api.importQuestions(formData);
  ElMessage.success('导入完成');
  await load();
};

onMounted(load);
</script>
