<template>
  <div class="login-page" data-testid="login-page">
    <BaseCard class="login-card" data-testid="login-card">
      <template #header>
        <div class="login-header">
          <div class="brand">OSE 备考系统</div>
          <div class="subtitle">单用户软件设计师备考平台</div>
        </div>
      </template>
      
      <el-alert 
        title="默认账号：admin / OseAdmin@2026" 
        type="info" 
        :closable="false" 
        show-icon
        class="login-alert"
      />
      
      <el-form 
        :model="form" 
        label-position="top" 
        class="login-form"
        data-testid="login-form" 
        @submit.prevent="onSubmit"
      >
        <el-form-item label="用户名">
          <el-input 
            v-model="form.username" 
            placeholder="请输入用户名"
            data-testid="login-username" 
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input 
            v-model="form.password" 
            type="password" 
            show-password 
            placeholder="请输入密码"
            data-testid="login-password" 
            prefix-icon="Lock"
          />
        </el-form-item>
        <el-button 
          type="primary" 
          class="login-submit-btn"
          data-testid="login-submit" 
          :loading="loading" 
          @click="onSubmit"
        >
          登录
        </el-button>
      </el-form>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import BaseCard from '@/components/ui/card/BaseCard.vue';

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

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top left, #eff6ff 0%, #ffffff 40%),
              radial-gradient(circle at bottom right, #f8fafc 0%, #ffffff 40%);
  padding: var(--space-6);
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.login-header {
  padding: var(--space-2) 0;
}

.brand {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}

.subtitle {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 4px;
}

.login-alert {
  margin-bottom: var(--space-6);
  border-radius: var(--radius-md);
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  padding-bottom: 4px;
}

.login-submit-btn {
  width: 100%;
  margin-top: var(--space-4);
  height: 44px;
  font-size: 16px;
  font-weight: 600;
}
</style>
