# 任务进度表

> 最后更新：2026-03-08
> 状态说明：✅ 已完成 | 🔄 进行中 | ⏳ 待开始

---

## 开发模式说明

**增量式开发流程**：Service → 单元测试 → Controller → 接口测试 → 下一功能

每个功能模块独立完成开发、测试、接口验证后再进行下一个，确保质量。

---

## 开发阶段总览

| 阶段 | 名称 | 进度 | 状态 |
|------|------|------|------|
| 阶段 1 | 基础搭建 | 100% | ✅ 已完成 |
| 阶段 2 | 核心 CRUD | 40% | 🔄 进行中 |
| 阶段 3 | 接口完善 | 0% | ⏳ 待开始 |
| 阶段 4 | 认证与联调 | 0% | ⏳ 待开始 |

---

## 详细任务清单

### 阶段 1：基础搭建 ✅

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 1.1 | 创建 Spring Boot 项目 | ✅ | 已完成，使用 Spring Initializr |
| 1.2 | 配置 PostgreSQL 数据源 | ✅ | application.yaml 已配置 |
| 1.3 | 数据库建表 | ✅ | board, kanban_column, job_card 表已创建 |
| 1.4 | 编写 Entity 实体类 | ✅ | Board.java, KanbanColumn.java, JobCard.java |
| 1.5 | 配置 JPA | ✅ | application.yaml 已配置 |
| 1.6 | DTO 类创建 | ✅ | Request/Response DTO 已全部创建 |

**阶段 1 完成物：**
- [x] pom.xml（JPA、PostgreSQL、SpringDoc 依赖）
- [x] application.yaml（数据库和 JPA 配置）
- [x] Entity 层（3 个实体类）
- [x] Repository 层（3 个 Repository 接口）
- [x] DTO 层（全部 Request/Response DTO）

---

### 阶段 2：核心 CRUD（增量式开发）🔄

#### 迭代 1：Board 模块 ✅

| 步骤 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 1 | BoardService 接口 + 实现 | ✅ | createBoard(), loadBoard() |
| 2 | BoardService 单元测试 | ✅ | 10 个测试用例 |
| 3 | BoardController | ✅ | /board/load, /board/create |
| 4 | 接口测试 | ✅ | HTTP 测试文件 + Swagger 验证 |

#### 迭代 2：Column 模块 ✅（已完成）

| 步骤 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 1 | ColumnService 接口 + 实现 | ✅ | updateColumn() |
| 2 | ColumnService 单元测试 | ✅ | 11 个测试用例 |
| 3 | ColumnController | ✅ | /board/column/update |
| 4 | 接口测试 | ✅ | HTTP 测试 + Swagger 验证 |

#### 迭代 3：JobCard 模块 🔄（当前迭代）

| 步骤 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 1 | JobCardService 接口 + 实现 | ✅ | createCard(), updateCard(), moveCard(), deleteCard() |
| 2 | JobCardService 单元测试 | ✅ | 14 个测试用例 |
| 3 | JobCardController | ✅ | /board/card/* 接口 |
| 4 | 接口测试 | ✅ | HTTP 测试 + Swagger 验证 |

#### 功能完成度

| 功能 | 状态 | 说明 |
|------|------|------|
| 创建看板 | ✅ | Service + Controller + 测试 完成 |
| 加载看板 | ✅ | Service + Controller + 测试 完成 |
| 更新列 | ✅ | 迭代 2 完成 |
| 创建卡片 | ✅ | 迭代 3 完成 |
| 更新卡片 | ✅ | 迭代 3 完成 |
| 移动卡片 | ✅ | 迭代 3 完成 |
| 软删除卡片 | ✅ | 迭代 3 完成 |

**API 文档：**
- [x] BoardController 接口文档 (`docs/API_BOARD_CONTROLLER.md`)
- [ ] ColumnController 接口文档（迭代 2 完成）
- [ ] JobCardController 接口文档（迭代 3 完成）

---

### 阶段 3：接口完善 ✅（已完成）

在所有增量迭代完成后进行：

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 3.1 | 全局异常处理 GlobalExceptionHandler | ✅ | 统一异常响应 |
| 3.2 | 参数校验 @Valid | ⏳ | 待后续需要时添加（需引入 Hibernate Validator） |
| 3.3 | 自定义异常类 | ✅ | BusinessException 等 |
| 3.4 | 日志记录 SLF4J | ✅ | 添加操作日志 |

**接口实现状态：**

| 方法 | 路径 | 说明 | 状态 | 所属迭代 |
|------|------|------|------|----------|
| POST | /board/load | 加载看板完整数据 | ✅ | 迭代 1 |
| POST | /board/create | 创建看板 | ✅ | 迭代 1 |
| POST | /board/column/update | 更新列 | ✅ | 迭代 2 |
| POST | /board/card/create | 新建卡片 | ✅ | 迭代 3 |
| POST | /board/card/update | 更新卡片 | ✅ | 迭代 3 |
| POST | /board/card/move | 移动卡片 | ✅ | 迭代 3 |
| POST | /board/card/delete | 软删除卡片 | ✅ | 迭代 3 |

---

### 阶段 4：认证与联调（第 8 天起）🔄

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 4.1 | JWT 工具类 JwtUtil | ✅ | Token 生成与解析 |
| 4.2 | JWT 过滤器 | ✅ | 请求拦截验证 |
| 4.3 | 登录接口 | ✅ | /auth/login |
| 4.4 | Controller 集成 JWT | ✅ | 从 Header 提取 userId |
| 4.5 | 用户表创建 | ✅ | User 实体 + Repository |
| 4.6 | 登录接口完善 | ✅ | 用户验证 + 自动创建 |
| 4.7 | Swagger 修复 | ✅ | SpringDoc 升级至 2.8.5 |
| 4.8 | 与前端联调 | ⏳ | 待进行 |

---

## 当前工作焦点

**当前阶段：** 阶段 4 - JWT 认证与联调 🔄

**已完成：**
1. ✅ 添加 JWT 依赖 (jjwt)
2. ✅ 创建 JwtUtil 工具类
3. ✅ 创建 JWT 过滤器 (JwtAuthenticationFilter)
4. ✅ 创建登录接口 (/auth/login)
5. ✅ Controller 从 Header 提取 userId

**待进行：**
- 与前端联调
- Bug 修复与优化

---

## 技术债务/待优化项

| 项目 | 优先级 | 状态 | 说明 |
|------|--------|------|------|
| MyBatis 联表查询优化 | 中 | ✅ 已完成 | loadBoard 已使用 MyBatis 一次联表查询，JPA 方案已注释保留 |
| 自定义异常类 | 低 | ⏳ | 目前使用 RuntimeException，可创建业务异常类 |
| 日志记录 | 低 | ⏳ | 添加 SLF4J 日志记录 |
| 参数校验 | 中 | ⏳ | 添加 @Valid 注解进行请求参数校验 |

---

## 文档清单

| 文档 | 说明 |
|------|------|
| `docs/BACKEND_DEVELOPMENT_GUIDE.md` | 后端开发指南（API定义） |
| `docs/DATABASE_SCHEMA.md` | 数据库表结构 |
| `docs/TASK_PROGRESS.md` | 本文件，任务进度表 |
| `docs/API_BOARD_CONTROLLER.md` | BoardController 接口文档 |
| `docs/API_TESTING_GUIDE.md` | API 测试指南 |

### HTTP 测试文件

| 文件 | 说明 |
|------|------|
| `src/test/http/board-api-test.http` | 看板接口 HTTP 测试文件（新增） |

---

## 文件清单

### 已完成文件

```
src/main/java/com/example/myfirstspringboot/
├── Entity/
│   ├── Board.java ✅
│   ├── KanbanColumn.java ✅ (已简化，customAttributes 改为 String)
│   └── JobCard.java ✅ (已简化，tags/extra 改为 String)
├── repository/
│   ├── BoardRepository.java ✅
│   ├── KanbanColumnRepository.java ✅
│   ├── JobCardRepository.java ✅
│   └── UserRepository.java ✅ (阶段 4)
├── dto/request/
│   ├── CreateBoardRequest.java ✅
│   ├── LoadBoardRequest.java ✅
│   ├── CreateCardRequest.java ✅
│   ├── UpdateCardRequest.java ✅
│   ├── MoveCardRequest.java ✅
│   ├── DeleteCardRequest.java ✅
│   └── UpdateColumnRequest.java ✅
├── dto/response/
│   ├── BoardDto.java ✅
│   ├── ColumnDto.java ✅ (已简化，customAttributes 改为 String)
│   ├── JobCardDto.java ✅ (已简化，tags/extra 改为 String)
│   └── BoardDataDto.java ✅
├── service/
│   ├── BoardService.java ✅
│   ├── ColumnService.java ✅ (迭代 2)
│   ├── JobCardService.java ✅ (迭代 3)
│   └── impl/
│       ├── BoardServiceImpl.java ✅ (纯 JPA 方案)
│       ├── ColumnServiceImpl.java ✅ (迭代 2)
│       └── JobCardServiceImpl.java ✅ (迭代 3)
├── controller/
│   ├── BoardController.java ✅
│   ├── ColumnController.java ✅ (迭代 2)
│   ├── JobCardController.java ✅ (迭代 3)
│   └── AuthController.java ✅ (阶段 4)
├── Entity/
│   ├── Board.java ✅
│   ├── KanbanColumn.java ✅
│   ├── JobCard.java ✅
│   └── User.java ✅ (阶段 4)
├── exception/
│   ├── ApiResponse.java ✅
│   ├── BusinessException.java ✅ (阶段 3)
│   ├── GlobalExceptionHandler.java ✅ (阶段 3)
│   ├── ResourceNotFoundException.java ✅ (阶段 3)
│   └── UnauthorizedException.java ✅ (阶段 3)
├── util/
│   ├── DtoConverter.java ✅
│   └── JwtUtil.java ✅ (阶段 4)
└── config/
    ├── OpenApiConfig.java ✅
    ├── BrowserLauncher.java ✅
    ├── JwtAuthenticationFilter.java ✅ (阶段 4)
    └── WebConfig.java ✅ (阶段 4)
```

### 已删除文件（MyBatis 相关）

```
❌ config/MyBatisConfig.java
❌ config/JsonbTypeHandler.java
❌ config/StringArrayTypeHandler.java
❌ config/UuidTypeHandler.java
❌ mapper/BoardMapper.java
❌ mapper/BoardMapper.xml
```

### 测试文件

```
src/test/java/com/example/myfirstspringboot/
└── service/impl/
    ├── BoardServiceImplTest.java ✅
    ├── ColumnServiceImplTest.java ✅ (迭代 2，11 个测试用例)
    └── JobCardServiceImplTest.java ✅ (迭代 3，14 个测试用例)
```

### HTTP 测试文件

```
src/test/http/
└── board-api-test.http ✅
```

---

## 里程碑

| 里程碑 | 目标日期 | 状态 |
|--------|----------|------|
| 基础架构搭建完成 | 第 2 天末 | ✅ 已完成 |
| 迭代 1：Board 模块 | 第 3 天末 | ✅ 已完成 |
| 迭代 2：Column 模块 | 第 4 天末 | ✅ 已完成 |
| 迭代 3：JobCard 模块 | 第 5 天末 | ✅ 已完成 |
| 接口完善（异常处理/日志） | 第 6 天末 | ✅ 已完成 |
| JWT 认证完成 | 第 8 天末 | ✅ 已完成 |
| 前端联调完成 | 第 10 天末 | ⏳ |

---

## 备注

- **开发流程**：遵循增量式开发，Service → 单元测试 → Controller → 接口测试
- **技术栈**：纯 JPA 方案（已移除 MyBatis）
- Service 层包含完整的业务逻辑，不是简单的 CRUD 转发
- 单元测试覆盖正常流程和异常场景
- 每个迭代完成后在 Swagger UI 验证接口
