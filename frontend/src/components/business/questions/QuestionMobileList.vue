<template>
  <PageSection class="mobile-list-card">
    <el-skeleton v-if="loading" :rows="4" animated />

    <el-empty v-else-if="rows.length === 0" description="暂无题目数据" :image-size="100" />

    <div v-else class="mobile-list">
      <el-card v-for="row in rows" :key="row.id" shadow="never" class="mobile-item">
        <div class="item-header">
          <span class="item-title">{{ row.title }}</span>
          <el-tag size="small" :type="row.type === 'MORNING_SINGLE' ? 'primary' : 'warning'">
            {{ row.type === 'MORNING_SINGLE' ? '单选题' : '案例题' }}
          </el-tag>
        </div>

        <div class="item-meta">
          <el-tag size="small" effect="plain">年份 {{ row.year || '-' }}</el-tag>
          <el-tag size="small" effect="plain">难度 {{ row.difficulty || '-' }}</el-tag>
        </div>

        <div class="item-tags">
          <el-tag
            v-for="item in row.knowledgePoints"
            :key="item.id"
            size="small"
            effect="plain"
          >
            {{ item.name }}
          </el-tag>
        </div>

        <div class="item-actions">
          <el-button size="small" @click="$emit('edit', row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="$emit('remove', row.id)">删除</el-button>
        </div>
      </el-card>
    </div>
  </PageSection>
</template>

<script setup lang="ts">
import PageSection from '@/components/ui/layout/PageSection.vue';

defineProps<{
  rows: any[];
  loading: boolean;
}>();

defineEmits<{
  (e: 'edit', row: any): void;
  (e: 'remove', id: number): void;
}>();
</script>

<style scoped>
.mobile-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.mobile-item {
  border: 1px solid var(--el-border-color-lighter);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-2);
}

.item-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.5;
}

.item-meta,
.item-tags,
.item-actions {
  margin-top: var(--space-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.item-actions {
  justify-content: flex-end;
}
</style>
