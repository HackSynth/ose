<template>
  <PageSection title="练习配置">
    <el-form label-position="top" :model="form" data-testid="practice-form">
      <div class="form-grid">
        <el-form-item label="练习模式" data-testid="practice-session-type">
          <el-select v-model="form.sessionType" placeholder="模式" class="full-width">
            <el-option label="知识点专项" value="KNOWLEDGE" />
            <el-option label="随机练习" value="RANDOM" />
            <el-option label="错题针对性练习" value="MISTAKE" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="题型" data-testid="practice-question-type">
          <el-select v-model="form.questionType" placeholder="题型" class="full-width">
            <el-option label="上午题 (单选)" value="MORNING_SINGLE" />
            <el-option label="下午题 (案例)" value="AFTERNOON_CASE" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="知识点 (可选)" data-testid="practice-knowledge-point">
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
        </el-form-item>
        
        <el-form-item label="题目数量" data-testid="practice-count">
          <el-input-number v-model="form.count" :min="1" :max="20" class="full-width" />
        </el-form-item>
      </div>
    </el-form>

    <div class="form-action-bar">
      <el-button type="primary" :loading="loading" @click="$emit('create')">
        进入练习会话
      </el-button>
    </div>
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  form: any;
  knowledgeOptions: any[];
  loading?: boolean;
}>();

defineEmits(['create']);
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.full-width {
  width: 100%;
}
</style>
