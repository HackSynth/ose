import http from './http';

export const analyticsApi = {
  summary: () => http.get('/analytics/summary'),
  trends: () => http.get('/analytics/trends'),
};
