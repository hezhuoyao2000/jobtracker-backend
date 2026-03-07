# 任务进度表

> 最后更新：2026-03-05
> 状态说明：✅ 已完成 | 🔄 进行中 | ⏳ 待开始

---

## 开发阶段总览

| 阶段 | 名称 | 进度 | 状态 |
|------|------|------|------|
| 阶段 1 | 基础搭建 | 100% | ✅ 已完成 |
| 阶段 2 | 核心 CRUD | 40% | 🔄 进行中 |
| 阶段 3 | 接口与权限 | 0% | ⏳ 待开始 |
| 阶段 4 | 认证与联调 | 0% | ⏳ 待开始 |

---

## 详细任务清单

### 阶段 1：基础搭建（第 1-2 天）✅

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 1.1 | 创建 Spring Boot 项目 | ✅ | 已完成，使用 Spring Initializr |
| 1.2 | 配置 PostgreSQL 数据源 | ✅ | application.yaml 已配置 |
| 1.3 | 数据库建表 | ✅ | board, kanban_column, job_card 表已创建 |
| 1.4 | 编写 Entity 实体类 | ✅ | Board.java, KanbanColumn.java, JobCard.java |
| 1.5 | 配置 JPA | ✅ | application.yaml 已配置 |
| 1.6 | 配置 MyBatis | ✅ | pom.xml 依赖已添加，BoardMapper.java 已创建 |

**阶段 1 完成物：**
- [x] pom.xml（包含 JPA、MyBatis、PostgreSQL 依赖）
- [x] application.yaml（数据库和 JPA/MyBatis 配置）
- [x] Entity 层（3 个实体类）
- [x] Repository 层（3 个 Repository 接口）
- [x] Mapper 层（BoardMapper.java + BoardMapper.xml）

---

### 阶段 2：核心 CRUD（第 3-5 天）🔄

#### 2.1 Board 相关

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 2.1.1 | BoardService 接口 | ✅ | 已创建 |
| 2.1.2 | BoardServiceImpl 实现 | ✅ | createBoard(), loadBoard() 已完成 |
| 2.1.3 | BoardService 单元测试 | ✅ | 10 个测试用例已编写 |
| 2.1.4 | BoardController | ✅ | 已完成，含详细 JavaDoc 和 API 文档 |

#### 2.2 Column 相关

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 2.2.1 | ColumnService 接口 | ⏳ | 待创建 |
| 2.2.2 | ColumnServiceImpl 实现 | ⏳ | 待创建 |
| 2.2.3 | ColumnService 单元测试 | ⏳ | 待创建 |
| 2.2.4 | ColumnController | ⏳ | 待创建 |

#### 2.3 JobCard 相关

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 2.3.1 | JobCardService 接口 | ⏳ | 待创建 |
| 2.3.2 | JobCardServiceImpl 实现 | ⏳ | 待创建 |
| 2.3.3 | JobCardService 单元测试 | ⏳ | 待创建 |
| 2.3.4 | JobCardController | ⏳ | 待创建 |

#### 2.4 功能完成度

| 功能 | 状态 | 说明 |
|------|------|------|
| 创建看板 | ✅ | Service + Controller 完成 |
| 加载看板 | ✅ | Service + Controller 完成 |
| 创建列 | ⏳ | 默认列创建已实现，独立接口待开发 |
| 更新列 | ⏳ | 待开发 |
| 创建卡片 | ⏳ | 待开发 |
| 更新卡片 | ⏳ | 待开发 |
| 移动卡片 | ⏳ | 待开发 |
| 删除卡片（软删除） | ⏳ | 待开发 |

**API 文档：**
- [x] BoardController 接口文档 (`docs/API_BOARD_CONTROLLER.md`)

---

### 阶段 3：接口与权限（第 6-7 天）🔄

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 3.1 | BoardController | ✅ | 已完成，包含详细 JavaDoc |
| 3.2 | 统一响应格式 ApiResponse | ✅ | 已创建 |
| 3.3 | 全局异常处理 GlobalExceptionHandler | ⏳ | 待创建 |
| 3.4 | REST 接口完整实现 | 🔄 | 进行中 |
| 3.5 | JWT 预留（userId 写死测试） | ✅ | 已在 Controller 中预留 TODO |
| 3.6 | Swagger/OpenAPI 文档 | ✅ | SpringDoc 集成完成，含 DTO 注解 |
| 3.7 | 启动自动打开浏览器 | ✅ | 自动打开 Swagger UI |

**接口实现状态：**

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | /board/load | 加载看板完整数据 | ✅ |
| POST | /board/create | 创建看板 | ✅ |
| POST | /board/card/create | 新建卡片 | ⏳ |
| POST | /board/card/update | 更新卡片 | ⏳ |
| POST | /board/card/move | 移动卡片 | ⏳ |
| POST | /board/card/delete | 软删除卡片 | ⏳ |
| POST | /board/column/update | 更新列 | ⏳ |

---

### 阶段 4：认证与联调（第 8 天起）⏳

| 序号 | 任务 | 状态 | 备注 |
|------|------|------|------|
| 4.1 | 集成 JWT 认证 | ⏳ | 待开发 |
| 4.2 | 与前端联调 | ⏳ | 待进行 |
| 4.3 | Bug 修复与优化 | ⏳ | 待进行 |

---

## 当前工作焦点

**正在进行：** 阶段 2 - 核心 CRUD

**下一步任务：**
1. 完成 ColumnService（更新列功能）
2. 完成 JobCardService（CRUD 功能）
3. 创建 BoardController 并测试接口

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
| `docs/API_TESTING_GUIDE.md` | API 测试指南（新增） |

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
│   ├── KanbanColumn.java ✅
│   └── JobCard.java ✅
├── repository/
│   ├── BoardRepository.java ✅
│   ├── KanbanColumnRepository.java ✅
│   └── JobCardRepository.java ✅
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
│   ├── ColumnDto.java ✅
│   ├── JobCardDto.java ✅
│   └── BoardDataDto.java ✅
├── mapper/
│   ├── BoardMapper.java ✅
│   └── BoardMapper.xml ✅
├── service/
│   ├── BoardService.java ✅
│   └── impl/BoardServiceImpl.java ✅
├── exception/
│   └── ApiResponse.java ✅
├── util/
│   └── DtoConverter.java ✅
└── config/
    ├── MyBatisConfig.java ✅
    ├── OpenApiConfig.java ✅
    └── BrowserLauncher.java ✅
```

### 测试文件

```
src/test/java/com/example/myfirstspringboot/
└── service/impl/
    └── BoardServiceImplTest.java ✅
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
| Board 核心功能完成 | 第 3 天末 | 🔄 进行中 |
| 全部 CRUD 完成 | 第 5 天末 | ⏳ |
| REST 接口完成 | 第 7 天末 | ⏳ |
| JWT 认证完成 | 第 8 天末 | ⏳ |
| 前端联调完成 | 第 10 天末 | ⏳ |

---

## 备注

- 开发过程中遵循开发指南的命名规范和分层架构
- Service 层包含完整的业务逻辑，不是简单的 CRUD 转发
- 单元测试覆盖正常流程和异常场景
- **JPA 与 MyBatis 共存**：简单 CRUD 用 JPA，复杂查询用 MyBatis
- **loadBoard 方法**：默认使用 MyBatis 联表查询（1 次 SQL），JPA 方案（3 次查询）已注释保留供学习对比
