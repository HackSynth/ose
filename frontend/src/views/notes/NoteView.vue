<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">学习笔记</h2>
        <p style="margin:6px 0 0;color:#64748b;">支持 Markdown、收藏、搜索和关联知识点 / 题目。</p>
      </div>
      <div style="display:flex;gap:8px;">
        <el-input v-model="keyword" placeholder="搜索笔记" clearable @change="load" style="width:260px;" />
        <el-button type="primary" @click="openCreate">新增笔记</el-button>
      </div>
    </div>

    <div class="split-layout">
      <el-card class="panel-card">
        <template #header><span>笔记列表</span></template>
        <el-table :data="rows" stripe>
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column prop="summary" label="摘要" min-width="260" />
          <el-table-column prop="favorite" label="收藏" width="90">
            <template #default="{ row }">{{ row.favorite ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeNote(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
      <el-card class="panel-card">
        <template #header><span>Markdown 预览</span></template>
        <div v-html="previewHtml" style="min-height:320px;"></div>
      </el-card>
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑笔记' : '新增笔记'" width="960px">
      <div class="split-layout">
        <div>
          <el-form label-position="top" :model="form">
            <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
            <el-form-item label="摘要"><el-input v-model="form.summary" /></el-form-item>
            <el-form-item label="内容（Markdown）"><el-input v-model="form.content" type="textarea" :rows="14" /></el-form-item>
            <el-form-item label="收藏"><el-switch v-model="form.favorite" /></el-form-item>
            <el-form-item label="关联项">
              <div style="display:flex;flex-direction:column;gap:8px;width:100%;">
                <div v-for="(link, index) in form.links" :key="index" style="display:grid;grid-template-columns:140px 1fr 80px;gap:8px;">
                  <el-select v-model="link.linkType">
                    <el-option label="知识点" value="KNOWLEDGE" />
                    <el-option label="题目" value="QUESTION" />
                    <el-option label="模拟考" value="EXAM" />
                  </el-select>
                  <el-select v-model="link.targetId" filterable>
                    <el-option v-for="item in targetOptions(link.linkType)" :key="item.id" :label="item.label" :value="item.id" />
                  </el-select>
                  <el-button @click="form.links.splice(index, 1)">删除</el-button>
                </div>
                <el-button @click="form.links.push({ linkType: 'KNOWLEDGE', targetId: null })">新增关联</el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
        <div>
          <div style="font-weight:600;margin-bottom:12px;">实时预览</div>
          <div v-html="previewHtml" style="border:1px solid #e5e7eb;border-radius:12px;padding:16px;min-height:400px;background:#fff;"></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { marked } from 'marked';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';

const rows = ref<any[]>([]);
const keyword = ref('');
const visible = ref(false);
const editingId = ref<number | null>(null);
const knowledge = ref<any[]>([]);
const questions = ref<any[]>([]);
const exams = ref<any[]>([]);
const form = reactive<any>({ title: '', summary: '', content: '', favorite: false, links: [] as any[] });

const previewHtml = computed(() => marked.parse(form.content || '') as string);

const load = async () => {
  rows.value = await api.notes(keyword.value ? { q: keyword.value } : undefined) as any[];
  knowledge.value = await api.knowledgeTree() as any[];
  questions.value = await api.questions() as any[];
  exams.value = await api.exams() as any[];
};

const flatten = (items: any[], list: any[] = []) => {
  items.forEach((item) => {
    list.push({ id: item.id, label: `${item.code} - ${item.name}` });
    if (item.children?.length) flatten(item.children, list);
  });
  return list;
};

const targetOptions = (type: string) => {
  if (type === 'QUESTION') return questions.value.map((item) => ({ id: item.id, label: item.title }));
  if (type === 'EXAM') return exams.value.map((item) => ({ id: item.id, label: item.name }));
  return flatten(knowledge.value, []);
};

const reset = () => {
  editingId.value = null;
  Object.assign(form, { title: '', summary: '', content: '', favorite: false, links: [] });
};

const openCreate = () => {
  reset();
  visible.value = true;
};

const openEdit = (row: any) => {
  editingId.value = row.id;
  Object.assign(form, JSON.parse(JSON.stringify(row)));
  visible.value = true;
};

const save = async () => {
  const payload = { ...form };
  if (editingId.value) await api.updateNote(editingId.value, payload);
  else await api.createNote(payload);
  ElMessage.success('笔记已保存');
  visible.value = false;
  await load();
};

const removeNote = async (id: number) => {
  await ElMessageBox.confirm('确认删除这条笔记吗？', '删除确认', { type: 'warning' });
  await api.deleteNote(id);
  ElMessage.success('已删除');
  await load();
};

onMounted(load);
</script>
