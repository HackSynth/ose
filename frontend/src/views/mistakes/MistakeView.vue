<template>
  <div class="page-container" data-testid="mistakes-page">
    <PageHeader 
      title="错题复习" 
      description="自动沉淀错题，结合艾宾浩斯遗忘曲线，按错因与复习状态持续推进掌握程度。"
    >
      <template #actions>
        <el-select 
          v-model="status" 
          data-testid="mistakes-status-filter" 
          clearable 
          placeholder="按复习状态筛选" 
          style="width:200px;" 
          @change="load"
        >
          <el-option label="新错题 (NEW)" value="NEW" />
          <el-option label="准备复习 (READY)" value="READY" />
          <el-option label="已复习 (REVIEWED)" value="REVIEWED" />
          <el-option label="已掌握 (MASTERED)" value="MASTERED" />
        </el-select>
        <el-button type="primary" plain @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-card class="business-card" shadow="never">
      <el-table :data="rows" stripe data-testid="mistakes-table" style="width: 100%">
        <el-table-column prop="questionTitle" label="题目描述" min-width="280">
          <template #default="{ row }">
            <div class="question-title-cell">{{ row.questionTitle }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgePointName" label="所属知识点" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.knowledgePointName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reasonType" label="错误原因" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.reasonType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewStatus" label="复习状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.reviewStatus)">
              {{ row.reviewStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nextReviewAt" label="下次复习日期" width="140" align="center" />
        <el-table-column prop="reviewCount" label="复习轮次" width="100" align="center">
          <template #default="{ row }">
            <span class="review-count">{{ row.reviewCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑状态</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="更新错题复习计划" width="520px" destroy-on-close>
      <el-form label-position="top" :model="form" data-testid="mistake-form">
        <el-form-item label="错因分类" required>
          <el-input v-model="form.reasonType" data-testid="mistake-reason-type" placeholder="例如：概念混淆、计算失误等" />
        </el-form-item>
        <el-form-item label="复习进度" required>
          <el-select v-model="form.reviewStatus" data-testid="mistake-review-status" style="width:100%;">
            <el-option label="NEW - 新错题" value="NEW" />
            <el-option label="READY - 待复习" value="READY" />
            <el-option label="REVIEWED - 已复习一次" value="REVIEWED" />
            <el-option label="MASTERED - 已彻底掌握" value="MASTERED" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划下次复习日期" required>
          <el-date-picker 
            v-model="form.nextReviewAt" 
            data-testid="mistake-next-review-at" 
            type="date" 
            value-format="YYYY-MM-DD" 
            style="width:100%;" 
            placeholder="选择日期"
          />
        </el-form-item>
        <el-form-item label="备注 / 关键点总结">
          <el-input 
            v-model="form.note" 
            data-testid="mistake-note" 
            type="textarea" 
            :rows="4" 
            placeholder="记录下该题的考点和容易踩坑的地方..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" data-testid="mistake-save-button" @click="save">保存变更</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PageHeader from '@/components/ui/layout/PageHeader.vue';

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
  Object.assign(form, {
    reasonType: row.reasonType,
    reviewStatus: row.reviewStatus,
    nextReviewAt: row.nextReviewAt,
    note: row.note || ''
  });
  visible.value = true;
};

const save = async () => {
  if (!currentId.value) return;
  await api.updateMistake(currentId.value, form);
  ElMessage.success('错题复习记录已成功更新');
  visible.value = false;
  await load();
};

const statusType = (val: string) => {
  if (val === 'NEW') return 'danger';
  if (val === 'READY') return 'warning';
  if (val === 'REVIEWED') return 'primary';
  if (val === 'MASTERED') return 'success';
  return 'info';
};

onMounted(load);
</script>

<style scoped>
.question-title-cell {
  font-weight: 500;
  color: var(--text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.review-count {
  font-weight: 700;
  color: var(--color-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>
