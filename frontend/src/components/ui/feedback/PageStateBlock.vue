<template>
  <div class="page-state-block">
    <el-skeleton v-if="loading" animated>
      <template #template>
        <div class="skeleton-wrap">
          <el-skeleton-item variant="p" style="width: 60%" />
          <el-skeleton-item variant="p" style="width: 100%" />
          <el-skeleton-item variant="p" style="width: 100%" />
          <el-skeleton-item variant="p" style="width: 90%" />
          <el-skeleton-item variant="p" style="width: 100%" />
        </div>
      </template>
    </el-skeleton>

    <el-result
      v-else-if="errorMessage"
      icon="error"
      title="页面加载失败"
      :sub-title="errorMessage"
    >
      <template #extra>
        <el-button type="primary" @click="$emit('retry')">重试</el-button>
      </template>
    </el-result>

    <el-empty
      v-else-if="empty"
      :description="emptyDescription"
      :image-size="120"
    />

    <slot v-else />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  loading?: boolean;
  errorMessage?: string;
  empty?: boolean;
  emptyDescription?: string;
}>(), {
  loading: false,
  errorMessage: '',
  empty: false,
  emptyDescription: '暂无数据',
});

defineEmits<{
  retry: [];
}>();
</script>

<style scoped>
.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
</style>
