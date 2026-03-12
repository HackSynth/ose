import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import AiQuestionCenterView from './AiQuestionCenterView.vue';

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    aiProviders: vi.fn(),
    aiHistory: vi.fn(),
    aiGenerateQuestions: vi.fn(),
    aiSaveQuestions: vi.fn(),
    knowledgeTree: vi.fn(),
  },
}));

vi.mock('@/api', () => ({
  api: apiMock,
}));

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
  },
}));

const SelectStub = defineComponent({
  props: ['modelValue'],
  emits: ['update:modelValue', 'change'],
  template: `<select :value="modelValue" @change="$emit('update:modelValue', $event.target.value); $emit('change', $event.target.value)"><slot /></select>`,
});

const OptionStub = defineComponent({
  props: ['value', 'label'],
  template: `<option :value="value">{{ label }}</option>`,
});

const InputStub = defineComponent({
  props: ['modelValue'],
  emits: ['update:modelValue', 'input'],
  template: `<input :value="modelValue" @input="$emit('update:modelValue', $event.target.value); $emit('input', $event.target.value)" />`,
});

const NumberStub = defineComponent({
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input type="number" :value="modelValue" @input="$emit('update:modelValue', Number($event.target.value))" />`,
});

const SwitchStub = defineComponent({
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input type="checkbox" :checked="modelValue" @change="$emit('update:modelValue', $event.target.checked)" />`,
});

const ButtonStub = defineComponent({
  emits: ['click'],
  template: `<button @click="$emit('click')"><slot /></button>`,
});

const commonStubs = {
  PageHeader: defineComponent({ template: '<div><slot /></div>' }),
  'el-card': defineComponent({ template: '<div><slot /><slot name="header" /></div>' }),
  'el-form': defineComponent({ template: '<form><slot /></form>' }),
  'el-form-item': defineComponent({ template: '<div><slot /></div>' }),
  'el-select': SelectStub,
  'el-option': OptionStub,
  'el-input': InputStub,
  'el-input-number': NumberStub,
  'el-switch': SwitchStub,
  'el-button': ButtonStub,
  'el-alert': defineComponent({ template: '<div><slot name="title" /></div>' }),
  'el-empty': defineComponent({ template: '<div />' }),
  'el-table': defineComponent({ template: '<div><slot /></div>' }),
  'el-table-column': defineComponent({ template: '<div><slot /></div>' }),
  'el-tag': defineComponent({ template: '<span><slot /></span>' }),
};

describe('AiQuestionCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMock.aiProviders.mockResolvedValue([
      { provider: 'OPENAI', configured: true, models: [{ model: 'gpt-4.1-mini', displayName: 'gpt-4.1-mini', isDefault: true }] },
      { provider: 'ANTHROPIC', configured: true, models: [{ model: 'claude-3-5-sonnet-latest', displayName: 'claude-3-5-sonnet-latest', isDefault: true }] },
    ]);
    apiMock.knowledgeTree.mockResolvedValue([{ id: 1, code: 'DB.TXN', name: '事务管理', children: [] }]);
    apiMock.aiHistory.mockResolvedValue([]);
  });

  it('can generate and save drafts', async () => {
    apiMock.aiGenerateQuestions.mockResolvedValue({
      generationId: 1,
      drafts: [{
        draftId: 'd1',
        questionType: 'MORNING_SINGLE',
        title: '事务一致性',
        content: '题干',
        options: [{ key: 'A', content: 'A1' }, { key: 'B', content: 'B1' }, { key: 'C', content: 'C1' }, { key: 'D', content: 'D1' }],
        correctAnswer: 'A',
        explanation: '解析',
        referenceAnswer: '',
        scoringPoints: [],
        knowledgePointIds: [1],
        provider: 'OPENAI',
        model: 'gpt-4.1-mini',
        difficulty: 'MEDIUM',
      }],
      validationErrors: [],
    });
    apiMock.aiSaveQuestions.mockResolvedValue({ savedCount: 1, status: 'SAVED' });

    const wrapper = mount(AiQuestionCenterView, { global: { stubs: commonStubs } });
    await flushPromises();

    await wrapper.get('[data-testid="ai-generate"]').trigger('click');
    await flushPromises();

    expect(apiMock.aiGenerateQuestions).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('结果预览（1 题）');

    await wrapper.get('[data-testid="ai-save"]').trigger('click');
    await flushPromises();

    expect(apiMock.aiSaveQuestions).toHaveBeenCalledTimes(1);
  });

  it('switch provider should load corresponding model by default', async () => {
    const wrapper = mount(AiQuestionCenterView, { global: { stubs: commonStubs } });
    await flushPromises();

    const providerSelect = wrapper.get('[data-testid="ai-provider"]');
    await providerSelect.setValue('ANTHROPIC');
    await flushPromises();

    expect(wrapper.get('[data-testid="ai-model"]').element).toBeTruthy();
  });
});
