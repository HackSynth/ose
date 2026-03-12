<template>
  <PageSection title="创建模拟卷">
    <el-form label-position="top" :model="form">
      <PageFormGrid :min-item-width="220">
        <el-form-item label="名称" data-testid="exam-name">
          <el-input v-model="form.name" placeholder="例如：2025年上半年软件设计师模拟" />
        </el-form-item>
        <el-form-item label="考试类型" data-testid="exam-type">
          <el-select v-model="form.type" placeholder="请选择类型" class="full-width">
            <el-option label="上午卷 (单选题)" value="MORNING" />
            <el-option label="下午卷 (案例题)" value="AFTERNOON" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长 (分钟)" data-testid="exam-duration">
          <el-input-number v-model="form.durationMinutes" :min="30" :max="240" class="full-width" />
        </el-form-item>
      </PageFormGrid>

      <el-form-item label="模拟说明" data-testid="exam-description">
        <el-input 
          v-model="form.description" 
          type="textarea" 
          :rows="2" 
          placeholder="简要描述本次模拟的目标或重点" 
        />
      </el-form-item>

      <el-form-item :label="`选择题目 (${form.questionIds.length} 题)`" data-testid="exam-question-ids">
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
      </el-form-item>
    </el-form>

    <div class="form-action-bar">
      <el-button type="primary" :loading="loading" @click="$emit('create')">
        立即创建模拟卷
      </el-button>
    </div>
  </PageSection>
</template>

<script setup lang="ts">
import PageFormGrid from '@/components/ui/form/PageFormGrid.vue';
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  form: any;
  questionOptions: any[];
  loading?: boolean;
}>();

defineEmits(['create']);
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
