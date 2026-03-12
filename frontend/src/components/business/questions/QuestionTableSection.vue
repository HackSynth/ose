<template>
  <PageSection>
    <el-table :data="rows" stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="title" label="题目名称" min-width="240">
        <template #default="{ row }">
          <div class="question-title-cell">
            <span class="q-title">{{ row.title }}</span>
            <span class="q-source">{{ row.source }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="type" label="题型" min-width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'MORNING_SINGLE' ? 'primary' : 'warning'">
            {{ row.type === 'MORNING_SINGLE' ? '单选题' : '案例题' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="year" label="年份" min-width="80" align="center" />

      <el-table-column prop="difficulty" label="难度" min-width="80" align="center">
        <template #default="{ row }">
          <span class="difficulty-val">{{ row.difficulty }}</span>
        </template>
      </el-table-column>

      <el-table-column label="关联知识点" min-width="200">
        <template #default="{ row }">
          <div class="tag-group">
            <el-tag v-for="item in row.knowledgePoints" :key="item.id" size="small" effect="plain">
              {{ item.name }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$emit('edit', row)">编辑</el-button>
          <el-button link type="danger" @click="$emit('remove', row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="暂无题目数据" :image-size="100" />
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
.question-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.q-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.q-source {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.difficulty-val {
  font-weight: 700;
  color: var(--el-color-warning);
}
</style>
