<template>
  <el-header class="app-topbar">
    <div class="topbar-left">
      <el-button
        v-if="isMobile"
        class="menu-button"
        text
        @click="$emit('toggle-menu')"
      >
        <el-icon size="20"><Menu /></el-icon>
      </el-button>

      <div class="title-area">
        <span class="title" data-testid="page-title">{{ title }}</span>
        <span v-if="!isMobile" class="subtitle">单用户软件设计师备考工作台</span>
      </div>
    </div>

    <el-dropdown trigger="click">
      <span class="user-trigger" data-testid="topbar-user-trigger">
        {{ userName || '管理员' }}
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item data-testid="logout-button" @click="$emit('logout')">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </el-header>
</template>

<script setup lang="ts">
import { ArrowDown, Menu } from '@element-plus/icons-vue';

defineProps<{
  title: string;
  isMobile: boolean;
  userName?: string;
}>();

defineEmits<{
  (e: 'toggle-menu'): void;
  (e: 'logout'): void;
}>();
</script>

<style scoped>
.app-topbar {
  height: var(--header-height);
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-4);
  position: sticky;
  top: 0;
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.menu-button {
  margin-right: var(--space-2);
}

.title-area {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.user-trigger {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-primary);
}
</style>
