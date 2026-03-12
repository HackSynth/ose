<template>
  <div class="page-container" data-testid="questions-page">
    <PageHeader 
      title="题库管理" 
      description="维护软件设计师备考的上午卷（单选）与下午卷（案例），支持批量导入导出与知识点关联。"
    >
      <template #actions>
        <el-dropdown trigger="click">
          <el-button>导入 / 导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
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
      </template>
    </PageHeader>

    <el-card class="business-card filter-card" shadow="never">
      <div class="filter-grid">
        <el-input 
          v-model="filters.keyword" 
          placeholder="搜索题干、标题或考点关键词" 
          clearable 
          @change="load" 
          prefix-icon="Search"
        />
        <el-select v-model="filters.type" clearable placeholder="题目类型" @change="load">
          <el-option label="上午题 (单选)" value="MORNING_SINGLE" />
          <el-option label="下午题 (案例)" value="AFTERNOON_CASE" />
        </el-select>
        <el-select v-model="filters.knowledgePointId" clearable filterable placeholder="关联知识点" @change="load">
          <el-option v-for="item in knowledgeOptions" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
        </el-select>
        <div class="year-picker">
          <span class="label">年份:</span>
          <el-input-number v-model="filters.year" :min="2010" :max="2030" controls-position="right" @change="load" />
        </div>
      </div>
    </el-card>

    <el-card class="business-card" shadow="never">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="title" label="题目名称" min-width="240">
          <template #default="{ row }">
            <div class="question-title-cell">
              <span class="q-title">{{ row.title }}</span>
              <span class="q-source">{{ row.source }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="题型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'MORNING_SINGLE' ? 'primary' : 'warning'">
              {{ row.type === 'MORNING_SINGLE' ? '单选题' : '案例题' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年份" width="80" align="center" />
        <el-table-column prop="difficulty" label="难度" width="80" align="center">
          <template #default="{ row }">
            <span class="difficulty-val">{{ row.difficulty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联知识点" min-width="200">
          <template #default="{ row }">
            <div class="tag-group">
              <el-tag v-for="item in row.knowledgePoints" :key="item.id" size="small" effect="plain" class="kp-tag">
                {{ item.name }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeQuestion(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog 
      v-model="visible" 
      :title="editingId ? '编辑题目详情' : '录入新题目'" 
      width="800px"
      destroy-on-close
    >
      <el-form label-position="top" :model="form" class="question-form">
        <div class="form-grid-top">
          <el-form-item label="题型" required>
            <el-select v-model="form.type" style="width:100%;">
              <el-option label="上午题 (单选)" value="MORNING_SINGLE" />
              <el-option label="下午题 (案例)" value="AFTERNOON_CASE" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题" required>
            <el-input v-model="form.title" placeholder="简短描述题目内容" />
          </el-form-item>
          <el-form-item label="真题年份">
            <el-input-number v-model="form.year" :min="2010" :max="2030" style="width:100%;" />
          </el-form-item>
          <el-form-item label="建议难度 (1-5)">
            <el-rate v-model="form.difficulty" style="margin-top: 8px;" />
          </el-form-item>
          <el-form-item label="题目来源">
            <el-input v-model="form.source" placeholder="如：2024上半年真题" />
          </el-form-item>
          <el-form-item label="默认分值">
            <el-input-number v-model="form.score" :min="1" :max="50" style="width:100%;" />
          </el-form-item>
        </div>

        <el-form-item label="完整题干内容" required>
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="6" 
            placeholder="在此输入详细的题目描述、背景资料或考题原文..."
          />
        </el-form-item>

        <div class="form-grid-mid">
          <el-form-item label="自定义标签 (多个用逗号分隔)">
            <el-input v-model="tagText" placeholder="如：重点, 难点, 2024" />
          </el-form-item>
          <el-form-item label="关联知识点 (多选)" required>
            <el-select v-model="form.knowledgePointIds" multiple filterable style="width:100%;" placeholder="搜索知识点">
              <el-option v-for="item in knowledgeOptions" :key="item.id" :label="`[${item.code}] ${item.name}`" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>

        <!-- Morning Specific -->
        <template v-if="form.type === 'MORNING_SINGLE'">
          <div class="options-container">
            <label class="section-label">选项配置</label>
            <div class="options-grid">
              <el-form-item v-for="option in form.options" :key="option.key" :label="`选项 ${option.key}`">
                <el-input v-model="option.content" />
              </el-form-item>
            </div>
            <el-form-item label="标准答案" required>
              <el-radio-group v-model="form.correctAnswer">
                <el-radio-button label="A" />
                <el-radio-button label="B" />
                <el-radio-button label="C" />
                <el-radio-button label="D" />
              </el-radio-group>
            </el-form-item>
            <el-form-item label="考点解析">
              <el-input v-model="form.explanation" type="textarea" :rows="3" placeholder="详细解释该题的解题思路和正确答案原因..." />
            </el-form-item>
          </div>
        </template>

        <!-- Afternoon Specific -->
        <template v-else>
          <el-form-item label="参考答案及评分要点" required>
            <el-input v-model="form.referenceAnswer" type="textarea" :rows="5" placeholder="列出详细的评分标准和关键步骤得分点..." />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="save">保存至题库</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import { ArrowDown, Search } from '@element-plus/icons-vue';
import { api } from '@/api';
import PageHeader from '@/components/ui/layout/PageHeader.vue';

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

const reset = () => {
  editingId.value = null;
  Object.assign(form, {
    type: 'MORNING_SINGLE', title: '', content: '', options: [{ key: 'A', content: '' }, { key: 'B', content: '' }, { key: 'C', content: '' }, { key: 'D', content: '' }],
    correctAnswer: 'A', explanation: '', referenceAnswer: '', year: 2025, difficulty: 3, source: 'OSE 备考题库', knowledgePointIds: [], score: 1, active: true,
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
  const payload = { 
    ...form, 
    tags: tagText.value.split(',').map((item) => item.trim()).filter(Boolean), 
    score: Number(form.score) 
  };
  if (editingId.value) await api.updateQuestion(editingId.value, payload);
  else await api.createQuestion(payload);
  ElMessage.success('题目已成功入库');
  visible.value = false;
  await load();
};

const removeQuestion = async (id: number) => {
  await ElMessageBox.confirm('题目删除后无法找回，确认继续？', '警告', { 
    confirmButtonText: '确定删除', 
    cancelButtonText: '取消',
    type: 'warning' 
  });
  await api.deleteQuestion(id);
  ElMessage.success('题目已移除');
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
  ElMessage.success('全量数据导入成功');
  await load();
};

onMounted(load);
</script>

<style scoped>
.filter-card {
  margin-bottom: var(--space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: var(--space-4);
  align-items: center;
}

.year-picker {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.year-picker .label {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.question-title-cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.q-title {
  font-weight: 600;
  color: var(--text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.q-source {
  font-size: 11px;
  color: var(--text-tertiary);
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.difficulty-val {
  font-weight: 700;
  color: var(--color-warning);
}

.form-grid-top, .form-grid-mid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-x-4);
}

.section-label {
  display: block;
  font-weight: 700;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--border-light);
  color: var(--text-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

@media (max-width: 960px) {
  .filter-grid { grid-template-columns: 1fr 1fr; }
  .form-grid-top, .form-grid-mid { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 640px) {
  .filter-grid { grid-template-columns: 1fr; }
  .form-grid-top, .form-grid-mid, .options-grid { grid-template-columns: 1fr; }
}
</style>
