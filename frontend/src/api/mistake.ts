import http from './http';

export const mistakeApi = {
  list: (params?: Record<string, unknown>) => http.get('/mistakes', { params }),
  update: (id: number, payload: unknown) => http.patch(`/mistakes/${id}`, payload),
};
