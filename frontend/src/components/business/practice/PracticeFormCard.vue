<template>
  <el-card class="business-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">练习配置</span>
      </div>
    </template>
    
    <div class="form-grid" data-testid="practice-form">
      <div class="form-item" data-testid="practice-session-type">
        <label>练习模式</label>
        <el-select v-model="form.sessionType" placeholder="模式" class="full-width">
          <el-option label="知识点专项" value="KNOWLEDGE" />
          <el-option label="随机练习" value="RANDOM" />
          <el-option label="错题针对性练习" value="MISTAKE" />
        </el-select>
      </div>
      
      <div class="form-item" data-testid="practice-question-type">
        <label>题型</label>
        <el-select v-model="form.questionType" placeholder="题型" class="full-width">
          <el-option label="上午题 (单选)" value="MORNING_SINGLE" />
          <el-option label="下午题 (案例)" value="AFTERNOON_CASE" />
        </el-select>
      </div>
      
      <div class="form-item" data-testid="practice-knowledge-point">
        <label>知识点 (可选)</label>
        <el-select 
          v-model="form.knowledgePointId" 
          clearable 
          filterable
          placeholder="搜索知识点" 
          class="full-width"
        >
          <el-option 
            v-for="item in knowledgeOptions" 
            :key="item.id" 
            :label="`${item.code} - ${item.name}`" 
            :value="item.id" 
          />
        </el-select>
      </div>
      
      <div class="form-item" data-testid="practice-count">
        <label>题目数量</label>
        <el-input-number v-model="form.count" :min="1" :max="20" class="full-width" />
      </div>
    </div>

    <div class="form-actions">
      <el-button type="primary" :loading="loading" @click="$emit('create')">
        进入练习会话
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  form: any;
  knowledgeOptions: any[];
  loading?: boolean;
}>();

defineEmits(['create']);
</script>

<style scoped>
.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
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
</style>
