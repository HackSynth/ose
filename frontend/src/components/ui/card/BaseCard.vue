<template>
  <el-card class="business-card ui-card" :shadow="shadow" :body-style="bodyStyle">
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
.card-header {
  font-weight: 700;
  color: var(--el-text-color-primary);
}
</style>
