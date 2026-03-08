import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { api } from '@/api';
import type { AuthPayload, UserProfile } from '@/types';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('ose-token') || '');
  const user = ref<UserProfile | null>(JSON.parse(localStorage.getItem('ose-user') || 'null'));

  const isAuthenticated = computed(() => Boolean(token.value));

  const setSession = (payload: AuthPayload) => {
    token.value = payload.token;
    user.value = payload.user;
    localStorage.setItem('ose-token', payload.token);
    localStorage.setItem('ose-user', JSON.stringify(payload.user));
  };

  const clearSession = () => {
    token.value = '';
    user.value = null;
    localStorage.removeItem('ose-token');
    localStorage.removeItem('ose-user');
  };

  const login = async (payload: { username: string; password: string }) => {
    const result = await api.login(payload) as AuthPayload;
    setSession(result);
    return result;
  };

  const fetchMe = async () => {
    if (!token.value) return null;
    const profile = await api.me() as UserProfile;
    user.value = profile;
    localStorage.setItem('ose-user', JSON.stringify(profile));
    return profile;
  };

  return { token, user, isAuthenticated, login, fetchMe, clearSession, setSession };
});
