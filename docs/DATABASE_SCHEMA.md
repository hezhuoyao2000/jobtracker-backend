# 数据库设计与 Entity 详解

> 本文档包含完整的数据库建表 SQL 语句和 JPA Entity 设计代码，供参考和维护使用

---

## 一、建表 SQL（PostgreSQL）

### 1.1 看板表 - board

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
```

### 1.2 列表列 - kanban_column

```sql
-- 列表列
CREATE TABLE "kanban_column" (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id            UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    is_default          BOOLEAN NOT NULL DEFAULT true,
    custom_attributes   JSONB,
    CONSTRAINT fk_column_board FOREIGN KEY (board_id) REFERENCES board(id)
);

CREATE INDEX idx_column_board_id ON "kanban_column"(board_id);
```

### 1.3 职位卡片 - job_card

```sql
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

---

## 二、JPA Entity 设计

### 2.1 Board

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

### 2.2 KanbanColumn

```java
@Entity
@Table(name = "kanban_column")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumn {
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

### 2.3 JobCard

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

> **说明**：
> - `JsonBinaryType` 来自 `io.hypersistence:hibernate-types` 依赖
> - `tags` 字段若用 `TEXT[]` 需额外配置 TypeHandler，新手可先用 `@Column(columnDefinition = "TEXT")` 存 JSON 字符串，再手动解析

---

## 三、Repository 层

### 3.1 JPA Repository

```java
// BoardRepository.java
public interface BoardRepository extends JpaRepository<Board, UUID> {
    List<Board> findByUserIdOrderByCreatedAtAsc(String userId);
    Optional<Board> findByIdAndUserId(UUID id, String userId);
}

// KanbanColumnRepository.java
public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, UUID> {
    List<KanbanColumn> findByBoardIdOrderBySortOrderAsc(UUID boardId);
    boolean existsByIdAndBoardId(UUID columnId, UUID boardId);
}

// JobCardRepository.java
public interface JobCardRepository extends JpaRepository<JobCard, UUID> {
    List<JobCard> findByBoardIdAndDeletedAtIsNull(UUID boardId);
    Optional<JobCard> findByIdAndDeletedAtIsNull(UUID id);
}
```

### 3.2 MyBatis Mapper（加载完整 BoardData）

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
<mapper namespace="com.example.myfirstspringboot.mapper.BoardMapper">
    <resultMap id="BoardDataMap" type="com.example.myfirstspringboot.dto.response.BoardDataResult">
        <association property="board" resultMap="BoardMap"/>
        <collection property="columns" resultMap="ColumnMap"/>
        <collection property="cards" resultMap="JobCardMap"/>
    </resultMap>

    <select id="findBoardDataByUserId" resultMap="BoardDataMap">
        SELECT b.*, c.*, j.*
        FROM board b
        LEFT JOIN "kanban_column" c ON c.board_id = b.id
        LEFT JOIN job_card j ON j.board_id = b.id AND j.deleted_at IS NULL
        WHERE b.user_id = #{userId}
        ORDER BY b.created_at ASC, c.sort_order ASC
        LIMIT 1
    </select>
</mapper>
```

> **说明**：联表结果需在 Service 中重组为 `BoardDataDto`，或定义 `BoardDataResult` 配合 MyBatis 嵌套映射。新手可先用 3 次 JPA 查询（board → columns → cards）实现，再尝试 MyBatis 联表。

---

## 四、依赖配置（pom.xml 片段）

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
