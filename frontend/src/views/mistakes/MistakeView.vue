<template>
  <div class="page-container" data-testid="mistakes-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">错题复习</h2>
        <p style="margin:6px 0 0;color:#64748b;">自动沉淀错题，按错因与复习状态持续推进。</p>
      </div>
      <el-select v-model="status" data-testid="mistakes-status-filter" clearable placeholder="筛选状态" style="width:180px;" @change="load">
        <el-option label="NEW" value="NEW" />
        <el-option label="READY" value="READY" />
        <el-option label="REVIEWED" value="REVIEWED" />
        <el-option label="MASTERED" value="MASTERED" />
      </el-select>
    </div>

    <el-card class="panel-card">
      <el-table :data="rows" stripe data-testid="mistakes-table">
        <el-table-column prop="questionTitle" label="题目" min-width="280" />
        <el-table-column prop="knowledgePointName" label="知识点" min-width="160" />
        <el-table-column prop="reasonType" label="错因" width="120" />
        <el-table-column prop="reviewStatus" label="状态" width="120" />
        <el-table-column prop="nextReviewAt" label="下次复习" width="140" />
        <el-table-column prop="reviewCount" label="复习次数" width="100" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">更新</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="更新错题状态" width="520px">
      <el-form label-position="top" :model="form" data-testid="mistake-form">
        <el-form-item label="错因分类"><el-input v-model="form.reasonType" data-testid="mistake-reason-type" /></el-form-item>
        <el-form-item label="复习状态">
          <el-select v-model="form.reviewStatus" data-testid="mistake-review-status" style="width:100%;">
            <el-option label="NEW" value="NEW" />
            <el-option label="READY" value="READY" />
            <el-option label="REVIEWED" value="REVIEWED" />
            <el-option label="MASTERED" value="MASTERED" />
          </el-select>
        </el-form-item>
        <el-form-item label="下次复习日期">
          <el-date-picker v-model="form.nextReviewAt" data-testid="mistake-next-review-at" type="date" value-format="YYYY-MM-DD" style="width:100%;" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.note" data-testid="mistake-note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" data-testid="mistake-save-button" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';

const rows = ref<any[]>([]);
const status = ref('');
const visible = ref(false);
const currentId = ref<number | null>(null);
const form = reactive<any>({ reasonType: '', reviewStatus: 'READY', nextReviewAt: '', note: '' });

const load = async () => {
  rows.value = await api.mistakes(status.value ? { status: status.value } : undefined) as any[];
};

const openEdit = (row: any) => {
  currentId.value = row.id;
  Object.assign(form, row);
  visible.value = true;
};

const save = async () => {
  if (!currentId.value) return;
  await api.updateMistake(currentId.value, form);
  ElMessage.success('错题记录已更新');
  visible.value = false;
  await load();
};

onMounted(load);
</script>
