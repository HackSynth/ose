<template>
  <el-dialog
    v-model="visible"
    :title="editingId ? '编辑题目详情' : '录入新题目'"
    :width="isMobile ? '100%' : '800px'"
    :fullscreen="isMobile"
    class="editor-dialog"
    destroy-on-close
  >
    <el-form ref="formRef" label-position="top" :model="form" :rules="rules" scroll-to-error>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <el-form-item label="题型" prop="type" required>
            <el-select v-model="form.type" style="width:100%;">
              <el-option label="上午题 (单选)" value="MORNING_SINGLE" />
              <el-option label="下午题 (案例)" value="AFTERNOON_CASE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="标题" prop="title" required>
            <el-input v-model="form.title" placeholder="简短描述题目内容" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="真题年份">
            <el-input-number v-model="form.year" :min="2010" :max="2030" style="width:100%;" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="题目来源">
            <el-input v-model="form.source" placeholder="如：2024上半年真题" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="建议难度 (1-5)">
            <el-rate v-model="form.difficulty" style="margin-top: 8px;" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="默认分值">
            <el-input-number v-model="form.score" :min="1" :max="50" style="width:100%;" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="完整题干内容" prop="content" required>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="6"
          placeholder="在此输入详细的题目描述、背景资料或考题原文..."
        />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <el-form-item label="自定义标签 (多个用逗号分隔)">
            <el-input v-model="tagText" placeholder="如：重点, 难点, 2024" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="关联知识点 (多选)" prop="knowledgePointIds" required>
            <el-select v-model="form.knowledgePointIds" multiple filterable style="width:100%;" placeholder="搜索知识点">
              <el-option
                v-for="item in knowledgeOptions"
                :key="item.id"
                :label="`[${item.code}] ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <template v-if="form.type === 'MORNING_SINGLE'">
        <div class="options-container">
          <label class="section-label">选项配置</label>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" v-for="option in form.options" :key="option.key">
              <el-form-item :label="`选项 ${option.key}`">
                <el-input v-model="option.content" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="标准答案" prop="correctAnswer" required>
            <el-radio-group v-model="form.correctAnswer">
              <el-radio-button label="A" />
              <el-radio-button label="B" />
              <el-radio-button label="C" />
              <el-radio-button label="D" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="考点解析">
            <el-input
              v-model="form.explanation"
              type="textarea"
              :rows="3"
              placeholder="详细解释该题的解题思路和正确答案原因..."
            />
          </el-form-item>
        </div>
      </template>

      <template v-else>
        <el-form-item label="参考答案及评分要点" prop="referenceAnswer" required>
          <el-input
            v-model="form.referenceAnswer"
            type="textarea"
            :rows="5"
            placeholder="列出详细的评分标准和关键步骤得分点..."
          />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存至题库</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';

const visible = defineModel<boolean>({ required: true });
const tagText = defineModel<string>('tagText', { required: true });

const props = defineProps<{
  isMobile: boolean;
  editingId: number | null;
  form: any;
  knowledgeOptions: any[];
}>();

const emit = defineEmits<{
  (e: 'save'): void;
}>();

const formRef = ref<FormInstance>();

const rules: FormRules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入完整题干内容', trigger: 'blur' }],
  knowledgePointIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个关联知识点', trigger: 'change' }],
  correctAnswer: [{
    validator: (_rule, value, callback) => {
      if (props.form.type !== 'MORNING_SINGLE') {
        callback();
        return;
      }
      if (!value) {
        callback(new Error('请选择标准答案'));
        return;
      }
      const options = props.form.options || [];
      const target = options.find((item: any) => item.key === value);
      if (!target || !String(target.content || '').trim()) {
        callback(new Error('标准答案对应的选项内容不能为空'));
        return;
      }
      callback();
    },
    trigger: 'change',
  }],
  referenceAnswer: [{
    validator: (_rule, value, callback) => {
      if (props.form.type !== 'AFTERNOON_CASE') {
        callback();
        return;
      }
      if (!String(value || '').trim()) {
        callback(new Error('请输入参考答案及评分要点'));
        return;
      }
      callback();
    },
    trigger: 'blur',
  }],
};

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (props.form.type === 'MORNING_SINGLE') {
    const hasEmptyOptions = (props.form.options || []).some((item: any) => !String(item.content || '').trim());
    if (hasEmptyOptions) {
      await formRef.value?.validateField('correctAnswer').catch(() => false);
      return;
    }
  }

  emit('save');
};
</script>

<style scoped>
.section-label {
  display: block;
  font-weight: 700;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-primary);
}

@media (max-width: 767px) {
  .editor-dialog :deep(.el-dialog__body) {
    max-height: calc(100vh - 140px);
    overflow-y: auto;
  }
}
</style>
