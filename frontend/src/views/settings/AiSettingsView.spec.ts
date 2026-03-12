import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AiSettingsView from './AiSettingsView.vue';

const { apiMock, messageSuccess, messageError } = vi.hoisted(() => ({
  apiMock: {
    aiSettings: vi.fn(),
    aiSettingsModels: vi.fn(),
    updateAiSettings: vi.fn(),
    testAiSettings: vi.fn(),
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
  'el-form': defineComponent({ template: '<form><slot /></form>' }),
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

const settingsPayload = {
  configMode: 'HYBRID',
  encryptionKeyConfigured: true,
  databaseConfigWritable: true,
  providers: [
    {
      provider: 'OPENAI',
      enabled: true,
      configured: true,
      maskedKey: 'sk-***1234',
      storedMaskedKey: 'sk-***1234',
      baseUrl: 'https://api.openai.com',
      defaultModel: 'gpt-4.1-mini',
      timeoutMs: 10000,
      maxRetries: 2,
      temperature: 0.4,
      configSource: 'DB',
      healthStatus: 'SUCCESS',
      healthMessage: '最近测试通过',
      editable: true,
      keyManagedByEnv: false,
      hasStoredApiKey: true,
    },
    {
      provider: 'ANTHROPIC',
      enabled: false,
      configured: false,
      maskedKey: null,
      storedMaskedKey: null,
      baseUrl: 'https://api.anthropic.com',
      defaultModel: 'claude-3-5-sonnet-latest',
      timeoutMs: 12000,
      maxRetries: 1,
      temperature: 0.2,
      configSource: 'UNAVAILABLE',
      healthStatus: 'UNAVAILABLE',
      healthMessage: '未配置',
      editable: true,
      keyManagedByEnv: false,
      hasStoredApiKey: false,
    },
  ],
};

describe('AiSettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMock.aiSettings.mockResolvedValue(settingsPayload);
    apiMock.aiSettingsModels.mockImplementation(async (provider: string) => ({
      provider,
      suggestedDefaultModel: provider === 'OPENAI' ? 'gpt-4.1-mini' : 'claude-3-5-sonnet-latest',
      models: [
        {
          model: provider === 'OPENAI' ? 'gpt-4.1-mini' : 'claude-3-5-sonnet-latest',
          displayName: provider === 'OPENAI' ? 'gpt-4.1-mini' : 'claude-3-5-sonnet-latest',
          isDefault: true,
        },
      ],
    }));
    apiMock.updateAiSettings.mockResolvedValue(settingsPayload.providers[0]);
    apiMock.testAiSettings.mockResolvedValue({
      success: true,
      provider: 'OPENAI',
      model: 'gpt-4.1-mini',
      latencyMs: 120,
      message: 'OpenAI 连通性测试通过',
      configSource: 'DB',
    });
  });

  it('renders provider cards with masked status', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    expect(apiMock.aiSettings).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('OpenAI 配置');
    expect(wrapper.get('[data-testid="ai-settings-mask-OPENAI"]').text()).toContain('sk-***1234');
    expect(wrapper.get('[data-testid="ai-settings-source-OPENAI"]').text()).toContain('DB');
  });

  it('saves provider config with new key', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-settings-key-OPENAI"] input').setValue('sk-new-9876');
    await wrapper.get('[data-testid="ai-settings-save-OPENAI"]').trigger('click');
    await flushPromises();

    expect(apiMock.updateAiSettings).toHaveBeenCalledWith('OPENAI', expect.objectContaining({
      enabled: true,
      apiKey: 'sk-new-9876',
      clearApiKey: false,
    }));
    expect(messageSuccess).toHaveBeenCalledWith('配置已保存');
  });

  it('keeps existing key when user does not modify key', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-settings-save-OPENAI"]').trigger('click');
    await flushPromises();

    expect(apiMock.updateAiSettings).toHaveBeenCalledWith('OPENAI', expect.not.objectContaining({ apiKey: expect.anything() }));
  });

  it('clears stored key explicitly', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-settings-clear-OPENAI"]').trigger('click');
    await flushPromises();

    expect(apiMock.updateAiSettings).toHaveBeenCalledWith('OPENAI', expect.objectContaining({
      enabled: false,
      clearApiKey: true,
    }));
    expect(messageSuccess).toHaveBeenCalledWith('当前 Key 已清空');
  });

  it('shows connectivity test result after probing provider', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-settings-test-OPENAI"]').trigger('click');
    await flushPromises();

    expect(apiMock.testAiSettings).toHaveBeenCalledWith('OPENAI', expect.objectContaining({
      enabled: true,
      clearApiKey: false,
    }));
    expect(wrapper.get('[data-testid="ai-settings-test-result-OPENAI"]').text()).toContain('OpenAI 连通性测试通过');
  });

  it('validates empty key when enabling new provider', async () => {
    const wrapper = mount(AiSettingsView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-settings-enabled-ANTHROPIC"] input').setValue(true);
    await wrapper.get('[data-testid="ai-settings-save-ANTHROPIC"]').trigger('click');
    await flushPromises();

    expect(apiMock.updateAiSettings).not.toHaveBeenCalled();
    expect(messageError).toHaveBeenCalledWith('启用 Provider 时必须输入 API Key，或保留已有数据库密钥');
  });
});
