# OSE - Open Software Exam`n`n[![CI](https://github.com/hacksynth/ose/actions/workflows/ci.yml/badge.svg)](https://github.com/hacksynth/ose/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

OSE（Open Software Exam）是一个开源的软考（软件设计师）备考系统，提供题库练习、错题本、模拟考试、学情诊断、学习计划以及 AI 辅助讲解/出题能力。

## 功能亮点

- 知识点练习：支持随机、顺序和按知识点刷题。
- 错题本：自动沉淀错题，支持重练和标记掌握。
- 模拟考试：按上午/下午题型组织限时训练和成绩复盘。
- 学情诊断：根据答题数据分析薄弱知识点和学习趋势。
- 学习计划：结合目标考试日期生成每日任务。
- AI 能力：支持 AI 深度讲解、案例批改、智能出题和解题思路。

## 技术栈

- Next.js 15 App Router
- TypeScript
- Tailwind CSS + shadcn/ui 风格组件
- Prisma ORM + SQLite（开发环境）
- NextAuth.js v5 Credentials Provider
- OpenAI / Claude / Gemini / OpenAI-compatible 自定义 AI Provider

## 本地启动

```bash
npm install
cp .env.example .env
npm run prisma:migrate
npm run dev
```

访问 `http://localhost:3000/register` 注册账号，登录后进入 `/dashboard`。

常用命令：

```bash
npm run prisma:generate
npm run lint
npm run build
npm run prisma:studio
```

## 环境变量

开发环境默认使用 `.env`，可从 `.env.example` 复制：

```env
DATABASE_URL="file:./dev.db"
AUTH_SECRET="change-me"
AUTH_URL="http://localhost:3000"
NEXT_PUBLIC_EXAM_DATE="2026-05-23"
```

生产部署前请务必替换 `AUTH_SECRET`，并根据目标数据库调整 `DATABASE_URL`。

## AI 功能配置

OSE 支持四类 AI Provider。配置任意一种后，即可启用 AI 讲解、AI 批改、AI 出题、学情诊断和智能助手。

```env
AI_PROVIDER=claude

ANTHROPIC_API_KEY=your-key-here
ANTHROPIC_BASE_URL=
ANTHROPIC_MODEL=claude-sonnet-4-5-20250929

OPENAI_API_KEY=your-key-here
OPENAI_BASE_URL=
OPENAI_MODEL=gpt-4o-mini

GEMINI_API_KEY=your-key-here
GEMINI_MODEL=gemini-2.5-flash

CUSTOM_API_KEY=your-key-here
CUSTOM_BASE_URL=http://localhost:11434/v1
CUSTOM_MODEL=llama3
```

如果未设置 `AI_PROVIDER`，系统会按 Claude → OpenAI → Gemini → Custom 的优先级自动检测可用配置。`custom` 模式适用于 Ollama、LM Studio、vLLM、LocalAI、DeepSeek、通义千问等 OpenAI 兼容接口。

## 开源协作

欢迎参与项目建设：

- 贡献指南：`CONTRIBUTING.md`
- 行为准则：`CODE_OF_CONDUCT.md`
- 安全政策：`SECURITY.md`
- 支持说明：`SUPPORT.md`
- 更新日志：`CHANGELOG.md`

提交 PR 前请至少运行：

```bash
npm run lint
npm run build
```

## 许可证

本项目基于 MIT License 开源，详见 `LICENSE`。

