<template>
  <div data-testid="login-page" style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#eff6ff,#f8fafc);padding:24px;">
    <el-card style="width:420px;border-radius:20px;" data-testid="login-card">
      <template #header>
        <div>
          <div style="font-size:24px;font-weight:700;">OSE 备考系统</div>
          <div style="color:#64748b;font-size:13px;">单用户软件设计师备考平台</div>
        </div>
      </template>
      <el-alert title="默认账号：admin / OseAdmin@2026" type="info" :closable="false" style="margin-bottom:16px;" />
      <el-form :model="form" label-position="top" data-testid="login-form" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" data-testid="login-username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" data-testid="login-password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" style="width:100%;" data-testid="login-submit" :loading="loading" @click="onSubmit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const form = reactive({ username: 'admin', password: 'OseAdmin@2026' });

const onSubmit = async () => {
  loading.value = true;
  try {
    await authStore.login(form);
    router.push('/dashboard');
  } finally {
    loading.value = false;
  }
};
</script>
