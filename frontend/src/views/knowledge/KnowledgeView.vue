<template>
  <div class="page-container" data-testid="knowledge-page">
    <PageHeader 
      title="知识体系" 
      description="维护软件设计师备考知识树，细化各个章节考点，并实时追踪您的掌握度与学习笔记。"
    >
      <template #actions>
        <PageActionGroup>
          <el-button 
            type="primary" 
            data-testid="knowledge-create-root" 
            @click="openCreate(null)"
          >
            新增根节点
          </el-button>
        </PageActionGroup>
      </template>
    </PageHeader>

    <PageSection>
      <el-tree 
        data-testid="knowledge-tree" 
        :data="tree" 
        node-key="id" 
        default-expand-all 
        :props="{ label: 'name', children: 'children' }"
        class="knowledge-tree"
      >
        <template #default="{ data }">
          <div :data-testid="`knowledge-node-${data.code}`" class="tree-node">
            <div class="node-info">
              <span class="node-code">{{ data.code }}</span>
              <span class="node-name">{{ data.name }}</span>
              <div class="node-meta">
                <span class="meta-item">L{{ data.level }}</span>
                <span class="meta-item">权重: {{ data.weight }}</span>
                <el-divider direction="vertical" />
                <span class="meta-item mastery">掌握度: {{ data.masteryLevel }}%</span>
              </div>
            </div>
            <div class="node-actions">
              <el-button link type="primary" size="small" @click.stop="openCreate(data.id)">子节点</el-button>
              <el-button link size="small" @click.stop="openEdit(data)">编辑</el-button>
              <el-button link type="danger" size="small" @click.stop="removeNode(data.id)">删除</el-button>
            </div>
          </div>
        </template>
      </el-tree>
    </PageSection>

    <el-dialog 
      v-model="dialogVisible" 
      :title="editingId ? '编辑知识点' : '新增知识点'" 
      :width="isMobile ? '100%' : '640px'"
      :fullscreen="isMobile"
      class="edit-dialog"
      destroy-on-close
    >
      <el-form ref="formRef" label-position="top" :model="form" :rules="rules" scroll-to-error data-testid="knowledge-form">
        <div class="form-grid">
          <el-form-item label="编码" prop="code" required>
            <el-input v-model="form.code" data-testid="knowledge-code" placeholder="如：CS-01" />
          </el-form-item>
          <el-form-item label="名称" prop="name" required>
            <el-input v-model="form.name" data-testid="knowledge-name" placeholder="知识点名称" />
          </el-form-item>
          <el-form-item label="层级" prop="level">
            <el-input-number v-model="form.level" data-testid="knowledge-level" :min="1" :max="3" style="width:100%;" />
          </el-form-item>
          <el-form-item label="父节点">
            <el-select v-model="form.parentId" data-testid="knowledge-parent" clearable style="width:100%;" placeholder="顶级节点">
              <el-option v-for="item in flatNodes" :key="item.id" :label="`[${item.code}] ${item.name}`" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="当前掌握度 (%)">
            <el-slider v-model="form.masteryLevel" data-testid="knowledge-mastery" :min="0" :max="100" show-input />
          </el-form-item>
          <el-form-item label="考试权重 (1-10)">
            <el-input-number v-model="form.weight" data-testid="knowledge-weight" :min="1" :max="10" style="width:100%;" />
          </el-form-item>
        </div>
        <el-form-item label="学习备注 / 考点说明">
          <el-input 
            v-model="form.note" 
            data-testid="knowledge-note" 
            type="textarea" 
            :rows="4" 
            placeholder="记录下该知识点的核心考法、公式或容易混淆的地方..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" data-testid="knowledge-save" @click="save">保存知识点</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { api } from '@/api';
import { useMobile } from '@/composables/useMobile';
import PageActionGroup from '@/components/ui/layout/PageActionGroup.vue';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import PageSection from '@/components/ui/layout/PageSection.vue';

const { isMobile } = useMobile();
const tree = ref<any[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<any>({ code: '', name: '', level: 1, masteryLevel: 50, weight: 5, note: '', parentId: null, sortOrder: 1 });

const rules: FormRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择层级', trigger: 'change' }],
};

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
  Object.assign(form, {
    code: node.code,
    name: node.name,
    level: node.level,
    masteryLevel: node.masteryLevel,
    weight: node.weight,
    note: node.note || '',
    parentId: node.parentId,
    sortOrder: node.sortOrder || 1
  });
  dialogVisible.value = true;
};

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  const payload = { ...form };
  if (editingId.value) {
    await api.updateKnowledge(editingId.value, payload);
  } else {
    await api.createKnowledge(payload);
  }
  ElMessage.success('知识体系已成功更新');
  dialogVisible.value = false;
  await load();
};

const removeNode = async (id: number) => {
  await ElMessageBox.confirm(
    '删除该节点将同时移除其所有子节点及其关联的统计数据，是否确认？', 
    '危险操作确认', 
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
  );
  await api.deleteKnowledge(id);
  ElMessage.success('知识点已成功移除');
  await load();
};

onMounted(load);
</script>

<style scoped>
.knowledge-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: var(--space-4);
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.node-code {
  font-size: 11px;
  font-weight: 700;
  color: var(--el-color-primary);
  font-family: monospace;
}

.node-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.node-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.mastery {
  color: var(--el-text-color-regular);
  font-weight: 500;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .edit-dialog :deep(.el-dialog__body) {
    max-height: calc(100vh - 140px);
    overflow-y: auto;
  }
}
</style>
