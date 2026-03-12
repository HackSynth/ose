# OSE 交付报告

## 1. 本次完成范围
本轮在既有 OSE MVP 基础上完成了“质量增强 + 实用性增强”交付，包括：
- 补齐 Playwright 端到端测试基础设施与核心链路用例
- 为关键页面补充稳定的 `data-testid` 选择器
- 提供 Docker Compose 与本地两种 E2E 运行入口
- 扩展整包导入能力，支持设置、知识点、题库、笔记、学习计划的批量导入
- 提供整包导入模板、错误报告与重复数据处理策略
- 增加错题复习提醒、薄弱知识点推荐练习、考试日期变更后自动重排计划、模考复盘摘要
- 同步更新 README、实施计划与交付说明
- 启动 Cherry Studio 风格“模型服务中心”重构，完成 Provider + Model 后端服务层与 AI 出题链路切换
- 完成前端“模型服务”管理后台接入，支持 Provider、API Key、模型和默认模型的统一管理
- 启动前端 UI 重构第一阶段，已完成主布局统一与题库页组件化拆分，并补齐题库页移动端展示方案
- 完成前端 UI 重构第二阶段，新增通用 Page 结构组件并在多页面/多业务组件接入，统一表单、按钮、卡片样式规范
- 完成前端 UI 重构第三阶段，统一关键编辑弹窗校验规则与移动端全屏交互策略
- 完成前端 UI 重构第四阶段，新增通用响应式表单栅格组件并推进统计/练习/模型服务页面结构收敛
- 完成前端 UI 重构第五阶段，新增通用移动端卡片列表组件并完成多表格页小屏替代展示
- 完成前端 UI 重构第六阶段，统一练习/模考详情作答卡片交互并收口残留视觉风格
- 完成前端 UI 重构第七阶段，新增统一页面状态反馈组件并接入仪表盘/统计/计划页面

## 2. 关键设计决策
- E2E 采用 Playwright，优先覆盖真实浏览器交互链路，而非仅靠接口级测试。
- 通过 API 预置稳定测试数据，避免在 E2E 中重复依赖大量前置 UI 操作，提高回归稳定性。
- 前端统一补充 `data-testid`，避免选择器依赖样式类名或脆弱文案结构。
- 整包导入沿用现有导出结构，降低模板理解成本；同时提供模板下载与示例文件，提升可用性。
- 导入冲突策略仅保留 `OVERWRITE` 与 `SKIP` 两种，保持规则清晰、易于维护。
- 计划自动重排放在设置保存时触发，避免用户修改考试日期后还需额外执行同步动作。
- 薄弱知识点推荐与模考摘要使用已有练习、错题、模考数据做轻量聚合，不引入外部 AI 依赖。

## 3. 新增 / 修改文件概览
### 根目录
- `docker-compose.yml`
- `README.md`

### 文档
- `docs/implementation-plan.md`
- `docs/delivery-report.md`

### 前端
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/playwright.config.ts`
- `frontend/vitest.config.ts`
- `frontend/src/api/index.ts`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/views/auth/LoginView.vue`
- `frontend/src/views/settings/SettingsView.vue`
- `frontend/src/views/settings/AiSettingsView.vue`
- `frontend/src/views/plans/PlanView.vue`
- `frontend/src/views/knowledge/KnowledgeView.vue`
- `frontend/src/views/practice/PracticeView.vue`
- `frontend/src/views/mistakes/MistakeView.vue`
- `frontend/src/views/exams/ExamView.vue`
- `frontend/src/views/analytics/AnalyticsView.vue`
- `frontend/src/views/auth/LoginView.vue`
- `frontend/src/views/ai/AiQuestionCenterView.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/plans/PlanView.vue`
- `frontend/src/components/business/practice/PracticeQuestionCard.vue`
- `frontend/src/components/business/exam/ExamAttemptCard.vue`
- `frontend/src/components/business/dashboard/CompletionCard.vue`
- `frontend/src/components/ui/form/PageFormGrid.vue`
- `frontend/src/components/ui/data/MobileCardList.vue`
- `frontend/src/components/ui/feedback/PageStateBlock.vue`
- `frontend/tests/e2e/auth.spec.ts`
- `frontend/tests/e2e/core-flow.spec.ts`
- `frontend/tests/e2e/helpers.ts`

### 后端
- `backend/src/main/java/com/ose/data/DataController.java`
- `backend/src/main/java/com/ose/data/DataImportDtos.java`
- `backend/src/main/java/com/ose/data/DataImportService.java`
- `backend/src/main/java/com/ose/settings/SettingController.java`
- `backend/src/main/java/com/ose/dashboard/DashboardDtos.java`
- `backend/src/main/java/com/ose/dashboard/DashboardService.java`
- `backend/src/main/java/com/ose/exam/ExamDtos.java`
- `backend/src/main/java/com/ose/exam/ExamService.java`
- `backend/src/main/java/com/ose/repository/KnowledgePointRepository.java`
- `backend/src/main/java/com/ose/repository/NoteRepository.java`
- `backend/src/main/java/com/ose/repository/QuestionRepository.java`
- `backend/src/test/java/com/ose/data/DataImportServiceTest.java`

### 模板与示例
- `backend/src/main/resources/templates/full-import-template.json`
- `examples/import/full-import-template.json`

## 4. 如何启动
```bash
cp .env.example .env
docker compose up -d --build
```
访问：
- 前端：`http://localhost`
- 后端健康检查：`http://localhost/healthz`
- 后端 API：`http://localhost:8080/api`

## 5. 如何测试
### 后端
```bash
docker compose run --rm backend-test
```

### 前端单测 / 构建
```bash
cd frontend
npm install
npm run test -- --run
npm run build
```

### Playwright E2E（推荐）
```bash
docker compose up -d --build
docker compose --profile tools run --rm e2e-test
```

### Playwright E2E（本地）
```bash
cd frontend
npm install
npm run test:e2e:install
PLAYWRIGHT_BASE_URL=http://127.0.0.1 npm run test:e2e
```

## 6. 如何部署
### 本地 / 单机部署
- 使用 Docker Compose 直接部署
- MySQL 数据持久化在 Compose volume 中
- 前端由 Nginx 托管并代理后端 API
- Compose 中额外提供 `backend-test`、`frontend-test`、`e2e-test` 工具服务，便于回归验证

### 基础 CI
- 保留现有 GitHub Actions 基础流程
- 可在后续将 `frontend/tests/e2e` 纳入 CI 的独立 job

## 7. 初始化账号与示例数据说明
- 默认账号：`admin`
- 默认密码：`OseAdmin@2026`
- 启动后自动写入：知识点树、示例题、计划、练习与错题、模拟考试、示例笔记
- 所有示例内容均为自拟，贴合软件设计师备考场景
- E2E 会基于上述基础数据，再通过 API 写入一组稳定的测试设置与学习计划

## 8. 已知限制
- 当前整包导入以“基础维护类数据”为主，不导入练习历史、错题历史与模考历史。
- 本地 Playwright 运行依赖宿主机浏览器环境；在沙箱或受限容器环境下建议优先使用 Compose 版 E2E。
- 薄弱知识点推荐和模考摘要基于规则聚合，不是 AI 诊断模型。
- 自动重排计划当前采用“按最新设置重新生成当前计划”的策略，尚未保留复杂的历史人工微调差异。

## 9. 下一步建议
- 为整包导入增加“预检模式”和更细粒度的字段级冲突报告。
- 补充 Playwright Trace / Report 归档到 CI，形成可追溯的回归报告。
- 将错题复习提醒扩展为“今日复习清单”与计划任务联动。
- 为薄弱知识点推荐增加更多维度，如最近趋势、主观题得分率、错因分布。
- 为模考复盘摘要提供可编辑模板和复盘标签分类。

## 10. AI 出题模块交付说明
### 10.1 完成内容
- 新增后端 `ai` 模块（Provider 抽象、Prompt 构建、结构化输出校验、生成/保存/历史/健康接口）。
- 新增前端“AI 出题中心”页面并加入主导航，支持 provider/model、题型、场景、难度、数量、语言、风格配置。
- 支持两类模式：临时练习集（仅预览不入库）与保存到题库（可先编辑后批量保存）。
- 保存后的题目自动标记 `AI 生成`，记录 `provider/model` 与来源字段。
- 新增 `ai_generation_records` 表记录生成参数、状态、错误信息、prompt hash、结果摘要。

### 10.2 接入方式与取舍
- OpenAI：使用 Responses API（HTTP）+ JSON Schema 结构化输出。
- Anthropic：使用 Messages API（HTTP）+ schema 约束 JSON 输出。
- SDK 取舍：本轮使用 Spring `RestClient` 统一 HTTP 调用，优先保证错误映射、代理兼容和依赖轻量；后续可替换为官方 Java SDK。

### 10.3 降级与安全
- 未配置 `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` 时，`/api/ai/providers` 显示未配置状态，生成接口返回明确错误提示。
- AI 服务不可用不会影响主系统其他功能。
- 不落库敏感 Key，仅保存 provider/model、请求摘要、状态与错误。

### 10.4 测试说明
- 后端单元测试：Prompt Builder、Schema/业务校验、Provider 路由、错误处理。
- 后端集成测试：Mock OpenAI、Mock Anthropic，覆盖生成 -> 保存 -> 历史链路。
- 前端单元测试：AI 出题页表单交互、生成预览、保存动作。
- E2E：新增 AI 页面用例，使用 mock 路由，不依赖真实 API Key。

### 10.5 已知限制与扩展方向
- 计费/Token 精确统计字段已预留，当前未对接精确账单回传。
- Anthropic 结构化输出当前采用 schema 指令约束，后续可升级到更强约束能力。
- 后续可扩展：AI 解析、AI 讲题、AI 学习建议、AI 个性化复盘。

## 11. AI Provider 配置中心阶段进展
### 11.1 本阶段已完成
- 新增 `ai_provider_settings` 表，支持数据库托管 Provider 配置。
- 新增 `AiSecretCryptoService`，使用 `AI_SECRET_ENCRYPTION_KEY` 对数据库中的 API Key 做对称加密。
- 新增 `AiProviderConfigurationResolver`，统一解析 `ENV / DB / HYBRID` 三种配置模式，并输出 `DB / ENV / ENV_FALLBACK / UNAVAILABLE` 来源。
- 新增 `/api/ai/settings*` 接口，支持摘要读取、保存更新、清空 Key、连通性测试、模型建议列表。
- 现有 OpenAI / Anthropic 出题客户端已改为通过统一解析器读取配置，不再在业务代码中分散读取环境变量。
- 新增前端 `AI 配置` 页面，支持每个 Provider 独立保存、清空 Key、查看掩码状态与运行连通性测试。

### 11.2 本阶段验证
- `docker compose run --rm backend-test`
- `cd frontend && npm test -- --run`
- `cd frontend && npm run build`
- `cd frontend && PLAYWRIGHT_EXECUTABLE_PATH=/usr/bin/chromium PLAYWRIGHT_LAUNCH_ARGS='--no-sandbox' PLAYWRIGHT_BASE_URL=http://127.0.0.1 npm run test:e2e -- tests/e2e/ai-settings.spec.ts`
- 覆盖加密/解密、Key 掩码、保存读取、clearApiKey、优先级解析、Provider 测试接口。
- 覆盖 AI 配置页渲染、保存配置、不修改 Key 保留原值、清空 Key、连通性测试与页面到 AI 出题页的闭环。

### 11.3 当前已知限制
- Compose 版 `e2e-test` 首次运行需要拉取 Playwright 镜像，首次准备时间较长；本轮已用本机 `chromium` 完成等价浏览器验证。

## 12. 模型服务中心后端阶段交付
### 12.1 本阶段已完成
- 新增 `ai_providers`、`ai_provider_api_keys`、`ai_models`、`ai_default_model_settings` 四张核心表，并提供旧 `ai_provider_settings` 到新结构的迁移脚本。
- 新增 `AiProviderService`、`AiProviderHealthService`、`AiProviderConfigurationResolver`、`AiApiKeyRotationService`、`AiModelRegistryService`、`AiDefaultModelService`。
- 新增 `/api/ai/providers*`、`/api/ai/default-models` API，支持 Provider CRUD、启停、连通性测试、模型管理、模型发现、默认模型设置。
- 新增 `OpenAiCompatibleProviderClient`，将 OpenAI / Anthropic / OpenAI-Compatible 三类 Provider 统一纳入新的解析与调度体系。
- 新增 `AiProviderUrlBuilder`，统一处理 `ROOT / FULL_OVERRIDE` 两种 Base URL 模式。
- AI 出题链路已改为通过 `providerId + model` 工作，同时保留默认模型回退与兼容接口。
- 前端原“AI 配置”页已切换为新的“模型服务”管理后台，并修复 AI 出题页 Provider 选项接口，避免误调用后台管理接口。

### 12.2 为什么参考 Cherry Studio
- Cherry Studio 在 Provider / Model 分层、Base URL 兼容、多 Key 轮询和默认模型思想上验证过可用性，适合复用其“服务治理”部分。
- OSE 不需要桌面聊天客户端的消息管理与插件生态，因此本次只保留对 Web 管理后台有价值的配置治理能力，并把默认模型映射到“AI 出题 / AI 复盘 / AI 学习辅助”场景。

### 12.3 本阶段验证
- `docker compose run --rm backend-test`
- 已覆盖 Provider CRUD、多 Key 轮询、Base URL ROOT / FULL_OVERRIDE、默认模型解析、Provider 测试接口、OpenAI / Anthropic / OpenAI-Compatible 路由分发、无可用模型错误处理。

### 12.4 当前状态与后续
- 前后端主链路已完成接入，旧 `/api/ai/settings*` 接口仍保留作为兼容层。
- 下一阶段重点转为补齐“模型服务”页的 Playwright E2E，并继续收敛旧兼容接口。

## 13. 前端 UI 重构第一阶段（layout + 题库页）
### 13.1 完成内容
- 主布局重构：`AppLayout` 统一为顶部栏 + 侧边栏结构，移动端使用 `Drawer` 菜单。
- 新增布局子组件：`AppTopbar`、`MobileMenuDrawer`，并更新 `AppSidebar` 结构与滚动容器。
- 题库页重构：拆分为筛选区、桌面表格区、移动卡片区、编辑弹窗四个业务组件。
- 页面职责收敛：`QuestionBankView` 保留 API 调用、状态与业务动作，UI 组件负责展示。
- 样式变量收口：补齐 `variables.css` 与 Element Plus token 映射，降低未定义变量风险。

### 13.2 本阶段验证
- `cd frontend && npm run test -- --run`：通过
- `cd frontend && npm run build`：通过
- `cd frontend && npm run lint`：失败（当前仓库未提供 `lint` script）

## 14. 前端 UI 重构第二阶段（通用结构 + 规范收敛）
### 14.1 完成内容
- 新增通用结构组件：
  - `components/ui/layout/PageSection.vue`
  - `components/ui/layout/PageActionGroup.vue`
- 全局样式规范化：
  - 在 `styles/base.css` 中统一 `business-card`、`form-action-bar`、`dialog-footer` 等基础样式
  - `PageHeader` 对齐 Element Plus token，操作区统一接入按钮组容器
- 页面接入：
  - `settings`、`plans`、`knowledge`、`mistakes`、`notes`、`ai question center`
- 业务组件接入：
  - `dashboard` 卡片组件（Task/Completion/ReviewReminder/PracticeRecommend/KnowledgeMastery/RecentRecords）
  - `exam`（ExamCreateCard/ExamListCard/ExamHistoryCard）
  - `practice`（PracticeFormCard）
  - `questions` 子组件（Filter/Table/MobileList）
- 结构优化：
  - `NoteView` 从大量内联样式重构为 `PageHeader + PageSection + PageActionGroup` 标准结构，并补齐移动端弹窗展示策略
  - `ExamCreateCard`、`PracticeFormCard` 改为标准 `el-form + el-form-item` 写法，减少手写 label 布局

### 14.2 本阶段验证
- `cd frontend && npm run test -- --run`：通过
- `cd frontend && npm run build`：通过
- `cd frontend && npm run lint`：失败（当前仓库未提供 `lint` script）

## 15. 前端 UI 重构第三阶段（弹窗校验 + 移动端交互）
### 15.1 完成内容
- 题库题目编辑弹窗：
  - 为 `QuestionEditorDialog` 增加 `el-form rules` 与提交前校验
  - 按题型执行差异化字段校验（上午题答案、下午题参考答案）
  - 小屏下统一全屏弹窗并启用内容区滚动
- 知识点编辑弹窗：
  - 为 `KnowledgeView` 弹窗表单增加规则（编码、名称、层级）
  - 保存按钮统一走 `validate` 后提交
  - 小屏下统一全屏弹窗 + 内容滚动
- 错题编辑弹窗：
  - 为 `MistakeView` 弹窗表单增加规则（错因、状态、下次复习日期）
  - 保存逻辑改为先校验后提交
  - 小屏下统一全屏弹窗 + 内容滚动
- 笔记编辑弹窗：
  - 为 `NoteView` 弹窗表单增加规则（标题、内容、关联项目标）
  - 关联项存在但未选择目标时拦截保存
  - 小屏下统一全屏弹窗 + 内容滚动

### 15.2 本阶段验证
- `cd frontend && npm run test -- --run`：通过
- `cd frontend && npm run build`：通过
- `cd frontend && npm run lint`：失败（当前仓库未提供 `lint` script）

## 16. 前端 UI 重构第四阶段（表单栅格 + 页面结构收敛）
### 16.1 完成内容
- 新增通用组件：
  - `components/ui/form/PageFormGrid.vue`
  - 统一表单栅格布局（桌面多列 + 移动端单列），替代页面内重复 `form-grid` 写法
- 表单页接入：
  - `practice/PracticeFormCard`
  - `exam/ExamCreateCard`
  - `knowledge/KnowledgeView`
  - `settings/SettingsView`
  - `ai/AiQuestionCenterView`
  - `settings/AiSettingsView`
- 页面结构收敛：
  - `AnalyticsView` 图表区统一改为 `PageSection`，图表配色统一为 Element Plus 默认色板
  - `PracticeView` 会话区改为 `PageSection + PageActionGroup`，移动端按钮区自动重排
  - `AiSettingsView` 顶部概览、默认模型、新增 Provider 区块改为 `PageSection`，减少页面级手写卡片结构

### 16.2 本阶段验证
- `cd frontend && npm run test -- --run`：通过
- `cd frontend && npm run build`：通过
- `cd frontend && npm run lint`：失败（当前仓库未提供 `lint` script）

## 17. 前端 UI 重构第五阶段（表格页移动端卡片化）
### 17.1 完成内容
- 新增通用组件：
  - `components/ui/data/MobileCardList.vue`
  - 统一封装移动端列表卡片容器与空状态，避免各页面重复编写
- 表格页接入：
  - `plans/PlanView`：任务列表支持移动端卡片展示与状态操作按钮
  - `mistakes/MistakeView`：错题列表支持移动端卡片展示与“编辑状态”操作
  - `notes/NoteView`：笔记列表支持移动端卡片展示与编辑/删除操作
  - `exam/ExamListCard`、`exam/ExamHistoryCard`：模拟卷与历史记录支持移动端卡片展示
  - `ai/AiQuestionCenterView`：生成历史支持移动端卡片展示
- 交互结果：
  - 桌面端继续保持 `el-table` 信息密度
  - 小屏端避免表格挤压与横向滚动，点击区域更适配触屏

### 17.2 本阶段验证
- `cd frontend && npm run test -- --run`：通过
- `cd frontend && npm run build`：通过
- `cd frontend && npm run lint`：失败（当前仓库未提供 `lint` script）

## 18. 前端 UI 重构第六阶段（详情作答 + 视觉收口）
### 18.1 完成内容
- 详情作答组件统一：
  - `PracticeQuestionCard`、`ExamAttemptCard` 操作区改为 `PageActionGroup`
  - 统一题目卡片结构，减少不同页面的按钮布局和间距差异
- 移动端可用性优化：
  - 小屏下详情头部、操作区、题干区、结果区自动重排
  - 缩减内容区内边距，提升长题干阅读与触控操作稳定性
- 风格收口：
  - `CompletionCard` 进度色板改为 Element Plus 默认 token 体系
  - `LoginView` 去除装饰性渐变背景，改为后台统一浅色页面基底

### 18.2 本阶段验证
- `cd frontend && npm run test -- --run`：通过
- `cd frontend && npm run build`：通过
- `cd frontend && npm run lint`：失败（当前仓库未提供 `lint` script）

## 19. 前端 UI 重构第七阶段（页面状态反馈统一）
### 19.1 完成内容
- 新增通用组件：
  - `components/ui/feedback/PageStateBlock.vue`
  - 统一封装页面级加载态（`el-skeleton`）、错误态（`el-result`）与空态（`el-empty`）
- 页面接入：
  - `DashboardView`：概览加载失败时给出错误提示和重试按钮
  - `AnalyticsView`：统计加载态/失败态统一，刷新按钮支持 loading 反馈
  - `PlanView`：计划页面首屏加载、失败重试和空状态统一
- 交互结果：
  - 核心页面首屏不再出现“静默空白”状态
  - 数据请求异常时用户可就地重试，减少刷新整页依赖

### 19.2 本阶段验证
- `cd frontend && npm run test -- --run`：通过
- `cd frontend && npm run build`：通过
- `cd frontend && npm run lint`：失败（当前仓库未提供 `lint` script）
