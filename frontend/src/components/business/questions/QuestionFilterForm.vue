<template>
  <PageSection class="filter-card">
    <el-form :model="filters" label-width="76px" label-position="left">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="关键字">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索题干、标题等"
              clearable
              @change="$emit('search')"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="题目类型">
            <el-select v-model="filters.type" clearable placeholder="全部" @change="$emit('search')" style="width: 100%;">
              <el-option label="上午题 (单选)" value="MORNING_SINGLE" />
              <el-option label="下午题 (案例)" value="AFTERNOON_CASE" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="关联知识点">
            <el-select
              v-model="filters.knowledgePointId"
              clearable
              filterable
              placeholder="选择知识点"
              @change="$emit('search')"
              style="width: 100%;"
            >
              <el-option
                v-for="item in knowledgeOptions"
                :key="item.id"
                :label="`${item.code} - ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="真题年份">
            <el-input-number
              v-model="filters.year"
              :min="2010"
              :max="2030"
              controls-position="right"
              @change="$emit('search')"
              style="width: 100%;"
            />
          </el-form-item>
        </el-col>

        <el-col :xs="24" class="filter-actions">
          <el-button type="primary" :loading="loading" @click="$emit('search')">查询</el-button>
          <el-button @click="$emit('reset')">重置</el-button>
        </el-col>
      </el-row>
    </el-form>
  </PageSection>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue';
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  filters: any;
  knowledgeOptions: any[];
  loading?: boolean;
}>();

defineEmits<{
  (e: 'search'): void;
  (e: 'reset'): void;
}>();
</script>

<style scoped>
.filter-card {
  margin-bottom: var(--space-4);
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

@media (max-width: 767px) {
  .filter-actions {
    justify-content: flex-start;
  }
}
</style>
