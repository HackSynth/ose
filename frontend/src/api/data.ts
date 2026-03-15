import http from './http';

export const dataApi = {
  exportAll: () => http.get('/export/full'),
  importAllBundle: (payload: unknown, strategy = 'OVERWRITE') => http.post(`/import/full?strategy=${strategy}`, payload),
  importAllFile: (formData: FormData, strategy = 'OVERWRITE') => http.post(`/import/full-file?strategy=${strategy}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
};
