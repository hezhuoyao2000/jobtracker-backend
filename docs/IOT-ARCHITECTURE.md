# IoT 模块架构设计与实现文档

> 本文档记录 IoT 模块的整体架构、数据流转、接口契约和实现细节。
> 每完成一个 Phase 就更新对应章节。

---

## 一、整体架构概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Java 服务端边界                                  │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐          │
│  │   Modbus TCP    │    │   MQTT (EMQX)   │    │     Kafka       │          │
│  │   localhost:502 │    │ localhost:1883  │    │  localhost:9092 │          │
│  │                 │    │                 │    │                 │          │
│  │  Python PLC     │───▶│  devices/data   │───▶│   device-data   │          │
│  │  模拟器          │    │   topic         │    │   topic         │          │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘          │
│           │                      │                      │                   │
│           ▼                      ▼                      ▼                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Java IoT 业务模块                             │   │
│  │                                                                     │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │   │
│  │  │   Phase 2    │  │   Phase 3    │  │   Phase 4    │              │   │
│  │  │   Gateway    │  │  Ingestion   │  │   Consumer   │              │   │
│  │  │   Service    │  │   Service    │  │   Service    │              │   │
│  │  │  (Modbus     │  │  (MQTT Sub)  │  │ (Kafka Sub)  │              │   │
│  │  │   → MQTT)    │  │  (→ Kafka)   │  │ (→ DB/Redis) │              │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘              │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        数据存储层                                    │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │   │
│  │  │  InfluxDB    │  │    Redis     │  │  PostgreSQL  │              │   │
│  │  │  时序数据     │  │  最新状态     │  │  业务数据     │              │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、Phase 2: 网关采集层 (Modbus → MQTT)

### 2.1 数据输入

**来源**: Python PLC 模拟器 (Modbus TCP Server)
**接口**: TCP Socket, 端口 502
**协议**: Modbus TCP
**从站ID**: 1

### 2.2 寄存器定义

| 寄存器地址 | 数据类型 | 值范围 | 业务含义 | 单位换算 |
|-----------|---------|--------|---------|---------|
| 0 | uint16 | 600-1000 | 温度原始值 | `value / 10.0` = °C |
| 1 | uint16 | 1000-3000 | 转速原始值 | `value` = RPM |

### 2.3 实现类

**文件**: `com.example.iot.gateway.ModbusGatewayService`

**核心方法**:
- `init()`: 应用启动时初始化 Modbus 连接
- `pollDeviceData()`: 定时轮询（频率可配置）
- `readModbusData()`: 读取并解析寄存器数据
- `publishToMqtt()`: 发布到 MQTT

### 2.4 数据输出

**目标**: MQTT Broker (EMQX)
**Topic**: `devices/data`
**QoS**: 1 (至少一次)
**格式**: JSON

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

### 2.5 配置参数

```yaml
iot:
  modbus:
    host: localhost
    port: 502
    slave-id: 1
    poll-interval-ms: 1000    # 轮询间隔，可调
    enabled: true             # 开关

  mqtt:
    broker: tcp://localhost:1883
    client-id: java-gateway-001
    topic: devices/data
    qos: 1
```

### 2.6 设计注意点

#### 1. 连接管理
- **问题**: Modbus TCP 连接可能断开（网络抖动、模拟器重启动）
- **方案**: 每次轮询前检查连接状态，断开时自动重连
- **重试策略**: 最多重试 3 次，每次间隔由调度器控制

#### 2. 错误处理
```java
// 连接失败
log.error("Failed to connect Modbus TCP: {} - {}", e.getClass().getSimpleName(), e.getMessage());

// 读取失败
log.error("Error polling Modbus data: {} - {}", e.getClass().getSimpleName(), e.getMessage());

// 发布失败
log.error("Failed to publish to MQTT: {} - {}", e.getClass().getSimpleName(), e.getMessage());
```

#### 3. 线程安全
- ModbusMaster 实例在连接断开时设为 null
- 使用 `@Scheduled` 单线程执行，避免并发问题

#### 4. JSON 序列化
- 使用简单字符串拼接，避免循环依赖或 Jackson 配置问题
- 保留原始值（rawTemperature/rawRpm）用于调试

### 2.7 协议注意事项

#### Modbus TCP
- **字节序**: 大端模式（Big Endian），j2mod 库已处理
- **功能码**: `readMultipleRegisters` 使用功能码 03 (Read Holding Registers)
- **超时**: 默认由操作系统控制，连接超时约 75 秒

#### MQTT
- **QoS 1**: 确保消息至少送达一次，但可能重复
- **Clean Session**: true，每次连接创建新会话
- **自动重连**: MqttConnectOptions 中启用

### 2.8 测试策略

#### 单元测试
**文件**: `src/test/java/com/example/iot/gateway/ModbusGatewayServiceTest.java`

**覆盖场景**:
| 测试方法 | 验证点 | 状态 |
|---------|--------|------|
| `testReadAndPublish_Success` | 正常数据流：原始值 → 转换 → MQTT 发布 | ✅ |
| `testTemperatureConversion_MinValue` | 边界值：原始值 600 → 60.0°C | ✅ |
| `testTemperatureConversion_MaxValue` | 边界值：原始值 1000 → 100.0°C | ✅ |
| `testReconnect_WhenDisconnected` | 连接断开时的重连逻辑 | ✅ |
| `testHandleModbusException` | Modbus 异常处理（不中断流程） | ✅ |
| `testHandleMqttException` | MQTT 异常处理（不中断流程） | ✅ |
| `testWhenDisabled` | 服务禁用时的行为 | ✅ |
| `testMqttQoS` | QoS 配置生效验证 | ✅ |

**测试技术**:
- Mockito 模拟 ModbusTCPMaster 和 MqttClient
- `lenient()` 避免 UnnecessaryStubbingException
- `setModbusMaster()` 注入 mock 对象
- ArgumentCaptor 捕获发布的消息内容
- 验证中间态（rawTemperature/rawRpm）和最终输出（temperature/rpm）

**运行命令**:
```bash
./mvnw test -Dtest=ModbusGatewayServiceTest
```

#### 集成测试
**文件**: `src/test/java/com/example/iot/gateway/ModbusGatewayIntegrationTest.java`

**测试环境**:
- 使用 `IotTestApplication` 专门测试启动类
- `application-test.yml` 配置（禁用数据源，启用 Mock 模式）
- 依赖 MQTT Broker (localhost:1883)

**覆盖场景**:
| 测试方法 | 验证点 | 状态 |
|---------|--------|------|
| `testMockDataPublishedToMqtt` | 端到端数据流验证 | ✅ |
| `testDataFormat` | JSON 格式符合 DeviceReading 规范 | ✅ |
| `testMessageCounting` | Mock 服务统计计数器正确 | ✅ |
| `testManualTrigger` | 手动触发数据生成 | ✅ |
| `testMqttQoS` | MQTT QoS 配置生效 | ✅ |

**验证数据**:
```json
{
  "deviceId": "device-001",
  "temperature": 84.8,      // 范围: 60.0-100.0
  "rpm": 2710,              // 范围: 1000-3000
  "timestamp": "2025-04-11T14:56:00Z",
  "rawTemperature": 848,    // 中间态: 原始值
  "rawRpm": 2710            // 中间态: 原始值
}
```

**运行命令**:
```bash
./mvnw test -Dtest=ModbusGatewayIntegrationTest
```

#### Mock 服务
**文件**: `src/main/java/com/example/iot/gateway/MockModbusGatewayService.java`

**用途**:
- 单元测试：不依赖真实 Modbus 设备
- 集成测试：验证 MQTT 发布链路
- 开发调试：快速验证下游流程

**启用方式**:
```yaml
iot:
  modbus:
    mock-enabled: true
```

### 2.9 调试验证

**步骤 1**: 启动 Python 模拟器
```bash
# 在 iot-infra 目录
docker compose up -d modbus-simulator
# 或直接运行 Python 脚本
python modbus_simulator.py
```

**步骤 2**: 启动 Java 服务
```bash
./mvnw spring-boot:run
```

**步骤 3**: 查看 MQTT 消息
```bash
mosquitto_sub -h localhost -t devices/data
```

**预期输出**:
```
{"deviceId":"device-001","temperature":84.8,"rpm":2710,"timestamp":"2025-04-11T14:56:00Z","rawTemperature":848,"rawRpm":2710}
```

**步骤 4**: 运行测试
```bash
# 运行单元测试
./mvnw test -Dtest=ModbusGatewayServiceTest

# 运行集成测试（需要 MQTT Broker）
./mvnw test -Dtest=ModbusGatewayIntegrationTest
```

---

## 三、Phase 3: 消息接入层 (MQTT → Kafka)

### 3.1 数据输入

**来源**: MQTT Broker (EMQX)
**Topic**: `devices/data`
**格式**: DeviceReading JSON
**QoS**: 1

### 3.2 数据输出

**目标**: Kafka
**Topic**: `device-data`
**分区**: 1
**副本**: 1
**Key**: deviceId (用于相同设备数据分区一致性)

### 3.3 实现方案

#### 方案选型：独立 MQTT Consumer Client

**决策**: 创建独立的 `MqttConsumerConfig`，不与 Gateway 的 Publisher 复用

**原因**:
1. **职责分离**: Gateway 只负责发布，Ingestion 只负责订阅
2. **Client ID 隔离**: 避免 MQTT Broker 将同一 Client ID 的旧连接踢掉
3. **回调独立**: 发布和订阅的回调逻辑不互相干扰
4. **生命周期独立**: Consumer 可以在应用启动后才订阅，不影响 Gateway 的发布

#### 实现类设计

**文件**: `com.example.iot.ingestion.MqttIngestionService`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MqttIngestionService {
    private final IotProperties iotProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private MqttClient mqttClient;
    
    @PostConstruct
    public void init() {
        // 初始化 MQTT Consumer Client
        // 订阅 devices/data topic
    }
    
    @PreDestroy
    public void cleanup() {
        // 断开连接
    }
}
```

### 3.4 消息处理流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        MQTT Message Arrived                      │
│                    (devices/data, QoS 1)                         │
└─────────────────────────┬───────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│              Step 1: JSON 反序列化                                │
│  DeviceReading reading = objectMapper.readValue(json, DeviceReading.class) │
│  - 失败：记录 ERROR，跳过消息（不抛异常）                          │
└─────────────────────────┬───────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│              Step 2: Kafka 发送                                   │
│  kafkaTemplate.send("device-data", deviceId, json)               │
│  - 使用异步发送（ListenableFuture）                               │
│  - SuccessCallback: DEBUG 日志                                    │
│  - FailureCallback: ERROR 日志（消息丢失，后续可加 DLQ）            │
└─────────────────────────────────────────────────────────────────┘
```

### 3.5 配置参数

```yaml
iot:
  mqtt:
    consumer:
      enabled: true
      client-id: java-ingestion-001  # 与 gateway 区分
      topic: devices/data
      qos: 1

spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      retries: 3
      acks: 1
```

### 3.6 错误处理策略

| 场景 | 处理 | 日志级别 |
|------|------|---------|
| JSON 解析失败 | 跳过该消息，继续接收 | ERROR |
| Kafka 发送失败 | 记录失败，不阻塞 MQTT 接收 | ERROR |
| MQTT 连接断开 | 依赖自动重连机制 | WARN → ERROR |
| 应用关闭 | @PreDestroy 清理资源 | INFO |

### 3.7 幂等性考虑

- MQTT QoS 1 可能导致消息重复
- Kafka Producer 配置 `enable.idempotence=true`（默认）
- 消费端通过 `deviceId + timestamp` 去重

### 3.8 实现状态

- [x] 创建 `MqttConsumerConfig`（独立 MQTT Consumer Client）
- [x] 创建 `MqttIngestionService`
- [x] MQTT 订阅 (@PostConstruct 初始化订阅)
- [x] 消息反序列化 (JSON → DeviceReading)
- [x] Kafka 发送 (使用 KafkaTemplate)
- [x] 错误处理和日志记录

**实现文件**：
- `MqttConsumerConfig.java` - 独立 Consumer Client 配置
- `MqttIngestionService.java` - MQTT → Kafka 转发逻辑

#### 单元测试
**文件**: `MqttIngestionServiceTest.java`

| 测试方法 | 验证点 |
|---------|--------|
| `testMessageReceivedAndSentToKafka` | 正常消息流转 |
| `testInvalidJsonHandling` | JSON 解析失败处理 |
| `testKafkaSendFailure` | Kafka 发送失败处理 |
| `testMqttReconnect` | MQTT 重连逻辑 |

#### 集成测试
**文件**: `MqttIngestionIntegrationTest.java`

```bash
# 1. 启动服务
./mvnw spring-boot:run -Dspring.profiles.active=test

# 2. 用 mosquitto_pub 发送测试消息
mosquitto_pub -h localhost -t devices/data -m '{"deviceId":"device-001","temperature":75.5,"rpm":2000,"timestamp":"2025-04-11T10:00:00Z"}'

# 3. 用 kafka-console-consumer 验证
kafka-console-consumer --bootstrap-server localhost:9092 --topic device-data --from-beginning
```

---

## 四、Phase 4: 数据消费层 (Kafka → DB/Redis/SSE)

### 4.1 数据输入

**来源**: Kafka
**Topic**: `device-data`
**消费组**: `iot-consumer-group`

### 4.2 数据输出

| 存储 | 用途 | 数据格式 |
|-----|------|---------|
| InfluxDB | 时序历史数据 | Point (measurement: device_metrics) |
| Redis | 最新状态缓存 | Key: device:latest:{deviceId}, TTL: 30s |
| SSE | 实时推送给前端 | Server-Sent Events 格式 |

### 4.3 实现类设计

**文件**: `com.example.iot.consumer.DeviceDataConsumer`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceDataConsumer {
    private final InfluxDBClient influxDBClient;
    private final StringRedisTemplate redisTemplate;
    private final SseEmitterManager sseEmitterManager;
    
    @KafkaListener(topics = "${iot.kafka.topic}", groupId = "${iot.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        DeviceReading reading = parseRecord(record);
        
        // 并行写入三个目的地
        writeToInfluxDB(reading);
        writeToRedis(reading);
        pushToSse(reading);
    }
}
```

### 4.4 待实现功能

- [ ] 创建 `DeviceDataConsumer`
- [ ] @KafkaListener 消费消息
- [ ] InfluxDB 写入
- [ ] Redis 写入（含 TTL）
- [ ] SSE 推送（替代 WebSocket）

---

## 五、Phase 5: SSE 实时推送层

### 5.1 方案变更：WebSocket → SSE

**决策**: 使用 **Server-Sent Events (SSE)** 替代 WebSocket

**原因**:
1. **单向推送**: 服务端 → 客户端，不需要双向通信
2. **基于 HTTP**: 天然支持认证、负载均衡，无需额外协议
3. **连接管理**: 通过标准 TCP 连接生命周期管理，前端关闭自动断开
4. **自动重连**: 浏览器内置 EventSource 自动重连机制
5. **简单性**: 无需 WebSocket 握手、帧解析等复杂逻辑

### 5.2 SSE 技术特性

| 特性 | 说明 |
|------|------|
| 协议 | HTTP/1.1 (持久连接) |
| MIME 类型 | `text/event-stream` |
| 数据格式 | `data: {json}\n\n` |
| 连接数限制 | 浏览器默认 6 个同域连接 |
| 跨域 | 支持 CORS |
| 重连 | 浏览器自动重连，可通过 `retry:` 配置间隔 |

### 5.3 实现架构

**职责边界**：Redis `device:latest:*` 作为最新值缓存持续更新；SSE 仅作为推送出口，只有存在 SSE 连接时才会发布 Redis Pub/Sub 消息。

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端浏览器                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  EventSource('http://localhost:8080/api/iot/stream')        ││
│  │  - 自动处理重连                                              ││
│  │  - 连接关闭时自动停止接收                                     ││
│  └─────────────────────────┬───────────────────────────────────┘│
└────────────────────────────┼────────────────────────────────────┘
                             │ HTTP GET
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Java IoT 服务端                              │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  IotSseController                                           ││
│  │  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE) ││
│  │  └──> SseEmitter (Spring MVC)                               ││
│  └─────────────────────────┬───────────────────────────────────┘│
│                            │ register / remove emitter          │
│                            ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  SseEmitterManager (ConcurrentHashMap<String, SseEmitter>)  ││
│  │  - 管理活跃连接                                              ││
│  │  - 自动清理已完成的 emitter                                  ││
│  └─────────────────────────┬───────────────────────────────────┘│
│                            │ pushToAllEmitters()                │
└────────────────────────────┼────────────────────────────────────┘
                             │
┌─────────────────────────────────────────────────────────────────┐
│                 Kafka / Redis 消费链路                           │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  DeviceDataConsumer.onMessage()                              ││
│  │  - 写 Redis latest key                                       ││
│  │  - 若有 SSE 客户端：convertAndSend(pubsub-channel, json)      ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │ Redis Pub/Sub                    │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  RedisDeviceDataSubscriber.onMessage()                       ││
│  │  → sseEmitterManager.broadcast(json)                         ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 5.4 核心实现类

**Redis 推送出口（Pub/Sub）**：`iot.redis.pubsub-channel`（默认 `iot:device-data`）

**文件**: `com.example.iot.sse.SseEmitterManager`

```java
@Component
@Slf4j
public class SseEmitterManager {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(0L); // 不设置超时
        emitters.put(clientId, emitter);
        
        // 清理回调
        emitter.onCompletion(() -> remove(clientId));
        emitter.onTimeout(() -> remove(clientId));
        emitter.onError(e -> remove(clientId));
        
        return emitter;
    }
    
    public void broadcast(String jsonPayload) {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("device-data")
                    .data(jsonPayload));
            } catch (IOException e) {
                log.error("Failed to send SSE to client {}: {}", id, e.getMessage());
                remove(id);
            }
        });
    }
    
    private void remove(String clientId) {
        emitters.remove(clientId);
        log.info("SSE emitter removed: {}", clientId);
    }
}
```

**文件**: `com.example.iot.controller.IotSseController`

```java
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotSseController {
    private final SseEmitterManager sseEmitterManager;
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeviceData() {
        String clientId = UUID.randomUUID().toString();
        return sseEmitterManager.createEmitter(clientId);
    }
}
```

### 5.5 TCP 连接生命周期管理

**自动关闭机制**:

```
前端页面加载
    ↓
创建 EventSource → 发起 HTTP GET /api/iot/stream
    ↓
后端创建 SseEmitter，保持连接打开
    ↓
Kafka 消息到达 → pushToAllEmitters() → 发送 data: {...}\n\n
    ↓
前端页面关闭/刷新/网络断开
    ↓
TCP 连接关闭 (浏览器自动处理)
    ↓
SseEmitter 触发 onCompletion/onError 回调
    ↓
从 emitters Map 中移除
```

**关键设计**:
- 不设置 `SseEmitter` 超时（`new SseEmitter(0L)`）
- 依赖浏览器关闭 TCP 连接来触发清理
- 异常时主动移除 emitter 防止内存泄漏

### 5.6 前端使用示例

```javascript
// 连接 SSE
const eventSource = new EventSource('http://localhost:8080/api/iot/stream');

// 接收消息
eventSource.addEventListener('device-reading', (event) => {
    const data = JSON.parse(event.data);
    console.log('Received:', data);
    // 更新 UI...
});

// 错误处理
eventSource.onerror = (error) => {
    console.error('SSE error:', error);
    // 浏览器会自动重连，无需手动处理
};

// 页面关闭时自动断开
// 无需手动调用 close()，浏览器会自动处理
```

### 5.7 配置参数

```yaml
iot:
  sse:
    enabled: true
    path: /api/iot/stream
    event-name: device-reading
    # 无超时设置，依赖 TCP 连接生命周期
```

### 5.8 测试策略

#### 单元测试
**文件**: `SseEmitterManagerTest.java`

| 测试方法 | 验证点 |
|---------|--------|
| `testCreateEmitter` | emitter 创建成功 |
| `testBroadcastToMultipleClients` | 多客户端广播 |
| `testRemoveOnCompletion` | 完成后自动清理 |
| `testRemoveOnError` | 错误时自动清理 |

#### 集成测试
**文件**: `IotSseIntegrationTest.java`

```bash
# 1. 启动服务
./mvnw spring-boot:run

# 2. 用 curl 测试 SSE 连接
curl -N http://localhost:8080/api/iot/stream

# 3. 发送 Kafka 测试消息（应能在 curl 看到推送）
kafka-console-producer --bootstrap-server localhost:9092 --topic device-data
> {"deviceId":"device-001","temperature":80.0,"rpm":2500}

# 4. 用浏览器测试
# 打开浏览器控制台:
eventSource = new EventSource('http://localhost:8080/api/iot/stream');
eventSource.onmessage = (e) => console.log(e.data);
```

---

## 六、通用设计原则

### 5.1 日志规范
- 使用 Lombok `@Slf4j`
- 关键操作 INFO 级别
- 调试信息 DEBUG 级别
- 错误必须包含异常类型和消息

### 5.2 异常处理
- 不要吞掉异常，至少记录日志
- 外部服务调用要有重试机制
- 优雅降级（一个环节失败不影响其他环节）

### 5.3 配置管理
- 所有可变参数走配置文件
- 使用 IotProperties 集中管理
- 提供合理的默认值

### 5.4 测试策略
- 每个 Phase 可独立测试
- 使用 CLI 工具验证（mosquitto_sub, kafka-console-consumer）
- 保留原始值用于数据核对

---

## 七、变更记录

| 日期 | 版本 | 变更内容 |
|-----|------|---------|
| 2025-04-11 | v0.5 | Phase 3 实现完成：MqttConsumerConfig + MqttIngestionService |
| 2025-04-11 | v0.4 | Phase 3 设计完成，添加 MQTT → Kafka 方案；Phase 5 改为 SSE 实现 |
| 2025-04-11 | v0.3 | Phase 2 测试完成，8个单元测试 + 5个集成测试全部通过 |
| 2025-04-11 | v0.2 | Phase 2 完成，添加单元测试和集成测试 |
| 2025-04-11 | v0.1 | 创建文档，完成 Phase 2 设计和实现 |

---

## Phase 4 实施更新（2026-04-12）

### 当前实现状态

- 已新增 `com.example.iot.consumer.DeviceDataConsumer`
- 消费入口：`@KafkaListener(topics = "${iot.kafka.topic}")`
- InfluxDB measurement：`device_metrics`
- InfluxDB tag：`deviceId`
- InfluxDB field：`temperature`、`rpm`
- Redis key：`${iot.redis.key-prefix}{deviceId}`
- Redis TTL：`${iot.redis.ttl-seconds}`，当前默认 `30s`

### 当前测试状态

- 单元测试：`src/test/java/com/example/iot/consumer/DeviceDataConsumerTest.java`
- 集成测试：`src/test/java/com/example/iot/consumer/DeviceDataConsumerIntegrationTest.java`

### 单元测试覆盖点

- 合法 Kafka JSON 可同时写入 InfluxDB 和 Redis
- 非法 JSON 不会写入任何下游存储
- 缺少 `timestamp` 时使用兜底时间继续写 InfluxDB
- InfluxDB 写入失败时，Redis 仍继续更新
- Redis 写入失败时，InfluxDB 仍可正常写入

### 集成测试覆盖点

- 真实 Kafka 消息可落到 Redis
- 真实 Kafka 消息可落到 InfluxDB
- Redis TTL 生效
- 非法 Kafka JSON 不会写入 Redis / InfluxDB

### 手动验证入口

- InfluxDB Web UI：`http://localhost:8086`
- EMQX Dashboard：`http://localhost:18083`
  - 默认账号：`admin`
  - 默认密码：`public`
- Grafana：`http://localhost:3000`
  - 默认账号：`admin`
  - 默认密码：`admin`

### 关于“数据库数据去哪里看”

- Phase 4 的历史数据在 **InfluxDB**，浏览器直接打开 `http://localhost:8086`
- Phase 4 的最新缓存数据在 **Redis**，当前项目没有自带 Redis 可视化页面
- Redis 建议用命令行查看：

```bash
redis-cli GET device:latest:device-001
redis-cli TTL device:latest:device-001
```
