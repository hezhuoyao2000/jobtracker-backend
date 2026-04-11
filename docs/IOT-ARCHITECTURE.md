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

**来源**: MQTT Broker
**Topic**: `devices/data`
**格式**: DeviceReading JSON

### 3.2 数据输出

**目标**: Kafka
**Topic**: `device-data`
**分区**: 1
**副本**: 1

### 3.3 待实现功能

- [ ] 创建 `MqttIngestionService`
- [ ] MQTT 订阅 (@PostConstruct 初始化订阅)
- [ ] 消息反序列化 (JSON → DeviceReading)
- [ ] Kafka 发送 (使用 KafkaTemplate)
- [ ] 错误处理和日志记录

### 3.4 设计考虑

#### 1. MQTT Client 复用策略
**方案 A**: 创建新的 MqttClient Bean（推荐）
- 独立的 `consumer-client-id`，避免与 Gateway 冲突
- 独立的回调处理逻辑

**方案 B**: 复用现有 MqttClient
- 使用 `setCallback()` 覆盖回调
- 需要协调发布和订阅的回调逻辑

#### 2. 订阅时机
- 在 Bean 初始化后订阅（@PostConstruct）
- 确保 MqttClient 已连接
- 处理订阅异常，避免应用启动失败

#### 3. 消息处理流程
```
MQTT Message Arrived
    ↓
JSON String → DeviceReading (反序列化)
    ↓
KafkaTemplate.send("device-data", json)
    ↓
Success: 记录 DEBUG 日志
Failure: 记录 ERROR 日志，消息丢失（后续可添加重试）
```

#### 4. 错误处理
- JSON 反序列化失败：记录错误日志，跳过该消息，继续接收下一条
- Kafka 发送失败：记录错误，不抛异常（避免影响 MQTT 订阅）
- MQTT 连接断开：依赖 MqttClient 的自动重连机制

#### 5. Kafka 发送方式
- 使用异步发送（非阻塞，高性能）
- 添加发送成功/失败回调用于日志
- QoS 1 的 MQTT 消息可能重复，Kafka 消费端需要幂等

---

## 四、Phase 4: 数据消费层 (Kafka → DB/Redis)

### 4.1 数据输入

**来源**: Kafka
**Topic**: `device-data`
**消费组**: `iot-consumer-group`

### 4.2 数据输出

| 存储 | 用途 | 数据格式 |
|-----|------|---------|
| InfluxDB | 时序历史数据 | Point (measurement: device_metrics) |
| Redis | 最新状态缓存 | Key: device:latest:{deviceId}, TTL: 30s |

### 4.3 待实现功能

- [ ] 创建 `DeviceDataConsumer`
- [ ] @KafkaListener 消费消息
- [ ] InfluxDB 写入
- [ ] Redis 写入（含 TTL）
- [ ] WebSocket 广播

---

## 五、通用设计原则

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

## 六、变更记录

| 日期 | 版本 | 变更内容 |
|-----|------|---------|
| 2025-04-11 | v0.3 | Phase 2 测试完成，8个单元测试 + 5个集成测试全部通过 |
| 2025-04-11 | v0.2 | Phase 2 完成，添加单元测试和集成测试 |
| 2025-04-11 | v0.1 | 创建文档，完成 Phase 2 设计和实现 |
