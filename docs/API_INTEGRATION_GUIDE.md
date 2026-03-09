# 前后端接口对接指南

> 本文档供前端开发人员参考，明确接口规范和数据类型定义。
> **重要：所有数据类型以后端 Java DTO 定义为准，前端需根据本文档调整类型定义。**

---

## 一、接口规范概述

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 基础 URL | `http://localhost:8080`（开发环境） |
| 接口前缀 | `/board/*`, `/auth/*` |
| 请求方法 | 全部为 `POST` |
| 请求格式 | `application/json` |
| 响应格式 | `application/json` |

### 1.2 统一响应格式

所有接口返回统一包装格式：

```typescript
interface ApiResponse<T> {
  code: number;      // 状态码，200 表示成功
  message: string;   // 响应消息，"success" 或其他错误信息
  data: T;           // 实际数据，类型根据接口不同而变化
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 具体数据内容
  }
}
```

**错误响应示例：**

```json
{
  "code": 404,
  "message": "看板不存在",
  "data": null
}
```

### 1.3 认证方式

- 除登录/注册接口外，**所有接口都需要认证**
- 认证方式：请求头中携带 JWT Token
- Header 格式：`Authorization: Bearer <token>`

**Token 获取方式：**
1. 调用 `/auth/login` 或 `/auth/register` 获取 token
2. 将返回的 `token` 字段存储在本地
3. 后续请求在 Header 中携带该 token

---

## 二、数据类型定义（以后端为准）

### 2.1 类型映射表

| Java 类型 | TypeScript 类型 | 说明 |
|-----------|-----------------|------|
| `UUID` | `string` | UUID 字符串格式，如 `"550e8400-e29b-41d4-a716-446655440000"` |
| `String` | `string` | 普通字符串 |
| `Integer` | `number` | 整数 |
| `Boolean` | `boolean` | 布尔值 |
| `List<String>` | `string[]` | 字符串数组 |
| `Map<String, Object>` | `Record<string, any>` | 任意 JSON 对象 |
| `Instant` → `String` | `string` | ISO 8601 格式时间字符串，如 `"2026-03-07T10:30:00Z"` |

### 2.2 重要类型说明

#### UUID 类型
- 后端使用 `java.util.UUID` 类型
- 前端使用 `string` 类型表示
- 格式示例：`"550e8400-e29b-41d4-a716-446655440000"`

#### 时间类型
- 后端使用 `Instant` 类型，序列化为 ISO 8601 字符串
- 前端使用 `string` 类型
- 格式：`"2026-03-07T10:30:00Z"`（UTC 时间）

---

## 三、Response DTO 定义（后端 → 前端）

### 3.1 BoardDto - 看板信息

```typescript
interface BoardDto {
  id: string;           // UUID - 看板 ID
  userId: string;       // 用户 ID
  name: string;         // 看板名称
  createdAt: string;    // ISO 8601 格式 - 创建时间
  updatedAt: string;    // ISO 8601 格式 - 更新时间
}
```

**后端定义参考：** `dto/response/BoardDto.java`

### 3.2 ColumnDto - 看板列信息

```typescript
interface ColumnDto {
  id: string;           // UUID - 列 ID
  boardId: string;      // UUID - 所属看板 ID
  name: string;         // 列名称，如 "Applied"
  sortOrder: number;    // 排序顺序
  isDefault: boolean;   // 是否为系统默认列
  customAttributes: string | null;  // JSON 字符串，自定义属性
}
```

**后端定义参考：** `dto/response/ColumnDto.java`

### 3.3 JobCardDto - 职位卡片信息

```typescript
interface JobCardDto {
  id: string;           // UUID - 卡片 ID
  boardId: string;      // UUID - 所属看板 ID
  statusId: string;     // UUID - 所属列 ID（状态）
  jobTitle: string;     // 职位名称
  companyName: string;  // 公司名称
  jobLink: string | null;       // 职位链接
  sourcePlatform: string | null; // 来源平台，如 "Boss直聘"
  expired: boolean | null;       // 职位是否已过期
  jobLocation: string | null;    // 工作地点
  description: string | null;    // 职位描述
  appliedTime: string | null;    // ISO 8601 格式 - 申请时间
  tags: string | null;           // 标签，逗号分隔，如 "急招,大厂,高薪"
  comments: string | null;       // 备注
  extra: string | null;          // 扩展字段，JSON 字符串
  createdAt: string;    // ISO 8601 格式 - 创建时间
  updatedAt: string;    // ISO 8601 格式 - 更新时间
}
```

**后端定义参考：** `dto/response/JobCardDto.java`

**注意：**
- `tags` 字段在 Response 中是逗号分隔的字符串（如 `"急招,大厂,高薪"`）
- `extra` 字段是 JSON 字符串，如需使用需要 `JSON.parse()`

### 3.4 BoardDataDto - 看板完整数据

```typescript
interface BoardDataDto {
  board: BoardDto;              // 看板基本信息
  columns: ColumnDto[];         // 看板列列表
  cards: JobCardDto[];          // 看板卡片列表（未删除的）
}
```

**后端定义参考：** `dto/response/BoardDataDto.java`

---

## 四、Request DTO 定义（前端 → 后端）

### 4.1 LoadBoardRequest - 加载看板

```typescript
interface LoadBoardRequest {
  boardId?: string;     // UUID - 可选，不传则返回用户的第一个看板
}
```

**后端定义参考：** `dto/request/LoadBoardRequest.java`

### 4.2 CreateBoardRequest - 创建看板

```typescript
interface CreateBoardRequest {
  name?: string;        // 可选，为空则使用默认名称 "My Job Tracker"
}
```

**后端定义参考：** `dto/request/CreateBoardRequest.java`

### 4.3 CreateCardRequest - 创建卡片

```typescript
interface CreateCardRequest {
  boardId: string;      // UUID - 必填，看板 ID
  statusId: string;     // UUID - 必填，列 ID（卡片初始状态）
  jobTitle: string;     // 必填，职位名称
  companyName: string;  // 必填，公司名称
  jobLink?: string;     // 可选，职位链接
  sourcePlatform?: string;  // 可选，来源平台
  jobLocation?: string; // 可选，工作地点
  description?: string; // 可选，职位描述
  tags?: string[];      // 可选，标签数组
  comments?: string;    // 可选，备注
  extra?: Record<string, any>;  // 可选，扩展字段（任意 JSON 对象）
}
```

**后端定义参考：** `dto/request/CreateCardRequest.java`

**注意：**
- `tags` 在 Request 中是 `string[]` 数组，后端会转换为逗号分隔字符串存储
- `extra` 在 Request 中是 `Record<string, any>` 对象，后端会序列化为 JSON 字符串

### 4.4 UpdateCardRequest - 更新卡片

```typescript
interface UpdateCardRequest {
  cardId: string;       // UUID - 必填，卡片 ID
  statusId?: string;    // UUID - 可选，列 ID（可用于移动卡片）
  jobTitle?: string;    // 可选，职位名称
  companyName?: string; // 可选，公司名称
  jobLink?: string;     // 可选，职位链接
  sourcePlatform?: string;  // 可选，来源平台
  jobLocation?: string; // 可选，工作地点
  description?: string; // 可选，职位描述
  tags?: string[];      // 可选，标签数组
  comments?: string;    // 可选，备注
  extra?: Record<string, any>;  // 可选，扩展字段
}
```

**后端定义参考：** `dto/request/UpdateCardRequest.java`

### 4.5 MoveCardRequest - 移动卡片

```typescript
interface MoveCardRequest {
  cardId: string;           // UUID - 必填，卡片 ID
  targetStatusId: string;   // UUID - 必填，目标列 ID
}
```

**后端定义参考：** `dto/request/MoveCardRequest.java`

### 4.6 DeleteCardRequest - 删除卡片

```typescript
interface DeleteCardRequest {
  cardId: string;       // UUID - 必填，卡片 ID
}
```

**后端定义参考：** `dto/request/DeleteCardRequest.java`

### 4.7 UpdateColumnRequest - 更新列

```typescript
interface UpdateColumnRequest {
  columnId: string;                 // UUID - 必填，列 ID
  name?: string;                    // 可选，列名称
  sortOrder?: number;               // 可选，排序顺序
  customAttributes?: Record<string, any>;  // 可选，自定义属性（JSON 对象）
}
```

**后端定义参考：** `dto/request/UpdateColumnRequest.java`

---

## 五、认证相关 DTO

### 5.1 LoginRequest - 登录请求

```typescript
interface LoginRequest {
  username: string;     // 必填，用户名
  password: string;     // 必填，密码
}
```

**变更说明：**
- ✅ **2026-03-08 更新**：登录方式从 `userId` 改为 `username + password`
- 密码使用 BCrypt 加密存储和验证
- 登录失败统一返回 `401` 错误码，不区分用户名不存在或密码错误

### 5.2 LoginResponse - 登录响应

```typescript
interface LoginResponse {
  userId: string;       // 用户 ID（UUID）
  username: string;     // 用户名
  displayName: string;  // 显示名称
  token: string;        // JWT Token
  tokenType: string;    // Token 类型，固定为 "Bearer"
  currentBoard: BoardInfo;  // 当前看板信息
}

interface BoardInfo {
  boardId: string;      // 看板 ID（UUID）
  boardName: string;    // 看板名称
  hasBoard: boolean;    // 是否有看板（始终为 true）
}
```

**变更说明：**
- ✅ **2026-03-08 更新**：增加了 `username` 和 `displayName` 字段
- ✅ **2026-03-08 更新**：增加 `currentBoard` 字段，包含用户当前看板信息
- 如果用户没有看板，后端会自动创建默认看板

### 5.3 RegisterRequest - 注册请求

```typescript
interface RegisterRequest {
  username: string;         // 必填，用户名（唯一）
  password: string;         // 必填，密码（至少6位）
  displayName?: string;     // 可选，显示名称（不传则使用 username）
  email?: string;           // 可选，邮箱
}
```

**变更说明：**
- ✅ **2026-03-08 更新**：`password` 变为必填字段，长度至少6位
- 密码使用 BCrypt 加密存储

### 5.4 RegisterResponse - 注册响应

```typescript
interface RegisterResponse {
  userId: string;       // 用户 ID（UUID 格式）
  username: string;     // 用户名
  displayName: string;  // 显示名称
  token: string;        // JWT Token
  tokenType: string;    // Token 类型，固定为 "Bearer"
  currentBoard: BoardInfo;  // 初始看板信息
}

interface BoardInfo {
  boardId: string;      // 看板 ID（UUID）
  boardName: string;    // 看板名称
  hasBoard: boolean;    // 是否有看板（始终为 true）
}
```

**变更说明：**
- ✅ **2026-03-08 更新**：增加了 `displayName` 字段
- ✅ **2026-03-08 更新**：增加 `currentBoard` 字段，注册时自动创建默认看板
- 默认看板名称格式：`{username} 的求职看板`

---

## 六、接口列表

### 6.1 认证接口（无需认证）

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| POST | `/auth/login` | `LoginRequest` | `ApiResponse<LoginResponse>` | 用户登录（用户名+密码） |
| POST | `/auth/register` | `RegisterRequest` | `ApiResponse<RegisterResponse>` | 用户注册（用户名+密码） |

### 6.2 看板接口（需认证）

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| POST | `/board/load` | `LoadBoardRequest` | `ApiResponse<BoardDataDto>` | 加载看板完整数据 |
| POST | `/board/create` | `CreateBoardRequest` | `ApiResponse<BoardDto>` | 创建看板 |

### 6.3 列接口（需认证）

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| POST | `/board/column/update` | `UpdateColumnRequest` | `ApiResponse<ColumnDto>` | 更新列信息 |

### 6.4 卡片接口（需认证）

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| POST | `/board/card/create` | `CreateCardRequest` | `ApiResponse<JobCardDto>` | 创建卡片 |
| POST | `/board/card/update` | `UpdateCardRequest` | `ApiResponse<JobCardDto>` | 更新卡片 |
| POST | `/board/card/move` | `MoveCardRequest` | `ApiResponse<JobCardDto>` | 移动卡片 |
| POST | `/board/card/delete` | `DeleteCardRequest` | `ApiResponse<void>` | 删除卡片（软删除） |

---

## 七、前端 TypeScript 类型定义（参考）

以下是完整的 TypeScript 类型定义，前端可直接使用：

```typescript
// ============================================
// 基础类型
// ============================================
type UUID = string;

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// ============================================
// Response DTO（后端 → 前端）
// ============================================

interface BoardDto {
  id: UUID;
  userId: string;
  name: string;
  createdAt: string;  // ISO 8601
  updatedAt: string;  // ISO 8601
}

interface ColumnDto {
  id: UUID;
  boardId: UUID;
  name: string;
  sortOrder: number;
  isDefault: boolean;
  customAttributes: string | null;
}

interface JobCardDto {
  id: UUID;
  boardId: UUID;
  statusId: UUID;
  jobTitle: string;
  companyName: string;
  jobLink: string | null;
  sourcePlatform: string | null;
  expired: boolean | null;
  jobLocation: string | null;
  description: string | null;
  appliedTime: string | null;  // ISO 8601
  tags: string | null;  // 逗号分隔
  comments: string | null;
  extra: string | null;  // JSON 字符串
  createdAt: string;  // ISO 8601
  updatedAt: string;  // ISO 8601
}

interface BoardDataDto {
  board: BoardDto;
  columns: ColumnDto[];
  cards: JobCardDto[];
}

// ============================================
// Request DTO（前端 → 后端）
// ============================================

interface LoadBoardRequest {
  boardId?: UUID;
}

interface CreateBoardRequest {
  name?: string;
}

interface CreateCardRequest {
  boardId: UUID;
  statusId: UUID;
  jobTitle: string;
  companyName: string;
  jobLink?: string;
  sourcePlatform?: string;
  jobLocation?: string;
  description?: string;
  tags?: string[];
  comments?: string;
  extra?: Record<string, any>;
}

interface UpdateCardRequest {
  cardId: UUID;
  statusId?: UUID;
  jobTitle?: string;
  companyName?: string;
  jobLink?: string;
  sourcePlatform?: string;
  jobLocation?: string;
  description?: string;
  tags?: string[];
  comments?: string;
  extra?: Record<string, any>;
}

interface MoveCardRequest {
  cardId: UUID;
  targetStatusId: UUID;
}

interface DeleteCardRequest {
  cardId: UUID;
}

interface UpdateColumnRequest {
  columnId: UUID;
  name?: string;
  sortOrder?: number;
  customAttributes?: Record<string, any>;
}

// ============================================
// 认证相关
// ============================================

interface LoginRequest {
  username: string;     // 必填，用户名
  password: string;     // 必填，密码
}

interface BoardInfo {
  boardId: string;      // 看板 ID（UUID）
  boardName: string;    // 看板名称
  hasBoard: boolean;    // 是否有看板
}

interface LoginResponse {
  userId: string;       // 用户 ID（UUID）
  username: string;     // 用户名
  displayName: string;  // 显示名称
  token: string;        // JWT Token
  tokenType: string;    // Token 类型
  currentBoard: BoardInfo;  // 当前看板信息
}

interface RegisterRequest {
  username: string;         // 必填，用户名
  password: string;         // 必填，密码（至少6位）
  displayName?: string;     // 可选，显示名称
  email?: string;           // 可选，邮箱
}

interface RegisterResponse {
  userId: UUID;
  username: string;
  displayName: string;
  token: string;
  tokenType: string;
  currentBoard: BoardInfo;  // 初始看板信息
}
```

---

## 八、API 调用示例

### 8.1 封装请求工具

```typescript
const BASE_URL = 'http://localhost:8080';

async function apiCall<T>(
  endpoint: string,
  body: unknown,
  requiresAuth: boolean = true
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (requiresAuth) {
    const token = localStorage.getItem('token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });

  const result: ApiResponse<T> = await response.json();

  if (result.code !== 200) {
    throw new Error(result.message);
  }

  return result.data;
}
```

### 8.2 登录示例

```typescript
async function login(username: string, password: string): Promise<LoginResponse> {
  const data = await apiCall<LoginResponse>(
    '/auth/login',
    { username, password },
    false  // 不需要认证
  );
  // 保存用户信息
  localStorage.setItem('token', data.token);
  localStorage.setItem('userId', data.userId);
  localStorage.setItem('username', data.username);
  localStorage.setItem('displayName', data.displayName);

  // 保存当前看板信息（后端会自动创建默认看板）
  if (data.currentBoard?.hasBoard) {
    localStorage.setItem('currentBoardId', data.currentBoard.boardId);
    localStorage.setItem('currentBoardName', data.currentBoard.boardName);
  }

  return data;
}

// 登录后直接跳转到看板
async function handleLogin(username: string, password: string) {
  const response = await login(username, password);

  // 跳转到用户的看板
  if (response.currentBoard?.hasBoard) {
    router.push(`/board/${response.currentBoard.boardId}`);
  }
}
```

### 8.3 注册示例

```typescript
async function register(
  username: string,
  password: string,
  displayName?: string
): Promise<RegisterResponse> {
  const data = await apiCall<RegisterResponse>(
    '/auth/register',
    { username, password, displayName },
    false  // 不需要认证
  );
  // 保存用户信息
  localStorage.setItem('token', data.token);
  localStorage.setItem('userId', data.userId);
  localStorage.setItem('username', data.username);
  localStorage.setItem('displayName', data.displayName);

  // 保存初始看板信息（注册时后端自动创建）
  if (data.currentBoard?.hasBoard) {
    localStorage.setItem('currentBoardId', data.currentBoard.boardId);
    localStorage.setItem('currentBoardName', data.currentBoard.boardName);
  }

  return data;
}

// 注册后直接跳转到看板
async function handleRegister(username: string, password: string) {
  const response = await register(username, password);

  // 注册成功后直接进入看板（无需再请求创建）
  if (response.currentBoard?.hasBoard) {
    router.push(`/board/${response.currentBoard.boardId}`);
  }
}
```

### 8.4 加载看板示例

```typescript
async function loadBoard(boardId?: string): Promise<BoardDataDto> {
  return apiCall<BoardDataDto>('/board/load', { boardId });
}
```

### 8.5 创建卡片示例

```typescript
async function createCard(request: CreateCardRequest): Promise<JobCardDto> {
  return apiCall<JobCardDto>('/board/card/create', request);
}

// 使用示例
const newCard = await createCard({
  boardId: '550e8400-e29b-41d4-a716-446655440000',
  statusId: '550e8400-e29b-41d4-a716-446655440001',
  jobTitle: '高级前端工程师',
  companyName: '字节跳动',
  tags: ['急招', '大厂'],
  extra: { salary: '30k-50k' }
});
```

### 8.6 更新卡片示例

```typescript
async function updateCard(request: UpdateCardRequest): Promise<JobCardDto> {
  return apiCall<JobCardDto>('/board/card/update', request);
}

// 使用示例：只更新职位名称
await updateCard({
  cardId: '550e8400-e29b-41d4-a716-446655440010',
  jobTitle: '资深前端工程师'
});
```

### 8.7 移动卡片示例

```typescript
async function moveCard(cardId: string, targetStatusId: string): Promise<JobCardDto> {
  return apiCall<JobCardDto>('/board/card/move', {
    cardId,
    targetStatusId
  });
}
```

### 8.8 删除卡片示例

```typescript
async function deleteCard(cardId: string): Promise<void> {
  await apiCall<void>('/board/card/delete', { cardId });
}
```

---

## 九、注意事项与常见问题

### 9.1 类型对齐要点

1. **UUID 统一使用 string**
   - 所有 ID 字段（`id`, `boardId`, `statusId`, `cardId` 等）都是 UUID 字符串
   - 不要尝试使用 number 或其他类型

2. **时间字段统一使用 ISO 8601 字符串**
   - `createdAt`, `updatedAt`, `appliedTime` 都是 `string` 类型
   - 格式：`"2026-03-07T10:30:00Z"`
   - 使用 `new Date(dateString)` 可以方便转换

3. **注意 Request/Response 中 tags 的类型差异**
   - Request：`string[]` 数组
   - Response：`string`（逗号分隔）
   - 前端需要做适当转换

4. **注意 Request/Response 中 extra 的类型差异**
   - Request：`Record<string, any>`（任意对象）
   - Response：`string`（JSON 字符串）
   - 读取时需要 `JSON.parse()`，发送时直接传对象

### 9.2 可选字段处理

- 类型定义中带 `?` 的字段（如 `jobLink?: string`）表示可选
- 可选字段不传时，后端会保持原值不变（更新操作）或使用默认值
- 建议不要传 `null`，直接省略该字段即可

### 9.3 错误处理

- 统一通过 `code` 字段判断成功与否
- `code === 200` 表示成功
- 其他 code 表示错误，错误信息在 `message` 字段
- 常见错误码：
  - `400` - 请求参数错误（如密码长度不足）
  - `401` - 未认证（Token 无效或过期）或登录密码错误
  - `403` - 无权限
  - `404` - 资源不存在
  - `409` - 冲突（如用户名已存在）

#### 认证相关错误示例

**登录失败：**
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

**用户名已存在：**
```json
{
  "code": 409,
  "message": "用户名已存在",
  "data": null
}
```

**密码长度不足：**
```json
{
  "code": 400,
  "message": "密码不能为空且长度至少6位",
  "data": null
}
```

### 9.4 Swagger 文档

- 启动后端服务后访问：`http://localhost:8080/swagger-ui/index.html`
- 可以在线查看接口文档和进行接口测试
- 文档中的 Schema 与本文档一致

---

## 十、文档更新记录

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2026-03-08 | v1.0 | 初始版本，包含所有接口和类型定义 |
| 2026-03-08 | v2.0 | **认证系统重大更新**：<br>- 登录接口改为 `username + password`<br>- 注册接口要求提供 `password`（至少6位）<br>- 登录/注册响应增加 `displayName` 字段<br>- 密码使用 BCrypt 加密存储 |
| 2026-03-08 | v2.1 | **注册/登录流程优化**：<br>- 注册时自动创建默认看板<br>- 登录时自动为无看板用户创建默认看板<br>- 登录/注册响应增加 `currentBoard` 字段<br>- 用户注册/登录后可直接进入看板 |

---

**如有疑问或需要调整，请及时与后端开发人员沟通。**
