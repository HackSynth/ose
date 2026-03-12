<template>
  <el-container class="app-layout">
    <el-aside v-if="!isMobile" width="220px" class="desktop-sidebar">
      <AppSidebar />
    </el-aside>

    <MobileMenuDrawer v-model="drawerVisible">
      <AppSidebar @menu-click="drawerVisible = false" />
    </MobileMenuDrawer>

    <el-container class="main-container">
      <AppTopbar
        :title="currentTitle"
        :is-mobile="isMobile"
        :user-name="authStore.user?.displayName"
        @toggle-menu="drawerVisible = true"
        @logout="logout"
      />

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
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useMobile } from '@/composables/useMobile';
import AppSidebar from './AppSidebar.vue';
import AppTopbar from './components/AppTopbar.vue';
import MobileMenuDrawer from './components/MobileMenuDrawer.vue';

const { isMobile } = useMobile();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const drawerVisible = ref(false);
const currentTitle = computed(() => (route.meta?.title as string) || 'OSE');

const logout = () => {
  authStore.clearSession();
  router.push('/login');
};
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  background-color: var(--el-bg-color-page);
}

.desktop-sidebar {
  border-right: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
}

.main-container {
  min-width: 0;
}

.app-main {
  padding: 0;
  overflow-x: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
