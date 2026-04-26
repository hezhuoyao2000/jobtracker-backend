# IOT功能概览
对服务的轮廓认识可以大致分为：
  - config        负责“准备好能用的外部客户端和配置”
  - entrypoint    负责“什么时候触发”
  - application   负责“这一段业务怎么走”
  - infrastructure 负责“具体怎么跟外部系统通信”

## 1. modbus环节概览
应用启动以后，ModbusPollingTask 会先喊 ModbusCollectionService 去连一下 Modbus 设备。连上以后，系统每秒钟跑一次采集任务。每次任务都会先看看 Modbus 开关有没有打开，再看看连接还在不在。如果连接断了，就尝试重连，最多试 3 次。连接正常的话，就通过 ModbusDeviceReader 去读两个寄存器：第一个寄存器是温度原始值，第二个寄存器是转速原始值。读回来以后组装成一个 DeviceReading，里面有设备 ID、温度、转速、采集时间、原始温度值、原始转速值。最后它把这个对象交给MqttDeviceReadingPublisher，发布到 MQTT 的 devices/data topic。发布完以后，Modbus 这一段就完成了，后面由MQTT 接入模块继续往 Kafka 送。

 Modbus 的拓展能力：
  - 多设备采集：现在基本是固定 device-001，以后可以配置多个设备地址、slaveId、寄存器区间。
  - 更多寄存器字段：比如电压、电流、压力、湿度、运行状态、报警码。
  - 寄存器映射配置化：不要把寄存器 0、1 写死在代码里，而是用配置描述每个字段怎么读取、怎么换算。
  - 采集质量标记：给 DeviceReading 增加采集状态，例如 OK、TIMEOUT、BAD_RESPONSE。
  - 设备离线检测：连续失败多少次后标记设备离线，并向下游发设备状态事件。
  - 批量读取优化：一次读取更多连续寄存器，减少网络请求次数。
  - 不同 Modbus 区域支持：除了 Holding Registers，也支持 Coils、Discrete Inputs、Input Registers。
  - 更细的重试策略：比如指数退避、按设备单独计数、恢复后清零。
  - 采集限流或暂停：当 MQTT/Kafka 下游不可用时，可以降低采集频率或暂存。
  - 本地缓冲：严格来说这就开始接近持久化了，可以做短暂内存队列或本地文件缓冲，但这不是当前实现。

###  modbus配置
  Modbus 配置主要是：
```
host: localhost   
port: 502
slave-id: 1
poll-interval-ms: 1000
enabled: true
mock-enabled: false
```
  | 配置 | 作用 |
  |---|---|
  | host | Modbus TCP 设备地址 |
  | port | Modbus TCP 端口，默认 502 |
  | slave-id | Modbus 从站 ID |
  | poll-interval-ms | 轮询间隔，默认 1000ms |
  | enabled | 是否启用 Modbus 采集 |
  | mock-enabled | 是否启用 Mock 采集模式 |




### 2. mqtt环节
mqtt有两个角色，publisher，modbus采集数据后，把数据发布到mqtt，consumer，订阅mqtt，把数据发到kafka

  MQTT 配置：

  Publisher 配置：

  | 配置 | 作用 |
  |---|---|
  | broker | MQTT Broker 地址 |
  | client-id | 发布端 client id，默认 java-gateway-001 |
  | topic | 发布 topic，默认 devices/data |
  | qos | 发布 QoS，默认 1 |
  | connection-timeout | 连接超时时间 |
  | keep-alive-interval | MQTT 心跳间隔 |

  Consumer 配置：

  | 配置 | 作用 |
  |---|---|
  | consumer.enabled | 是否启用 MQTT 订阅接入 |
  | consumer.client-id | 消费端 client id，默认 java-ingestion-001 |
  | consumer.topic | 订阅 topic，默认 devices/data |
  | consumer.qos | 订阅 QoS，默认 1 |
  | consumer.reconnect-interval-ms | 维护重连和重订阅的间隔 |

MQTT环节行为概览：

Modbus模块最后调用mqtt的publish方法，通过通MQTT Client把数据发布到外部 MQTT Broker里，项目内部有messageArrived()，没人调用也能收数据，这是MQTT客户端库的回调机制。 外部MQTT Broker 推消息然后messageArrived()收消息，
```
  Java 应用启动
    ↓
  创建 mqttConsumerClient
    ↓
  MqttDeviceDataListener 把自己注册成 callback
    ↓
  mqttConsumerClient 连接 MQTT Broker
    ↓
  mqttConsumerClient 订阅 devices/data
    ↓
  Broker 有新消息
    ↓
  Paho 客户端库自动调用 messageArrived(...)

  所以 messageArrived 不是“有感知能力”，而是被 MQTT 客户端库回调。
```

objectMapper.readValue(payload, DeviceReading.class);目的是校验 MQTT payload 是不是合法的 DeviceReading JSON以及取出 deviceId 作为 Kafka key

发给kafka后，@KafkaListener 的onMessage方法监听 Kafka topic



  Modbus 那边每秒主动去设备读两个寄存器，读出来后先变成 DeviceReading 对象。这个对象已经不是裸  寄存器了，里面有温度、转速、时间戳和原始值。然后 MQTT 发布器把这个对象用 ObjectMapper 转成  JSON，主动发到外部 MQTT Broker 的 devices/data topic。
  MQTT Broker 收到消息后，Java 应用里的 MQTT consumer 因为已经订阅了这个 topic，所以 Paho 客户  端库会自动回调 messageArrived。这个方法自己不做主逻辑，只是把 topic 和 payload 交给  MqttIngestionService.ingest。MqttIngestionService 把 JSON 解析成 DeviceReading，确认这个消息是合法设备数据，同时取出 deviceId 当 Kafka key，然后把原始 JSON 发到 Kafka 的 device-data  topic。

  Kafka 那边，KafkaDeviceDataListener 因为有 @KafkaListener，所以 Spring Kafka 会自动监听  topic。有消息时，它的 onMessage 会被框架自动调用，然后交给 DeviceDataIngestService.ingest。  这里再次把 JSON 解析成 DeviceReading，因为这一步才是真正要做存储消费。接着先尝试写  InfluxDB，再写 Redis 最新状态，如果当前有 SSE 客户端，就把 Redis 里那份 JSON 发到 Redis Pub/  Sub，最后 Redis listener 收到后推给 SSE 客户端。

  所以你可以这样记：

  Modbus 主动读设备
  MQTT 主动承接设备消息
  Kafka 承接进入后端处理链路
  InfluxDB 存历史
  Redis 存最新
  SSE 推实时

  当前架构的核心不是一个超长 Service 方法一路调到底，而是：

  每个中间件切一刀，每一段有自己的 Listener + Application Service。