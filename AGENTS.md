# AGENTS.md

本文件为 Codex CLI / Code Agent 在本仓库中工作的统一说明。

## 项目概览

- 技术栈：Java 17、Spring Boot 3.5.5、MyBatis-Plus、PostgreSQL、SpringDoc OpenAPI、JWT、Kafka、MQTT、Redis、InfluxDB。
- 主业务：求职看板后端，包含认证、看板、列、卡片接口。
- IoT 子模块：当前已实现 Modbus -> MQTT 与 MQTT -> Kafka。
- 构建工具：Maven Wrapper，Windows 下使用 `.\mvnw.cmd`。

## 构建与运行

```bash
# 构建
.\mvnw.cmd clean package

# 启动应用
.\mvnw.cmd spring-boot:run

# 带数据库密码启动
.\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_PASSWORD=your_password"

# 运行全部测试
.\mvnw.cmd test

# 运行单个测试类
.\mvnw.cmd test -Dtest=BoardServiceImplTest

# 运行 IoT 测试
.\mvnw.cmd test -Dtest=ModbusGatewayServiceTest,ModbusGatewayIntegrationTest
```

启动后文档地址：

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 目录与结构

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

## 关键约定

- 当前仓库真实持久层方案是 `MyBatis-Plus + Mapper`，不是 JPA Repository。
- 控制器统一返回 `ApiResponse<T>`。
- 认证链路依赖 JWT 过滤器，部分控制器从 request attribute 中读取 `userId`。
- `JobCard.deletedAt` 使用 MyBatis-Plus 逻辑删除。
- 默认看板列必须保持一致：
  `Wish list`、`Applied`、`Interviewing`、`Offered`、`Rejected`。
- IoT topic 保持与配置一致：
  MQTT `devices/data`，Kafka `device-data`。

## 编码规则

- 不要擅自重命名现有非常规目录或包名，例如 `Controller/`、`Entity/`。
- 继续使用构造器注入，优先 `@RequiredArgsConstructor`。
- 新增持久层逻辑时，优先匹配现有 Mapper 风格，不要混入新的 Repository 模式。
- 修改接口时先以源码为准，不要直接相信旧文档中的 `/api/...` 示例。

## 注释规则

- 新增或重写文件时，必须补充“必要的注释”，不能只写裸代码。
- 类需要有简短块级注释，说明职责和所在阶段或用途。
- 公开方法、测试方法、关键辅助方法需要有函数块级注释，说明输入、行为和验证目标。
- 复杂判断、异步处理、测试同步等待、关键 mock/stub、关键资源初始化，需要补充必要的行内注释。
- 注释要解释“为什么这样做”或“这一段在保证什么”，不要写无信息量废话。
- 注释默认使用中文，除非文件本身已经明确采用英文风格。

## 测试规则

- 修改一个 Service，优先补对应单元测试。
- 涉及外部集成链路时，补对应集成测试。
- 单元测试优先 Mockito；集成测试优先真实链路或最接近真实运行方式的验证。
- 如果测试依赖外部基础设施，要在最终说明里明确写出依赖项和执行方式。

## 已知问题

- 仓库内部分旧文档存在编码损坏，源码和 `application.yaml` 才是事实来源。
- 旧文档有些地方仍写成 JPA Repository，但当前实现不是。
- 旧文档有些接口路径示例与当前控制器实现不一致。

## 参考文档

- `CLAUDE.md`
- `docs/BACKEND_DEVELOPMENT_GUIDE.md`
- `docs/DATABASE_SCHEMA.md`
- `docs/IOT-ARCHITECTURE.md`
- `docs/JAVA-DEV-GUIDE.md`
- `.aiassistant/rules/springrules.md`
