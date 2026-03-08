import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: '/dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '仪表盘', menu: true } },
      { path: '/plans', name: 'plans', component: () => import('@/views/plans/PlanView.vue'), meta: { title: '学习计划', menu: true } },
      { path: '/knowledge', name: 'knowledge', component: () => import('@/views/knowledge/KnowledgeView.vue'), meta: { title: '知识体系', menu: true } },
      { path: '/questions', name: 'questions', component: () => import('@/views/questions/QuestionBankView.vue'), meta: { title: '题库管理', menu: true } },
      { path: '/practice', name: 'practice', component: () => import('@/views/practice/PracticeView.vue'), meta: { title: '练习系统', menu: true } },
      { path: '/mistakes', name: 'mistakes', component: () => import('@/views/mistakes/MistakeView.vue'), meta: { title: '错题复习', menu: true } },
      { path: '/exams', name: 'exams', component: () => import('@/views/exams/ExamView.vue'), meta: { title: '模拟考试', menu: true } },
      { path: '/notes', name: 'notes', component: () => import('@/views/notes/NoteView.vue'), meta: { title: '学习笔记', menu: true } },
      { path: '/analytics', name: 'analytics', component: () => import('@/views/analytics/AnalyticsView.vue'), meta: { title: '统计分析', menu: true } },
      { path: '/settings', name: 'settings', component: () => import('@/views/settings/SettingsView.vue'), meta: { title: '系统设置', menu: true } },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if (to.meta.public) {
    if (authStore.isAuthenticated && to.path === '/login') {
      return '/dashboard';
    }
    return true;
  }
  if (!authStore.isAuthenticated) {
    return '/login';
  }
  if (!authStore.user) {
    try {
      await authStore.fetchMe();
    } catch {
      authStore.clearSession();
      return '/login';
    }
  }
  return true;
});

export default router;
