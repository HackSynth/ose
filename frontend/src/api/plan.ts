import http from './http';

export const planApi = {
  current: () => http.get('/plans/current'),
  generate: () => http.post('/plans/generate'),
  updateTask: (id: number, payload: unknown) => http.patch(`/tasks/${id}`, payload),
  rebalance: (payload?: unknown) => http.post('/tasks/rebalance', payload ?? {}),
};
