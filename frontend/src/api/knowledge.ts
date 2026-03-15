import http from './http';

export const knowledgeApi = {
  tree: () => http.get('/knowledge-points/tree'),
  create: (payload: unknown) => http.post('/knowledge-points', payload),
  update: (id: number, payload: unknown) => http.put(`/knowledge-points/${id}`, payload),
  delete: (id: number) => http.delete(`/knowledge-points/${id}`),
};
