# AGENTS.md

This file gives Codex CLI agents the minimum working context for this repository.

## Project Summary

- Stack: Java 17, Spring Boot 3.5.5, MyBatis-Plus, PostgreSQL, SpringDoc OpenAPI, JWT, Kafka, MQTT, Redis, InfluxDB.
- Main domain: job-kanban backend with auth, board, column, and card APIs.
- Secondary domain: IoT ingestion pipeline. Current codebase includes Modbus -> MQTT and MQTT -> Kafka pieces.
- Build tool: Maven Wrapper (`mvnw.cmd` on Windows).

## Build And Run

```bash
# build
mvnw.cmd clean package

# run app
mvnw.cmd spring-boot:run

# run app with DB password
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_PASSWORD=your_password"

# run all tests
mvnw.cmd test

# run one test class
mvnw.cmd test -Dtest=BoardServiceImplTest

# run IoT tests
mvnw.cmd test -Dtest=ModbusGatewayServiceTest,ModbusGatewayIntegrationTest
```

API docs after startup:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

If the browser auto-open is noisy, set `app.auto-open-browser=false` in `application.yaml`.

## Real Project Structure

Source layout is not fully conventional. Follow the actual package names in the repo.

```text
src/main/java/com/example/myfirstspringboot/
  Controller/         # controllers; note uppercase C
  Entity/             # MyBatis-Plus entities; note uppercase E
  config/
  dto/request/
  dto/response/
  exception/
  mapper/             # MyBatis mapper interfaces
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

Important existing classes:

- Auth: `AuthController`, `AuthService`, `AuthServiceImpl`, `JwtUtil`, `JwtAuthenticationFilter`
- Board flow: `BoardController`, `ColumnController`, `JobCardController`
- Data layer: `BoardMapper`, `KanbanColumnMapper`, `JobCardMapper`, `UserMapper`
- DTO conversion: `DtoConverter`
- Errors: `ApiResponse`, `BusinessException`, `ResourceNotFoundException`, `UnauthorizedException`, `GlobalExceptionHandler`
- IoT: `ModbusGatewayService`, `MockModbusGatewayService`, `MqttIngestionService`

## Architecture Notes

- This codebase currently uses MyBatis-Plus and mapper interfaces, not Spring Data JPA repositories.
- Entities use MyBatis-Plus annotations such as `@TableName`, `@TableField`, `@TableId`, and `@TableLogic`.
- Service layer follows interface + implementation.
- Controllers return the shared `ApiResponse<T>` wrapper.
- Authentication is request-filter based. `BoardController` reads `userId` from request attributes set by JWT auth flow.
- `JobCard.deletedAt` uses logical delete via MyBatis-Plus: `@TableLogic(value = "NULL", delval = "NOW()")`.

## Main HTTP Surface

Current controller roots in code:

- `/auth`
- `/board`

Do not assume `/api/...` routes unless you verify them in source first.

Common operations:

- `POST /auth/register`
- `POST /auth/login`
- `POST /board/load`
- `POST /board/create`
- card and column operations are handled from the board-related controllers

## Data And Config

Main configuration file:

- `src/main/resources/application.yaml`

Current defaults worth knowing:

- PostgreSQL: `jdbc:postgresql://localhost:${DB_PORT:2015}/${DB_NAME:mysite_dev}`
- DB user default: `${DB_USER:hezhuoyao}`
- DB password comes from `${DB_PASSWORD:}`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- MQTT broker: `tcp://localhost:1883`
- InfluxDB: `http://localhost:8086`

OpenAPI and browser behavior are configured under:

- `springdoc.*`
- `app.auto-open-browser`
- `app.swagger-path`

## Testing

Existing test coverage includes:

- `BoardServiceImplTest`
- `ColumnServiceImplTest`
- `JobCardServiceImplTest`
- `ModbusGatewayServiceTest`
- `ModbusGatewayIntegrationTest`
- `IotTestApplication`

Prefer targeted test runs when touching one service.

## Working Rules For Agents

- Preserve existing package casing such as `Controller` and `Entity`; do not silently rename packages as part of unrelated work.
- Keep using constructor injection via Lombok `@RequiredArgsConstructor`.
- Reuse `ApiResponse` and existing exception types instead of inventing new response envelopes.
- When adding persistence logic, match current MyBatis-Plus style instead of mixing in a new repository pattern.
- When changing board/card behavior, keep default board columns consistent:
  `Wish list`, `Applied`, `Interviewing`, `Offered`, `Rejected`.
- When editing auth flow, verify both controller endpoints and JWT filter behavior.
- When editing IoT code, keep topic names aligned with config:
  MQTT topic `devices/data`, Kafka topic `device-data`.

## Known Pitfalls

- Several repo documents contain encoding damage. Treat source code and `application.yaml` as the source of truth.
- Older docs may mention JPA repositories; the actual implementation in this repo is mapper-based.
- Some examples in docs use `/api/...` paths, but the controllers in source use `/auth` and `/board`.
- The project contains both core app code and IoT code; avoid changing shared config casually.

## Useful References

- `CLAUDE.md`
- `docs/BACKEND_DEVELOPMENT_GUIDE.md`
- `docs/DATABASE_SCHEMA.md`
- `docs/IOT-ARCHITECTURE.md`
- `docs/JAVA-DEV-GUIDE.md`
- `.aiassistant/rules/springrules.md`
