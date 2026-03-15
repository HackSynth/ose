import http from './http';

export const settingsApi = {
  get: () => http.get('/settings'),
  update: (payload: unknown) => http.put('/settings', payload),
};
