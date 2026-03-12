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
export type AiProviderType = 'OPENAI' | 'ANTHROPIC';
export type AiProviderConfigSource = 'ENV' | 'DB' | 'ENV_FALLBACK' | 'UNAVAILABLE';
export type AiProviderHealthStatus = 'UNKNOWN' | 'SUCCESS' | 'FAILED' | 'UNAVAILABLE';

export interface AiModelOption {
  model: string;
  displayName: string;
  isDefault: boolean;
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
