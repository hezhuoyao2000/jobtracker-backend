# 用户认证系统说明文档

> 本文档介绍当前用户认证登录注册系统的实现情况、安全状况以及与业界标准的对比。
>
> **状态：已实施标准认证（2026-03-08 更新）**

---

## 一、系统概述

### 1.1 当前实现的功能

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户注册 | ✅ | 用户名+密码注册，密码 BCrypt 加密存储 |
| 用户登录 | ✅ | 用户名+密码验证登录 |
| JWT Token 生成 | ✅ | 使用 jjwt 库生成 HS256 签名 Token |
| JWT Token 验证 | ✅ | 过滤器自动验证请求中的 Token |
| Token 过期处理 | ✅ | 默认 7 天有效期 |
| 密码加密存储 | ✅ | BCrypt 加密，安全存储 |
| 用户表管理 | ✅ | users 表支持完整字段 |

### 1.2 接口列表

| 方法 | 路径 | 说明 | 需认证 |
|------|------|------|--------|
| POST | `/auth/register` | 用户注册（用户名+密码） | ❌ |
| POST | `/auth/login` | 用户登录（用户名+密码） | ❌ |
| POST | `/board/load` | 加载看板 | ✅ |
| POST | `/board/create` | 创建看板 | ✅ |
| POST | `/board/column/update` | 更新列 | ✅ |
| POST | `/board/card/create` | 创建卡片 | ✅ |
| POST | `/board/card/update` | 更新卡片 | ✅ |
| POST | `/board/card/move` | 移动卡片 | ✅ |
| POST | `/board/card/delete` | 删除卡片 | ✅ |

---

## 二、系统现状

### 2.1 已实现的安全功能 ✅

1. **密码验证**
   - 登录时必须提供正确的用户名和密码
   - 密码使用 BCrypt 算法加密存储
   - 登录失败统一返回 `401`，不区分用户名不存在或密码错误（防止用户枚举）

2. **JWT 认证机制**
   - 基于 Token 的无状态认证
   - 请求头 `Authorization: Bearer <token>` 方式传递
   - Token 有效期 7 天

3. **统一异常处理**
   - 401 未认证错误统一返回
   - 403 无权限错误处理
   - 400 参数错误处理（如密码长度不足）
   - 409 冲突处理（用户名已存在）

4. **Swagger 集成**
   - API 文档自动生成
   - 支持在线接口测试

5. **CORS 配置**
   - 支持跨域访问配置
   - 允许携带认证凭证

### 2.2 未实现的功能（后续迭代）

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 邮箱验证 | 注册不验证邮箱格式和唯一性 | 🟡 中 |
| Token 刷新机制 | 无 Refresh Token | 🟡 中 |
| Token 黑名单/登出 | 无法主动失效 Token | 🟡 中 |
| 登录限流 | 无防暴力破解机制 | 🟡 中 |
| 用户资料修改 | 无法修改用户名/邮箱等 | 🟢 低 |
| 密码找回/重置 | 无此功能 | 🟢 低 |
| 多设备登录管理 | 无法查看/管理登录设备 | 🟢 低 |

---

## 三、安全分析

### 3.1 已修复的安全问题 ✅

| 问题 | 修复方式 | 状态 |
|------|----------|------|
| 无密码验证 | 添加密码验证，BCrypt 加密存储 | ✅ 已修复 |
| 密码明文存储 | 使用 BCrypt 算法加密 | ✅ 已修复 |
| 用户枚举攻击 | 登录失败统一返回 "用户名或密码错误" | ✅ 已修复 |

### 3.2 当前安全风险 ⚠️

| 风险 | 严重程度 | 说明 |
|------|----------|------|
| Token 泄露风险 | 🟡 中 | 无 HTTPS 时 Token 可被截获 |
| 无登录限流 | 🟡 中 | 可被暴力破解密码 |
| 敏感信息日志 | 🟢 低 | Token 可能出现在日志中 |
| 邮箱未验证 | 🟢 低 | 注册时不验证邮箱真实性 |

### 3.3 已采取的安全措施 ✅

1. **BCrypt 密码加密**
   - 使用 `BCryptPasswordEncoder` 加密存储密码
   - 自动加盐，每次加密结果不同

2. **JWT 签名验证**
   - 使用密钥签名，防止 Token 篡改
   - 密钥可配置，建议生产环境使用强密钥

3. **Token 有效期限制**
   - 默认 7 天过期，减少长期泄露风险

4. **CORS 白名单**
   - 可配置允许的来源域名

5. **SQL 注入防护**
   - 使用 JPA 参数化查询

---

## 四、与业界标准对比

### 4.1 已实现的核心功能

| 业界标准功能 | 本系统状态 | 说明 |
|--------------|------------|------|
| 密码 + 盐哈希存储 | ✅ 已实现 | 使用 BCrypt |
| JWT Token 认证 | ✅ 已实现 | jjwt 0.12.3 |
| Token 有效期控制 | ✅ 已实现 | 默认 7 天 |

### 4.2 待实现的核心功能

| 业界标准功能 | 本系统状态 | 说明 |
|--------------|------------|------|
| 登录失败锁定 | ❌ 未实现 | 连续失败 5 次锁定 30 分钟 |
| 双因素认证 (2FA) | ❌ 未实现 | 短信/邮箱/OTP 验证 |
| OAuth2/第三方登录 | ❌ 未实现 | 微信/Google/GitHub 登录 |
| 会话管理 | ❌ 未实现 | 查看在线设备、强制下线 |
| 审计日志 | ❌ 未实现 | 记录登录/操作日志 |
| RBAC 权限控制 | ❌ 未实现 | 角色权限管理 |

### 4.3 技术栈对比

| 组件 | 业界常用 | 本系统使用 | 评价 |
|------|----------|------------|------|
| JWT 库 | jjwt, Auth0 | jjwt 0.12.3 | ✅ 主流选择 |
| 密码哈希 | BCrypt, Argon2 | BCrypt | ✅ 业界标准 |
| 认证框架 | Spring Security | 自定义 Filter | ⚠️ 简化实现，够用 |
| Token 存储 | Redis + DB | 仅 JWT | ⚠️ 无状态但无法吊销 |

---

## 五、API 使用示例

### 5.1 注册请求

```http
POST /auth/register
Content-Type: application/json

{
    "username": "john_doe",
    "password": "securePassword123",
    "displayName": "John Doe",
    "email": "john@example.com"
}
```

**响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "username": "john_doe",
        "displayName": "John Doe",
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer"
    }
}
```

### 5.2 登录请求

```http
POST /auth/login
Content-Type: application/json

{
    "username": "john_doe",
    "password": "securePassword123"
}
```

**响应（成功）：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "username": "john_doe",
        "displayName": "John Doe",
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer"
    }
}
```

**响应（失败）：**
```json
{
    "code": 401,
    "message": "用户名或密码错误",
    "data": null
}
```

### 5.3 调用需认证接口

```http
POST /board/load
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
    "boardId": null
}
```

---

## 六、前端适配指南

### 6.1 变更要点

**2026-03-08 更新后，前端需要调整：**

1. **登录接口**：
   - 旧：`{ userId: string }`
   - 新：`{ username: string, password: string }`

2. **注册接口**：
   - 旧：`{ username: string, ... }`
   - 新：`{ username: string, password: string, ... }`

3. **登录/注册响应**：
   - 新增：`username`, `displayName` 字段

### 6.2 前端示例代码

```typescript
// 登录
async function login(username: string, password: string) {
    const response = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    const result = await response.json();
    if (result.code === 200) {
        localStorage.setItem('token', result.data.token);
        localStorage.setItem('userId', result.data.userId);
        localStorage.setItem('username', result.data.username);
        localStorage.setItem('displayName', result.data.displayName);
    }
    return result;
}

// 注册
async function register(username: string, password: string, displayName?: string) {
    const response = await fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, displayName })
    });
    const result = await response.json();
    if (result.code === 200) {
        localStorage.setItem('token', result.data.token);
        // ... 保存其他字段
    }
    return result;
}
```

---

## 七、后续开发计划

### 7.1 第一阶段：增强安全（推荐）

1. **登录限流**
   - 同一 IP 5 分钟内最多 5 次登录尝试
   - 失败 5 次锁定账户 30 分钟

2. **邮箱验证**
   - 注册发送验证邮件
   - 邮箱唯一性检查

3. **Token 刷新机制**
   - Access Token: 2 小时
   - Refresh Token: 30 天
   - 刷新接口 `/auth/refresh`

4. **登出功能**
   - Token 黑名单（Redis 存储）
   - `/auth/logout` 接口

### 7.2 第二阶段：企业级（可选）

1. **Spring Security 集成**
   - 完整的安全框架
   - 方法级权限控制 `@PreAuthorize`

2. **OAuth2 第三方登录**
   - 微信登录
   - GitHub 登录

3. **审计日志**
   - 记录所有登录/登出/敏感操作

4. **多因素认证 (MFA)**
   - TOTP 基于时间的一次性密码

---

## 八、生产环境建议

### 8.1 配置文件

**JWT 配置（application.yml）：**

```yaml
jwt:
  secret: ${JWT_SECRET}  # 从环境变量读取，32位以上随机字符串
  expiration: 7200000    # 2 小时
```

**生产环境建议配置：**

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 7200000    # 2 小时

server:
  ssl:
    enabled: true
```

### 8.2 部署检查清单

- [ ] 配置强 JWT 密钥（环境变量）
- [ ] 启用 HTTPS
- [ ] 配置 CORS 白名单（生产环境域名）
- [ ] 配置日志级别（避免敏感信息泄露）
- [ ] 设置 Token 过期时间（建议 2 小时）

---

## 九、总结

当前认证系统已实施**标准密码认证**：

| 功能 | 状态 |
|------|------|
| ✅ BCrypt 密码加密存储 | 已实施 |
| ✅ 用户名+密码登录验证 | 已实施 |
| ✅ JWT Token 认证 | 已实施 |
| ✅ 统一错误处理 | 已实施 |
| ⚠️ 登录限流 | 待实施 |
| ⚠️ Token 刷新/登出 | 待实施 |
| ❌ OAuth2 登录 | 可选 |

**系统已适用于：**
- ✅ 功能开发和测试
- ✅ 前端联调
- ✅ 演示和原型
- ⚠️ 生产环境（建议增加登录限流和 HTTPS）

---

## 十、文档更新记录

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2026-03-08 | v1.0 | 初始版本，无密码认证 |
| 2026-03-08 | v2.0 | **标准认证系统**：<br>- 添加 BCrypt 密码加密<br>- 登录验证用户名+密码<br>- 更新 API 响应字段 |
