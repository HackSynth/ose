<template>
  <div class="page-container" data-testid="ai-settings-page">
    <PageHeader
      title="AI 配置"
      description="统一管理 OpenAI 与 Claude 的 API Key、默认模型、超时、重试与连通性测试。"
    />

    <el-alert
      v-if="globalNotice"
      :title="globalNotice.title"
      :type="globalNotice.type"
      :description="globalNotice.description"
      :closable="false"
      show-icon
      class="page-alert"
      data-testid="ai-settings-global-notice"
    />

    <div class="provider-grid">
      <el-card
        v-for="provider in providers"
        :key="provider.provider"
        class="business-card provider-card"
        shadow="never"
        :data-testid="`ai-settings-card-${provider.provider}`"
      >
        <template #header>
          <div class="card-header">
            <div>
              <div class="card-title">{{ providerLabel(provider.provider) }}</div>
              <div class="card-subtitle">
                当前来源：
                <span :data-testid="`ai-settings-source-${provider.provider}`">{{ provider.configSource }}</span>
              </div>
            </div>
            <div class="tag-group">
              <el-tag :type="provider.configured ? 'success' : 'info'" effect="light">
                {{ provider.configured ? '已配置' : '未配置' }}
              </el-tag>
              <el-tag :type="healthTagType(provider.healthStatus)" effect="plain">
                {{ healthLabel(provider.healthStatus) }}
              </el-tag>
            </div>
          </div>
        </template>

        <div class="status-grid">
          <div>
            <div class="status-label">当前生效 Key</div>
            <div class="status-value" :data-testid="`ai-settings-mask-${provider.provider}`">
              {{ provider.maskedKey || '未配置' }}
            </div>
          </div>
          <div>
            <div class="status-label">页面托管 Key</div>
            <div class="status-value">
              {{ provider.storedMaskedKey || '未托管' }}
            </div>
          </div>
          <div>
            <div class="status-label">健康摘要</div>
            <div class="status-value">
              {{ provider.healthMessage || healthLabel(provider.healthStatus) }}
            </div>
          </div>
        </div>

        <el-alert
          v-if="providerNotice(provider)"
          :title="providerNotice(provider)?.title"
          :description="providerNotice(provider)?.description"
          type="warning"
          :closable="false"
          show-icon
          class="provider-notice"
          :data-testid="`ai-settings-env-notice-${provider.provider}`"
        />

        <el-form label-position="top" class="provider-form">
          <div class="form-grid">
            <el-form-item label="启用数据库托管">
              <div :data-testid="`ai-settings-enabled-${provider.provider}`">
                <el-switch
                  v-model="forms[provider.provider].enabled"
                  :disabled="!provider.editable"
                  active-text="已启用"
                  inactive-text="未启用"
                />
              </div>
            </el-form-item>

            <el-form-item label="API Key">
              <div :data-testid="`ai-settings-key-${provider.provider}`">
                <el-input
                  v-model="forms[provider.provider].apiKey"
                  type="password"
                  show-password
                  :disabled="!provider.editable"
                  :placeholder="provider.hasStoredApiKey ? '留空表示保留当前 Key' : '输入新的 API Key'"
                />
              </div>
              <div class="field-hint">
                {{ keyHint(provider) }}
              </div>
            </el-form-item>

            <el-form-item label="Base URL">
              <div :data-testid="`ai-settings-base-url-${provider.provider}`">
                <el-input v-model="forms[provider.provider].baseUrl" :disabled="!provider.editable" />
              </div>
            </el-form-item>

            <el-form-item label="默认模型">
              <div :data-testid="`ai-settings-model-${provider.provider}`">
                <el-select
                  v-model="forms[provider.provider].defaultModel"
                  filterable
                  allow-create
                  default-first-option
                  :disabled="!provider.editable"
                  style="width: 100%;"
                >
                  <el-option
                    v-for="model in modelOptions[provider.provider] || []"
                    :key="model.model"
                    :label="model.displayName"
                    :value="model.model"
                  />
                </el-select>
              </div>
            </el-form-item>

            <el-form-item label="请求超时 (ms)">
              <div :data-testid="`ai-settings-timeout-${provider.provider}`">
                <el-input-number
                  v-model="forms[provider.provider].timeoutMs"
                  :min="1000"
                  :max="120000"
                  :disabled="!provider.editable"
                  style="width: 100%;"
                />
              </div>
            </el-form-item>

            <el-form-item label="最大重试次数">
              <div :data-testid="`ai-settings-retries-${provider.provider}`">
                <el-input-number
                  v-model="forms[provider.provider].maxRetries"
                  :min="0"
                  :max="10"
                  :disabled="!provider.editable"
                  style="width: 100%;"
                />
              </div>
            </el-form-item>

            <el-form-item label="温度">
              <div :data-testid="`ai-settings-temperature-${provider.provider}`">
                <el-input-number
                  v-model="forms[provider.provider].temperature"
                  :min="0"
                  :max="2"
                  :step="0.1"
                  :precision="1"
                  :disabled="!provider.editable"
                  style="width: 100%;"
                />
              </div>
            </el-form-item>
          </div>

          <div
            v-if="testResults[provider.provider]"
            class="test-result"
            :data-testid="`ai-settings-test-result-${provider.provider}`"
          >
            <el-alert
              :title="testResults[provider.provider]?.success ? '连通性测试通过' : '连通性测试失败'"
              :description="testResultText(testResults[provider.provider])"
              :type="testResults[provider.provider]?.success ? 'success' : 'error'"
              :closable="false"
              show-icon
            />
          </div>

          <div class="action-row">
            <el-button
              :loading="testing[provider.provider]"
              :data-testid="`ai-settings-test-${provider.provider}`"
              @click="testProvider(provider)"
            >
              测试连接
            </el-button>
            <el-button
              type="primary"
              :loading="saving[provider.provider]"
              :disabled="!provider.editable"
              :data-testid="`ai-settings-save-${provider.provider}`"
              @click="saveProvider(provider)"
            >
              保存配置
            </el-button>
            <el-button
              type="danger"
              plain
              :disabled="!provider.editable || !provider.hasStoredApiKey"
              :data-testid="`ai-settings-clear-${provider.provider}`"
              @click="clearProviderKey(provider)"
            >
              清空 Key
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import type {
  AiProviderConnectionTestResult,
  AiProviderHealthStatus,
  AiProviderSettingsSummary,
  AiProviderType,
  AiSettingsResponse,
  AiModelOption,
} from '@/types';

type ProviderForm = {
  enabled: boolean;
  apiKey: string;
  baseUrl: string;
  defaultModel: string;
  timeoutMs: number;
  maxRetries: number;
  temperature: number;
};

const settings = ref<AiSettingsResponse | null>(null);
const saving = reactive<Record<AiProviderType, boolean>>({ OPENAI: false, ANTHROPIC: false });
const testing = reactive<Record<AiProviderType, boolean>>({ OPENAI: false, ANTHROPIC: false });
const forms = reactive<Record<AiProviderType, ProviderForm>>({
  OPENAI: {
    enabled: false,
    apiKey: '',
    baseUrl: '',
    defaultModel: '',
    timeoutMs: 30000,
    maxRetries: 1,
    temperature: 0.2,
  },
  ANTHROPIC: {
    enabled: false,
    apiKey: '',
    baseUrl: '',
    defaultModel: '',
    timeoutMs: 30000,
    maxRetries: 1,
    temperature: 0.2,
  },
});
const modelOptions = reactive<Record<AiProviderType, AiModelOption[]>>({ OPENAI: [], ANTHROPIC: [] });
const testResults = reactive<Partial<Record<AiProviderType, AiProviderConnectionTestResult>>>({});

const providers = computed(() => settings.value?.providers || []);

const globalNotice = computed(() => {
  if (!settings.value) return null;
  if (settings.value.configMode === 'ENV') {
    return {
      title: '环境变量只读模式',
      type: 'warning' as const,
      description: '当前配置来自环境变量；如需在页面中托管密钥，请配置数据库加密主密钥并切换到数据库模式',
    };
  }
  if (!settings.value.encryptionKeyConfigured) {
    return {
      title: '未配置数据库加密主密钥',
      type: 'warning' as const,
      description: '当前实例可以读取环境变量与现有数据库摘要，但不能安全托管新的 API Key。',
    };
  }
  return {
    title: '配置优先级',
    type: 'info' as const,
    description: '当前采用统一配置解析器：数据库启用配置优先，环境变量兜底，不可用时优雅降级。',
  };
});

const providerLabel = (provider: AiProviderType) => (provider === 'OPENAI' ? 'OpenAI 配置' : 'Anthropic / Claude 配置');

const healthLabel = (status: AiProviderHealthStatus) => {
  switch (status) {
    case 'SUCCESS':
      return '最近测试通过';
    case 'FAILED':
      return '最近测试失败';
    case 'UNAVAILABLE':
      return '当前不可用';
    default:
      return '尚未测试';
  }
};

const healthTagType = (status: AiProviderHealthStatus) => {
  switch (status) {
    case 'SUCCESS':
      return 'success';
    case 'FAILED':
    case 'UNAVAILABLE':
      return 'danger';
    default:
      return 'info';
  }
};

const providerNotice = (provider: AiProviderSettingsSummary) => {
  if (settings.value?.configMode === 'ENV') {
    return {
      title: '当前实例通过环境变量托管密钥',
      description: '页面中只能查看摘要与做连通性测试，保存与清空操作已禁用。',
    };
  }
  if (provider.keyManagedByEnv && !settings.value?.encryptionKeyConfigured) {
    return {
      title: '当前配置来自环境变量',
      description: '如需改为页面托管密钥，请先配置 AI_SECRET_ENCRYPTION_KEY。',
    };
  }
  if (provider.keyManagedByEnv && provider.configSource !== 'DB') {
    return {
      title: '当前配置来自环境变量',
      description: '保存后将切换为数据库托管配置，数据库配置会优先生效。',
    };
  }
  return null;
};

const keyHint = (provider: AiProviderSettingsSummary) => {
  if (provider.hasStoredApiKey) {
    return '留空表示保留当前数据库密钥；输入新值会覆盖原值。';
  }
  return '未输入新 Key 时不会写入明文，只有保存时才会加密托管。';
};

const toPayload = (provider: AiProviderSettingsSummary) => {
  const form = forms[provider.provider];
  const payload: Record<string, unknown> = {
    enabled: form.enabled,
    clearApiKey: false,
    baseUrl: form.baseUrl,
    defaultModel: form.defaultModel,
    timeoutMs: form.timeoutMs,
    maxRetries: form.maxRetries,
    temperature: form.temperature,
  };
  if (form.apiKey.trim()) {
    payload.apiKey = form.apiKey.trim();
  }
  return payload;
};

const validateProvider = (provider: AiProviderSettingsSummary) => {
  const form = forms[provider.provider];
  if (form.enabled && !form.apiKey.trim() && !provider.hasStoredApiKey) {
    return '启用 Provider 时必须输入 API Key，或保留已有数据库密钥';
  }
  if (!form.defaultModel.trim()) {
    return '请填写默认模型';
  }
  if (!form.baseUrl.trim()) {
    return '请填写 Base URL';
  }
  return null;
};

const applyForms = () => {
  providers.value.forEach((provider) => {
    forms[provider.provider] = {
      enabled: provider.enabled,
      apiKey: '',
      baseUrl: provider.baseUrl,
      defaultModel: provider.defaultModel,
      timeoutMs: provider.timeoutMs,
      maxRetries: provider.maxRetries,
      temperature: provider.temperature,
    };
    delete testResults[provider.provider];
  });
};

const load = async () => {
  settings.value = await api.aiSettings();
  applyForms();
  await Promise.all((settings.value.providers || []).map(async (provider) => {
    const response = await api.aiSettingsModels(provider.provider);
    modelOptions[provider.provider] = response.models || [];
  }));
};

const saveProvider = async (provider: AiProviderSettingsSummary) => {
  const validationMessage = validateProvider(provider);
  if (validationMessage) {
    ElMessage.error(validationMessage);
    return;
  }
  saving[provider.provider] = true;
  try {
    await api.updateAiSettings(provider.provider, toPayload(provider));
    ElMessage.success('配置已保存');
    await load();
  } finally {
    saving[provider.provider] = false;
  }
};

const clearProviderKey = async (provider: AiProviderSettingsSummary) => {
  saving[provider.provider] = true;
  try {
    await api.updateAiSettings(provider.provider, {
      enabled: false,
      clearApiKey: true,
      baseUrl: forms[provider.provider].baseUrl,
      defaultModel: forms[provider.provider].defaultModel,
      timeoutMs: forms[provider.provider].timeoutMs,
      maxRetries: forms[provider.provider].maxRetries,
      temperature: forms[provider.provider].temperature,
    });
    ElMessage.success('当前 Key 已清空');
    await load();
  } finally {
    saving[provider.provider] = false;
  }
};

const testProvider = async (provider: AiProviderSettingsSummary) => {
  const validationMessage = provider.editable ? validateProvider(provider) : null;
  if (validationMessage) {
    ElMessage.error(validationMessage);
    return;
  }
  testing[provider.provider] = true;
  try {
    testResults[provider.provider] = await api.testAiSettings(provider.provider, toPayload(provider));
  } finally {
    testing[provider.provider] = false;
  }
};

const testResultText = (result?: AiProviderConnectionTestResult) => {
  if (!result) return '';
  const latency = result.latencyMs != null ? ` · ${result.latencyMs} ms` : '';
  return `${result.provider} / ${result.model}${latency} · ${result.message}`;
};

onMounted(load);
</script>

<style scoped>
.page-alert {
  margin-bottom: var(--space-4);
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: var(--space-4);
}

.provider-card {
  min-height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  align-items: flex-start;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.card-subtitle {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.tag-group {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  justify-content: flex-end;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.status-label {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-bottom: 4px;
}

.status-value {
  color: var(--text-primary);
  font-weight: 600;
  word-break: break-word;
}

.provider-notice {
  margin-bottom: var(--space-4);
}

.provider-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.field-hint {
  margin-top: 6px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.test-result {
  margin-top: var(--space-2);
}

.action-row {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-2);
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .status-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
