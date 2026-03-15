import http from './http';

export const examApi = {
  list: () => http.get('/exams'),
  attempts: () => http.get('/exam-attempts'),
  create: (payload: unknown) => http.post('/exams', payload),
  start: (id: number) => http.post(`/exams/${id}/attempts`),
  getAttempt: (id: number) => http.get(`/exam-attempts/${id}`),
  submit: (id: number, payload: unknown) => http.post(`/exam-attempts/${id}/submit`, payload),
  scoreAfternoon: (id: number, payload: unknown) => http.post(`/exam-attempts/${id}/score-afternoon`, payload),
};
