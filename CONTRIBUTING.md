# Contributing to OSE / 参与 OSE 贡献

## Welcome / 欢迎

Thank you for your interest in OSE. We are building an open, self-hostable, AI-powered exam preparation platform for China's Software Professional Qualification Exam. Good contributions include code, question data, documentation, design feedback, deployment recipes, and careful bug reports.

感谢你关注 OSE。我们希望把 OSE 打造成一个开放、可自部署、AI 驱动的软考备考平台。代码、题库、文档、设计建议、部署经验和高质量 Bug 报告都非常有价值。

## Ways to Contribute / 贡献方式

- **Report bugs / 报告 Bug**: use the Bug Report issue template and include reproduction steps.
- **Request features / 提交功能建议**: describe the scenario, expected workflow, and possible implementation.
- **Contribute code / 贡献代码**: pick a good first issue or propose a focused change.
- **Contribute question data / 贡献题库数据**: add multiple-choice questions, case analysis questions, explanations, and knowledge tags.
- **Improve docs / 改进文档**: clarify setup, deployment, AI provider configuration, and exam domain knowledge.
- **Translate / 翻译**: help with English UI copy, Chinese documentation, and future i18n resources.

## Development Setup / 开发环境搭建

Prerequisites:

- Node.js 20+ supported by the project metadata; Node.js 22 LTS is recommended.
- npm 10+.
- Git.
- Optional: Rust toolchain for Tauri desktop builds.

Setup:

```bash
git clone https://github.com/hacksynth/ose.git
cd ose
cp .env.example .env
npm install
npx prisma migrate dev
npx prisma db seed
npm run dev
```

Useful commands:

```bash
npm run lint
npm run typecheck
npm run build
npm run db:studio
npm run tauri:dev
```

## Project Structure / 项目结构

```text
src/
  app/                 Next.js App Router pages and API routes
  app/api/             Backend API endpoints
  components/          Reusable UI and feature components
  components/ui/       shadcn/ui-style primitives
  lib/                 Business logic, auth, AI providers, analytics
  prisma/              Prisma schema, migrations, and seed data
src-tauri/             Tauri desktop shell and bundle configuration
docs/                  User, contributor, and deployment documentation
.github/              Issue templates, workflows, and community files
```

## Coding Standards / 编码规范

- Use TypeScript strict mode. Avoid `any` unless the boundary is genuinely dynamic.
- Run ESLint and Prettier before submitting a PR.
- Keep server-only logic in `src/lib` or API routes; keep interactive UI in client components.
- Prefer existing UI primitives in `src/components/ui`.
- Name React components in `PascalCase`, hooks as `useSomething`, utilities in `camelCase`, and constants in `SCREAMING_SNAKE_CASE` when global.
- Keep database access behind Prisma and avoid raw SQL unless there is a strong reason.
- Use Conventional Commits:

```text
feat: add question search
fix: handle desktop IPv4 startup
docs: improve self-hosting guide
data: add 2024 multiple-choice questions
```

PR expectations:

- One focused change per PR.
- Link related issues.
- Include screenshots for visible UI changes.
- Confirm lint, typecheck, build, and relevant manual tests.
- Explain migrations or breaking changes clearly.

## Question Bank Contribution Guide / 题库贡献指南

Question data should be accurate, traceable, and mapped to the knowledge hierarchy.

For multiple-choice questions, include:

- Stable question code, e.g. `SD-2024-AM-001`.
- Knowledge topic ID or topic title.
- Stem, four options, correct option, explanation, difficulty, and source.

Example:

```json
{
  "code": "SD-2024-AM-001",
  "type": "SINGLE_CHOICE",
  "topic": "Software Engineering",
  "stem": "Which model emphasizes iterative risk analysis?",
  "options": [
    { "label": "A", "content": "Waterfall" },
    { "label": "B", "content": "Spiral" },
    { "label": "C", "content": "V-Model" },
    { "label": "D", "content": "Prototype" }
  ],
  "answer": "B",
  "explanation": "The spiral model combines iteration with explicit risk analysis.",
  "difficulty": "MEDIUM",
  "source": "Contributor"
}
```

For case analysis questions, include scenario background, sub-questions, reference answers, score points, and scoring rubric. See [docs/question-format.md](docs/question-format.md) for the full format.

Quality requirements:

- Do not submit copyrighted exam content unless it is legally redistributable.
- Avoid ambiguous wording.
- Provide explanations for every answer.
- Tag at least one knowledge point.
- Keep formatting plain Markdown where possible.

## First Good Issues / 新手任务建议

Good first contributions usually have a small surface area:

- Improve docs for one deployment path.
- Add explanations to existing knowledge points.
- Add a small batch of original practice questions.
- Add a UI empty state or loading state.
- Add unit tests for one API route.
- Improve mobile spacing on one practice page.

See [.github/GOOD_FIRST_ISSUES.md](.github/GOOD_FIRST_ISSUES.md) for a curated list.
