import http from './http';

export const noteApi = {
  list: (params?: Record<string, unknown>) => http.get('/notes', { params }),
  create: (payload: unknown) => http.post('/notes', payload),
  update: (id: number, payload: unknown) => http.put(`/notes/${id}`, payload),
  delete: (id: number) => http.delete(`/notes/${id}`),
};
