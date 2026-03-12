export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface UserProfile {
  id: number;
  username: string;
  displayName: string;
  role: string;
}

export interface AuthPayload {
  token: string;
  user: UserProfile;
}

export type AiConfigMode = 'ENV' | 'DB' | 'HYBRID';
export type AiProviderType = 'OPENAI' | 'ANTHROPIC' | 'OPENAI_COMPATIBLE';
export type AiProviderConfigSource = 'ENV' | 'DB' | 'HYBRID' | 'ENV_FALLBACK' | 'UNAVAILABLE';
export type AiProviderHealthStatus = 'UNKNOWN' | 'SUCCESS' | 'FAILED' | 'UNAVAILABLE';
export type AiBaseUrlMode = 'ROOT' | 'FULL_OVERRIDE';
export type AiModelType = 'CHAT';
export type AiKeyRotationStrategy = 'SEQUENTIAL_ROUND_ROBIN';

export interface AiQuestionProviderOption {
  providerId: string;
  provider: AiProviderType;
  displayName: string;
  configured: boolean;
  statusMessage: string;
  models: AiModelOption[];
}

export interface AiModelOption {
  model: string;
  displayName: string;
  isDefault: boolean;
}

export interface AiProviderApiKeySummary {
  id: string;
  maskedKey: string;
  enabled: boolean;
  sortOrder: number;
  consecutiveFailures: number;
  lastUsedAt: string | null;
  lastFailedAt: string | null;
}

export interface AiProviderModelDetail {
  id: string;
  providerId: string;
  modelId: string;
  displayName: string;
  modelType: AiModelType;
  capabilityTags: string[];
  enabled: boolean;
  defaultForQuestionGeneration: boolean;
  defaultForReviewSummary: boolean;
  defaultForPracticeRecommendation: boolean;
  sortOrder: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AiAdminProviderDetail {
  id: string;
  providerType: AiProviderType;
  displayName: string;
  enabled: boolean;
  apiKeys: AiProviderApiKeySummary[];
  keyRotationStrategy: AiKeyRotationStrategy;
  baseUrl: string | null;
  baseUrlMode: AiBaseUrlMode;
  defaultModel: string | null;
  timeoutMs: number | null;
  maxRetries: number | null;
  temperature: number | null;
  remark: string | null;
  configSource: AiProviderConfigSource;
  healthStatus: AiProviderHealthStatus;
  healthMessage: string | null;
  lastCheckedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  editable: boolean;
  deletable: boolean;
  models: AiProviderModelDetail[];
}

export interface AiDefaultModelSelection {
  providerId: string;
  modelId: string;
}

export interface AiDefaultModelsResponse {
  questionGeneration: AiDefaultModelSelection | null;
  reviewSummary: AiDefaultModelSelection | null;
  practiceRecommendation: AiDefaultModelSelection | null;
}

export interface AiProviderTestResult {
  success: boolean;
  providerId: string;
  providerType: AiProviderType;
  model: string | null;
  latencyMs: number | null;
  message: string;
  configSource: AiProviderConfigSource;
}

export interface AiDiscoverModelsResponse {
  success: boolean;
  message: string;
  models: AiProviderModelDetail[];
}

export interface AiProviderSettingsSummary {
  provider: AiProviderType;
  enabled: boolean;
  configured: boolean;
  maskedKey: string | null;
  storedMaskedKey: string | null;
  baseUrl: string;
  defaultModel: string;
  timeoutMs: number;
  maxRetries: number;
  temperature: number;
  configSource: AiProviderConfigSource;
  healthStatus: AiProviderHealthStatus;
  healthMessage: string | null;
  editable: boolean;
  keyManagedByEnv: boolean;
  hasStoredApiKey: boolean;
}

export interface AiSettingsResponse {
  configMode: AiConfigMode;
  encryptionKeyConfigured: boolean;
  databaseConfigWritable: boolean;
  providers: AiProviderSettingsSummary[];
}

export interface AiProviderConnectionTestResult {
  success: boolean;
  provider: AiProviderType;
  model: string;
  latencyMs: number;
  message: string;
  configSource: AiProviderConfigSource;
}

export interface AiProviderModelListResponse {
  provider: AiProviderType;
  suggestedDefaultModel: string;
  models: AiModelOption[];
}
