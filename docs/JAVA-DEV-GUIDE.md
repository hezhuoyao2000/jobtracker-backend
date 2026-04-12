# Java 端 IoT 模块开发指导文档

## 核心原则：接口解耦，分层开发

```
┌─────────────────────────────────────────────────────────────────┐
│                      Java 服务端边界                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   Modbus     │    │    MQTT      │    │   Kafka      │      │
│  │  客户端接口   │    │   消息接口    │    │   消息接口    │      │
│  │  (j2mod)     │    │  (Paho)      │    │ (Spring-Kafka)│     │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘      │
│         │                   │                   │              │
│         ▼                   ▼                   ▼              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Java IoT 业务模块                           │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │   │
│  │  │   Gateway   │  │  Ingestion  │  │    Consumer     │  │   │
│  │  │  Service    │  │   Service   │  │    Service      │  │   │
│  │  │  (轮询读取)  │  │ (MQTT→Kafka)│  │(Kafka→DB/Redis) │  │   │
│  │  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘  │   │
│  │         │                │                   │          │   │
│  │         ▼                ▼                   ▼          │   │
│  │  ┌─────────────────────────────────────────────────────┐│   │
│  │  │              数据流转                                ││   │
│  │  │  DeviceReading → JSON → MQTT → Kafka → DB/Redis     ││   │
│  │  └─────────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────────┘   │
│         │                   │                   │              │
│         ▼                   ▼                   ▼              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  外部接口层   │    │  外部接口层   │    │   外部接口层  │      │
│  │ localhost:502│    │ localhost:1883│    │ localhost:9092│     │
│  │  (Modbus TCP)│    │   (EMQX)     │    │   (Kafka)    │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                │
│  ★ 只要接口契约满足，Java 端可独立开发和测试                     │
│  ★ Python 模拟器、其他服务可用 Mock/真实容器替换                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 一、接口契约定义（核心）

### 1.1 Modbus 接口契约

**连接参数**：
| 参数 | 值 | 说明 |
|------|-----|------|
| 协议 | Modbus TCP | 标准 Modbus TCP 协议 |
| 主机 | `localhost` | 可配置 |
| 端口 | `502` | 标准 Modbus 端口 |
| 从站ID | `1` | 默认从站地址 |
| 轮询间隔 | `1000ms` | 每秒读取一次 |

**寄存器定义**：
| 寄存器地址 | 数据类型 | 值范围 | 业务含义 | 单位换算 |
|-----------|---------|--------|---------|---------|
| 0 | uint16 | 600-1000 | 温度 | `value / 10.0` = °C |
| 1 | uint16 | 1000-3000 | 转速 | `value` = RPM |

**读取方式**：
```java
// 使用 j2mod 读取 Holding Registers
Register[] registers = master.readMultipleRegisters(0, 2);
int tempRaw = registers[0].getValue();     // 原始值 848
double tempCelsius = tempRaw / 10.0;       // 转换后 84.8°C
int rpm = registers[1].getValue();         // 转速 2710 RPM
```

### 1.2 内部数据模型契约

**DeviceReading 类**（所有模块共用）：
```java
public class DeviceReading {
    private String deviceId;        // 设备ID，固定 "device-001"
    private double temperature;     // 温度值，单位 °C
    private int rpm;                // 转速值，单位 RPM
    private Instant timestamp;      // ISO-8601 时间戳
    
    // 可选：携带原始值用于调试
    private int rawTemperature;
    private int rawRpm;
}
```

**JSON 序列化格式**：
```json
{
    "deviceId": "device-001",
    "temperature": 84.8,
    "rpm": 2710,
    "timestamp": "2025-04-11T14:56:00Z",
    "rawTemperature": 848,
    "rawRpm": 2710
}
```

### 1.3 MQTT 接口契约

**连接参数**：
| 参数 | 值 |
|------|-----|
| Broker URL | `tcp://localhost:1883` |
| Client ID | `java-gateway-001` |
| 用户名 | 无（匿名）|
| 密码 | 无（匿名）|
| QoS | `1`（至少一次）|

**Topic 定义**：
| Topic | 方向 | 用途 | 消息格式 |
|-------|------|------|---------|
| `devices/data` | Publish | 网关发布设备数据 | DeviceReading JSON |
| `devices/data` | Subscribe | Ingestion 订阅消费 | DeviceReading JSON |

### 1.4 Kafka 接口契约

**连接参数**：
| 参数 | 值 |
|------|-----|
| Bootstrap Servers | `localhost:9092` |
| Topic | `device-data` |
| Partition | `1` |
| Replication | `1` |

**生产者配置**：
```yaml
key-serializer: org.apache.kafka.common.serialization.StringSerializer
value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

**消费者配置**：
```yaml
group-id: iot-consumer-group
auto-offset-reset: earliest
key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

**消息格式**：与 MQTT 相同（DeviceReading JSON）

### 1.5 InfluxDB 接口契约

**连接参数**：
| 参数 | 值 |
|------|-----|
| URL | `http://localhost:8086` |
| Token | `my-super-secret-token` |
| Org | `iot-demo` |
| Bucket | `device-metrics` |

**数据映射**：
| DeviceReading 字段 | InfluxDB 元素 | 说明 |
|-------------------|--------------|------|
| `timestamp` | `_time` | 时间戳 |
| `deviceId` | Tag `deviceId` | 索引标签 |
| `temperature` | Field `temperature` | 温度值 |
| `rpm` | Field `rpm` | 转速值 |
| - | Measurement `device_metrics` | 表名 |

**写入示例**：
```java
Point point = Point.measurement("device_metrics")
    .addTag("deviceId", reading.getDeviceId())
    .addField("temperature", reading.getTemperature())
    .addField("rpm", reading.getRpm())
    .time(reading.getTimestamp(), WritePrecision.NS);
```

### 1.6 Redis 接口契约

**连接参数**：
| 参数 | 值 |
|------|-----|
| Host | `localhost` |
| Port | `6379` |
| Database | `0` |

**Key 规范**：
| 用途 | Key 格式 | Value 格式 | TTL |
|------|---------|-----------|-----|
| 最新状态 | `device:latest:{deviceId}` | DeviceReading JSON | 30秒 |

**示例**：
```
Key: device:latest:device-001
Value: {"deviceId":"device-001","temperature":84.8,...}
TTL: 30
```

---

## 二、分层开发计划（可独立执行）

### Phase 1: 基础配置层（1-2小时）

**目标**：搭建项目结构，配置外部服务连接

**可独立测试**：✅ 不依赖 Python 模拟器

**任务清单**：
- [ ] 添加 Maven 依赖（pom.xml）
- [ ] 创建 `application.yml` 配置
- [ ] 创建 Config 类（MqttConfig, InfluxDbConfig, WebSocketConfig）
- [ ] 创建 DeviceReading 模型类

**验收标准**：
```bash
# 1. 项目能正常启动
mvn spring-boot:run

# 2. 各客户端 Bean 初始化成功（看日志）
#    - MQTT Client connected
#    - InfluxDB Client connected
#    - Kafka Producer/Consumer ready
```

---

### Phase 2: 网关采集层（2-3小时）

**目标**：从 Modbus 读取数据，发布到 MQTT

**可独立测试**：✅ 可用 Mock 数据测试 MQTT 发布

**任务清单**：
- [ ] 创建 `ModbusGatewayService.java`
- [ ] 实现 Modbus TCP 连接
- [ ] 实现 @Scheduled 轮询（每秒）
- [ ] 实现数据转换（Register → DeviceReading）
- [ ] 实现 MQTT 发布

**独立测试方案**（不依赖 Python）：
```java
// 方案 A：Mock Modbus 读取
@Profile("test")
@Component
public class MockModbusGatewayService {
    @Scheduled(fixedRate = 1000)
    public void mockRead() {
        DeviceReading reading = new DeviceReading();
        reading.setDeviceId("device-001");
        reading.setTemperature(60 + Math.random() * 40);  // 60-100°C
        reading.setRpm(1000 + (int)(Math.random() * 2000)); // 1000-3000 RPM
        reading.setTimestamp(Instant.now());
        
        mqttClient.publish("devices/data", toJson(reading));
    }
}
```

**验收标准**：
```bash
# 1. 启动服务（使用 Mock）
mvn spring-boot:run -Dspring.profiles.active=test

# 2. 用 MQTT 客户端查看消息
mosquitto_sub -h localhost -t devices/data
# 应每秒收到一条 JSON 消息
```

---

### Phase 3: 消息接入层（1-2小时）

**目标**：从 MQTT 订阅，转发到 Kafka

**可独立测试**：✅ 可用 MQTT CLI 工具测试

**任务清单**：
- [ ] 创建 `MqttIngestionService.java`
- [ ] 实现 MQTT 订阅（@PostConstruct）
- [ ] 实现消息反序列化
- [ ] 实现 Kafka 发送

**独立测试方案**：
```bash
# 1. 启动 Java 服务

# 2. 用 mosquitto_pub 发送测试消息
mosquitto_pub -h localhost -t devices/data -m '{"deviceId":"device-001","temperature":75.5,"rpm":2000}'

# 3. 用 kafka-console-consumer 查看
kafka-console-consumer --bootstrap-server localhost:9092 --topic device-data --from-beginning
```

**验收标准**：
- MQTT 消息成功发送到 Kafka Topic

---

### Phase 4: 数据消费层（2-3小时）

**目标**：消费 Kafka，写入 InfluxDB 和 Redis

**可独立测试**：✅ 可用 kafka-console-producer 测试

**任务清单**：
- [ ] 创建 `DeviceDataConsumer.java`
- [ ] 实现 @KafkaListener
- [ ] 实现 InfluxDB 写入
- [ ] 实现 Redis 写入（含 TTL）
- [ ] 实现 WebSocket 广播

**独立测试方案**：
```bash
# 1. 启动 Java 服务

# 2. 用 kafka-console-producer 发送测试消息
kafka-console-producer --bootstrap-server localhost:9092 --topic device-data
> {"deviceId":"device-001","temperature":80.0,"rpm":2500,"timestamp":"2025-04-11T10:00:00Z"}

# 3. 验证 InfluxDB
influx query 'from(bucket:"device-metrics") |> range(start:-1h)'

# 4. 验证 Redis
redis-cli GET device:latest:device-001
```

**验收标准**：
- Kafka 消息 → InfluxDB 有记录
- Kafka 消息 → Redis 有记录（30秒过期）

---

### Phase 5: SSE 实时推送（1-2小时）

**目标**：实现 SSE Server，推送实时数据

**可独立测试**：✅ 可用浏览器或 curl 测试

**职责边界（重要）**：
- Redis `device:latest:*` 作为“最新值缓存”应持续更新（用于后续 Phase 6 查询、以及断线后快速恢复最新值）
- SSE 只负责“推送出口”：只有前端建立 `/api/iot/stream` 连接时才推送；无连接时不发布 Redis Pub/Sub

**任务清单**：
- [ ] 创建 `SseEmitterManager.java`
- [ ] 实现 emitter 管理（ConcurrentHashMap）
- [ ] 实现 broadcast 方法
- [ ] 创建 `IotSseController.java` 提供 `/api/iot/stream` 端点
- [ ] 在 Consumer 中发布 Redis Pub/Sub（`iot.redis.pubsub-channel`，默认 `iot:device-data`）

**独立测试方案**：
```bash
# 1. 启动 Java 服务

# 2. 用 curl 连接 SSE
curl -N http://localhost:8080/api/iot/stream

# 3. 发送 Kafka 测试消息（见 Phase 4）
# 应能在 curl 看到推送的 JSON

# 4. 浏览器测试
# 打开控制台执行:
eventSource = new EventSource('http://localhost:8080/api/iot/stream');
eventSource.addEventListener('device-data', (e) => console.log(JSON.parse(e.data)));
```

**前端使用示例**：
```javascript
const eventSource = new EventSource('http://localhost:8080/api/iot/stream');

eventSource.addEventListener('device-data', (event) => {
    const data = JSON.parse(event.data);
    console.log('Received:', data);
    // 更新 UI...
});

// 页面关闭时自动断开，无需手动处理
```

---

### Phase 6: REST API 层（1-2小时）

**目标**：提供查询接口

**任务清单**：
- [ ] 创建 `IotDemoController.java`
- [ ] 实现 `GET /api/iot/latest/{deviceId}`
- [ ] 实现 `GET /api/iot/history/{deviceId}?minutes=5`

**独立测试**：
```bash
# 查询最新状态
curl http://localhost:8080/api/iot/latest/device-001

# 查询历史数据
curl "http://localhost:8080/api/iot/history/device-001?minutes=5"
```

---

### Phase 7: 端到端联调（可选）

**目标**：连接 Python 模拟器，完整链路测试

**此时才需要**：
- Python PLC 模拟器运行

---

## 三、项目结构规范

```
src/main/java/com/example/iot/
├── config/
│   ├── MqttConfig.java              # MQTT 客户端配置
│   ├── InfluxDbConfig.java          # InfluxDB 客户端配置
│   ├── MqttConsumerConfig.java      # MQTT Consumer 配置 (Phase 3)
│   └── KafkaConfig.java             # Kafka 配置（可选）
├── model/
│   └── DeviceReading.java           # 核心数据模型
├── gateway/
│   ├── ModbusGatewayService.java    # Phase 2: 网关采集 (Publisher)
│   └── MockModbusGatewayService.java # Mock 数据生成
├── ingestion/
│   └── MqttIngestionService.java    # Phase 3: MQTT → Kafka (Subscriber)
├── consumer/
│   └── DeviceDataConsumer.java      # Phase 4: Kafka → DB/Redis/SSE
├── sse/
│   └── SseEmitterManager.java       # Phase 5: SSE 推送管理
├── controller/
│   ├── IotSseController.java        # Phase 5: SSE 端点 (/api/iot/stream)
│   └── IotDemoController.java       # Phase 6: REST API
└── config/
    └── IotProperties.java           # 配置属性类
```

---

## 四、配置模板

### 4.1 Maven 依赖（pom.xml 新增）

```xmln<!-- 在原有 dependencies 中添加 -->

<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- MQTT (Eclipse Paho) -->
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>

<!-- InfluxDB Client -->
<dependency>
    <groupId>com.influxdb</groupId>
    <artifactId>influxdb-client-java</artifactId>
    <version>6.10.0</version>
</dependency>

<!-- Modbus (j2mod) -->
<dependency>
    <groupId>com.ghgande</groupId>
    <artifactId>j2mod</artifactId>
    <version>3.2.0</version>
</dependency>

<!-- 注：SSE 使用 Spring MVC 内置支持，无需额外依赖 -->
```

### 4.2 application.yml 配置

```yaml
# IoT 模块配置
iot:
  modbus:
    host: localhost
    port: 502
    slave-id: 1
    poll-interval-ms: 1000
    enabled: true  # 可切换 Mock
    mock-enabled: false

  mqtt:
    broker: tcp://localhost:1883
    client-id: java-gateway-001  # Phase 2: Gateway 发布者
    topic: devices/data
    qos: 1
    connection-timeout: 10
    keep-alive-interval: 60
    consumer:
      enabled: true
      client-id: java-ingestion-001  # Phase 3: Ingestion 消费者
      topic: devices/data
      qos: 1

  kafka:
    topic: device-data
    consumer:
      group-id: iot-consumer-group

  influxdb:
    url: http://localhost:8086
    token: my-super-secret-token
    org: iot-demo
    bucket: device-metrics

  redis:
    key-prefix: "device:latest:"
    ttl-seconds: 30

  sse:
    enabled: true
    path: /api/iot/stream
    event-name: device-reading

spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3
      acks: 1
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

  redis:
    host: localhost
    port: 6379
    database: 0
```

---

## 五、开发检查清单

### 启动前检查
- [ ] Docker 容器全部运行 (`docker compose ps`)
- [ ] 端口未被占用 (502, 1883, 9092, 6379, 8086)

### 各阶段检查
- [ ] Phase 1: 应用启动无报错
- [ ] Phase 2: MQTT 能收到消息（Mock 模式）
- [ ] Phase 3: Kafka 能收到消息
- [ ] Phase 4: InfluxDB 和 Redis 有数据
- [ ] Phase 5: SSE 客户端能收到推送 (`curl -N http://localhost:8080/api/iot/stream`)
- [ ] Phase 6: REST API 返回正确数据

### 端到端检查（联调时）
- [ ] Python 模拟器运行中
- [ ] Java 服务读取到 Modbus 数据
- [ ] 完整链路数据流转正常

---

## 六、快速启动命令

```bash
# 1. 启动基础设施
cd iot-infra
docker compose up -d

# 2. 检查容器状态
docker compose ps

# 3. 启动 Java 服务（开发模式）
cd /path/to/java-project
mvn spring-boot:run

# 4. 测试 MQTT（独立终端）
mosquitto_sub -h localhost -t devices/data

# 5. 测试 Kafka（独立终端）
kafka-console-consumer --bootstrap-server localhost:9092 --topic device-data --from-beginning

# 6. 测试 SSE（独立终端）
curl -N http://localhost:8080/api/iot/stream
```

---

## 总结

**核心理念**：Java 端通过明确的**接口契约**与外部系统交互：
- 不依赖 Python 模拟器的具体实现
- 每个开发阶段可独立测试
- 使用 Mock/CLI 工具即可验证大部分功能
- 只在最后联调阶段需要完整环境

**开发顺序建议**：
```
Phase 1 → Phase 2(Mock模式) → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7(联调)
```

---

## 2026-04-12 开发更新：Phase 4 已进入实现阶段

### 已新增代码

- `src/main/java/com/example/iot/consumer/DeviceDataConsumer.java`
- `src/test/java/com/example/iot/consumer/DeviceDataConsumerTest.java`
- `src/test/java/com/example/iot/consumer/DeviceDataConsumerIntegrationTest.java`

### 测试策略说明

- `DeviceDataConsumerTest` 使用 Mockito mock，不接真实 Kafka / Redis / InfluxDB
- `DeviceDataConsumerIntegrationTest` 接真实 Kafka、Redis、InfluxDB
- 集成测试中的消息体仍然是测试构造数据，不是生产设备真实采样值

### 浏览器查看入口

- InfluxDB：`http://localhost:8086`
- EMQX：`http://localhost:18083`
- Grafana：`http://localhost:3000`

### Redis 查看命令

```bash
redis-cli GET device:latest:device-001
redis-cli TTL device:latest:device-001
```
