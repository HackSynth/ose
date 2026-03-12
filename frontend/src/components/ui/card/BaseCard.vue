<template>
  <el-card class="ui-card" :shadow="shadow" :body-style="bodyStyle">
    <template v-if="$slots.header" #header>
      <div class="card-header">
        <slot name="header"></slot>
      </div>
    </template>
    <slot></slot>
  </el-card>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  shadow?: 'always' | 'hover' | 'never';
  bodyStyle?: any;
}>(), {
  shadow: 'never'
});
</script>

<style scoped>
.ui-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  background-color: var(--bg-surface);
}

.card-header {
  font-weight: 700;
  color: var(--text-primary);
}
</style>
