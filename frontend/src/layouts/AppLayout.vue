<template>
  <el-container class="app-container">
    <el-aside class="app-sidebar" :width="sidebarWidth" data-testid="app-sidebar">
      <div class="sidebar-logo" data-testid="app-logo">OSE 备考系统</div>
      <el-menu
        :default-active="route.path"
        class="sidebar-menu"
        router
        data-testid="app-menu"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.path"
          :index="item.path"
          :data-testid="item.testId"
        >
          <template #title>
            <span>{{ item.title }}</span>
          </template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="app-header">
        <div class="header-left">
          <div class="header-title" data-testid="page-title">{{ currentTitle }}</div>
          <div class="header-subtitle">单用户软件设计师备考工作台</div>
        </div>
        <div class="header-right">
          <el-tag effect="plain" round class="user-tag" data-testid="current-user">
            {{ authStore.user?.displayName || '管理员' }}
          </el-tag>
          <el-button link class="logout-btn" data-testid="logout-button" @click="logout">
            退出登录
          </el-button>
        </div>
      </el-header>
      
      <el-main class="app-main" data-testid="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const sidebarWidth = '240px';

const menus = computed(() => router.getRoutes()
  .filter((item) => item.meta?.menu)
  .map((item) => ({
    path: item.path,
    title: item.meta?.title as string,
    testId: `menu-${item.name as string}`,
  })));

const currentTitle = computed(() => (route.meta?.title as string) || 'OSE');

const logout = () => {
  authStore.clearSession();
  router.push('/login');
};
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: var(--bg-app);
}

.app-sidebar {
  background-color: var(--bg-inverse);
  color: var(--text-inverse);
  transition: all 0.3s;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  padding: var(--space-6) var(--space-5);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: #fff;
}

.sidebar-menu {
  background-color: transparent !important;
  border: none;
  flex: 1;
  padding: 0 var(--space-2);
}

.sidebar-menu :deep(.el-menu-item) {
  color: #94a3b8 !important;
  height: 44px;
  line-height: 44px;
  margin-bottom: 4px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: #fff !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: var(--color-primary) !important;
  color: #fff !important;
}

.main-container {
  overflow-x: hidden;
}

.app-header {
  height: var(--header-height);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--bg-surface);
  border-bottom: 1px solid var(--border-light);
  padding: 0 var(--space-6);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.header-subtitle {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.user-tag {
  font-weight: 500;
}

.app-main {
  padding: 0; /* PageContainer in views will handle padding */
  overflow-x: hidden;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
