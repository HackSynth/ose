<template>
  <el-container style="min-height:100vh;">
    <el-aside width="220px" style="background:#0f172a;color:#fff;" data-testid="app-sidebar">
      <div style="padding:20px;font-size:20px;font-weight:700;" data-testid="app-logo">OSE 备考系统</div>
      <el-menu
        :default-active="route.path"
        background-color="#0f172a"
        text-color="#cbd5e1"
        active-text-color="#60a5fa"
        router
        data-testid="app-menu"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.path"
          :index="item.path"
          :data-testid="item.testId"
        >
          {{ item.title }}
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;justify-content:space-between;align-items:center;background:#fff;border-bottom:1px solid #e5e7eb;">
        <div>
          <div style="font-size:18px;font-weight:600;" data-testid="page-title">{{ currentTitle }}</div>
          <div style="font-size:12px;color:#6b7280;">单用户软件设计师备考工作台</div>
        </div>
        <div style="display:flex;align-items:center;gap:12px;">
          <el-tag type="info" data-testid="current-user">{{ authStore.user?.displayName }}</el-tag>
          <el-button plain data-testid="logout-button" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main data-testid="app-main">
        <router-view />
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
