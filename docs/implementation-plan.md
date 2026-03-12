# OSE 实施计划

## 当前状态
- OSE MVP 已完成：认证、仪表盘、计划、知识体系、题库、练习、错题、模拟、笔记、统计、导出、健康检查均已交付。
- 本轮进入“质量增强 + 实用性增强”阶段，目标是在不重做既有能力的前提下补齐端到端验证、整包导入、备考体验增强，并同步完善文档与交付说明。

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

### 阶段 E：AI Provider 配置中心（进行中）
- 已完成后端配置中心基础设施：数据库表、加密服务、统一配置解析器、健康探测 API、`ENV / DB / HYBRID` 优先级策略
- 已完成后端测试：密钥加解密、掩码、保存/清空、优先级解析、配置测试接口
- 下一步补齐前端 AI 配置页面、交互测试与 E2E 链路

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
