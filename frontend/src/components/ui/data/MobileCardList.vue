<template>
  <div class="mobile-card-list" data-testid="mobile-card-list">
    <el-empty v-if="!items.length" :description="emptyDescription" :image-size="96" />
    <template v-else>
      <el-card
        v-for="(item, index) in items"
        :key="itemKey ? String(item[itemKey]) : index"
        class="business-card mobile-card-item"
        shadow="never"
      >
        <slot name="item" :item="item" :index="index" />
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  items: any[];
  emptyDescription?: string;
  itemKey?: string;
}>(), {
  emptyDescription: '暂无数据',
  itemKey: '',
});
</script>

<style scoped>
.mobile-card-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.mobile-card-item :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
</style>
