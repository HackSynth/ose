<template>
  <div class="sidebar-container" data-testid="app-sidebar">
    <div class="sidebar-logo" data-testid="app-logo">OSE 备考系统</div>

    <el-scrollbar class="sidebar-scrollbar">
      <el-menu
        :default-active="route.path"
        router
        class="sidebar-menu"
        data-testid="app-menu"
        @select="handleSelect"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.path"
          :index="item.path"
          :data-testid="item.testId"
        >
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const emit = defineEmits<{
  (e: 'menu-click'): void;
}>();

const route = useRoute();
const router = useRouter();

const menus = computed(() => router.getRoutes()
  .filter((item) => item.meta?.menu)
  .map((item) => ({
    path: item.path,
    title: item.meta?.title as string,
    testId: `menu-${item.name as string}`,
  })));

const handleSelect = () => {
  emit('menu-click');
};
</script>

<style scoped>
.sidebar-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar-logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-4);
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-light);
}

.sidebar-scrollbar {
  flex: 1;
}

.sidebar-menu {
  border-right: none;
}
</style>
