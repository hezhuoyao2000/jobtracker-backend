<div align="center">

# ✨ 求职看板后端 API ✨

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791.svg)

**[📚 Swagger API 文档](http://localhost:8080/swagger-ui/index.html)**

简体中文 | [English](./README.en-US.md)

</div>

## 📋 项目概述

求职看板后端 API 是一个基于 Spring Boot 构建的现代化 RESTful API 服务，为求职申请跟踪看板提供完整的后端支持。该项目采用分层架构设计，支持用户认证、JWT 令牌、看板管理等功能。

## ✨ 核心功能

- **用户认证**: 注册、登录功能，JWT Token 认证
- **看板管理**: 创建、加载用户看板数据
- **列管理**: 支持自定义列（待投递、已投递、面试中、收到 Offer、已拒绝）
- **卡片管理**: 完整的 CRUD 操作，支持拖拽排序
- **软删除**: 卡片支持软删除功能
- **RESTful API**: 完整的 API 接口，配合 Swagger 文档

## 🛠 技术栈

### 后端框架
- **Java 17** - 编程语言
- **Spring Boot 3.5.5** - 应用框架
- **MyBatis Plus** - 数据访问层（支持复杂SQL、逻辑删除）
- **PostgreSQL** - 数据库（UUID 主键、jsonb 字段支持）

### 安全与认证
- **JWT** - JSON Web Token 认证
- **Spring Security** - 安全框架

### 文档与工具
- **SpringDoc OpenAPI** - API 文档（Swagger UI）
- **Lombok** - 简化代码编写
- **Maven** - 项目构建工具

## 🚀 快速开始

### 环境要求
- Java 17+
- PostgreSQL 数据库
- Maven

### 安装步骤

1. 克隆仓库：
```bash
git clone <仓库地址>
cd myfirstspringboot
```

2. 配置数据库连接（application.yml）：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password
```

3. 构建项目：
```bash
mvnw.cmd clean package
# 或
./mvnw clean package
```

4. 启动应用：
```bash
mvnw.cmd spring-boot:run
# 或带数据库密码
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_PASSWORD=your_password"
```

5. 访问 API 文档：
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- API Docs (JSON): http://localhost:8080/v3/api-docs

## 📁 项目结构

```
src/main/java/com/example/myfirstspringboot/
├── config/           # 配置类
├── controller/      # 控制器层
├── dto/             # 数据传输对象
│   ├── request/     # 请求 DTO
│   └── response/    # 响应 DTO
├── entity/          # JPA 实体
├── exception/       # 异常处理
├── repository/      # 数据访问层
├── service/         # 业务逻辑层
│   └── impl/        # 实现类
└── util/            # 工具类
```

## 📖 API 接口

### 认证接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |

### 看板接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/boards | 创建看板 |
| GET | /api/boards/{userId} | 获取用户看板 |

### 列接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/columns | 创建列 |
| PUT | /api/columns/{id} | 更新列 |
| DELETE | /api/columns/{id} | 删除列 |

### 卡片接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/cards | 创建卡片 |
| PUT | /api/cards/{id} | 更新卡片 |
| DELETE | /api/cards/{id} | 删除卡片 |
| PUT | /api/cards/{id}/move | 移动卡片 |

## 🧪 测试

```bash
# 运行所有测试
mvnw.cmd test

# 运行特定测试类
mvnw.cmd test -Dtest=BoardServiceImplTest
```

## 📈 项目状态

**当前版本**: v1.0.0

### ✅ 已完成功能
- 用户注册与登录
- JWT Token 认证
- 看板 CRUD 操作
- 列 CRUD 操作
- 卡片 CRUD 操作
- 卡片拖拽排序
- 软删除功能
- 全局异常处理
- Swagger API 文档
- 响应数据封装

### 🔄 进行中/计划中
- 批量操作优化
- 缓存支持
- 性能监控

## 📄 许可证

本项目采用 MIT 许可证开源。
