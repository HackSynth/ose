<template>
  <el-card class="business-card page-section" shadow="never" :body-style="bodyStyle">
    <template v-if="$slots.header || title || $slots.actions" #header>
      <div class="page-section-header">
        <div class="page-section-title">
          <slot name="header">
            <span>{{ title }}</span>
          </slot>
        </div>
        <div v-if="$slots.actions" class="page-section-actions">
          <slot name="actions" />
        </div>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string;
  bodyStyle?: Record<string, string>;
}>(), {
  title: '',
  bodyStyle: () => ({}),
});
</script>

<style scoped>
.page-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.page-section-title {
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.page-section-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

@media (max-width: 767px) {
  .page-section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-section-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
