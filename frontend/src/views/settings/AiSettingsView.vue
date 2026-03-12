<template>
  <div class="page-container" data-testid="ai-settings-page">
    <PageHeader
      title="模型服务"
      description="管理 AI Provider、API Key、模型清单与场景默认模型。"
    />

    <el-alert
      :title="modeTitle"
      :description="modeDescription"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
      data-testid="ai-settings-mode-notice"
    />

    <div class="summary-grid">
      <el-card class="business-card" shadow="never">
        <template #header><span class="card-title">服务概览</span></template>
        <div class="summary-content">
          <div class="summary-item">Provider：{{ providers.length }}</div>
          <div class="summary-item">启用中：{{ enabledProviders }}</div>
          <div class="summary-item">模型总数：{{ totalModels }}</div>
          <div class="summary-item">API Key：{{ totalKeys }}</div>
        </div>
      </el-card>

      <el-card class="business-card" shadow="never">
        <template #header><span class="card-title">默认模型</span></template>
        <div class="default-grid">
          <el-form-item label="AI 出题">
            <div data-testid="ai-default-question-generation">
              <el-select v-model="defaultForms.questionGeneration" clearable filterable style="width: 100%;">
                <el-option
                  v-for="option in defaultModelOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="复盘摘要">
            <div data-testid="ai-default-review-summary">
              <el-select v-model="defaultForms.reviewSummary" clearable filterable style="width: 100%;">
                <el-option
                  v-for="option in defaultModelOptions"
                  :key="`review-${option.value}`"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="推荐练习">
            <div data-testid="ai-default-practice-recommendation">
              <el-select v-model="defaultForms.practiceRecommendation" clearable filterable style="width: 100%;">
                <el-option
                  v-for="option in defaultModelOptions"
                  :key="`practice-${option.value}`"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
          </el-form-item>
        </div>
        <el-button
          type="primary"
          :loading="savingDefaults"
          data-testid="ai-default-save"
          @click="saveDefaultModels"
        >
          保存默认模型
        </el-button>
      </el-card>
    </div>

    <el-card class="business-card create-card" shadow="never">
      <template #header><span class="card-title">新增 Provider</span></template>
      <div class="provider-form-grid">
        <el-form-item label="显示名称">
          <div data-testid="ai-provider-create-name">
            <el-input v-model="createForm.displayName" placeholder="例如 OpenRouter 生产网关" />
          </div>
        </el-form-item>
        <el-form-item label="Provider 类型">
          <div data-testid="ai-provider-create-type">
            <el-select v-model="createForm.providerType" style="width: 100%;">
              <el-option label="OpenAI" value="OPENAI" />
              <el-option label="Anthropic" value="ANTHROPIC" />
              <el-option label="OpenAI-Compatible" value="OPENAI_COMPATIBLE" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="Base URL">
          <div data-testid="ai-provider-create-base-url">
            <el-input v-model="createForm.baseUrl" placeholder="可留空使用默认值" />
          </div>
        </el-form-item>
        <el-form-item label="地址模式">
          <div data-testid="ai-provider-create-base-url-mode">
            <el-select v-model="createForm.baseUrlMode" style="width: 100%;">
              <el-option label="ROOT" value="ROOT" />
              <el-option label="FULL_OVERRIDE" value="FULL_OVERRIDE" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="配置来源">
          <div data-testid="ai-provider-create-config-source">
            <el-select v-model="createForm.configSource" style="width: 100%;">
              <el-option
                v-for="option in configSourceOptions(createForm.providerType)"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="默认启用">
          <div data-testid="ai-provider-create-enabled">
            <el-switch v-model="createForm.enabled" />
          </div>
        </el-form-item>
      </div>
      <el-button
        type="primary"
        :loading="creatingProvider"
        data-testid="ai-provider-create-submit"
        @click="createProvider"
      >
        新增 Provider
      </el-button>
    </el-card>

    <div class="provider-list">
      <el-card
        v-for="provider in providers"
        :key="provider.id"
        class="business-card provider-card"
        shadow="never"
        :data-testid="`ai-provider-card-${provider.id}`"
      >
        <template #header>
          <div class="provider-header">
            <div>
              <div class="card-title">{{ provider.displayName }}</div>
              <div class="card-subtitle">{{ provider.providerType }} / {{ provider.configSource }}</div>
            </div>
            <div class="header-actions">
              <el-tag :type="provider.enabled ? 'success' : 'info'" effect="light">
                {{ provider.enabled ? '已启用' : '未启用' }}
              </el-tag>
              <el-tag :type="healthTagType(provider.healthStatus)" effect="plain">
                {{ provider.healthStatus }}
              </el-tag>
              <el-button
                size="small"
                :loading="testing[provider.id]"
                :data-testid="`ai-provider-test-${provider.id}`"
                @click="testProvider(provider)"
              >
                测试
              </el-button>
              <el-button
                size="small"
                :type="provider.enabled ? 'warning' : 'success'"
                :disabled="!provider.editable"
                :data-testid="`ai-provider-toggle-${provider.id}`"
                @click="toggleProvider(provider)"
              >
                {{ provider.enabled ? '停用' : '启用' }}
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="!provider.deletable"
                :data-testid="`ai-provider-delete-${provider.id}`"
                @click="deleteProvider(provider)"
              >
                删除
              </el-button>
            </div>
          </div>
        </template>

        <el-alert
          v-if="provider.healthMessage || testResults[provider.id]"
          :title="testResults[provider.id]?.success ? '连通性测试通过' : 'Provider 状态'"
          :description="testResults[provider.id]?.message || provider.healthMessage || '未执行测试'"
          :type="testResults[provider.id]?.success ? 'success' : 'info'"
          :closable="false"
          show-icon
          class="provider-alert"
        />

        <div class="provider-form-grid">
          <el-form-item label="显示名称">
            <div :data-testid="`ai-provider-name-${provider.id}`">
              <el-input v-model="providerForms[provider.id].displayName" :disabled="!provider.editable" />
            </div>
          </el-form-item>
          <el-form-item label="Base URL">
            <div :data-testid="`ai-provider-base-url-${provider.id}`">
              <el-input v-model="providerForms[provider.id].baseUrl" :disabled="!provider.editable" />
            </div>
          </el-form-item>
          <el-form-item label="地址模式">
            <div :data-testid="`ai-provider-base-url-mode-${provider.id}`">
              <el-select v-model="providerForms[provider.id].baseUrlMode" :disabled="!provider.editable" style="width: 100%;">
                <el-option label="ROOT" value="ROOT" />
                <el-option label="FULL_OVERRIDE" value="FULL_OVERRIDE" />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="配置来源">
            <div :data-testid="`ai-provider-config-source-${provider.id}`">
              <el-select v-model="providerForms[provider.id].configSource" :disabled="!provider.editable" style="width: 100%;">
                <el-option
                  v-for="option in configSourceOptions(provider.providerType)"
                  :key="`${provider.id}-${option}`"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="默认模型">
            <div :data-testid="`ai-provider-default-model-${provider.id}`">
              <el-select
                v-model="providerForms[provider.id].defaultModel"
                allow-create
                filterable
                default-first-option
                :disabled="!provider.editable"
                style="width: 100%;"
              >
                <el-option
                  v-for="model in provider.models"
                  :key="model.id"
                  :label="model.displayName"
                  :value="model.modelId"
                />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="超时 (ms)">
            <div :data-testid="`ai-provider-timeout-${provider.id}`">
              <el-input-number v-model="providerForms[provider.id].timeoutMs" :min="1000" :max="120000" :disabled="!provider.editable" style="width: 100%;" />
            </div>
          </el-form-item>
          <el-form-item label="最大重试">
            <div :data-testid="`ai-provider-retries-${provider.id}`">
              <el-input-number v-model="providerForms[provider.id].maxRetries" :min="0" :max="10" :disabled="!provider.editable" style="width: 100%;" />
            </div>
          </el-form-item>
          <el-form-item label="温度">
            <div :data-testid="`ai-provider-temperature-${provider.id}`">
              <el-input-number
                v-model="providerForms[provider.id].temperature"
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

        <el-form-item label="备注">
          <div :data-testid="`ai-provider-remark-${provider.id}`">
            <el-input v-model="providerForms[provider.id].remark" type="textarea" :rows="2" :disabled="!provider.editable" />
          </div>
        </el-form-item>

        <div class="section-actions">
          <el-button
            type="primary"
            :loading="savingProvider[provider.id]"
            :disabled="!provider.editable"
            :data-testid="`ai-provider-save-${provider.id}`"
            @click="saveProvider(provider)"
          >
            保存 Provider
          </el-button>
        </div>

        <div class="section-block">
          <div class="section-head">
            <span class="section-title">API Key</span>
          </div>
          <div class="key-list">
            <div
              v-for="key in provider.apiKeys"
              :key="key.id"
              class="key-item"
              :data-testid="`ai-key-item-${provider.id}-${key.id}`"
            >
              <div class="key-meta">
                <strong>{{ key.maskedKey }}</strong>
                <span>排序 {{ key.sortOrder }}</span>
                <span>失败 {{ key.consecutiveFailures }}</span>
              </div>
              <div class="key-actions">
                <el-button
                  size="small"
                  :disabled="!provider.editable"
                  :data-testid="`ai-key-toggle-${provider.id}-${key.id}`"
                  @click="toggleKey(provider.id, key.id, !key.enabled)"
                >
                  {{ key.enabled ? '停用' : '启用' }}
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :disabled="!provider.editable"
                  :data-testid="`ai-key-delete-${provider.id}-${key.id}`"
                  @click="deleteKey(provider.id, key.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="provider.editable" class="inline-form">
            <div :data-testid="`ai-key-input-${provider.id}`">
              <el-input
                v-model="keyForms[provider.id].apiKey"
                type="password"
                show-password
                placeholder="输入新的 API Key"
              />
            </div>
            <el-button
              type="primary"
              :loading="addingKey[provider.id]"
              :data-testid="`ai-key-add-${provider.id}`"
              @click="addKey(provider.id)"
            >
              新增 Key
            </el-button>
          </div>
        </div>

        <div class="section-block">
          <div class="section-head">
            <span class="section-title">模型列表</span>
            <el-button
              size="small"
              :disabled="!provider.editable"
              :loading="discovering[provider.id]"
              :data-testid="`ai-model-discover-${provider.id}`"
              @click="discoverModels(provider)"
            >
              自动发现
            </el-button>
          </div>

          <div class="model-list">
            <div
              v-for="model in provider.models"
              :key="model.id"
              class="model-item"
              :data-testid="`ai-model-item-${provider.id}-${model.id}`"
            >
              <div class="model-meta">
                <strong>{{ model.displayName }}</strong>
                <span>{{ model.modelId }}</span>
                <span>{{ model.enabled ? '启用' : '停用' }}</span>
              </div>
              <div class="model-actions">
                <el-button
                  size="small"
                  :disabled="!provider.editable"
                  :data-testid="`ai-model-toggle-${provider.id}-${model.id}`"
                  @click="toggleModel(provider.id, model)"
                >
                  {{ model.enabled ? '停用' : '启用' }}
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :disabled="!provider.editable"
                  :data-testid="`ai-model-delete-${provider.id}-${model.id}`"
                  @click="deleteModel(provider.id, model.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="provider.editable" class="model-form-grid">
            <div :data-testid="`ai-model-create-id-${provider.id}`">
              <el-input v-model="modelForms[provider.id].modelId" placeholder="模型 ID，例如 gpt-4.1-mini" />
            </div>
            <div :data-testid="`ai-model-create-name-${provider.id}`">
              <el-input v-model="modelForms[provider.id].displayName" placeholder="显示名称" />
            </div>
            <div :data-testid="`ai-model-create-tags-${provider.id}`">
              <el-input v-model="modelForms[provider.id].capabilityTags" placeholder="标签，逗号分隔" />
            </div>
            <el-button
              type="primary"
              :loading="addingModel[provider.id]"
              :data-testid="`ai-model-create-${provider.id}`"
              @click="addModel(provider.id)"
            >
              新增模型
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '@/api';
import PageHeader from '@/components/ui/layout/PageHeader.vue';
import type {
  AiAdminProviderDetail,
  AiBaseUrlMode,
  AiDefaultModelsResponse,
  AiProviderConfigSource,
  AiProviderModelDetail,
  AiProviderTestResult,
  AiProviderType,
} from '@/types';

type ProviderForm = {
  displayName: string;
  baseUrl: string;
  baseUrlMode: AiBaseUrlMode;
  configSource: AiProviderConfigSource;
  defaultModel: string;
  timeoutMs: number;
  maxRetries: number;
  temperature: number;
  remark: string;
};

type KeyForm = {
  apiKey: string;
};

type ModelForm = {
  modelId: string;
  displayName: string;
  capabilityTags: string;
};

const providers = ref<AiAdminProviderDetail[]>([]);
const defaultModels = ref<AiDefaultModelsResponse | null>(null);
const configMode = ref<'ENV' | 'DB' | 'HYBRID'>('HYBRID');

const savingDefaults = ref(false);
const creatingProvider = ref(false);
const testing = reactive<Record<string, boolean>>({});
const savingProvider = reactive<Record<string, boolean>>({});
const addingKey = reactive<Record<string, boolean>>({});
const addingModel = reactive<Record<string, boolean>>({});
const discovering = reactive<Record<string, boolean>>({});
const testResults = reactive<Record<string, AiProviderTestResult | null>>({});

const providerForms = reactive<Record<string, ProviderForm>>({});
const keyForms = reactive<Record<string, KeyForm>>({});
const modelForms = reactive<Record<string, ModelForm>>({});

const defaultForms = reactive({
  questionGeneration: '',
  reviewSummary: '',
  practiceRecommendation: '',
});

const createForm = reactive<{
  displayName: string;
  providerType: AiProviderType;
  baseUrl: string;
  baseUrlMode: AiBaseUrlMode;
  configSource: AiProviderConfigSource;
  enabled: boolean;
}>({
  displayName: '',
  providerType: 'OPENAI',
  baseUrl: '',
  baseUrlMode: 'ROOT',
  configSource: 'HYBRID',
  enabled: true,
});

const enabledProviders = computed(() => providers.value.filter((item) => item.enabled).length);
const totalModels = computed(() => providers.value.reduce((sum, item) => sum + item.models.length, 0));
const totalKeys = computed(() => providers.value.reduce((sum, item) => sum + item.apiKeys.length, 0));

const defaultModelOptions = computed(() => providers.value.flatMap((provider) => provider.models
  .filter((model) => model.enabled)
  .map((model) => ({
    value: serializeSelection(provider.id, model.id),
    label: `${provider.displayName} / ${model.displayName}`,
  }))));

const modeTitle = computed(() => {
  if (configMode.value === 'ENV') return '当前实例处于 ENV 模式';
  if (configMode.value === 'DB') return '当前实例处于 DB 模式';
  return '当前实例处于 HYBRID 模式';
});

const modeDescription = computed(() => {
  if (configMode.value === 'ENV') {
    return '数据库 Provider 页面主要用于查看，真实配置以环境变量为准。';
  }
  if (configMode.value === 'DB') {
    return '当前实例只使用数据库托管的 Provider、Key 与模型。';
  }
  return '数据库 Provider 优先生效，OpenAI / Anthropic 仍可回退到环境变量兜底。';
});

const serializeSelection = (providerId: string, modelId: string) => `${providerId}::${modelId}`;

const splitSelection = (value: string) => {
  if (!value) return null;
  const [providerId, modelId] = value.split('::');
  if (!providerId || !modelId) return null;
  return { providerId, modelId };
};

const configSourceOptions = (providerType: AiProviderType) => {
  if (providerType === 'OPENAI_COMPATIBLE') {
    return ['DB'] as AiProviderConfigSource[];
  }
  return ['DB', 'HYBRID'] as AiProviderConfigSource[];
};

const healthTagType = (status: string) => {
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'UNAVAILABLE') return 'warning';
  return 'info';
};

const syncProviderForms = () => {
  for (const provider of providers.value) {
    providerForms[provider.id] = {
      displayName: provider.displayName,
      baseUrl: provider.baseUrl || '',
      baseUrlMode: provider.baseUrlMode,
      configSource: provider.configSource,
      defaultModel: provider.defaultModel || '',
      timeoutMs: provider.timeoutMs || 30000,
      maxRetries: provider.maxRetries || 0,
      temperature: provider.temperature ?? 0.2,
      remark: provider.remark || '',
    };
    keyForms[provider.id] = { apiKey: '' };
    modelForms[provider.id] = { modelId: '', displayName: '', capabilityTags: '' };
  }
};

const syncDefaultForms = () => {
  defaultForms.questionGeneration = defaultModels.value?.questionGeneration
    ? serializeSelection(defaultModels.value.questionGeneration.providerId, defaultModels.value.questionGeneration.modelId)
    : '';
  defaultForms.reviewSummary = defaultModels.value?.reviewSummary
    ? serializeSelection(defaultModels.value.reviewSummary.providerId, defaultModels.value.reviewSummary.modelId)
    : '';
  defaultForms.practiceRecommendation = defaultModels.value?.practiceRecommendation
    ? serializeSelection(defaultModels.value.practiceRecommendation.providerId, defaultModels.value.practiceRecommendation.modelId)
    : '';
};

const load = async () => {
  const [providerData, defaultData, settingsData] = await Promise.all([
    api.aiAdminProviders(),
    api.aiDefaultModels(),
    api.aiSettings(),
  ]);
  providers.value = providerData;
  defaultModels.value = defaultData;
  configMode.value = settingsData.configMode;
  syncProviderForms();
  syncDefaultForms();
};

const saveDefaultModels = async () => {
  savingDefaults.value = true;
  try {
    defaultModels.value = await api.updateAiDefaultModels({
      questionGeneration: splitSelection(defaultForms.questionGeneration),
      reviewSummary: splitSelection(defaultForms.reviewSummary),
      practiceRecommendation: splitSelection(defaultForms.practiceRecommendation),
    });
    syncDefaultForms();
    ElMessage.success('默认模型已保存');
    await load();
  } finally {
    savingDefaults.value = false;
  }
};

const createProvider = async () => {
  if (!createForm.displayName.trim()) {
    ElMessage.error('请输入 Provider 显示名称');
    return;
  }
  creatingProvider.value = true;
  try {
    await api.createAiProvider({
      displayName: createForm.displayName.trim(),
      providerType: createForm.providerType,
      baseUrl: createForm.baseUrl.trim() || null,
      baseUrlMode: createForm.baseUrlMode,
      enabled: createForm.enabled,
      configSource: createForm.configSource,
    });
    createForm.displayName = '';
    createForm.baseUrl = '';
    ElMessage.success('Provider 已创建');
    await load();
  } finally {
    creatingProvider.value = false;
  }
};

const saveProvider = async (provider: AiAdminProviderDetail) => {
  savingProvider[provider.id] = true;
  try {
    const form = providerForms[provider.id];
    await api.updateAiProvider(provider.id, {
      displayName: form.displayName.trim(),
      baseUrl: form.baseUrl.trim() || null,
      baseUrlMode: form.baseUrlMode,
      defaultModel: form.defaultModel.trim() || null,
      timeoutMs: form.timeoutMs,
      maxRetries: form.maxRetries,
      temperature: form.temperature,
      remark: form.remark.trim() || null,
      configSource: form.configSource,
    });
    ElMessage.success('Provider 已更新');
    await load();
  } finally {
    savingProvider[provider.id] = false;
  }
};

const testProvider = async (provider: AiAdminProviderDetail) => {
  testing[provider.id] = true;
  try {
    testResults[provider.id] = await api.testAiProvider(provider.id);
    ElMessage.success(testResults[provider.id]?.success ? '连通性测试通过' : '连通性测试已返回失败结果');
    await load();
  } finally {
    testing[provider.id] = false;
  }
};

const toggleProvider = async (provider: AiAdminProviderDetail) => {
  if (provider.enabled) {
    await api.disableAiProvider(provider.id);
    ElMessage.success('Provider 已停用');
  } else {
    await api.enableAiProvider(provider.id);
    ElMessage.success('Provider 已启用');
  }
  await load();
};

const deleteProvider = async (provider: AiAdminProviderDetail) => {
  await api.deleteAiProvider(provider.id);
  ElMessage.success('Provider 已删除');
  await load();
};

const addKey = async (providerId: string) => {
  const apiKey = keyForms[providerId].apiKey.trim();
  if (!apiKey) {
    ElMessage.error('请输入 API Key');
    return;
  }
  addingKey[providerId] = true;
  try {
    await api.addAiProviderKey(providerId, { apiKey, enabled: true });
    keyForms[providerId].apiKey = '';
    ElMessage.success('API Key 已新增');
    await load();
  } finally {
    addingKey[providerId] = false;
  }
};

const toggleKey = async (providerId: string, keyId: string, enabled: boolean) => {
  await api.updateAiProviderKey(providerId, keyId, { secretAction: 'KEEP', enabled });
  ElMessage.success(enabled ? 'API Key 已启用' : 'API Key 已停用');
  await load();
};

const deleteKey = async (providerId: string, keyId: string) => {
  await api.deleteAiProviderKey(providerId, keyId);
  ElMessage.success('API Key 已删除');
  await load();
};

const addModel = async (providerId: string) => {
  const form = modelForms[providerId];
  if (!form.modelId.trim() || !form.displayName.trim()) {
    ElMessage.error('请输入模型 ID 和显示名称');
    return;
  }
  addingModel[providerId] = true;
  try {
    await api.createAiProviderModel(providerId, {
      modelId: form.modelId.trim(),
      displayName: form.displayName.trim(),
      capabilityTags: form.capabilityTags.split(',').map((item) => item.trim()).filter(Boolean),
      enabled: true,
    });
    modelForms[providerId] = { modelId: '', displayName: '', capabilityTags: '' };
    ElMessage.success('模型已新增');
    await load();
  } finally {
    addingModel[providerId] = false;
  }
};

const toggleModel = async (providerId: string, model: AiProviderModelDetail) => {
  await api.updateAiProviderModel(providerId, model.id, {
    enabled: !model.enabled,
    modelId: model.modelId,
    displayName: model.displayName,
    modelType: model.modelType,
    capabilityTags: model.capabilityTags,
    sortOrder: model.sortOrder,
  });
  ElMessage.success(!model.enabled ? '模型已启用' : '模型已停用');
  await load();
};

const deleteModel = async (providerId: string, modelId: string) => {
  await api.deleteAiProviderModel(providerId, modelId);
  ElMessage.success('模型已删除');
  await load();
};

const discoverModels = async (provider: AiAdminProviderDetail) => {
  discovering[provider.id] = true;
  try {
    const result = await api.discoverAiProviderModels(provider.id);
    ElMessage.success(result.message);
    await load();
  } finally {
    discovering[provider.id] = false;
  }
};

watch(() => createForm.providerType, (providerType) => {
  const options = configSourceOptions(providerType);
  if (!options.includes(createForm.configSource)) {
    createForm.configSource = options[0];
  }
});

onMounted(load);
</script>

<style scoped>
.page-alert,
.create-card,
.summary-grid,
.provider-list {
  margin-bottom: var(--space-4);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.summary-content,
.default-grid,
.provider-form-grid,
.model-form-grid {
  display: grid;
  gap: var(--space-4);
}

.summary-content {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.default-grid,
.provider-form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.summary-item,
.card-subtitle,
.section-title,
.key-meta span,
.model-meta span {
  color: var(--text-secondary);
}

.provider-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.provider-header,
.section-head,
.key-item,
.model-item {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
}

.provider-header,
.section-head {
  align-items: center;
}

.header-actions,
.key-actions,
.model-actions,
.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.provider-alert,
.section-block {
  margin-top: var(--space-4);
}

.key-list,
.model-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.key-item,
.model-item,
.inline-form,
.model-form-grid {
  padding: var(--space-3);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.7);
}

.inline-form {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  margin-top: var(--space-3);
}

.inline-form > div {
  flex: 1;
}

.model-form-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: var(--space-3);
}

.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

@media (max-width: 1024px) {
  .summary-grid,
  .default-grid,
  .provider-form-grid,
  .model-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .summary-grid,
  .default-grid,
  .provider-form-grid,
  .model-form-grid,
  .summary-content {
    grid-template-columns: 1fr;
  }

  .provider-header,
  .section-head,
  .key-item,
  .model-item,
  .inline-form {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
