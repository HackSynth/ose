# OSE 仓库协作说明

## 项目结构

- `frontend/`：Vue 3 + TypeScript + Vite 前端。
- `backend/`：Java 17 + Spring Boot 后端。
- `docs/`：需求、架构、实施计划、交付说明。
- `examples/import/`：导入模板与示例数据文件。

## 常用命令

- 启动全部服务：`docker compose up -d --build`
- 查看服务状态：`docker compose ps`
- 后端测试：`docker compose run --rm backend-test`
- 前端测试：`docker compose run --rm frontend-test`
- 健康检查：`curl http://localhost/healthz`

## 开发约束

- 中文文档优先，命名保持统一、语义清晰。
- 变更时优先修复根因，不做一次性 hack。
- 修改接口时同步更新前端契约、README 与相关文档。
- 新增关键逻辑时补最小有效测试。
- 种子数据必须保持“可演示、非侵权、自拟内容”。

## 交付标准

- `docker compose up -d --build` 可启动前端、后端、数据库。
- 默认管理员账号可登录。
- 核心链路与文档保持可验证状态。

## 提交与文档同步规则（高优先级，长期生效）

### Git 提交规则

- 每完成一个“可独立验证的小阶段”就提交一次，避免将不相关改动堆叠到同一提交。
- 每次提交前必须先做本阶段相关验证。
- 前端改动验证：`test` / `build` / `lint`。
- 后端改动验证：`test`。
- 全链路改动验证：`docker compose config` / smoke test / e2e（如适用）。
- 提交粒度保持单一职责，一个提交只做一类事情（功能、修复、测试、文档等）。
- 不要 `amend` 已有提交，除非用户明确要求。
- 不要改写历史、不要强推、不要执行危险 Git 操作。
- 每次提交后必须提供状态摘要。
- 状态摘要必须包含提交信息（commit message）。
- 状态摘要必须包含改动范围。
- 状态摘要必须包含验证结果。

### 文档同步规则

- 只要功能、配置、命令、页面、接口、部署方式、测试方式、环境变量发生变化，必须同步更新相关文档。
- `README.md`：启动方式、配置方式、使用方式、页面入口、测试命令、部署命令、环境变量变化时必须更新。
- `docs/architecture.md`：模块结构、数据流、provider 抽象、配置来源、接口边界、存储策略变化时必须更新。
- `docs/implementation-plan.md`：每完成一个里程碑或新增一轮范围都要更新状态。
- `docs/delivery-report.md`：每轮交付结束后补充完成内容、验证结果、已知限制。
- `.env.example`：新增、删除、修改环境变量时必须同步更新。
- `AGENTS.md`：新增可复用开发规则、提交流程、验证流程、目录约定、文档约定时必须更新。

### 每轮工作顺序

1. 修改代码
2. 运行相关验证
3. 修复失败
4. 更新 `README.md` / `docs/*` / `.env.example` / `AGENTS.md`
5. `git add`
6. `git commit`
7. 输出本轮摘要
8. 继续下一轮

### 提交信息规范

- 使用 Conventional Commits：`feat:` / `fix:` / `refactor:` / `test:` / `docs:` / `chore:`。
- 如果一轮同时包含代码与文档更新，以主要改动类型命名提交。

### Definition of Done 补充

- 功能完成必须同时满足代码完成。
- 功能完成必须同时满足验证通过。
- 功能完成必须同时满足文档已更新。
- 功能完成必须同时满足环境变量模板已更新（如适用）。
- 功能完成必须同时满足 `AGENTS.md` 已更新（规则变化时）。
- 功能完成必须同时满足已完成 Git 提交。
