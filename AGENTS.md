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
