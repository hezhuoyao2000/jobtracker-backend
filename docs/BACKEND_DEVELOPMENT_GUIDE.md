# myfirstspringboot 后端开发计划与指南

> 技术栈：Java + Spring Boot + MyBatis + Spring Data JPA + PostgreSQL  
> 面向 Java 后端新手，建议手写代码以熟悉流程

---

## 一、项目概述

### 1.1 业务说明

职位跟踪看板应用，用户可创建看板、在列中管理职位卡片（投递状态：Wish list → Applied → Interviewing → Offered → Rejected）。

### 1.2 技术选型说明

| 技术 | 用途 |
|------|------|
| Spring Boot | 应用框架 |
| Spring Data JPA | 简单 CRUD、实体映射、Repository |
| MyBatis | 复杂查询、联表、自定义 SQL（练习手写） |
| PostgreSQL | 关系型数据库 |
| JWT | 用户认证（后续接入） |

**JPA 与 MyBatis 分工建议**：
- **JPA**：单表 CRUD、按 id/boardId 查询、简单关联
- **MyBatis**：加载完整 BoardData（board + columns + cards）、联表查询、批量操作

---

## 二、开发阶段规划

### 阶段 1：基础搭建（第 1–2 天）

1. 创建 Spring Boot 项目（Spring Initializr）
2. 配置 PostgreSQL 数据源
3. 建表、编写 Entity
4. 配置 JPA + MyBatis 共存

### 阶段 2：核心 CRUD（第 3–5 天）

1. Board 的创建、查询
2. Column 的创建、查询（含默认列初始化）
3. JobCard 的创建、更新、软删除、移动

### 阶段 3：接口与权限（第 6–7 天）

1. 实现全部 REST 接口
2. 统一响应格式、异常处理
3. 预留 JWT 校验（可先写死 userId 测试）

### 阶段 4：认证与联调（第 8 天起）

1. 集成 JWT
2. 与前端联调

---

## 三、项目结构

```
myfirstspringboot/
├── src/main/java/com/example/myfirstspringboot/
│   ├── MyfirstspringbootApplication.java
│   ├── config/                    # 配置类
│   │   └── MyBatisConfig.java
│   ├── Entity/                    # JPA 实体（对应数据库表）
│   │   ├── Board.java
│   │   ├── KanbanColumn.java
│   │   └── JobCard.java
│   ├── dto/                       # 请求/响应 DTO
│   │   ├── request/
│   │   │   ├── CreateBoardRequest.java
│   │   │   ├── CreateCardRequest.java
│   │   │   ├── UpdateCardRequest.java
│   │   │   ├── MoveCardRequest.java
│   │   │   ├── DeleteCardRequest.java
│   │   │   └── UpdateColumnRequest.java
│   │   └── response/
│   │       ├── BoardDto.java
│   │       ├── ColumnDto.java
│   │       ├── JobCardDto.java
│   │       └── BoardDataDto.java
│   ├── repository/                # 数据访问层（JPA Repository）
│   │   ├── BoardRepository.java
│   │   ├── KanbanColumnRepository.java
│   │   └── JobCardRepository.java
│   ├── mapper/                    # MyBatis Mapper 接口
│   │   └── BoardMapper.java
│   ├── service/                   # 业务逻辑层（接口定义）
│   │   ├── BoardService.java
│   │   ├── ColumnService.java
│   │   └── JobCardService.java
│   │   └── impl/                  # Service 实现类
│   │       ├── BoardServiceImpl.java
│   │       ├── ColumnServiceImpl.java
│   │       └── JobCardServiceImpl.java
│   ├── controller/                # 控制器
│   │   └── BoardController.java
│   ├── exception/                 # 异常与统一响应
│   │   ├── GlobalExceptionHandler.java
│   │   └── ApiResponse.java
│   └── util/                      # 工具类
│       └── DtoConverter.java
├── src/main/resources/
│   ├── application.yml
│   └── mapper/
│       └── BoardMapper.xml
├── src/test/
│   └── java/com/example/myfirstspringboot/
│       ├── MyfirstspringbootApplicationTests.java
│       └── service/
│           └── impl/
│               └── BoardServiceImplTest.java
└── pom.xml
```

### 目录说明

| 目录 | 说明 |
|------|------|
| `config/` | Spring 配置类，如 MyBatis 配置、JPA 配置等 |
| `Entity/` | JPA 实体类，映射数据库表结构 |
| `dto/` | 数据传输对象，分为 `request/` 和 `response/` |
| `repository/` | JPA Repository 接口，负责单表 CRUD |
| `mapper/` | MyBatis Mapper 接口，负责复杂 SQL 查询 |
| `service/` | 业务逻辑层，接口定义在根目录，实现在 `impl/` 子目录 |
| `controller/` | REST 控制器，处理 HTTP 请求 |
| `exception/` | 全局异常处理和统一响应格式 |
| `util/` | 工具类，如 DTO 转换器 |
| `src/test/` | 单元测试目录，测试类与源码目录结构对应 |

---

## 四、数据库设计

完整的建表 SQL 和 Entity 设计代码已移至 [`DATABASE_SCHEMA.md`](./DATABASE_SCHEMA.md)，本文档仅保留字段功能说明。

### 4.1 表结构概览

| 表名 | 说明 | 主键 | 主要字段 |
|------|------|------|----------|
| `board` | 看板主表 | `id` (UUID) | `user_id`, `name`, `created_at`, `updated_at` |
| `kanban_column` | 看板列（状态列） | `id` (UUID) | `board_id`, `name`, `sort_order`, `is_default` |
| `job_card` | 职位卡片 | `id` (UUID) | `board_id`, `status_id`, `job_title`, `company_name`, `deleted_at` |

### 4.2 字段说明

| 表 | 字段 | 类型 | 说明 |
|----|------|------|------|
| board | `user_id` | VARCHAR(255) | 归属用户（后续对应用户表/JWT） |
| board | `created_at` / `updated_at` | TIMESTAMP WITH TIME ZONE | 由数据库或应用层维护 |
| board | `name` | VARCHAR(255) | 看板名称，默认值为 'My Job Tracker' |
| kanban_column | `sort_order` | INT | 列排序顺序 |
| kanban_column | `is_default` | BOOLEAN | 是否为系统默认列 |
| kanban_column | `custom_attributes` | JSONB | 自定义属性（扩展用途） |
| job_card | `status_id` | UUID | 外键指向 `kanban_column.id` |
| job_card | `deleted_at` | TIMESTAMP WITH TIME ZONE | 软删除字段，NULL 表示未删除 |
| job_card | `tags` | TEXT[] | 标签数组（PostgreSQL 特有类型） |
| job_card | `extra` | JSONB | 扩展字段，存储任意 JSON 数据 |
| job_card | `board_id` | UUID | 外键指向 `board.id` |

### 4.3 外键关系

```
board (1) ──→ (N) kanban_column (board_id)
board (1) ──→ (N) job_card (board_id)
kanban_column (1) ──→ (N) job_card (status_id)
```

- `kanban_column.board_id` → `board.id` (ON DELETE CASCADE)
- `job_card.board_id` → `board.id` (ON DELETE CASCADE)
- `job_card.status_id` → `kanban_column.id`

> 详细 SQL 建表语句请查看 [`DATABASE_SCHEMA.md`](./DATABASE_SCHEMA.md)

---

## 五、Entity 设计与 Repository 层

完整的 Entity 设计和 Repository 接口定义请查看 [`DATABASE_SCHEMA.md`](./DATABASE_SCHEMA.md)。

### 5.1 技术说明

- **JPA Entity**: 使用 `@Entity` 注解映射数据库表，`@Table` 指定表名
- **主键生成**: 使用 `GenerationType.UUID` 生成 UUID 类型主键
- **时间戳**: 使用 `Instant` 类型，通过 `@PrePersist` 和 `@PreUpdate` 自动维护
- **JSONB 支持**: 使用 `hibernate-types` 库的 `@Type(JsonBinaryType.class)`
- **软删除**: 通过 `deleted_at` 字段实现，查询时需添加 `deletedAt IS NULL` 条件

### 5.2 Repository 层说明

| Repository | 继承 | 主要方法 |
|------------|------|----------|
| `BoardRepository` | `JpaRepository<Board, UUID>` | `findByUserIdOrderByCreatedAtAsc`, `findByIdAndUserId` |
| `KanbanColumnRepository` | `JpaRepository<KanbanColumn, UUID>` | `findByBoardIdOrderBySortOrderAsc`, `existsByIdAndBoardId` |
| `JobCardRepository` | `JpaRepository<JobCard, UUID>` | `findByBoardIdAndDeletedAtIsNull`, `findByIdAndDeletedAtIsNull` |

> **注意**: Entity 类名使用 `KanbanColumn` 而非 `Column`，避免与 JPA 保留字冲突。

---

## 六、Service 层业务逻辑

### 6.1 加载看板（LoadBoard）

```
输入：userId（JWT）、可选 boardId
1. 若 boardId 为空：取该用户第一个 board
2. 若 boardId 不为空：校验 board 属于 userId
3. 查 columns：WHERE board_id = ?
4. 查 cards：WHERE board_id = ? AND deleted_at IS NULL
5. 组装 BoardDataDto 返回
```

### 6.2 创建看板（CreateBoard）

```
输入：name、userId
1. 创建 Board 实体，设置 userId、name
2. save(board)
3. 插入默认 5 列：Wish list, Applied, Interviewing, Offered, Rejected
4. 返回 BoardDto
```

### 6.3 创建卡片（CreateCard）

```
输入：CreateCardRequest、userId
1. 校验 board 属于 userId
2. 校验 statusId 对应的 column 属于该 board
3. 构建 JobCard，设置 boardId、statusId、jobTitle、companyName 等
4. save(jobCard)
5. 返回 JobCardDto
```

### 6.4 更新卡片（UpdateCard）

```
输入：UpdateCardRequest、userId
1. 查 jobCard，校验其 board 属于 userId
2. 若更新 statusId，校验新 column 属于该 board
3. 只更新传入的非空字段
4. save(jobCard)
5. 返回 JobCardDto
```

### 6.5 移动卡片（MoveCard）

```
输入：cardId、targetStatusId、userId
1. 查 jobCard，校验其 board 属于 userId
2. 校验 targetStatusId 对应的 column 属于该 board
3. jobCard.setStatusId(targetStatusId)
4. save(jobCard)
5. 返回 JobCardDto
```

### 6.6 软删除卡片（DeleteCard）

```
输入：cardId、userId
1. 查 jobCard，校验其 board 属于 userId
2. jobCard.setDeletedAt(Instant.now())
3. save(jobCard)
4. 返回成功
```

### 6.7 更新列（UpdateColumn）

```
输入：UpdateColumnRequest、userId
1. 查 column，通过 boardId 校验 board 属于 userId
2. 只更新传入的非空字段
3. save(column)
4. 返回 ColumnDto
```

---

## 七、Controller 接口定义

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /board/load | 加载看板完整数据 |
| POST | /board/create | 创建看板 |
| POST | /board/card/create | 新建卡片 |
| POST | /board/card/update | 更新卡片 |
| POST | /board/card/move | 移动卡片 |
| POST | /board/card/delete | 软删除卡片 |
| POST | /board/column/update | 更新列 |

> 所有接口暂不实现 DELETE 方法，删除操作用 POST + 软删除。

### 示例：BoardController

```java
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    @PostMapping("/load")
    public ApiResponse<BoardDataDto> loadBoard(
            @RequestBody LoadBoardRequest req,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String userId = extractUserIdFromJwt(auth);  // 暂可写死 "user-1"
        BoardDataDto data = boardService.loadBoard(userId, req.getBoardId());
        return ApiResponse.success(data);
    }

    @PostMapping("/create")
    public ApiResponse<BoardDto> createBoard(
            @RequestBody CreateBoardRequest req,
            @RequestHeader("Authorization") String auth) {
        String userId = extractUserIdFromJwt(auth);
        BoardDto dto = boardService.createBoard(userId, req.getName());
        return ApiResponse.success(dto);
    }
}
```

---

## 八、DTO 与日期格式

### 8.1 约定

- 请求/响应中的日期字段使用 **ISO 8601 字符串**（如 `2026-02-20T10:30:00Z`）
- Entity 内部使用 `Instant`，在 Service 或 Mapper 中转换为字符串

### 8.2 转换示例

```java
// Entity → dto
public static JobCardDto toDto(JobCard entity) {
    JobCardDto dto = new JobCardDto();
    dto.setId(entity.getId().toString());
    dto.setBoardId(entity.getBoardId().toString());
    dto.setStatusId(entity.getStatusId().toString());
    dto.setJobTitle(entity.getJobTitle());
    dto.setCompanyName(entity.getCompanyName());
    if (entity.getCreatedAt() != null) {
        dto.setCreatedAt(entity.getCreatedAt().toString());
    }
    // ... 其他字段
    return dto;
}
```

---

## 九、依赖配置（pom.xml 片段）

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.hypersistence</groupId>
        <artifactId>hypersistence-utils-hibernate-63</artifactId>
        <version>3.7.0</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 十、application.yml 示例

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myfirstspringboot
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # 开发可用 update，生产用 validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        default_schema: public
    database-platform: org.hibernate.dialect.PostgreSQLDialect

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.myfirstspringboot.entity
  configuration:
    map-underscore-to-camel-case: true
```

---

## 十一、开发顺序建议

1. **建库建表** → 执行建表 SQL
2. **Entity** → Board、Column、JobCard
3. **Repository** → 先用 JPA 实现基础查询
4. **Service** → 实现 LoadBoard、CreateBoard、CreateCard
5. **Controller** → 暴露接口，用 Postman 测试
6. **完善** → 其余接口、异常处理、统一响应
7. **MyBatis** → 可选：用 MyBatis 重写 LoadBoard 联表查询

---

## 十二、与前端类型对照

前端类型定义见：`src/app/services/types/backendtypes/backend.ts`  
后端 DTO 字段名、类型应与该文件保持一致，便于联调。

| 前端 DTO | 后端 Java 类 |
|----------|--------------|
| BoardDto | BoardDto |
| ColumnDto | ColumnDto |
| JobCardDto | JobCardDto |
| BoardDataDto | BoardDataDto |
| CreateCardRequestDto | CreateCardRequest |
| UpdateCardRequestDto | UpdateCardRequest |
| MoveCardRequestDto | MoveCardRequest |
| DeleteCardRequestDto | DeleteCardRequest |

---

## 十三、常见问题

1. **PostgreSQL 中 `order` 为保留字**：建表、写 SQL 时用双引号 `"order"`
2. **UUID 类型**：Java 用 `java.util.UUID`，数据库用 `UUID`
3. **JSONB / 数组**：可用 `hibernate-types` 或自定义 `AttributeConverter`
4. **软删除**：查询时统一加 `deleted_at IS NULL` 条件
