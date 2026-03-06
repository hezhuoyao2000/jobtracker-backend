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
│   ├── entity/                    # JPA 实体（对应数据库表）
│   │   ├── Board.java
│   │   ├── Column.java
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
│   ├── repository/                # 数据访问层
│   │   ├── BoardRepository.java       # JPA
│   │   ├── ColumnRepository.java      # JPA
│   │   └── JobCardRepository.java     # JPA
│   ├── mapper/                    # MyBatis Mapper（XML）
│   │   └── BoardMapper.java
│   ├── service/                   # 业务逻辑层
│   │   ├── BoardService.java
│   │   ├── ColumnService.java
│   │   └── JobCardService.java
│   ├── controller/                # 控制器
│   │   └── BoardController.java
│   └── exception/                 # 异常与统一响应
│       ├── GlobalExceptionHandler.java
│       └── ApiResponse.java
├── src/main/resources/
│   ├── application.yml
│   └── mapper/
│       └── BoardMapper.xml
└── pom.xml
```

---

## 四、数据库设计

### 4.1 建表 SQL（PostgreSQL）

```sql
-- 看板表
CREATE TABLE board (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL DEFAULT 'My Job Tracker',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_board_user_id ON board(user_id);

-- 列表
CREATE TABLE "kanban_column" (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id            UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    sort_order             INT NOT NULL DEFAULT 0,
    is_default          BOOLEAN NOT NULL DEFAULT true,
    custom_attributes   JSONB,
    CONSTRAINT fk_column_board FOREIGN KEY (board_id) REFERENCES board(id)
);

CREATE INDEX idx_column_board_id ON "kanban_column"(board_id);

-- 职位卡片表
CREATE TABLE job_card (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id        UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    status_id       UUID NOT NULL REFERENCES "kanban_column"(id),
    job_title       VARCHAR(500) NOT NULL,
    company_name    VARCHAR(255) NOT NULL,
    job_link        VARCHAR(1000),
    source_platform VARCHAR(100),
    expired         BOOLEAN DEFAULT false,
    job_location    VARCHAR(255),
    description     TEXT,
    applied_time    TIMESTAMP WITH TIME ZONE,
    tags            TEXT[],
    comments        TEXT,
    extra           JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_job_card_board   FOREIGN KEY (board_id) REFERENCES board(id),
    CONSTRAINT fk_job_card_status  FOREIGN KEY (status_id) REFERENCES "kanban_column"(id)
);

CREATE INDEX idx_job_card_board_id   ON job_card(board_id);
CREATE INDEX idx_job_card_deleted_at ON job_card(deleted_at);
```

### 4.2 字段说明

| 表 | 字段                      | 说明                       |
|----|-------------------------|--------------------------|
| board | user_id                 | 归属用户（后续对应用户表）            |
| board | created_at / updated_at | 由数据库或应用层维护               |
| column | sort_order              | 列排序                      |
| job_card | status_id               | 外键指向 column.id           |
| job_card | deleted_at              | 软删除，NULL 表示未删除           |
| job_card | tags                    | 数组类型，PostgreSQL 用 TEXT[] |
| job_card | extra                   | JSON 类型，用 JSONB 存储       |

---

## 五、Entity 设计（JPA）

### 5.1 Board

```java
@Entity
@Table(name = "board")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### 5.2 Column

```java
@Entity
@Table(name = "column")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Column {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Type(JsonBinaryType.class)  // 需要依赖 hibernate-types
    @Column(name = "custom_attributes", columnDefinition = "jsonb")
    private Map<String, Object> customAttributes;
}
```

### 5.3 JobCard

```java
@Entity
@Table(name = "job_card")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCard {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "status_id", nullable = false)
    private UUID statusId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "job_link")
    private String jobLink;

    @Column(name = "source_platform")
    private String sourcePlatform;

    private Boolean expired;

    @Column(name = "job_location")
    private String jobLocation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "applied_time")
    private Instant appliedTime;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "text[]")  // 或使用 @Convert
    private List<String> tags;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> extra;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

> 说明：`JsonBinaryType` 来自 `io.hypersistence:hibernate-types`；`tags` 若用 `TEXT[]` 需额外配置 TypeHandler，新手可先用 `@Column(columnDefinition = "TEXT")` 存 JSON 字符串，再手动解析。

---

## 六、Repository 层

### 6.1 JPA Repository

```java
// BoardRepository.java
public interface BoardRepository extends JpaRepository<Board, UUID> {
    List<Board> findByUserIdOrderByCreatedAtAsc(String userId);
    Optional<Board> findByIdAndUserId(UUID id, String userId);
}

// ColumnRepository.java
public interface ColumnRepository extends JpaRepository<Column, UUID> {
    List<Column> findByBoardIdOrderByOrderAsc(UUID boardId);
    boolean existsByIdAndBoardId(UUID columnId, UUID boardId);
}

// JobCardRepository.java
public interface JobCardRepository extends JpaRepository<JobCard, UUID> {
    List<JobCard> findByBoardIdAndDeletedAtIsNull(UUID boardId);
    Optional<JobCard> findByIdAndDeletedAtIsNull(UUID id);
}
```

### 6.2 MyBatis Mapper（加载完整 BoardData）

```java
// BoardMapper.java
@Mapper
public interface BoardMapper {
    BoardDataResult findBoardDataByUserId(@Param("userId") String userId);
    BoardDataResult findBoardDataByBoardId(@Param("boardId") UUID boardId, @Param("userId") String userId);
}
```

```xml
<!-- BoardMapper.xml -->
<resultMap id="BoardDataMap" type="com.example.myfirstspringboot.dto.response.BoardDataResult">
    <association property="board" resultMap="BoardMap"/>
    <collection property="columns" resultMap="ColumnMap"/>
    <collection property="cards" resultMap="JobCardMap"/>
</resultMap>

<select id="findBoardDataByUserId" resultMap="BoardDataMap">
    SELECT b.*, c.*, j.*
    FROM board b
    LEFT JOIN "column" c ON c.board_id = b.id
    LEFT JOIN job_card j ON j.board_id = b.id AND j.deleted_at IS NULL
    WHERE b.user_id = #{userId}
    ORDER BY b.created_at ASC, c."order" ASC
    LIMIT 1
</select>
```

> 联表结果需在 Service 中重组为 `BoardDataDto`，或定义 `BoardDataResult` 配合 MyBatis 嵌套映射。新手可先用 3 次 JPA 查询（board → columns → cards）实现，再尝试 MyBatis 联表。

---

## 七、Service 层业务逻辑

### 7.1 加载看板（LoadBoard）

```
输入：userId（JWT）、可选 boardId
1. 若 boardId 为空：取该用户第一个 board
2. 若 boardId 不为空：校验 board 属于 userId
3. 查 columns：WHERE board_id = ?
4. 查 cards：WHERE board_id = ? AND deleted_at IS NULL
5. 组装 BoardDataDto 返回
```

### 7.2 创建看板（CreateBoard）

```
输入：name、userId
1. 创建 Board 实体，设置 userId、name
2. save(board)
3. 插入默认 5 列：Wish list, Applied, Interviewing, Offered, Rejected
4. 返回 BoardDto
```

### 7.3 创建卡片（CreateCard）

```
输入：CreateCardRequest、userId
1. 校验 board 属于 userId
2. 校验 statusId 对应的 column 属于该 board
3. 构建 JobCard，设置 boardId、statusId、jobTitle、companyName 等
4. save(jobCard)
5. 返回 JobCardDto
```

### 7.4 更新卡片（UpdateCard）

```
输入：UpdateCardRequest、userId
1. 查 jobCard，校验其 board 属于 userId
2. 若更新 statusId，校验新 column 属于该 board
3. 只更新传入的非空字段
4. save(jobCard)
5. 返回 JobCardDto
```

### 7.5 移动卡片（MoveCard）

```
输入：cardId、targetStatusId、userId
1. 查 jobCard，校验其 board 属于 userId
2. 校验 targetStatusId 对应的 column 属于该 board
3. jobCard.setStatusId(targetStatusId)
4. save(jobCard)
5. 返回 JobCardDto
```

### 7.6 软删除卡片（DeleteCard）

```
输入：cardId、userId
1. 查 jobCard，校验其 board 属于 userId
2. jobCard.setDeletedAt(Instant.now())
3. save(jobCard)
4. 返回成功
```

### 7.7 更新列（UpdateColumn）

```
输入：UpdateColumnRequest、userId
1. 查 column，通过 boardId 校验 board 属于 userId
2. 只更新传入的非空字段
3. save(column)
4. 返回 ColumnDto
```

---

## 八、Controller 接口定义

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

## 九、DTO 与日期格式

### 9.1 约定

- 请求/响应中的日期字段使用 **ISO 8601 字符串**（如 `2026-02-20T10:30:00Z`）
- Entity 内部使用 `Instant`，在 Service 或 Mapper 中转换为字符串

### 9.2 转换示例

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

## 十、依赖配置（pom.xml 片段）

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

## 十一、application.yml 示例

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

## 十二、开发顺序建议

1. **建库建表** → 执行建表 SQL
2. **Entity** → Board、Column、JobCard
3. **Repository** → 先用 JPA 实现基础查询
4. **Service** → 实现 LoadBoard、CreateBoard、CreateCard
5. **Controller** → 暴露接口，用 Postman 测试
6. **完善** → 其余接口、异常处理、统一响应
7. **MyBatis** → 可选：用 MyBatis 重写 LoadBoard 联表查询

---

## 十三、与前端类型对照

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

## 十四、常见问题

1. **PostgreSQL 中 `order` 为保留字**：建表、写 SQL 时用双引号 `"order"`
2. **UUID 类型**：Java 用 `java.util.UUID`，数据库用 `UUID`
3. **JSONB / 数组**：可用 `hibernate-types` 或自定义 `AttributeConverter`
4. **软删除**：查询时统一加 `deleted_at IS NULL` 条件
