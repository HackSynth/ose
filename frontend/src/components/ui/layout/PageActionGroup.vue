<template>
  <div class="page-action-group">
    <slot />
  </div>
</template>

<style scoped>
.page-action-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

@media (max-width: 767px) {
  .page-action-group {
    width: 100%;
  }
}
</style>
