# 贡献指南

感谢你愿意参与 OSE（Open Software Exam）！本项目欢迎 bug 修复、功能改进、文档补充、题库内容完善和体验优化。

## 开始之前

- 请先阅读 `README.md`，确认本地环境可以正常启动。
- 提交较大的功能前，建议先创建 Issue 说明动机、方案和影响范围。
- 涉及 AI、鉴权、数据库迁移的改动，请在 PR 中写清楚验证方式。

## 本地开发

```bash
npm install
cp .env.example .env
npm run prisma:migrate
npm run dev
```

常用检查命令：

```bash
npm run prisma:generate
npm run lint
npm run build
```

## 提交规范

建议使用简洁清晰的提交信息：

- `feat: add ai question generation`
- `fix: handle empty practice result`
- `docs: update deployment guide`
- `chore: update ci workflow`

## Pull Request 要求

请确保 PR：

- 聚焦一个主题，避免混入无关重构。
- 包含必要的文档、迁移或示例配置更新。
- 通过 `npm run lint` 和 `npm run build`。
- 若新增 Prisma schema，请提交对应迁移目录。
- 不提交 `.env`、数据库文件、日志或构建产物。

## 代码风格

- 使用 TypeScript，保持类型清晰。
- 优先复用现有组件和工具函数。
- UI 保持 SproutSpace 温暖风格与中文界面。
- 修复问题时尽量处理根因，不做过度抽象。

## 题库与内容贡献

- 题目、解析和案例内容应准确、原创或确认可开源使用。
- 请避免直接复制受版权保护的真题全文。
- 解析需说明正确答案依据，并指出常见误区。
