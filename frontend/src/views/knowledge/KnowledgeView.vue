<template>
  <div class="page-container" data-testid="knowledge-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">知识体系</h2>
        <p style="margin:6px 0 0;color:#64748b;">维护软件设计师备考知识树，并记录掌握度与备注。</p>
      </div>
      <el-button type="primary" data-testid="knowledge-create-root" @click="openCreate(null)">新增根节点</el-button>
    </div>

    <el-card class="panel-card">
      <el-tree data-testid="knowledge-tree" :data="tree" node-key="id" default-expand-all :props="{ label: 'name', children: 'children' }">
        <template #default="{ data }">
          <div :data-testid="`knowledge-node-${data.code}`" style="display:flex;justify-content:space-between;align-items:center;width:100%;gap:16px;">
            <div style="display:flex;flex-direction:column;gap:4px;">
              <strong>{{ data.name }}</strong>
              <span style="font-size:12px;color:#64748b;">{{ data.code }} · L{{ data.level }} · 掌握度 {{ data.masteryLevel }}%</span>
            </div>
            <div style="display:flex;gap:8px;">
              <el-tag type="success">权重 {{ data.weight }}</el-tag>
              <el-button link type="primary" @click.stop="openCreate(data.id)">新增子节点</el-button>
              <el-button link @click.stop="openEdit(data)">编辑</el-button>
              <el-button link type="danger" @click.stop="removeNode(data.id)">删除</el-button>
            </div>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑知识点' : '新增知识点'" width="640px">
      <el-form label-position="top" :model="form" data-testid="knowledge-form">
        <div class="card-grid">
          <el-form-item label="编码"><el-input v-model="form.code" data-testid="knowledge-code" /></el-form-item>
          <el-form-item label="名称"><el-input v-model="form.name" data-testid="knowledge-name" /></el-form-item>
          <el-form-item label="层级"><el-input-number v-model="form.level" data-testid="knowledge-level" :min="1" :max="3" style="width:100%;" /></el-form-item>
          <el-form-item label="父节点">
            <el-select v-model="form.parentId" data-testid="knowledge-parent" clearable style="width:100%;">
              <el-option v-for="item in flatNodes" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="掌握度"><el-input-number v-model="form.masteryLevel" data-testid="knowledge-mastery" :min="0" :max="100" style="width:100%;" /></el-form-item>
          <el-form-item label="权重"><el-input-number v-model="form.weight" data-testid="knowledge-weight" :min="1" :max="10" style="width:100%;" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="form.note" data-testid="knowledge-note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" data-testid="knowledge-save" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '@/api';

const tree = ref<any[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const form = reactive<any>({ code: '', name: '', level: 1, masteryLevel: 50, weight: 5, note: '', parentId: null, sortOrder: 1 });

const flatten = (items: any[], list: any[] = []) => {
  items.forEach((item) => {
    list.push(item);
    if (item.children?.length) flatten(item.children, list);
  });
  return list;
};

const flatNodes = computed(() => flatten(tree.value, []));

const load = async () => {
  tree.value = await api.knowledgeTree() as any[];
};

const resetForm = () => {
  Object.assign(form, { code: '', name: '', level: 1, masteryLevel: 50, weight: 5, note: '', parentId: null, sortOrder: 1 });
  editingId.value = null;
};

const openCreate = (parentId: number | null) => {
  resetForm();
  form.parentId = parentId;
  if (parentId) {
    const parent = flatNodes.value.find((item) => item.id === parentId);
    form.level = Math.min((parent?.level || 1) + 1, 3);
  }
  dialogVisible.value = true;
};

const openEdit = (node: any) => {
  editingId.value = node.id;
  Object.assign(form, node, { sortOrder: 1 });
  dialogVisible.value = true;
};

const save = async () => {
  const payload = { ...form };
  if (editingId.value) {
    await api.updateKnowledge(editingId.value, payload);
  } else {
    await api.createKnowledge(payload);
  }
  ElMessage.success('知识点已保存');
  dialogVisible.value = false;
  await load();
};

const removeNode = async (id: number) => {
  await ElMessageBox.confirm('删除后不可恢复，确认继续？', '删除确认', { type: 'warning' });
  await api.deleteKnowledge(id);
  ElMessage.success('已删除');
  await load();
};

onMounted(load);
</script>
