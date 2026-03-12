<template>
  <div class="ui-page-header">
    <div class="header-content">
      <h1 class="title">{{ title }}</h1>
      <p v-if="description" class="description">{{ description }}</p>
    </div>
    <div class="header-actions page-action-group" v-if="$slots.actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<style scoped>
.ui-page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-6);
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0;
  line-height: 1.2;
}

.description {
  font-size: 14px;
  color: var(--el-text-color-regular);
  margin: 0;
}

.header-actions {
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .ui-page-header {
    flex-direction: column;
    gap: var(--space-4);
  }
}
</style>
