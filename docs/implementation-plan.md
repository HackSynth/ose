# OSE 实施计划

## 当前状态
- OSE MVP 已完成：认证、仪表盘、计划、知识体系、题库、练习、错题、模拟、笔记、统计、导出、健康检查均已交付。
- 本轮进入“质量增强 + 实用性增强”阶段，目标是在不重做既有能力的前提下补齐端到端验证、整包导入、备考体验增强，并同步完善文档与交付说明。
- 前端 UI 重构已启动：第一阶段（layout + 题库页）已完成，后续阶段继续推进到通用表单、表格页与详情弹窗的一致性收敛。

## UI 重构阶段 1（已完成）
- 统一主布局：桌面端固定侧边栏、移动端抽屉菜单、统一顶部栏交互。
- 题库页组件化：拆分筛选区、表格区、移动端卡片区、编辑弹窗，页面容器只保留数据请求与业务动作。
- 响应式落地：题库页移动端不再压缩表格，改为卡片列表展示与操作按钮。
- 样式收口：补齐全局变量映射到 Element Plus token，减少页面内散落的临时色值依赖风险。

## UI 重构阶段 2（已完成）
- 新增通用页面结构组件：`PageSection`（统一区块卡片）、`PageActionGroup`（统一操作区按钮布局）。
- 全局样式补充统一规范：`business-card`、`form-action-bar`、`dialog-footer` 等基础规则集中到 `base.css`。
- 页面接入范围：
  - 视图：`settings`、`plans`、`knowledge`、`mistakes`、`notes`、`ai question center`
  - 业务组件：`dashboard/*`、`exam/ExamCreateCard`、`exam/ExamListCard`、`exam/ExamHistoryCard`、`practice/PracticeFormCard`
- 题库子组件：筛选区、表格区、移动卡片区统一切换到通用区块组件
- 结果：减少页面内重复卡片头部/按钮区/表单布局样式，提升后续页面改造复用效率。

## UI 重构阶段 3（已完成）
- 统一关键编辑弹窗的表单提交流程：保存前统一执行 `formRef.validate()`，拦截无效提交。
- 新增弹窗表单规则覆盖：
  - 题库题目编辑：题型/标题/题干/知识点必填，按题型校验答案字段。
  - 知识点编辑：编码、名称、层级必填。
  - 错题编辑：错因、复习状态、下次复习日期必填。
  - 笔记编辑：标题、内容必填；关联项存在时必须选择目标对象。
- 移动端交互统一：上述弹窗在小屏统一 `fullscreen`，内容区支持滚动，保证长表单可操作性。
- 结果：编辑类流程的输入质量与移动端可用性显著提升，且不改变原有业务接口与数据流。

## UI 重构阶段 4（已完成）
- 新增通用表单栅格组件：`components/ui/form/PageFormGrid.vue`，统一表单项多列布局与移动端单列收敛。
- 页面接入：
  - `practice/PracticeFormCard`、`exam/ExamCreateCard`、`knowledge/KnowledgeView`、`settings/SettingsView`
  - `ai/AiQuestionCenterView`、`settings/AiSettingsView`
- 页面结构收敛：
  - `AnalyticsView` 图表区统一改为 `PageSection`，并将图表配色收敛到 Element Plus 默认色板（`#409EFF/#67C23A/#E6A23C/#F56C6C/#909399`）。
  - `PracticeView` 会话区统一改为 `PageSection + PageActionGroup`，移动端操作区自动纵向重排。
- 结果：进一步减少页面级重复 `form-grid` 样式和散落卡片结构，提升全站样式一致性与后续维护效率。

## UI 重构阶段 5（已完成）
- 新增通用移动端列表组件：`components/ui/data/MobileCardList.vue`，统一表格页在小屏的卡片化展示。
- 页面/组件接入：
  - `plans/PlanView`（任务表格 -> 移动卡片列表）
  - `mistakes/MistakeView`（错题表格 -> 移动卡片列表）
  - `notes/NoteView`（笔记表格 -> 移动卡片列表）
  - `exam/ExamListCard`、`exam/ExamHistoryCard`（模拟卷与历史记录表格 -> 移动卡片列表）
  - `ai/AiQuestionCenterView`（生成历史表格 -> 移动卡片列表）
- 结果：桌面端保留 `el-table` 信息密度，移动端改为可读可点的卡片结构，降低横向滚动与文本挤压风险。

## UI 重构阶段 6（已完成）
- 详情作答卡片收敛：
  - `practice/PracticeQuestionCard`、`exam/ExamAttemptCard` 统一接入 `PageActionGroup` 操作区布局。
  - 增补移动端样式：标题区、操作区、题干区和结果区在小屏下自动重排，避免按钮拥挤与文本换行错位。
- 视觉风格收口：
  - `dashboard/CompletionCard` 进度条颜色切换到 Element Plus 默认 token（danger/warning/success/primary）。
  - `auth/LoginView` 去除装饰性渐变背景，改为 Element Plus 页面底色，视觉与后台页保持一致。
- 结果：详情页和作答页在移动端交互更稳定，且全站视觉更贴近 Element Plus 默认浅蓝风格。

## AI 出题专项（本轮新增）
### 阶段 A：最小可用版本（已完成）
- 新增统一 Provider 抽象层：`AiProviderClient`、`OpenAiProviderClient`、`AnthropicProviderClient`
- 新增 AI 出题 API：`/api/ai/questions/generate`、`/api/ai/questions/save`、`/api/ai/providers`、`/api/ai/models`、`/api/ai/history`、`/api/ai/health`
- 新增前端“AI 出题中心”页面，支持 provider/model 切换、参数配置、结果预览编辑、保存入库/丢弃

### 阶段 B：结构化输出与校验（已完成）
- Prompt 层强制 JSON 输出，区分上午题/下午题 schema
- 服务端执行 schema 校验 + 业务校验（字段完整性、选项合法性、重复性）
- 低质量题/缺失题/重复题拦截后不可保存

### 阶段 C：可观测与降级（已完成）
- 新增 `ai_generation_records` 表，记录参数、provider/model、状态、错误、prompt hash 与结果
- API Key 缺失时优雅降级：显示未配置状态，不影响主系统功能
- 错误信息对齐：拒答、超时、配额不足、格式错误均返回可理解提示

### 阶段 D：测试补齐（已完成）
- 后端单测：Prompt Builder、Schema/业务校验、Provider 路由、错误处理
- 后端集成测试：Mock OpenAI / Mock Anthropic，覆盖生成 -> 预览 -> 保存链路
- 前端测试：AI 出题页面交互、provider/model 切换、预览保存
- E2E：AI 页面生成上午题、保存题库、查看历史（mock AI 接口，不依赖真实 Key）

### 阶段 E：AI Provider 配置中心（已完成）
- 新增前端 `AI 配置` 页面，支持 OpenAI / Anthropic 两张配置卡片、掩码展示、保存、清空、连通性测试
- 新增后端配置中心基础设施：数据库表、加密服务、统一配置解析器、健康探测 API、`ENV / DB / HYBRID` 优先级策略
- 已完成测试：后端加解密/保存/优先级/接口测试，前端页面交互测试，以及“配置 -> 测试 -> AI 出题”E2E 链路

### 阶段 F：模型服务中心重构（已完成）
- 已完成 Cherry Studio 风格 Provider + Model 后端骨架，但按 OSE 场景裁剪成“模型服务”而非聊天工作台
- 已新增 Provider / API Key / Model / Default Models 四类核心表与对应服务层
- 已支持 OpenAI、Anthropic、OpenAI-Compatible、ROOT / FULL_OVERRIDE、默认模型解析、多 Key 顺序轮询、模型发现与健康检查
- 已将 AI 出题链路切换到新解析器，支持通过 `providerId + model` 显式指定，未指定时自动回退默认模型
- 当前状态：前后端已切换到新的“模型服务”后台页；AI 出题页已改为使用 `providerId + model`，旧 `/api/ai/settings*` 兼容接口仅保留给回退场景

## 里程碑 1：E2E 测试基础设施
- 目标：补齐 Playwright 基础设施、稳定数据准备、可维护选择器与 Docker 运行入口。
- 涉及文件/目录：`frontend/package.json`、`frontend/playwright.config.ts`、`frontend/tests/e2e/`、`frontend/src/views/**`、`frontend/src/layouts/**`、`docker-compose.yml`
- 完成标准：
  - 已引入 Playwright 并提供本地与 Compose 两种运行方式
  - 核心页面已补充稳定的 `data-testid`
  - E2E 支持通过 API 预置稳定测试数据，避免依赖脆弱 UI 前置步骤
- 验证命令：
  - `cd frontend && npm run test:e2e:install`
  - `cd frontend && npm run test:e2e`
  - `docker compose --profile tools run --rm e2e-test`

## 里程碑 2：核心备考链路端到端验证
- 目标：为 10 条关键链路补齐可回归的浏览器级用例。
- 涉及文件/目录：`frontend/tests/e2e/auth.spec.ts`、`frontend/tests/e2e/core-flow.spec.ts`、`frontend/tests/e2e/helpers.ts`
- 完成标准：
  - 覆盖管理员登录
  - 覆盖设置考试日期并保存
  - 覆盖自动生成学习计划
  - 覆盖浏览知识点
  - 覆盖上午题自动判分
  - 覆盖下午题提交自评
  - 覆盖错题自动入库
  - 覆盖创建并提交模拟考试
  - 覆盖统计页数据展示
  - 覆盖整包导出
- 验证命令：
  - `docker compose up -d --build`
  - `docker compose --profile tools run --rm e2e-test`

## 里程碑 3：整包导入能力扩展
- 目标：在现有整包导出基础上补齐题库、知识点、笔记、学习计划的整包导入。
- 涉及文件/目录：`backend/src/main/java/com/ose/data/**`、`backend/src/main/resources/templates/**`、`backend/src/test/java/com/ose/data/**`、`frontend/src/views/settings/**`、`frontend/src/api/**`、`examples/import/`
- 完成标准：
  - 支持 JSON 整包导入
  - 支持模板下载与文件上传导入
  - 支持 `OVERWRITE` / `SKIP` 两类重复数据处理策略
  - 提供字段校验、分区导入结果、错误报告与警告信息
  - 至少可导入设置、知识点、题库、笔记、当前学习计划
- 验证命令：
  - `docker compose run --rm backend-test`
  - `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/import/template`

## 里程碑 4：备考体验增强
- 目标：围绕学习连续性与复盘效率，增强计划调度和推荐能力。
- 涉及文件/目录：`backend/src/main/java/com/ose/settings/**`、`backend/src/main/java/com/ose/dashboard/**`、`backend/src/main/java/com/ose/exam/**`、`frontend/src/views/dashboard/**`、`frontend/src/views/practice/**`、`frontend/src/views/exams/**`、`frontend/src/views/settings/**`
- 完成标准：
  - 保存考试日期后自动重排当前计划
  - 仪表盘展示错题复习提醒
  - 仪表盘展示薄弱知识点练习推荐，并可一键跳转练习页
  - 模考记录展示系统生成的复盘摘要
- 验证命令：
  - `docker compose run --rm backend-test`
  - `cd frontend && npm run build`

## 里程碑 5：文档与交付说明更新
- 目标：同步 README、交付报告和增强阶段说明，确保后续可复用。
- 涉及文件/目录：`README.md`、`docs/delivery-report.md`、`docs/implementation-plan.md`
- 完成标准：
  - README 增补 E2E、本地运行、整包导入、体验增强说明
  - 交付报告反映本轮增强范围、验证结果和已知限制
  - 文档与当前代码一致
- 验证命令：
  - `rg "Playwright|整包导入|复习提醒|薄弱知识点|复盘摘要" README.md docs/delivery-report.md docs/implementation-plan.md`

## 里程碑 6：最终集成验证
- 目标：完成本轮增强的回归验证并修复失败。
- 涉及文件/目录：全仓库
- 完成标准：
  - 前端单测通过
  - 前端构建通过
  - 后端测试通过
  - Compose 配置校验通过
  - Compose E2E 通过
  - `docker compose up -d --build` 后前后端与数据库均正常
- 验证命令：
  - `cd frontend && npm run test -- --run`
  - `cd frontend && npm run build`
  - `docker compose run --rm backend-test`
  - `docker compose config`
  - `docker compose --profile tools run --rm e2e-test`
  - `docker compose ps`
