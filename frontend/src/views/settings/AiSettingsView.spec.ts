import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AiSettingsView from './AiSettingsView.vue';

const { apiMock, messageSuccess, messageError } = vi.hoisted(() => ({
  apiMock: {
    aiAdminProviders: vi.fn(),
    aiDefaultModels: vi.fn(),
    aiSettings: vi.fn(),
    updateAiDefaultModels: vi.fn(),
    createAiProvider: vi.fn(),
    updateAiProvider: vi.fn(),
    deleteAiProvider: vi.fn(),
    enableAiProvider: vi.fn(),
    disableAiProvider: vi.fn(),
    testAiProvider: vi.fn(),
    addAiProviderKey: vi.fn(),
    updateAiProviderKey: vi.fn(),
    deleteAiProviderKey: vi.fn(),
    createAiProviderModel: vi.fn(),
    updateAiProviderModel: vi.fn(),
    deleteAiProviderModel: vi.fn(),
    discoverAiProviderModels: vi.fn(),
  },
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}));

vi.mock('@/api', () => ({
  api: apiMock,
}));

vi.mock('element-plus', () => ({
  ElMessage: {
    success: messageSuccess,
    error: messageError,
  },
}));

const SelectStub = defineComponent({
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue'],
  template: `
    <select :value="modelValue" :disabled="disabled" @change="$emit('update:modelValue', $event.target.value)">
      <slot />
    </select>
  `,
});

const OptionStub = defineComponent({
  props: ['value', 'label'],
  template: '<option :value="value">{{ label }}</option>',
});

const InputStub = defineComponent({
  props: ['modelValue', 'disabled', 'type', 'placeholder'],
  emits: ['update:modelValue'],
  template: `
    <input
      :value="modelValue"
      :disabled="disabled"
      :type="type || 'text'"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
});

const NumberStub = defineComponent({
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue'],
  template: `
    <input
      type="number"
      :value="modelValue"
      :disabled="disabled"
      @input="$emit('update:modelValue', Number($event.target.value))"
    />
  `,
});

const SwitchStub = defineComponent({
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue'],
  template: `
    <input
      type="checkbox"
      :checked="modelValue"
      :disabled="disabled"
      @change="$emit('update:modelValue', $event.target.checked)"
    />
  `,
});

const ButtonStub = defineComponent({
  props: ['disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
});

const commonStubs = {
  PageHeader: defineComponent({ template: '<div><slot /></div>' }),
  'el-card': defineComponent({ template: '<div><slot name="header" /><slot /></div>' }),
  'el-form-item': defineComponent({ template: '<div><slot /></div>' }),
  'el-input': InputStub,
  'el-input-number': NumberStub,
  'el-switch': SwitchStub,
  'el-select': SelectStub,
  'el-option': OptionStub,
  'el-button': ButtonStub,
  'el-tag': defineComponent({ template: '<span><slot /></span>' }),
  'el-alert': defineComponent({
    props: ['title', 'description'],
    template: '<div>{{ title }} {{ description }}<slot /></div>',
  }),
};

const providerPayload = [
  {
    id: 'env-openai',
    providerType: 'OPENAI',
    displayName: 'OpenAI（ENV）',
    enabled: true,
    apiKeys: [{ id: 'key-1', maskedKey: 'sk-***1234', enabled: true, sortOrder: 0, consecutiveFailures: 0, lastUsedAt: null, lastFailedAt: null }],
    keyRotationStrategy: 'SEQUENTIAL_ROUND_ROBIN',
    baseUrl: 'https://api.openai.com',
    baseUrlMode: 'ROOT',
    defaultModel: 'gpt-4.1-mini',
    timeoutMs: 10000,
    maxRetries: 2,
    temperature: 0.3,
    remark: 'env',
    configSource: 'ENV',
    healthStatus: 'SUCCESS',
    healthMessage: '可用',
    lastCheckedAt: null,
    createdAt: null,
    updatedAt: null,
    editable: false,
    deletable: false,
    models: [
      {
        id: 'model-1',
        providerId: 'env-openai',
        modelId: 'gpt-4.1-mini',
        displayName: 'gpt-4.1-mini',
        modelType: 'CHAT',
        capabilityTags: ['env'],
        enabled: true,
        defaultForQuestionGeneration: true,
        defaultForReviewSummary: false,
        defaultForPracticeRecommendation: false,
        sortOrder: 0,
        createdAt: null,
        updatedAt: null,
      },
    ],
  },
  {
    id: 'db-provider',
    providerType: 'OPENAI_COMPATIBLE',
    displayName: 'OpenRouter',
    enabled: true,
    apiKeys: [],
    keyRotationStrategy: 'SEQUENTIAL_ROUND_ROBIN',
    baseUrl: 'https://openrouter.ai/api/v1',
    baseUrlMode: 'ROOT',
    defaultModel: 'openai/gpt-4.1-mini',
    timeoutMs: 15000,
    maxRetries: 1,
    temperature: 0.2,
    remark: null,
    configSource: 'DB',
    healthStatus: 'UNKNOWN',
    healthMessage: '未测试',
    lastCheckedAt: null,
    createdAt: null,
    updatedAt: null,
    editable: true,
    deletable: true,
    models: [
      {
        id: 'model-2',
        providerId: 'db-provider',
        modelId: 'openai/gpt-4.1-mini',
        displayName: 'GPT 4.1 Mini',
        modelType: 'CHAT',
        capabilityTags: ['reasoning'],
        enabled: true,
        defaultForQuestionGeneration: false,
        defaultForReviewSummary: true,
        defaultForPracticeRecommendation: true,
        sortOrder: 0,
        createdAt: null,
        updatedAt: null,
      },
    ],
  },
];

describe('AiSettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMock.aiAdminProviders.mockResolvedValue(providerPayload);
    apiMock.aiDefaultModels.mockResolvedValue({
      questionGeneration: { providerId: 'env-openai', modelId: 'model-1' },
      reviewSummary: { providerId: 'db-provider', modelId: 'model-2' },
      practiceRecommendation: { providerId: 'db-provider', modelId: 'model-2' },
    });
    apiMock.aiSettings.mockResolvedValue({ configMode: 'HYBRID' });
    apiMock.updateAiDefaultModels.mockResolvedValue({
      questionGeneration: { providerId: 'db-provider', modelId: 'model-2' },
      reviewSummary: { providerId: 'db-provider', modelId: 'model-2' },
      practiceRecommendation: { providerId: 'db-provider', modelId: 'model-2' },
    });
    apiMock.createAiProvider.mockResolvedValue(providerPayload[1]);
    apiMock.updateAiProvider.mockResolvedValue(providerPayload[1]);
    apiMock.testAiProvider.mockResolvedValue({
      success: true,
      providerId: 'db-provider',
      providerType: 'OPENAI_COMPATIBLE',
      model: 'openai/gpt-4.1-mini',
      latencyMs: 120,
      message: '测试通过',
      configSource: 'DB',
    });
    apiMock.addAiProviderKey.mockResolvedValue({});
    apiMock.createAiProviderModel.mockResolvedValue({});
    apiMock.discoverAiProviderModels.mockResolvedValue({ success: true, message: '已同步 3 个模型', models: [] });
  });

  it('renders provider cards and summary', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    expect(apiMock.aiAdminProviders).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('服务概览');
    expect(wrapper.text()).toContain('OpenRouter');
    expect(wrapper.get('[data-testid="ai-settings-mode-notice"]').text()).toContain('HYBRID');
  });

  it('creates provider with current form values', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-provider-create-name"] input').setValue('Moonshot');
    await wrapper.get('[data-testid="ai-provider-create-type"] select').setValue('OPENAI_COMPATIBLE');
    await wrapper.get('[data-testid="ai-provider-create-submit"]').trigger('click');
    await flushPromises();

    expect(apiMock.createAiProvider).toHaveBeenCalledWith(expect.objectContaining({
      displayName: 'Moonshot',
      providerType: 'OPENAI_COMPATIBLE',
    }));
    expect(messageSuccess).toHaveBeenCalledWith('Provider 已创建');
  });

  it('adds api key for editable provider', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-key-input-db-provider"] input').setValue('sk-live-123');
    await wrapper.get('[data-testid="ai-key-add-db-provider"]').trigger('click');
    await flushPromises();

    expect(apiMock.addAiProviderKey).toHaveBeenCalledWith('db-provider', {
      apiKey: 'sk-live-123',
      enabled: true,
    });
    expect(messageSuccess).toHaveBeenCalledWith('API Key 已新增');
  });

  it('saves default model selections', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-default-question-generation"] select').setValue('db-provider::model-2');
    await wrapper.get('[data-testid="ai-default-save"]').trigger('click');
    await flushPromises();

    expect(apiMock.updateAiDefaultModels).toHaveBeenCalledWith(expect.objectContaining({
      questionGeneration: { providerId: 'db-provider', modelId: 'model-2' },
    }));
    expect(messageSuccess).toHaveBeenCalledWith('默认模型已保存');
  });
});
