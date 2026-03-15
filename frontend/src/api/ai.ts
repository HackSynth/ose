import http from './http';
import type {
  AiAdminProviderDetail,
  AiDefaultModelsResponse,
  AiDiscoverModelsResponse,
  AiProviderConnectionTestResult,
  AiProviderModelListResponse,
  AiProviderModelDetail,
  AiProviderApiKeySummary,
  AiProviderTestResult,
  AiProviderType,
  AiQuestionProviderOption,
  AiSettingsResponse,
} from '@/types';

const AI_REQUEST_TIMEOUT_MS = 90000;

export const aiApi = {
  providers: () => http.get<AiQuestionProviderOption[]>('/ai/provider-options'),
  models: (params?: Record<string, unknown>) => http.get('/ai/models', { params }),
  adminProviders: () => http.get<AiAdminProviderDetail[]>('/ai/providers'),
  createProvider: (payload: unknown) => http.post<AiAdminProviderDetail>('/ai/providers', payload),
  updateProvider: (id: string, payload: unknown) => http.put<AiAdminProviderDetail>(`/ai/providers/${id}`, payload),
  deleteProvider: (id: string) => http.delete(`/ai/providers/${id}`),
  enableProvider: (id: string) => http.post<AiAdminProviderDetail>(`/ai/providers/${id}/enable`),
  disableProvider: (id: string) => http.post<AiAdminProviderDetail>(`/ai/providers/${id}/disable`),
  testProvider: (id: string) => http.post<AiProviderTestResult>(`/ai/providers/${id}/test`, {}, { timeout: AI_REQUEST_TIMEOUT_MS }),
  addProviderKey: (id: string, payload: unknown) => http.post<AiProviderApiKeySummary>(`/ai/providers/${id}/keys`, payload),
  updateProviderKey: (id: string, keyId: string, payload: unknown) => http.put<AiProviderApiKeySummary>(`/ai/providers/${id}/keys/${keyId}`, payload),
  deleteProviderKey: (id: string, keyId: string) => http.delete(`/ai/providers/${id}/keys/${keyId}`),
  createProviderModel: (id: string, payload: unknown) => http.post<AiProviderModelDetail>(`/ai/providers/${id}/models`, payload),
  updateProviderModel: (id: string, modelId: string, payload: unknown) => http.put<AiProviderModelDetail>(`/ai/providers/${id}/models/${modelId}`, payload),
  deleteProviderModel: (id: string, modelId: string) => http.delete(`/ai/providers/${id}/models/${modelId}`),
  discoverProviderModels: (id: string) => http.post<AiDiscoverModelsResponse>(`/ai/providers/${id}/models/discover`),
  defaultModels: () => http.get<AiDefaultModelsResponse>('/ai/default-models'),
  updateDefaultModels: (payload: unknown) => http.put<AiDefaultModelsResponse>('/ai/default-models', payload),
  settings: () => http.get<AiSettingsResponse>('/ai/settings'),
  updateSettings: (provider: AiProviderType, payload: unknown) => http.put(`/ai/settings/${provider}`, payload),
  testSettings: (provider: AiProviderType, payload?: unknown) => http.post<AiProviderConnectionTestResult>(
    `/ai/settings/${provider}/test`,
    payload ?? {},
    { timeout: AI_REQUEST_TIMEOUT_MS },
  ),
  settingsModels: (provider: AiProviderType) => http.get<AiProviderModelListResponse>(`/ai/settings/${provider}/models`),
  generateQuestions: (payload: unknown) => http.post('/ai/questions/generate', payload, { timeout: AI_REQUEST_TIMEOUT_MS }),
  saveQuestions: (payload: unknown) => http.post('/ai/questions/save', payload),
  history: () => http.get('/ai/history'),
  health: () => http.get('/ai/health'),
};
