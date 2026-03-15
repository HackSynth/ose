import http from './http';

export const authApi = {
  login: (payload: { username: string; password: string }) => http.post('/auth/login', payload),
  me: () => http.get('/auth/me'),
};
