# IoT 工业数据采集链路 — 基础设施搭建需求文档

## 项目背景

在现有 Java 工单管理系统项目基础上，新增一套工业 IoT 数据采集演示模块。目的是打通从设备数据模拟到实时看板展示的完整数据链路，作为技术学习 Demo 使用，无需考虑生产数据安全。

---

## 目标架构

```
模拟PLC (Python pymodbus Server)
        ↓ Modbus TCP 轮询读取
Java 网关程序 (Gateway Service)
        ↓ MQTT Publish
EMQX (MQTT Broker 容器)
        ↓ MQTT Subscribe → Kafka Produce
Java 接入服务 (Ingestion Service)
        ↓
    Kafka Topic: device-data
        ↓
Java Consumer (两路并行)
    ↙               ↘
InfluxDB            Redis
(时序持久化)        (最新状态缓存)
                        ↓
                  WebSocket 推送
                        ↓
                  前端实时看板 (已有 React 前端，新增一个页面)
```

**注意：**
- Modbus 和 OPC UA 是通信协议，不需要独立容器，用 Python 库模拟即可
- EMQX、Kafka、InfluxDB、Redis 需要独立容器
- 新增模块与现有工单系统代码隔离，放在独立 package 下

---

## 任务一：新建基础设施目录

### 1.1 目录结构

在项目根目录之外（或同级）新建目录 `iot-infra/`，结构如下：

```
iot-infra/
  docker-compose.yml
  emqx/
    emqx.conf
  kafka/
    （Kafka 使用 KRaft 模式，不需要 ZooKeeper）
  influxdb/
    （通过环境变量初始化，不需要额外配置文件）
  redis/
    redis.conf
```

### 1.2 docker-compose.yml 要求

包含以下四个服务，全部使用**命名 volume** 挂载数据（方便随时清空重来）：

#### Redis
- 镜像：`redis:7-alpine`
- 端口映射：`6379:6379`
- 挂载 `redis.conf`，开启 appendonly 持久化

#### Kafka（KRaft 模式，不需要 ZooKeeper）
- 镜像：`confluentinc/cp-kafka:7.6.0`
- 端口映射：`9092:9092`
- 使用 KRaft 模式（单节点），需要设置以下关键环境变量：
  - `KAFKA_NODE_ID: 1`
  - `KAFKA_PROCESS_ROLES: broker,controller`
  - `KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093`
  - `KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:29093`
  - `KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092`
  - `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT`
  - `KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER`
  - `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1`
  - `CLUSTER_ID`：需要生成一个合法的 base64 UUID，固定写死即可，例如 `MkU3OEVBNTcwNTJENDM2Qg==`
- 启动后自动创建 Topic `device-data`，partition 1，replication 1

#### EMQX（MQTT Broker）
- 镜像：`emqx:5`
- 端口映射：
  - `1883:1883`（MQTT TCP）
  - `18083:18083`（Web 管理后台，默认账号 admin/public）
- 允许匿名连接（关闭认证），在 `emqx.conf` 中配置：
  ```
  allow_anonymous = true
  ```

#### InfluxDB
- 镜像：`influxdb:2.7`
- 端口映射：`8086:8086`
- 通过环境变量完成初始化（无需手动操作）：
  - `DOCKER_INFLUXDB_INIT_MODE: setup`
  - `DOCKER_INFLUXDB_INIT_USERNAME: admin`
  - `DOCKER_INFLUXDB_INIT_PASSWORD: password123`
  - `DOCKER_INFLUXDB_INIT_ORG: iot-demo`
  - `DOCKER_INFLUXDB_INIT_BUCKET: device-metrics`
  - `DOCKER_INFLUXDB_INIT_ADMIN_TOKEN: my-super-secret-token`（固定 token，方便开发）

#### 网络
- 所有服务加入同一个自定义 bridge 网络 `iot-net`，服务间用服务名互相访问

#### 可选（推荐加上）：Grafana
- 镜像：`grafana/grafana:latest`
- 端口：`3000:3000`
- 默认账号：admin/admin
- 启动后手动在界面里添加 InfluxDB 数据源即可

---

## 任务二：模拟 PLC 脚本（Python）

新建文件 `iot-infra/simulator/plc_simulator.py`

### 要求

- 使用 `pymodbus` 库启动一个 Modbus TCP Server，监听 `0.0.0.0:502`
- 每秒随机更新以下两个寄存器的值，模拟传感器数据：
  - 寄存器地址 0：温度，范围 60~100（整数，单位 °C × 10，即 650 = 65.0°C）
  - 寄存器地址 1：转速，范围 1000~3000（RPM）
- 同时在控制台每秒打印当前寄存器值，方便观察

### 依赖
```
pymodbus==3.6.0
```

新建 `iot-infra/simulator/requirements.txt`，写入上述依赖。

---

## 任务三：Java 项目新增 IoT 模块

在现有 Spring Boot 项目中新增以下内容，**不修改任何现有代码**。

### 3.1 新增 Maven 依赖（pom.xml）

```xml
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

<!-- WebSocket (Spring Boot Starter) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 3.2 application.yml 新增配置

```yaml
# IoT 模块配置
iot:
  modbus:
    host: localhost
    port: 502
    poll-interval-ms: 1000    # 每秒轮询一次

  mqtt:
    broker: tcp://localhost:1883
    client-id: java-gateway-001
    topic: devices/data

  influxdb:
    url: http://localhost:8086
    token: my-super-secret-token
    org: iot-demo
    bucket: device-metrics

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: iot-consumer-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### 3.3 新增 package 结构

在 `com.yourproject.iot` 下新建以下类（**全部新建，不修改现有文件**）：

```
com.yourproject.iot/
  config/
    MqttConfig.java          # MQTT 客户端 Bean 配置
    InfluxDbConfig.java      # InfluxDB 客户端 Bean 配置
    WebSocketConfig.java     # WebSocket 配置
  model/
    DeviceReading.java       # 数据模型：deviceId, temperature, rpm, timestamp
  gateway/
    ModbusGatewayService.java    # 定时轮询 Modbus，转换后发 MQTT
  ingestion/
    MqttIngestionService.java    # 订阅 MQTT，转发到 Kafka
  consumer/
    DeviceDataConsumer.java      # 消费 Kafka，写 InfluxDB + 更新 Redis
  websocket/
    DeviceDataWebSocketHandler.java  # WebSocket Handler，推送实时数据给前端
  controller/
    IotDemoController.java       # REST 接口：查询 Redis 最新状态、查询 InfluxDB 历史数据
```

### 3.4 各类职责说明

**ModbusGatewayService.java**
- 使用 `@Scheduled(fixedRate = 1000)` 每秒执行一次
- 用 j2mod 的 `ModbusTCPMaster` 连接 plc_simulator，读取寄存器 0 和 1
- 将读取到的值封装成 `DeviceReading` 对象，序列化为 JSON
- 通过 MQTT 发布到 topic `devices/data`

**MqttIngestionService.java**
- Spring Bean 初始化时订阅 MQTT topic `devices/data`
- 收到消息后，反序列化为 `DeviceReading`
- 用 `KafkaTemplate` 发送到 Kafka topic `device-data`

**DeviceDataConsumer.java**
- 用 `@KafkaListener(topics = "device-data")` 消费消息
- 同时执行两件事：
  1. 用 InfluxDB Java Client 写入一条时序数据，measurement 为 `device_metrics`，tag 为 `deviceId`，field 为 `temperature` 和 `rpm`
  2. 用 `StringRedisTemplate` 将最新读数写入 Redis，key 为 `device:latest:{deviceId}`，value 为 JSON，设置 TTL 30 秒

**DeviceDataWebSocketHandler.java**
- 继承 `TextWebSocketHandler`
- 维护一个在线 session 列表
- 提供一个方法 `broadcast(String message)`，由 `DeviceDataConsumer` 在写完 Redis 后调用，将最新数据推送给所有连接的 WebSocket 客户端

**IotDemoController.java**
- `GET /api/iot/latest/{deviceId}`：从 Redis 读取最新状态返回
- `GET /api/iot/history/{deviceId}?minutes=5`：从 InfluxDB 查询最近 N 分钟历史数据返回

---

## 任务四：前端新增看板页面

在现有 React 前端项目中新增一个路由页面 `/iot-dashboard`。

### 要求

- 页面顶部显示设备实时状态卡片（温度 + 转速），数据来源：WebSocket 实时推送
- 页面下方显示两条折线图（温度趋势、转速趋势），数据来源：首次加载调用 `/api/iot/history` 获取历史数据，之后通过 WebSocket 追加新数据点
- 折线图使用已有的 ECharts 或 Chart.js（项目中已有哪个用哪个）
- WebSocket 连接地址：`ws://localhost:8080/ws/iot`
- 断线后每 3 秒自动重连一次

---

## 验收标准

完成后，按以下步骤验收：

1. 进入 `iot-infra/` 目录，执行 `docker compose up -d`，四个容器全部启动
2. 进入 `iot-infra/simulator/` 目录，执行 `python plc_simulator.py`，控制台每秒输出温度和转速
3. 启动 Java 项目，观察控制台日志，应看到 Modbus 读取 → MQTT 发布 → Kafka 消费 → InfluxDB 写入的日志输出
4. 打开 Redis CLI 执行 `GET device:latest:device-001`，应返回最新的 JSON 数据
5. 打开浏览器访问 `http://localhost:8086`，在 InfluxDB 界面查询 `device_metrics`，应有持续写入的数据
6. 打开前端 `/iot-dashboard` 页面，折线图应实时滚动更新

---

## 注意事项

- 所有新增代码放在独立 package 和独立文件中，不修改工单系统现有任何文件
- `plc_simulator.py` 中 deviceId 固定为 `device-001`，方便 Demo 演示
- 所有密码、token 均使用明文写死，这是 Demo 项目，不考虑安全
- Kafka KRaft 模式首次启动需要格式化 metadata，docker-compose 中通过 `command` 处理，需确保幂等（多次 up/down 不报错）
- EMQX 管理后台地址：`http://localhost:18083`，默认账号 `admin` / `public`
