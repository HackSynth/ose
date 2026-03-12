<template>
  <el-card class="business-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">创建模拟卷</span>
      </div>
    </template>
    
    <div class="form-grid">
      <div class="form-item" data-testid="exam-name">
        <label>名称</label>
        <el-input v-model="form.name" placeholder="例如：2025年上半年软件设计师模拟" />
      </div>
      <div class="form-item" data-testid="exam-type">
        <label>考试类型</label>
        <el-select v-model="form.type" placeholder="请选择类型" class="full-width">
          <el-option label="上午卷 (单选题)" value="MORNING" />
          <el-option label="下午卷 (案例题)" value="AFTERNOON" />
        </el-select>
      </div>
      <div class="form-item" data-testid="exam-duration">
        <label>时长 (分钟)</label>
        <el-input-number v-model="form.durationMinutes" :min="30" :max="240" class="full-width" />
      </div>
    </div>

    <div class="form-item" data-testid="exam-description" style="margin-top: var(--space-4);">
      <label>模拟说明</label>
      <el-input 
        v-model="form.description" 
        type="textarea" 
        :rows="2" 
        placeholder="简要描述本次模拟的目标或重点" 
      />
    </div>

    <div class="form-item" data-testid="exam-question-ids" style="margin-top: var(--space-4);">
      <label>选择题目 ({{ form.questionIds.length }} 题)</label>
      <el-select 
        v-model="form.questionIds" 
        multiple 
        filterable 
        collapse-tags
        collapse-tags-indicator
        placeholder="搜索题目关键字或类型" 
        class="full-width"
      >
        <el-option 
          v-for="item in questionOptions" 
          :key="item.id" 
          :label="`[${item.type}] ${item.title}`" 
          :value="item.id" 
        />
      </el-select>
    </div>

    <div class="form-actions">
      <el-button type="primary" :loading="loading" @click="$emit('create')">
        立即创建模拟卷
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  form: any;
  questionOptions: any[];
  loading?: boolean;
}>();

defineEmits(['create']);
</script>

<style scoped>
.business-card {
  margin-bottom: var(--space-6);
}

.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: var(--space-4);
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-item label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.full-width {
  width: 100%;
}

.form-actions {
  margin-top: var(--space-6);
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
