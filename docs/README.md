# 项目文档目录

> 本文档目录包含项目的核心开发和维护文档。

---

## 快速导航

| 文档 | 说明 | 目标读者 |
|------|------|----------|
| [API_INTEGRATION_GUIDE.md](./API_INTEGRATION_GUIDE.md) | **前后端接口对接指南** - 完整的 API 文档、DTO 定义、调用示例 | 前端开发者 |
| [AUTHENTICATION_SYSTEM.md](./AUTHENTICATION_SYSTEM.md) | **认证系统说明** - JWT 认证流程、登录/注册接口说明 | 前后端开发者 |
| [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) | **数据库表结构** - PostgreSQL 表定义、JPA 实体说明 | 后端开发者 |
| [BACKEND_DEVELOPMENT_GUIDE.md](./BACKEND_DEVELOPMENT_GUIDE.md) | **后端开发指南** - 项目架构、代码规范、开发流程 | 后端开发者 |
| [FRONTEND_MIGRATION_GUIDE.md](./FRONTEND_MIGRATION_GUIDE.md) | **前端迁移指南** - UUID 类型调整、类型定义修正 | 前端开发者 |

---

## 项目概述

这是一个 **求职看板管理** 的 Spring Boot 后端项目，提供：

- 用户认证（JWT + BCrypt 密码加密）
- 看板管理（Board）
- 列管理（Kanban Column）
- 职位卡片管理（Job Card）

### 技术栈

- Java 17 + Spring Boot 3.5.5
- MyBatis Plus + PostgreSQL
- JWT 认证（jjwt）
- SpringDoc OpenAPI（Swagger UI）

---

## 常用入口

### 本地开发环境

```bash
# 启动应用
mvnw.cmd spring-boot:run

# 运行测试
mvnw.cmd test

# 打包
mvnw.cmd clean package
```

### API 文档

启动应用后访问：

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### 主要接口

| 接口 | 路径 | 说明 |
|------|------|------|
| 登录/注册 | `/auth/login`, `/auth/register` | 获取 JWT Token |
| 加载看板 | `/board/load` | 获取看板完整数据 |
| 创建卡片 | `/board/card/create` | 在看板中创建职位卡片 |
| 移动卡片 | `/board/card/move` | 改变卡片所在列 |

---

## 文档状态

| 日期 | 状态 | 说明 |
|------|------|------|
| 2026-03-09 | 已完成 | 后端开发完成，文档已整理 |

---

## 归档文档

开发过程中的中间文档已移至 [archive/](./archive/) 目录：

- `TASK_PROGRESS.md` - 开发进度追踪表
- `API_BOARD_CONTROLLER.md` - 早期 Board 接口文档（已合并到 API_INTEGRATION_GUIDE）
- `API_TESTING_GUIDE.md` - 早期测试指南
- `UUID_AND_BOARD_DESIGN_FIX.md` - 设计决策过程文档

