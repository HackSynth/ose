import http from './http';

export const practiceApi = {
  createSession: (payload: unknown) => http.post('/practice/sessions', payload),
  getSession: (id: number) => http.get(`/practice/sessions/${id}`),
  submit: (id: number, payload: unknown) => http.post(`/practice/sessions/${id}/submit`, payload),
  reviewRecord: (id: number, payload: unknown) => http.post(`/practice/records/${id}/review`, payload),
  updateFlags: (id: number, payload: unknown) => http.patch(`/practice/records/${id}/flags`, payload),
};
