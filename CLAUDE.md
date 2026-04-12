# CLAUDE.md

本文件为 Claude Code 在本仓库中工作时提供最小但准确的上下文说明。

## 项目概览

- 技术栈：Java 17、Spring Boot 3.5.5、MyBatis-Plus、PostgreSQL、SpringDoc OpenAPI、JWT、Kafka、MQTT、Redis、InfluxDB。
- 主业务：求职看板后端，包含认证、看板、列、卡片相关接口。
- 次业务：IoT 数据接入链路，当前代码已包含 Modbus -> MQTT 与 MQTT -> Kafka 两段。
- 构建工具：Maven Wrapper，Windows 下使用 `mvnw.cmd`。

## 构建与运行

```bash
# 构建项目
mvnw.cmd clean package

# 启动应用
mvnw.cmd spring-boot:run

# 带数据库密码启动
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_PASSWORD=your_password"

# 运行全部测试
mvnw.cmd test

# 运行单个测试类
mvnw.cmd test -Dtest=BoardServiceImplTest

# 运行 IoT 相关测试
mvnw.cmd test -Dtest=ModbusGatewayServiceTest,ModbusGatewayIntegrationTest
```

应用启动后可访问：

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

如果不希望启动时自动打开浏览器，可在 `application.yaml` 中设置 `app.auto-open-browser=false`。

## API 文档

项目已集成 SpringDoc OpenAPI。

- Swagger UI：用于直接调试接口
- `/v3/api-docs`：用于查看原始 OpenAPI JSON

涉及接口变更时，优先以控制器源码和运行后的 Swagger 页面为准，不要只相信旧文档。

## 架构概览

### 技术结构

- **Java 17 + Spring Boot 3.5.5**
- **MyBatis-Plus**：当前仓库的主要数据访问方案
- **PostgreSQL**：主数据库，使用 UUID、JSON/文本扩展字段等
- **Lombok**：如 `@RequiredArgsConstructor`
- **SpringDoc OpenAPI**：接口文档

### 分层结构

```text
Controller -> Service(interface + impl) -> Mapper -> Database
                  |
                  -> DTO(request/response)
```

### 实际目录结构

源码目录存在一些非标准命名，修改时必须尊重现状：

```text
src/main/java/com/example/myfirstspringboot/
  Controller/         # 注意是大写 C
  Entity/             # 注意是大写 E
  config/
  dto/request/
  dto/response/
  exception/
  mapper/
  service/
  service/impl/
  util/

src/main/java/com/example/iot/
  config/
  gateway/
  ingestion/
  model/

src/test/java/com/example/myfirstspringboot/
src/test/java/com/example/iot/
```

### 关键实现约定

- **Service 层**：采用接口 + 实现类模式，如 `BoardService` / `BoardServiceImpl`
- **依赖注入**：优先使用构造器注入，通常通过 Lombok `@RequiredArgsConstructor`
- **数据访问**：当前实现基于 MyBatis-Plus 和 Mapper，不是纯 JPA Repository
- **统一响应**：控制器统一返回 `ApiResponse<T>`
- **认证方式**：JWT 过滤器处理认证，控制器从 request attribute 中读取 `userId`
- **逻辑删除**：`JobCard.deletedAt` 通过 MyBatis-Plus 的 `@TableLogic` 处理

## 主要模块

### 认证模块

核心类：

- `AuthController`
- `AuthService`
- `AuthServiceImpl`
- `JwtUtil`
- `JwtAuthenticationFilter`

当前主要接口：

- `POST /auth/register`
- `POST /auth/login`

### 看板模块

核心类：

- `BoardController`
- `ColumnController`
- `JobCardController`
- `BoardServiceImpl`
- `ColumnServiceImpl`
- `JobCardServiceImpl`

注意：当前控制器根路径实际是 `/board`，不是旧文档里常见的 `/api/boards` 风格。

常见接口包括：

- `POST /board/load`
- `POST /board/create`
- 卡片和列操作由 board 相关控制器继续承载

### 数据访问层

核心 Mapper：

- `BoardMapper`
- `KanbanColumnMapper`
- `JobCardMapper`
- `UserMapper`

实体类位于 `Entity/`，并使用 MyBatis-Plus 注解：

- `@TableName`
- `@TableField`
- `@TableId`
- `@TableLogic`

### IoT 模块

当前已存在的关键类：

- `ModbusGatewayService`
- `MockModbusGatewayService`
- `MqttIngestionService`
- `IotProperties`
- `MqttConfig`
- `MqttConsumerConfig`
- `KafkaConfig`
- `InfluxDbConfig`

当前链路以配置项为准：

- MQTT topic：`devices/data`
- Kafka topic：`device-data`

## 数据与配置

主配置文件：

- `src/main/resources/application.yaml`

当前默认配置中值得注意的部分：

- PostgreSQL：`jdbc:postgresql://localhost:${DB_PORT:2015}/${DB_NAME:mysite_dev}`
- 数据库用户默认值：`${DB_USER:hezhuoyao}`
- 数据库密码来自：`${DB_PASSWORD:}`
- Redis：`localhost:6379`
- Kafka：`localhost:9092`
- MQTT：`tcp://localhost:1883`
- InfluxDB：`http://localhost:8086`

文档和浏览器自动打开行为配置在：

- `springdoc.*`
- `app.auto-open-browser`
- `app.swagger-path`

## 数据模型说明

核心业务表仍围绕以下三张表展开：

- `board`
- `kanban_column`
- `job_card`

默认列名称需保持一致：

- `Wish list`
- `Applied`
- `Interviewing`
- `Offered`
- `Rejected`

如果修改看板或卡片逻辑，注意不要破坏默认列的初始化行为。

## 开发规范

### 代码风格

- 类名使用 `PascalCase`
- 方法和变量使用 `camelCase`
- 常量使用 `SCREAMING_SNAKE_CASE`
- 类型明显时可酌情使用 `var`
- 集合处理可以使用 Stream，但不要为了 Stream 而 Stream

### Service 层

- 业务逻辑放在 Service 层，不要让 Controller 直接堆逻辑
- 涉及多步原子操作时使用 `@Transactional`
- 查询型逻辑优先使用 `@Transactional(readOnly = true)`

### 控制器层

- 沿用现有 `ApiResponse<T>` 包装
- 沿用现有异常体系，如 `BusinessException`、`ResourceNotFoundException`、`UnauthorizedException`
- 新增接口前，先检查当前控制器路径风格，避免混入另一套 URL 体系

### 持久层

- 新增数据访问逻辑时，优先匹配当前 MyBatis-Plus/Mapper 风格
- 不要在未统一重构前混入新的 JPA Repository 模式
- 不要擅自重命名 `Controller/`、`Entity/` 这类已有非常规目录

## 测试

当前已有测试包括：

- `BoardServiceImplTest`
- `ColumnServiceImplTest`
- `JobCardServiceImplTest`
- `ModbusGatewayServiceTest`
- `ModbusGatewayIntegrationTest`
- `IotTestApplication`

建议：

- 改一个 Service，优先跑对应单测
- 改 IoT 链路，至少跑相关单测或集成测试

## 已知注意事项

- 仓库内部分旧文档存在编码损坏，源码和 `application.yaml` 才是事实来源
- 旧文档里有些内容写成了 JPA Repository，但当前仓库真实实现是 Mapper 风格
- 旧文档里有些接口示例使用 `/api/...`，但源码中的控制器路径实际为 `/auth` 和 `/board`
- 该仓库同时包含业务后端与 IoT 配置，改动共享配置时要谨慎

## 参考文档

- `docs/BACKEND_DEVELOPMENT_GUIDE.md`
- `docs/DATABASE_SCHEMA.md`
- `docs/IOT-ARCHITECTURE.md`
- `docs/JAVA-DEV-GUIDE.md`
- `.aiassistant/rules/springrules.md`
