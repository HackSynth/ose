# 前端 UI 重构验收清单（Element Plus）

## 1. 视觉一致性
- [x] 主要色板采用 Element Plus 默认语义色（主色/成功/警告/危险/信息）。
- [x] 页面背景、边框、文本颜色优先使用 Element Plus token 或项目变量映射。
- [x] 卡片、按钮、标签、表单输入具备统一过渡反馈，不出现突兀样式跳变。

## 2. 布局与结构
- [x] 页面统一使用 `PageHeader + PageSection + PageActionGroup` 结构范式。
- [x] 表单区域优先使用 `PageFormGrid`，避免页面内重复 `form-grid` 样式。
- [x] 桌面端与移动端布局规则明确（`split-layout` 在小屏自动单列）。

## 3. 页面状态反馈
- [x] 关键页面具备加载态（Skeleton）。
- [x] 关键页面具备错误态（Result + Retry）。
- [x] 关键页面具备空态（Empty）。
- [x] 页面状态逻辑通过 `usePageState` 统一管理，避免重复实现。

## 4. 移动端可用性
- [x] 宽表格页面在小屏提供卡片替代（`MobileCardList`）。
- [x] 编辑弹窗在小屏采用 `fullscreen`，正文可滚动。
- [x] 操作按钮在小屏支持换行或纵向重排，不出现点击区域过小问题。

## 5. 表单与交互
- [x] 编辑类弹窗保存前统一执行 `formRef.validate()`。
- [x] 必填项配置 `prop + rules`，避免无校验提交。
- [x] 操作按钮区（保存/取消/主要动作）布局统一。

## 6. 可维护性
- [x] 页面级通用能力组件化（`PageSection`/`PageFormGrid`/`MobileCardList`/`PageStateBlock`）。
- [x] 业务逻辑、接口结构与数据流保持不变（本轮仅 UI/结构重构）。
- [x] 文档已同步（`README`、`architecture`、`implementation-plan`、`delivery-report`）。

## 7. 验证命令
- [x] `cd frontend && npm run test -- --run`
- [x] `cd frontend && npm run build`
- [ ] `cd frontend && npm run lint`（当前仓库无 `lint` script）
