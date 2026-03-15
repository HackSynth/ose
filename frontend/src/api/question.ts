import http from './http';

export const questionApi = {
  list: (params?: Record<string, unknown>) => http.get('/questions', { params }),
  create: (payload: unknown) => http.post('/questions', payload),
  update: (id: number, payload: unknown) => http.put(`/questions/${id}`, payload),
  delete: (id: number) => http.delete(`/questions/${id}`),
  import: (formData: FormData) => http.post('/questions/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  templates: (format: string) => http.get(`/questions/templates/${format}`),
};
