import { ref } from 'vue';

export function usePageState() {
  const loading = ref(false);
  const errorMessage = ref('');

  const runWithState = async <T>(
    task: () => Promise<T>,
    options?: { fallback?: T; defaultErrorMessage?: string },
  ): Promise<T | null> => {
    loading.value = true;
    errorMessage.value = '';
    try {
      return await task();
    } catch (error: any) {
      errorMessage.value = error?.message || options?.defaultErrorMessage || '请稍后重试';
      return options?.fallback ?? null;
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    errorMessage,
    runWithState,
  };
}
