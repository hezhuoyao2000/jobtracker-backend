# Board 接口文档

> 文档版本：1.0
> 最后更新：2026-03-07
> 负责人：yourname

---

## 接口概览

| 接口 | 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|------|
| 加载看板 | POST | `/board/load` | 加载看板完整数据 | 待实现 |
| 创建看板 | POST | `/board/create` | 创建新看板 | 待实现 |

**基础 URL：** `http://localhost:8080`

**Content-Type：** `application/json`

---

## 1. 加载看板

### 接口信息

- **接口路径：** `POST /board/load`
- **接口描述：** 加载用户的看板完整数据，包含看板信息、所有列、所有卡片
- **权限要求：** 用户必须登录（当前未实现，userId 写死）

### 请求参数

**Header:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "boardId": "550e8400-e29b-41d4-a716-446655440000"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| boardId | UUID | 否 | 看板 ID，不传则返回用户的第一个看板 |

### 响应示例

**成功响应（指定 boardId）：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "board": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "user-1",
      "name": "我的看板",
      "createdAt": "2026-03-07T10:00:00Z",
      "updatedAt": "2026-03-07T10:00:00Z"
    },
    "columns": [
      {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "boardId": "550e8400-e29b-41d4-a716-446655440000",
        "name": "Wish list",
        "sortOrder": 0,
        "isDefault": true,
        "createdAt": "2026-03-07T10:00:00Z"
      },
      {
        "id": "660e8400-e29b-41d4-a716-446655440002",
        "boardId": "550e8400-e29b-41d4-a716-446655440000",
        "name": "Applied",
        "sortOrder": 1,
        "isDefault": true,
        "createdAt": "2026-03-07T10:00:00Z"
      }
    ],
    "cards": [
      {
        "id": "770e8400-e29b-41d4-a716-446655440010",
        "boardId": "550e8400-e29b-41d4-a716-446655440000",
        "statusId": "660e8400-e29b-41d4-a716-446655440001",
        "jobTitle": "软件工程师",
        "companyName": "某公司",
        "deletedAt": null,
        "createdAt": "2026-03-07T10:00:00Z"
      }
    ]
  }
}
```

**成功响应（不传 boardId，返回第一个看板）：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "board": { ... },
    "columns": [ ... ],
    "cards": [ ... ]
  }
}
```

**失败响应（看板不存在）：**
```json
{
  "code": 500,
  "message": "看板不存在",
  "data": null
}
```

**失败响应（看板不属于当前用户）：**
```json
{
  "code": 500,
  "message": "看板不存在",
  "data": null
}
```

### cURL 示例

```bash
# 场景 1：加载用户的第一个看板（不传 boardId）
curl -X POST http://localhost:8080/board/load \
  -H "Content-Type: application/json" \
  -d '{}'

# 场景 2：加载指定的看板
curl -X POST http://localhost:8080/board/load \
  -H "Content-Type: application/json" \
  -d '{"boardId": "550e8400-e29b-41d4-a716-446655440000"}'
```

### Postman 示例

1. 打开 Postman
2. 选择 `POST` 方法
3. 输入 URL: `http://localhost:8080/board/load`
4. 点击 `Body` → 选择 `raw` → 选择 `JSON`
5. 输入请求体（可选 boardId）
6. 点击 `Send`

---

## 2. 创建看板

### 接口信息

- **接口路径：** `POST /board/create`
- **接口描述：** 创建新的看板，并自动初始化 5 个默认状态列
- **权限要求：** 用户必须登录（当前未实现，userId 写死）

### 默认列说明

创建看板时，系统会自动创建以下 5 个默认列：

| 序号 | 列名 | 说明 |
|------|------|------|
| 1 | Wish list | 愿望清单，想投递的公司 |
| 2 | Applied | 已投递 |
| 3 | Interviewing | 面试中 |
| 4 | Offered | 已拿 Offer |
| 5 | Rejected | 已拒绝 |

### 请求参数

**Header:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "name": "我的求职看板"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 看板名称，为空则使用默认名称 "My Job Tracker" |

### 响应示例

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-1",
    "name": "我的求职看板",
    "createdAt": "2026-03-07T10:00:00Z",
    "updatedAt": "2026-03-07T10:00:00Z"
  }
}
```

**成功响应（默认名称）：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-1",
    "name": "My Job Tracker",
    "createdAt": "2026-03-07T10:00:00Z",
    "updatedAt": "2026-03-07T10:00:00Z"
  }
}
```

### cURL 示例

```bash
# 场景 1：自定义看板名称
curl -X POST http://localhost:8080/board/create \
  -H "Content-Type: application/json" \
  -d '{"name": "我的求职看板"}'

# 场景 2：使用默认名称（不传 name 或 name 为空）
curl -X POST http://localhost:8080/board/create \
  -H "Content-Type: application/json" \
  -d '{}'

# 或者
curl -X POST http://localhost:8080/board/create \
  -H "Content-Type: application/json" \
  -d '{"name": ""}'
```

### Postman 示例

1. 打开 Postman
2. 选择 `POST` 方法
3. 输入 URL: `http://localhost:8080/board/create`
4. 点击 `Body` → 选择 `raw` → 选择 `JSON`
5. 输入请求体：`{"name": "我的求职看板"}`
6. 点击 `Send`

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（JWT 认证失败） |
| 403 | 无权限访问 |
| 500 | 服务器内部错误 |

---

## 开发待办

### 当前状态

- [x] Controller 代码完成
- [x] Service 层对接
- [ ] JWT 认证集成
- [ ] 参数校验（@Valid）
- [ ] 全局异常处理

### 后续优化

1. **JWT 认证**：从 `Authorization` Header 中提取 userId
   ```java
   @PostMapping("/load")
   public ApiResponse<BoardDataDto> loadBoard(
           @RequestBody LoadBoardRequest request,
           @RequestHeader("Authorization") String token) {
       String userId = jwtUtil.parseUserId(token);
       // ...
   }
   ```

2. **参数校验**：添加 `@Valid` 注解
   ```java
   @PostMapping("/create")
   public ApiResponse<BoardDto> createBoard(
           @RequestBody @Valid CreateBoardRequest request) {
       // ...
   }
   ```

3. **全局异常处理**：统一错误响应格式
   ```java
   @ControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(RuntimeException.class)
       public ApiResponse<Void> handleException(RuntimeException e) {
           return ApiResponse.error(500, e.getMessage());
       }
   }
   ```

---

## 变更记录

| 版本 | 日期 | 变更内容 | 负责人 |
|------|------|----------|--------|
| 1.0 | 2026-03-07 | 初始版本，创建 load 和 create 接口文档 | yourname |
