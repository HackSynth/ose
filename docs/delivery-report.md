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
- `frontend/src/views/plans/PlanView.vue`
- `frontend/src/views/knowledge/KnowledgeView.vue`
- `frontend/src/views/practice/PracticeView.vue`
- `frontend/src/views/mistakes/MistakeView.vue`
- `frontend/src/views/exams/ExamView.vue`
- `frontend/src/views/analytics/AnalyticsView.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
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

### 11.2 本阶段验证
- `docker compose run --rm backend-test`
- 覆盖加密/解密、Key 掩码、保存读取、clearApiKey、优先级解析、Provider 测试接口。

### 11.3 当前剩余项
- 前端 AI 配置页面与交互提示尚未提交，将在下一阶段补齐。
- E2E 将在页面落地后串联“配置 Provider -> 测试连接 -> AI 出题”的完整链路。
