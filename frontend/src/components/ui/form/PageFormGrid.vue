<template>
  <div
    class="page-form-grid"
    :class="{ 'mobile-single': mobileSingle }"
    :style="{
      '--page-form-grid-min-width': normalizedMinWidth,
      '--page-form-grid-gap': gap,
    }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  minItemWidth?: number | string;
  gap?: string;
  mobileSingle?: boolean;
}>(), {
  minItemWidth: 220,
  gap: 'var(--space-4)',
  mobileSingle: true,
});

const normalizedMinWidth = computed(() => {
  if (typeof props.minItemWidth === 'number') {
    return `${props.minItemWidth}px`;
  }
  return props.minItemWidth;
});
</script>

<style scoped>
.page-form-grid {
  display: grid;
  gap: var(--page-form-grid-gap);
  grid-template-columns: repeat(auto-fit, minmax(var(--page-form-grid-min-width), 1fr));
}

@media (max-width: 767px) {
  .page-form-grid.mobile-single {
    grid-template-columns: 1fr;
  }
}
</style>
