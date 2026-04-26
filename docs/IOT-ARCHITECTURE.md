# IoT 模块架构设计与实现文档

> 本文档以当前 `src/main/java/com/example/iot` 源码结构为准，记录 IoT 模块的功能分层、数据流、配置项、接口契约和验证方式。
> 历史文档中的 `gateway`、`ingestion`、`consumer`、`controller` 旧包名已被重构后的分层替代。

---

## 一、当前模块分层

重构后的 IoT 模块按职责分为以下目录：

```text
src/main/java/com/example/iot/
  application/             # 应用服务层，编排采集、接入、落库和推送流程
  config/                  # Spring Bean 与 iot.* 配置绑定
  domain/                  # 领域数据模型
  entrypoint/              # 外部触发入口：HTTP、MQTT、Kafka、Redis、定时任务
  infrastructure/          # 外部系统适配：Modbus、MQTT、SSE
  sse/                     # 历史目录，当前核心 SSE 实现在 infrastructure/sse
```

核心设计约定：

- `entrypoint` 只负责接收外部事件或触发定时任务，不直接处理完整业务链路。
- `application` 负责用例编排，例如 Modbus 采集、MQTT 接入、Kafka 数据消费。
- `infrastructure` 封装外部协议或框架细节，例如 j2mod、Paho MQTT、Spring `SseEmitter`。
- `domain.DeviceReading` 是 Modbus、MQTT、Kafka、InfluxDB、Redis、SSE 之间共享的数据契约。
- 配置统一绑定到 `config.IotProperties`，实际配置来源为 `application.yaml` 和 `application-prod.yml`。

---

## 二、整体数据流

```text
Modbus TCP 设备/模拟器
    |
    | entrypoint.scheduler.ModbusPollingTask
    v
application.ModbusCollectionService
    |
    | infrastructure.modbus.ModbusDeviceReader
    | 读取 Holding Registers 0 和 1
    v
domain.DeviceReading
    |
    | infrastructure.mqtt.MqttDeviceReadingPublisher
    v
MQTT topic: devices/data
    |
    | entrypoint.listener.MqttDeviceDataListener
    v
application.MqttIngestionService
    |
    v
Kafka topic: device-data
    |
    | entrypoint.listener.KafkaDeviceDataListener
    v
application.DeviceDataIngestService
    |
    |-- InfluxDB measurement: device_metrics
    |-- Redis latest key: device:latest:{deviceId}
    |
    | 有 SSE 客户端时发布 Redis Pub/Sub
    v
Redis channel: iot:device-data
    |
    | entrypoint.listener.RedisDeviceDataListener
    v
infrastructure.sse.SseEmitterManager
    |
    v
HTTP SSE: GET /iot/stream
```

Mock 模式下，`entrypoint.scheduler.MockModbusPollingTask` 调用 `application.MockModbusCollectionService` 生成模拟 `DeviceReading`，后续仍复用 MQTT -> Kafka -> Redis/InfluxDB/SSE 链路。

---

## 三、领域数据契约

### 3.1 `DeviceReading`

文件：`src/main/java/com/example/iot/domain/DeviceReading.java`

```json
{
  "deviceId": "device-001",
  "temperature": 84.8,
  "rpm": 2710,
  "timestamp": "2026-04-26T10:30:00Z",
  "rawTemperature": 848,
  "rawRpm": 2710
}
```

字段含义：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | string | 设备标识，当前 Modbus 采集固定为 `device-001` |
| `temperature` | double | 温度业务值，来自 `rawTemperature / 10.0` |
| `rpm` | int | 转速业务值，来自 Modbus 原始寄存器值 |
| `timestamp` | Instant | 采集时间；Kafka 消费落 InfluxDB 时为空则使用当前时间兜底 |
| `rawTemperature` | Integer | 温度原始寄存器值，便于调试和核对 |
| `rawRpm` | Integer | 转速原始寄存器值，便于调试和核对 |

### 3.2 Modbus 寄存器约定

| 地址 | 数据类型 | 当前含义 | 转换规则 |
| --- | --- | --- | --- |
| 0 | uint16 | 温度原始值 | `temperature = value / 10.0` |
| 1 | uint16 | 转速原始值 | `rpm = value` |

读取方式由 `ModbusDeviceReader.read()` 实现，使用 j2mod `readMultipleRegisters(slaveId, 0, 2)`。

---

## 四、功能模块说明

### 4.1 Modbus 采集模块

职责：按配置周期从 Modbus TCP 读取设备数据，转换为 `DeviceReading`，并发布到 MQTT。

相关类：

| 类 | 职责 |
| --- | --- |
| `entrypoint.scheduler.ModbusPollingTask` | 应用启动后初始化真实 Modbus 连接，并按 `iot.modbus.poll-interval-ms` 调度采集 |
| `application.ModbusCollectionService` | 编排连接检查、重连、读取和 MQTT 发布 |
| `infrastructure.modbus.ModbusDeviceReader` | 封装 j2mod `ModbusTCPMaster`，负责连接、读取和断开标记 |
| `infrastructure.mqtt.MqttDeviceReadingPublisher` | 将 `DeviceReading` 序列化为 JSON 并发布到 MQTT |

关键行为：

- `iot.modbus.enabled=false` 时，真实采集服务不会连接或读取 Modbus。
- 真实模式由 `iot.modbus.mock-enabled=false` 启用，这是默认条件。
- 连接不可用时最多尝试 3 次重连，超过后重置重试计数，等待下一轮调度。
- MQTT publisher Bean 不在应用启动时强制连接 Broker，而是在首次发布时按需连接。
- MQTT 发布失败只记录日志，不抛出到调度线程。

### 4.2 Mock Modbus 采集模块

职责：在没有真实 Modbus 设备或模拟器时生成随机设备数据，复用下游链路。

相关类：

| 类 | 职责 |
| --- | --- |
| `entrypoint.scheduler.MockModbusPollingTask` | `iot.modbus.mock-enabled=true` 时按周期触发模拟数据生成 |
| `application.MockModbusCollectionService` | 生成温度和转速随机值，构造 `DeviceReading`，发布到 MQTT |

模拟数据范围：

- `rawTemperature`: 600 到 1000，业务温度为 60.0 到 100.0。
- `rawRpm`: 1000 到 3000。
- `deviceId`: 固定为 `device-001`。

### 4.3 MQTT 接入模块

职责：订阅 MQTT `devices/data`，校验 JSON 可反序列化为 `DeviceReading` 后转发到 Kafka。

相关类：

| 类 | 职责 |
| --- | --- |
| `config.MqttConsumerConfig` | 创建独立 MQTT Consumer Client，使用 `iot.mqtt.consumer.client-id` |
| `entrypoint.listener.MqttDeviceDataListener` | 维护 MQTT consumer 连接、订阅 topic，并把消息交给应用服务 |
| `application.MqttIngestionService` | 解析 MQTT payload，使用 `deviceId` 作为 Kafka key 发送原始 JSON |

关键行为：

- MQTT publisher 和 consumer 使用不同 `client-id`，避免 Broker 因同名客户端互踢。
- `MqttDeviceDataListener` 通过 `@Scheduled` 按 `iot.mqtt.consumer.reconnect-interval-ms` 维护连接和订阅状态。
- 非法 JSON 或不符合 `DeviceReading` 的 payload 不会发送到 Kafka。
- Kafka topic 来自 `iot.kafka.topic`，默认 `device-data`。

### 4.4 Kafka 消费与存储模块

职责：消费 Kafka `device-data`，写入时序库和最新状态缓存，并按需触发 SSE 推送。

相关类：

| 类 | 职责 |
| --- | --- |
| `entrypoint.listener.KafkaDeviceDataListener` | 通过 `@KafkaListener` 消费 `iot.kafka.topic` |
| `application.DeviceDataIngestService` | 解析 Kafka payload，写入 InfluxDB 和 Redis，并在存在 SSE 客户端时发布 Redis Pub/Sub |
| `config.KafkaConfig` | 创建 Kafka topic，分区 1、副本 1，并设置 retention |
| `config.InfluxDbConfig` | 在 `iot.influxdb.enabled=true` 时提供 InfluxDB client |

存储约定：

| 目标 | 约定 |
| --- | --- |
| InfluxDB measurement | `device_metrics` |
| InfluxDB tag | `deviceId` |
| InfluxDB fields | `temperature`、`rpm` |
| Redis key | `${iot.redis.key-prefix}${deviceId}`，默认 `device:latest:device-001` |
| Redis TTL | `${iot.redis.ttl-seconds}`，默认 30 秒 |
| Redis Pub/Sub channel | `${iot.redis.pubsub-channel}`，默认 `iot:device-data` |

容错行为：

- Kafka payload 无法解析时，不写 InfluxDB、Redis，也不触发 SSE。
- InfluxDB 未启用时跳过时序写入。
- InfluxDB 写入失败不会阻断 Redis 最新状态更新。
- Redis 写入失败不会回滚 InfluxDB。
- 只有 `SseEmitterManager.hasClients()` 为 true 时才发布 Redis Pub/Sub，避免无客户端时产生无意义广播。

### 4.5 SSE 实时推送模块

职责：向浏览器提供设备数据实时流。

相关类：

| 类 | 职责 |
| --- | --- |
| `entrypoint.controller.IotSseController` | 暴露 `GET /iot/stream`，返回 `text/event-stream` |
| `infrastructure.sse.SseEmitterManager` | 管理所有 `SseEmitter`，广播设备数据和心跳，清理断开的连接 |
| `entrypoint.listener.RedisDeviceDataListener` | 监听 Redis Pub/Sub 消息，并转发给 `SseEmitterManager` |
| `entrypoint.scheduler.SseHeartbeatTask` | 按 `iot.sse.heartbeat-interval-ms` 发送 SSE 心跳注释 |
| `config.RedisPubSubConfig` | 创建 Redis listener container 并订阅设备数据 channel |

HTTP 契约：

```http
GET /iot/stream
Accept: text/event-stream
```

响应事件：

```text
event: device-data
data: {"deviceId":"device-001","temperature":84.8,"rpm":2710,...}
```

连接管理：

- `SseEmitter` 使用 `new SseEmitter(0L)`，不主动设置超时。
- Controller 设置 `Cache-Control: no-cache`、`X-Accel-Buffering: no`、`Connection: keep-alive`。
- 发送数据或心跳失败时，`SseEmitterManager` 移除对应客户端，避免连接泄漏。
- 心跳默认 15 秒一次，配置项为 `iot.sse.heartbeat-interval-ms`。

前端示例：

```javascript
const eventSource = new EventSource("http://localhost:8080/iot/stream");

eventSource.addEventListener("device-data", (event) => {
  const reading = JSON.parse(event.data);
  console.log(reading);
});
```

---

## 五、配置项

默认开发配置来自 `src/main/resources/application.yaml`：

```yaml
iot:
  modbus:
    host: localhost
    port: 502
    slave-id: 1
    poll-interval-ms: 1000
    enabled: true
    mock-enabled: false

  mqtt:
    broker: tcp://localhost:1883
    client-id: java-gateway-001
    topic: devices/data
    qos: 1
    connection-timeout: 10
    keep-alive-interval: 60
    consumer:
      enabled: true
      client-id: java-ingestion-001
      topic: devices/data
      qos: 1
      reconnect-interval-ms: 5000

  kafka:
    topic: device-data
    retention-ms: 7200000

  influxdb:
    enabled: false
    url: http://localhost:8086
    token: my-super-secret-token
    org: iot-demo
    bucket: device-metrics

  redis:
    key-prefix: "device:latest:"
    ttl-seconds: 30
    pubsub-channel: "iot:device-data"

  sse:
    heartbeat-interval-ms: 15000
```

生产配置 `application-prod.yml` 使用环境变量覆盖外部服务地址，默认服务名面向 Docker 网络：

- MQTT Broker: `tcp://iot-emqx:1883`
- Kafka: `iot-kafka:19092`
- Redis: `iot-redis:6379`
- InfluxDB: `http://iot-influxdb:8086`
- Modbus: `plc-simulator:502`

---

## 六、运行与验证

### 6.1 启动依赖

完整链路需要以下外部组件：

- Modbus TCP 设备或模拟器，默认 `localhost:502`。
- MQTT Broker，默认 `localhost:1883`，topic 为 `devices/data`。
- Kafka，默认 `localhost:9092`，topic 为 `device-data`。
- Redis，默认 `localhost:6379`。
- InfluxDB 可选，只有 `iot.influxdb.enabled=true` 时写入。

### 6.2 启动应用

```bash
.\mvnw.cmd spring-boot:run
```

Mock 模式可通过配置启用：

```yaml
iot:
  modbus:
    enabled: true
    mock-enabled: true
```

### 6.3 CLI 验证点

查看 MQTT：

```bash
mosquitto_sub -h localhost -t devices/data
```

查看 Kafka：

```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic device-data --from-beginning
```

查看 Redis 最新值：

```bash
redis-cli GET device:latest:device-001
redis-cli TTL device:latest:device-001
```

查看 SSE：

```bash
curl -N http://localhost:8080/iot/stream
```

---

## 七、测试覆盖

当前测试目录仍保留历史包名，但测试对象已对应重构后的类。

| 测试文件 | 覆盖目标 |
| --- | --- |
| `src/test/java/com/example/iot/gateway/ModbusGatewayServiceTest.java` | `ModbusCollectionService`、`ModbusDeviceReader`、`MqttDeviceReadingPublisher` 的采集发布行为 |
| `src/test/java/com/example/iot/gateway/ModbusGatewayIntegrationTest.java` | Mock/真实采集到 MQTT 的端到端行为 |
| `src/test/java/com/example/iot/ingestion/MqttIngestionServiceTest.java` | MQTT payload 解析、Kafka 转发、异常处理 |
| `src/test/java/com/example/iot/ingestion/MqttIngestionIntegrationTest.java` | MQTT 到 Kafka 链路 |
| `src/test/java/com/example/iot/consumer/DeviceDataConsumerTest.java` | Kafka payload 写 InfluxDB、Redis、Pub/Sub 的容错行为 |
| `src/test/java/com/example/iot/consumer/DeviceDataConsumerIntegrationTest.java` | Kafka 到 Redis/InfluxDB 的真实链路 |
| `src/test/java/com/example/iot/sse/SseEmitterManagerTest.java` | SSE 连接管理、广播和断开清理 |
| `src/test/java/com/example/iot/sse/RedisDeviceDataSubscriberTest.java` | Redis Pub/Sub 消息转 SSE |
| `src/test/java/com/example/iot/sse/RedisPubSubIntegrationTest.java` | Redis Pub/Sub 到 SSE 的集成行为 |
| `src/test/java/com/example/iot/controller/IotSseControllerTest.java` | `/iot/stream` HTTP SSE 入口 |

常用命令：

```bash
.\mvnw.cmd test -Dtest=ModbusGatewayServiceTest
.\mvnw.cmd test -Dtest=MqttIngestionServiceTest
.\mvnw.cmd test -Dtest=DeviceDataConsumerTest
.\mvnw.cmd test -Dtest=SseEmitterManagerTest
```

集成测试依赖真实或测试环境中的 MQTT、Kafka、Redis、InfluxDB，执行前需要先启动对应基础设施。

---

## 八、设计注意事项

- 不要在 `entrypoint` 层直接堆业务逻辑；新入口应调用 `application` 服务。
- 新增外部协议适配时优先放入 `infrastructure`，并隐藏第三方库细节。
- 新增跨链路字段时先更新 `DeviceReading`，再同步 MQTT、Kafka、Redis、InfluxDB、SSE 的文档和测试。
- MQTT publisher 和 consumer 必须使用不同 client id。
- Kafka key 使用 `deviceId`，便于同设备数据保持分区一致性。
- SSE 只依赖 Redis Pub/Sub 广播，不直接从 Kafka listener 持有 HTTP 连接。
- InfluxDB 写入是可选能力，不能成为 Redis 最新状态和 SSE 推送的硬依赖。

---

## 九、变更记录

| 日期 | 版本 | 变更内容 |
| --- | --- | --- |
| 2026-04-26 | v1.0 | 根据重构后的 `application/domain/entrypoint/infrastructure/config` 分层重写 IoT 架构文档，修正 SSE 路径为 `/iot/stream`，同步 Redis Pub/Sub、心跳和 Kafka/InfluxDB/Redis 存储说明 |
| 2026-04-12 | v0.6 | 增加 Kafka 消费、InfluxDB、Redis 与 SSE 推送说明 |
| 2025-04-11 | v0.5 | 完成 MQTT 到 Kafka 接入说明 |
| 2025-04-11 | v0.4 | 完成 Modbus 到 MQTT 网关说明 |
