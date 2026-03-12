# OSE（Open Software Exam）架构设计

## 1. 系统上下文
OSE 是一个单用户、个人使用的 Web 备考系统，采用前后端分离架构：
- 前端：Vue 3 单页应用，提供中文 UI、学习流程操作与图表展示。
- 后端：Spring Boot REST API，负责认证、业务规则、数据聚合、导入导出与初始化。
- 数据库：MySQL 8，存储用户、知识点、题库、计划、练习、错题、模拟考、笔记、系统设置。
- 部署：Docker Compose 编排 `frontend`、`backend`、`mysql` 三个服务。

## 2. 模块划分

### 2.1 前端模块
- `auth`：登录、会话恢复、路由守卫。
- `layout`：主框架、导航、全局状态、响应式布局。
- `dashboard`：倒计时、任务概览、最近记录、知识进度。
- `knowledge`：知识树与掌握度维护。
- `questions`：题库列表、筛选、导入模板下载、编辑表单。
- `plans`：学习参数设置、计划生成、任务流转。
- `practice`：上午题练习、下午题练习、错题练习。
- `exams`：模拟考试创建、作答、计时、成绩历史。
- `mistakes`：错题聚合、复习安排、状态更新。
- `notes`：Markdown 笔记、关联对象、搜索。
- `analytics`：图表、趋势、分布统计。
- `settings`：系统参数、导出导入、健康检查。

### 2.2 后端模块
- `auth`：登录认证、JWT 签发、当前用户上下文。
- `core`：统一响应、异常处理、基础枚举、工具类。
- `settings`：考试日期、通过阈值、学习参数、复习周期管理。
- `dashboard`：聚合多个模块数据形成首页视图。
- `knowledge`：知识点树管理与掌握度维护。
- `question`：题库管理、筛选查询、导入导出。
- `plan`：学习计划生成、阶段编排、任务状态流转。
- `practice`：练习会话、作答记录、自动评分与标记行为。
- `mistake`：错题记录、错因、复习状态与下次复习时间。
- `exam`：模拟卷、考试记录、成绩汇总。
- `note`：笔记 CRUD、Markdown 内容、关联关系与搜索。
- `analytics`：统计口径计算与图表数据组装。
- `ai`：统一 Provider 抽象层、Prompt 构建、结构化输出校验、AI 生成记录、题目草稿保存。
- `ai`：统一 Provider 抽象层、Prompt 构建、结构化输出校验、AI 生成记录、Provider 配置中心、密钥加密与健康探测。
- `health`：健康检查与应用元信息。
- `bootstrap`：种子数据、默认管理员初始化。

## 3. 前后端边界

### 前端负责
- 页面路由、状态管理与表单交互
- 可视化展示与轻量级格式化
- 本地保存 JWT 与用户会话
- 通过 API 调用驱动业务流程

### 后端负责
- 所有业务规则与数据持久化
- 计划生成、评分、错题流转、统计聚合
- 导入导出数据格式校验
- 鉴权、输入校验、统一错误响应

### 统一约定
- API 前缀：`/api`
- 返回结构：`{ code, message, data, timestamp }`
- 认证方式：`Authorization: Bearer <token>`
- 时间格式：ISO-8601

## 4. 数据模型

### 4.1 核心实体
- `users`：管理员账户，预留未来多用户扩展字段。
- `system_settings`：考试日期、通过阈值、每周时长、学习偏好、复习周期。
- `knowledge_points`：知识树节点，支持三级层级、自关联父子结构。
- `questions`：题干、题型、难度、年份、来源、标签、正确答案、参考要点。
- `question_options`：上午选择题选项。
- `question_knowledge_rel`：题目与知识点多对多关系。
- `study_plans`：计划主记录，含阶段信息与生成参数快照。
- `study_tasks`：计划中的具体任务，支持日任务与状态流转。
- `practice_sessions`：一次练习或刷题会话。
- `practice_records`：单题作答记录、自动评分、自评结果、耗时。
- `mistake_records`：错题条目、错因、复习状态、下次复习时间。
- `mock_exams`：模拟考试定义。
- `mock_exam_questions`：模拟卷与题目的关联及顺序。
- `mock_exam_attempts`：模拟考试作答记录与成绩。
- `mock_exam_attempt_answers`：模拟考试单题作答详情。
- `notes`：Markdown 笔记及收藏状态。
- `note_links`：笔记与知识点 / 题目 / 模拟考关联。
- `ai_generation_records`：AI 生成记录、参数快照、provider/model、状态、错误、prompt hash、结果摘要。
- `ai_provider_settings`：Provider 启用状态、加密后的 API Key、掩码、默认模型、超时、重试、温度、配置来源、最近健康检查摘要。
- `import_jobs`：导入记录（MVP 可简化为日志表）。

### 4.2 关键字段设计
- `knowledge_points.level`：1 / 2 / 3。
- `questions.type`：`MORNING_SINGLE`、`AFTERNOON_CASE`。
- `study_tasks.status`：`TODO`、`IN_PROGRESS`、`DONE`、`DELAYED`、`MISSED`。
- `practice_records.result`：`CORRECT`、`WRONG`、`PARTIAL`。
- `mistake_records.review_status`：`NEW`、`READY`、`REVIEWED`、`MASTERED`。
- `mock_exam_attempts.status`：`IN_PROGRESS`、`SUBMITTED`。

## 5. 关键接口设计

### 5.1 认证
- `POST /api/auth/login`：账号密码登录，返回 token 与用户信息。
- `GET /api/auth/me`：获取当前用户。

### 5.2 仪表盘
- `GET /api/dashboard/overview`：返回倒计时、任务统计、掌握度、最近记录。

### 5.3 设置与计划
- `GET /api/settings`
- `PUT /api/settings`
- `POST /api/plans/generate`：按当前设置生成新计划。
- `GET /api/plans/current`
- `PATCH /api/tasks/{taskId}`：更新状态、完成度、优先级、延期日期。
- `POST /api/tasks/rebalance`：对延期任务重排。

### 5.4 知识点与题库
- `GET /api/knowledge-points/tree`
- `POST /api/knowledge-points`
- `PUT /api/knowledge-points/{id}`
- `DELETE /api/knowledge-points/{id}`
- `GET /api/questions`
- `POST /api/questions`
- `PUT /api/questions/{id}`
- `DELETE /api/questions/{id}`
- `POST /api/questions/import`
- `GET /api/questions/export`
- `GET /api/questions/templates/{format}`

### 5.5 练习与错题
- `POST /api/practice/sessions`：创建练习会话。
- `GET /api/practice/sessions/{id}`：获取题目列表。
- `POST /api/practice/sessions/{id}/submit`：提交单题或整场练习。
- `POST /api/practice/records/{id}/review`：下午题自评。
- `PATCH /api/practice/records/{id}/flags`：收藏 / 不会 / 加入复习。
- `GET /api/mistakes`
- `PATCH /api/mistakes/{id}`：更新错因、状态、复习时间。

### 5.6 模拟考试
- `GET /api/exams`
- `POST /api/exams`
- `POST /api/exams/{id}/attempts`
- `POST /api/exam-attempts/{attemptId}/submit`
- `POST /api/exam-attempts/{attemptId}/score-afternoon`

### 5.7 笔记与统计
- `GET /api/notes`
- `POST /api/notes`
- `PUT /api/notes/{id}`
- `DELETE /api/notes/{id}`
- `GET /api/analytics/summary`
- `GET /api/analytics/trends`

### 5.8 系统能力
- `GET /api/health`
- `GET /api/ai/settings`
- `PUT /api/ai/settings/{provider}`
- `POST /api/ai/settings/{provider}/test`
- `GET /api/ai/settings/{provider}/models`
- `POST /api/ai/questions/generate`
- `POST /api/ai/questions/save`
- `GET /api/ai/providers`
- `GET /api/ai/models`
- `GET /api/ai/history`
- `GET /api/ai/health`
- `GET /api/export/full`
- `POST /api/import/preview`

## 12. AI 出题模块设计
- Provider 抽象：`AiProviderClient` + `OpenAiProviderClient` + `AnthropicProviderClient`，业务层只依赖统一接口。
- 配置中心：`AiProviderSettingsService` + `AiProviderConfigurationResolver` 统一解析 `ENV / DB / HYBRID` 三种模式，避免业务代码分散读取环境变量。
- 密钥安全：数据库仅保存 `api_key_encrypted` 与 `api_key_mask`，明文 API Key 只存在于请求上下文与运行时内存中；加密主密钥来自 `AI_SECRET_ENCRYPTION_KEY`。
- 健康探测：`AiProviderHealthService` 支持对当前配置或待保存配置执行连通性测试，只返回状态摘要与失败原因，不回显明文 Key。
- 结构化输出：使用 `QuestionBatchSchema`（区分 `MorningQuestionSchema` / `AfternoonQuestionSchema`）约束返回 JSON，再做服务端 schema 与业务校验。
- Prompt 工程：按上午题、下午题、薄弱点强化、错题相似题场景生成专用提示词，固定中文输出与软件设计师考试风格。
- 持久化：`ai_generation_records` 保存生成参数、provider/model、状态、错误信息、prompt hash、结果摘要，且不落库 API Key。
- 降级策略：未配置 OpenAI/Anthropic Key 时，`/api/ai/providers` 与 `/api/ai/settings` 返回未配置或 `UNAVAILABLE` 状态，生成接口返回可理解错误，不影响主链路。

### AI 接入取舍说明
- OpenAI：使用官方推荐的 Responses API（HTTP 调用），并启用 JSON Schema 约束输出结构。
- Anthropic：使用 Messages API（HTTP 调用），按 schema 指令返回严格 JSON。
- 本项目当前选择稳定 HTTP 客户端（Spring `RestClient`）而非 SDK，原因是便于统一重试/错误映射与代理兼容，减少 SDK 版本耦合；后续可平滑替换为官方 Java SDK。

## 6. 认证与权限模型
- 仅保留一个逻辑角色：`ADMIN`。
- 不开放注册，系统启动时若无用户则自动创建默认管理员。
- 登录成功后签发 JWT；前端通过 Pinia 持久化到 `localStorage`。
- Spring Security 拦截所有 `/api/**`，仅放行 `/api/auth/login` 与 `/api/health`。
- 未来多用户扩展时，可在 `users`、业务表中添加 `owner_id`，但 MVP 不启用多租户逻辑。

## 7. 导入 / 导出方案
- 导入：支持 `CSV` 与 `JSON` 两种格式，MVP 先覆盖题库导入、部分计划 / 笔记导入。
- 导出：支持题库、笔记、当前计划、练习记录的 JSON 导出；题库另支持 CSV 导出。
- 模板：在仓库 `examples/import/` 提供示例文件，同时后端提供模板下载 API。
- 策略：导入时进行字段校验，失败项返回明细，不因单条错误导致全量崩溃。

## 8. 测试方案
- 后端：JUnit 5 + Spring Boot Test，覆盖认证、计划生成、练习评分、统计聚合。
- 前端：Vitest + Vue Test Utils，覆盖关键 Store、工具函数与少量组件行为。
- 集成验证：
  - `docker compose build`
  - `docker compose up -d`
  - 健康检查与登录 smoke test
- 回归重点：主链路 API、种子数据、前端关键页面可加载。

## 9. 部署方案
- `mysql`：使用 MySQL 8 镜像，挂载持久化卷。
- `backend`：多阶段 Dockerfile，Maven 构建后生成 Spring Boot 可执行 JAR。
- `frontend`：Node 构建后由 Nginx 提供静态文件，并反向代理 `/api` 到后端。
- `docker-compose.yml` 负责环境变量注入、依赖顺序与健康检查。
- 提供 `.env.example` 作为默认配置模板。

## 10. 安全与稳定性考虑
- 密码使用 BCrypt 哈希存储。
- 所有写接口使用 `@Valid` 做输入校验。
- 提供全局异常处理，避免栈信息泄露给前端。
- CORS 仅允许本地前端域名与 Compose 内反代场景。
- 通过 Flyway 管控数据库结构，减少手工建表漂移。
- 使用分页 / 限流式查询避免大列表一次性加载过量数据。
- 对导入数据进行大小与字段限制，降低异常文件风险。

## 11. 技术选型与取舍
- Spring Data JPA：相较 MyBatis，JPA 在本项目中能更快交付复杂实体关系与 CRUD 聚合，维护成本更低。
- Flyway：简单直接、适合个人项目持续演进。
- JWT：实现成本低，适合单用户本地部署；不引入 OAuth2 复杂度。
- Element Plus：成熟稳定、中文生态完善，适合快速搭建后台风格 UI。
- Pinia：官方推荐状态库，心智负担低。
- ECharts：图表能力成熟，可覆盖趋势与分布需求。
- Docker Compose：满足一键启动目标，避免本地环境差异。
