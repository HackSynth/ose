import { ref, type Ref, unref } from 'vue';
import { ElMessage } from 'element-plus';
import type { AxiosError } from 'axios';

export interface UseRequestOptions {
  showError?: boolean;
  showSuccess?: boolean;
  successMessage?: string;
}

export function useRequest<T>(
  requestFn: (...args: unknown[]) => Promise<T>,
  options: UseRequestOptions = {}
) {
  const { showError = true, showSuccess = false, successMessage } = options;

  const loading = ref(false) as Ref<boolean>;
  const error = ref<Error | null>(null) as Ref<Error | null>;
  const data = ref<T | null>(null) as Ref<T | null>;

  const execute = async (...args: unknown[]): Promise<T | null> => {
    loading.value = true;
    error.value = null;

    try {
      const result = await requestFn(...args);
      data.value = result;
      if (showSuccess || successMessage) {
        ElMessage.success(successMessage || '操作成功');
      }
      return result;
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      const message = axiosError.response?.data?.message || axiosError.message || '请求失败';
      error.value = new Error(message);
      if (showError) {
        ElMessage.error(message);
      }
      return null;
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    error,
    data,
    execute,
  };
}
