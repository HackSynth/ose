# OSE（Open Software Exam）

[![CI](https://github.com/HackSynth/ose/actions/workflows/ci.yml/badge.svg)](https://github.com/HackSynth/ose/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/HackSynth/ose?display_name=tag)](https://github.com/HackSynth/ose/releases)
[![License](https://img.shields.io/github/license/HackSynth/ose)](LICENSE)
[![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)](docker-compose.yml)
[![Vue 3](https://img.shields.io/badge/Vue%203-35495E?logo=vue.js&logoColor=4FC08D)](frontend/package.json)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)](backend/pom.xml)
[![MySQL 8](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)](docker-compose.yml)
[![Playwright](https://img.shields.io/badge/Playwright-E2E-2EAD33?logo=playwright&logoColor=white)](frontend/tests/e2e)

单用户、中文优先、PC 优先的软考（软件设计师）个人备考系统。项目覆盖从目标配置、学习计划、知识树、题库、练习、错题、模拟考试、笔记到统计分析的完整闭环，并补齐了 Playwright 端到端测试、整包导入能力与备考体验增强。

## 1. 功能概览
- 仪表盘：考试倒计时、今日任务、本周/本月完成情况、知识掌握度、最近错题 / 模拟 / 笔记
- 学习计划：基于考试日期与学习参数自动生成阶段计划、周任务与日任务；保存设置后自动重排当前计划
- 知识体系：维护一级 / 二级 / 三级知识点，记录掌握度、权重与备注
- 题库管理：支持上午题 / 下午题，支持筛选、导入模板、导出
- AI 出题中心：支持 OpenAI / Claude（Anthropic）按知识点、难度、场景生成上午题/下午题，支持预览编辑、临时练习与审核后入库
- 练习系统：支持按知识点、随机、错题练习；上午题自动判分，下午题自评；支持薄弱知识点一键推荐练习
- 错题复习：自动入库、错因分类、复习状态流转、下次复习时间、到期复习提醒
- 模拟考试：创建上午卷 / 下午卷模拟，保存成绩与历史记录，并生成模考后复盘摘要
- 学习笔记：Markdown、收藏、搜索、关联知识点 / 题目 / 模拟考
- 统计分析：知识点表现、计划完成趋势、模拟成绩趋势、错题分布
- 系统能力：健康检查、整包数据导出、整包 JSON 导入、题库模板下载
- 质量保障：Playwright E2E 覆盖核心备考链路

## 2. 技术栈
### 前端
- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- ECharts / Vue ECharts
- Playwright
- Vitest

### 后端
- Java 17
- Spring Boot 3.3.0
- Spring Data JPA
- Spring Security + JWT
- Flyway
- MySQL 8
- JUnit 5 / Spring Boot Test

### 部署
- Docker Compose
- Nginx（前端静态托管 + `/api` 反向代理）

## 3. 目录结构
```text
.
├── backend/                         # Spring Boot 后端
├── frontend/                        # Vue 3 前端
├── docs/                            # 需求、架构、计划、交付等文档
├── examples/import/                 # 导入模板与示例整包
├── docker-compose.yml               # 一键启动编排
├── .env.example                     # 环境变量模板
└── AGENTS.md                        # 仓库协作说明
```

## 4. 默认账号与示例数据
- 默认管理员账号：`admin`
- 默认管理员密码：`OseAdmin@2026`
- 启动后自动初始化：
  - 软件设计师知识点树
  - 自拟上午 / 下午示例题
  - 当前学习计划与任务
  - 练习记录与错题样例
  - 模拟卷与模拟记录
  - Markdown 笔记示例

## 5. 快速开始

### 5.1 准备环境
需要安装：
- Docker
- Docker Compose

### 5.2 配置环境变量
```bash
cp .env.example .env
```
如无特殊需求，可直接使用默认值。

AI 相关可选环境变量：
- `OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_DEFAULT_MODEL`、`OPENAI_MODELS`
- `ANTHROPIC_API_KEY`、`ANTHROPIC_BASE_URL`、`ANTHROPIC_DEFAULT_MODEL`、`ANTHROPIC_MODELS`
- `AI_REQUEST_TIMEOUT_MS`、`AI_MAX_RETRIES`、`AI_ENABLE_SAVE_REVIEW`

说明：
- 未配置 API Key 时，AI 模块会优雅降级（仅显示“未配置”状态），不影响其他功能。
- `AI_ENABLE_SAVE_REVIEW=true` 时，AI 入库题默认待审核（不进入可练习状态）。

### 5.3 一键启动
```bash
docker compose up -d --build
```
启动后访问：
- 前端：`http://localhost`
- 后端健康检查：`http://localhost/healthz`
- 后端 API：`http://localhost:8080/api`

### 5.4 登录系统
使用默认管理员账号登录：
- 用户名：`admin`
- 密码：`OseAdmin@2026`

## 6. 常用命令
### 查看状态
```bash
docker compose ps
```

### 查看日志
```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

### 停止服务
```bash
docker compose down
```

### 清理并重建
```bash
docker compose down -v
docker compose up -d --build
```

## 7. 测试与验证
### 后端测试
```bash
docker compose run --rm backend-test
```

### 前端单元测试
```bash
docker compose run --rm frontend-test
```
或本地运行：
```bash
cd frontend
npm install
npm run test -- --run
```

### 前端构建验证
```bash
cd frontend
npm run build
```

### Playwright E2E（推荐：Docker Compose）
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
说明：
- 本地运行前请确保前端、后端、MySQL 已启动并可访问。
- 本地浏览器依赖和沙箱限制因操作系统不同可能存在差异；若本地浏览器环境受限，优先使用 Compose 版 E2E。

### Compose 配置检查
```bash
docker compose config
```

### 本地健康检查
```bash
curl http://localhost/healthz
curl http://localhost:8080/api/health
```

## 8. 本地开发说明
### 前端本地开发
```bash
cd frontend
npm install
npm run dev
```
默认开发地址：`http://localhost:5173`

### 后端本地开发
如果本机已安装 Maven，也可以：
```bash
cd backend
mvn spring-boot:run
```
否则建议继续使用 Docker Compose 进行开发验证。

## 9. 导入导出
### 题库导入模板
- `examples/import/questions-template.csv`
- `examples/import/questions-template.json`

### 整包导入模板
- `examples/import/full-import-template.json`
- `GET /api/import/template`

### 支持的整包导入范围
- 系统设置
- 知识点树
- 题库
- 笔记
- 当前学习计划

### 重复数据处理策略
- `OVERWRITE`：覆盖匹配到的已有记录
- `SKIP`：跳过重复记录，并在导入结果中给出警告或跳过计数

### 推荐导入流程
1. 先从设置页点击“导出数据”，获取当前整包结构。
2. 参考 `examples/import/full-import-template.json` 或模板接口返回结构填写数据。
3. 在设置页选择重复数据处理策略。
4. 上传 JSON 文件执行导入。
5. 查看导入结果面板中的分区统计、警告和错误信息。

### 导入校验与限制
- 缺少关键字段时会在错误报告中标明 `scope`、`identifier` 与错误原因。
- 学习计划导入只导入当前有效计划，不回放历史计划链路。
- 当前整包导入不覆盖练习历史、错题历史与模考历史，仅导入用户维护类基础数据。

### 前端页面能力
- 题库页支持下载 CSV / JSON 模板
- 题库页支持上传 CSV / JSON 文件导入
- 设置页支持导出整包 JSON 数据
- 设置页支持上传整包 JSON 导入

## 10. E2E 覆盖范围
当前 Playwright 已覆盖以下核心链路：
1. 管理员登录
2. 设置考试日期并保存
3. 自动生成学习计划
4. 浏览知识点
5. 完成一组上午题并自动判分
6. 完成一组下午题并提交自评
7. 验证错题自动入库
8. 创建并提交模拟考试
9. 验证统计页数据展示
10. 执行整包导出

测试用例位于：
- `frontend/tests/e2e/auth.spec.ts`
- `frontend/tests/e2e/core-flow.spec.ts`
- `frontend/tests/e2e/helpers.ts`

## 11. 关键接口
- `POST /api/auth/login`
- `GET /api/dashboard/overview`
- `GET /api/settings`
- `PUT /api/settings`
- `POST /api/plans/generate`
- `GET /api/knowledge-points/tree`
- `GET /api/questions`
- `POST /api/ai/questions/generate`
- `POST /api/ai/questions/save`
- `GET /api/ai/providers`
- `GET /api/ai/models`
- `GET /api/ai/history`
- `GET /api/ai/health`
- `POST /api/practice/sessions`
- `GET /api/mistakes`
- `GET /api/exams`
- `GET /api/notes`
- `GET /api/analytics/summary`
- `GET /api/export/full`
- `POST /api/import/full`
- `POST /api/import/full-file`
- `GET /api/import/template`
- `GET /api/health`

## 12. 文档索引
- 需求基线：`docs/requirements.md`
- 关键假设：`docs/assumptions.md`
- 架构设计：`docs/architecture.md`
- 实施计划：`docs/implementation-plan.md`
- 初始化说明：`docs/initialization.md`
- 交付报告：`docs/delivery-report.md`

## 13. 已知说明
- MVP 面向单用户管理员，不开放注册。
- 下午题采用手动自评模式，重点支持复盘而非自动 NLP 评分。
- 示例题全部为自拟内容，不包含受版权保护的真题全文。
- 若本地浏览器运行受系统沙箱限制影响，优先使用 Compose 版 E2E。
