# API 接口测试指南

> 本文档说明如何测试当前已实现的看板接口，以及后续 JWT 认证改造的方案。

---

## 一、当前接口测试方法

### 方法 1：Swagger UI（推荐）

启动应用后访问：http://localhost:8080/swagger-ui/index.html

**操作步骤：**
1. 找到"看板管理"分组
2. 点击 `POST /board/create` → Try it out
3. 输入 JSON：`{ "name": "My Job Tracker" }`
4. 点击 Execute 执行
5. 复制响应中的 `id` 字段，用于后续测试

**优点：**
- 可视化操作，无需记忆 URL
- 自动生成请求示例
- 直观查看响应结构

---

### 方法 2：HTTP 测试文件

使用项目中的测试文件：`src/test/http/board-api-test.http`

**IntelliJ IDEA：**
1. 打开 `board-api-test.http`
2. 点击行首的 ▶ 绿色运行按钮
3. 在底部查看响应

**VS Code：**
1. 安装 "REST Client" 插件
2. 打开 `board-api-test.http`
3. 点击 "Send Request" 链接

**测试顺序：**
```
1. 运行"创建看板" → 复制返回的 id
2. 运行"加载用户的第一个看板" 或填入 id 运行"加载指定看板"
```

---

### 方法 3：curl 命令

**创建看板：**
```bash
curl -X POST http://localhost:8080/board/create \
  -H "Content-Type: application/json" \
  -d '{"name": "My Job Tracker"}'
```

**加载看板（不传 boardId）：**
```bash
curl -X POST http://localhost:8080/board/load \
  -H "Content-Type: application/json" \
  -d '{}'
```

**加载指定看板：**
```bash
curl -X POST http://localhost:8080/board/load \
  -H "Content-Type: application/json" \
  -d '{"boardId": "替换为实际UUID"}'
```

---

### 方法 4：Postman

1. 导入 URL：`http://localhost:8080`
2. 创建 Collection，添加请求：
   - Method: POST
   - URL: `{{baseUrl}}/board/create`
   - Body: raw JSON
3. 保存后复用

---

## 二、测试数据说明

当前用户 ID 写死为 `"user-1"`，所有数据都关联到这个用户。

**创建看板后的数据结构：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-1",
    "name": "My Job Tracker",
    "createdAt": "2026-03-07T10:30:00Z",
    "updatedAt": "2026-03-07T10:30:00Z"
  }
}
```

**加载看板返回的完整数据结构：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "board": { "id": "...", "name": "..." },
    "columns": [
      { "id": "...", "name": "Wish list", "sortOrder": 0 },
      { "id": "...", "name": "Applied", "sortOrder": 1 },
      { "id": "...", "name": "Interviewing", "sortOrder": 2 },
      { "id": "...", "name": "Offered", "sortOrder": 3 },
      { "id": "...", "name": "Rejected", "sortOrder": 4 }
    ],
    "cards": []
  }
}
```

---

## 三、后续 JWT 认证改造方案

是的，**后续需要将 Controller 中的 userId 提取方式改为从 JWT Token 中获取**。

### 3.1 需要修改的位置

当前代码中标记了 `// TODO` 的地方都需要改造：

**BoardController.java 第 67-70 行：**
```java
// TODO: 后续从 JWT Header 中提取 userId
// 例如：@RequestHeader("Authorization") String token
// String userId = jwtUtil.parseUserId(token);
String userId = "user-1";
```

**BoardController.java 第 103-104 行：**
```java
// TODO: 后续从 JWT Header 中提取 userId
String userId = "user-1";
```

### 3.2 改造方案

#### 方案 A：每个方法提取 Token（简单）

```java
@PostMapping("/load")
public ApiResponse<BoardDataDto> loadBoard(
        @RequestBody LoadBoardRequest request,
        @RequestHeader("Authorization") String authHeader) {
    String userId = jwtUtil.extractUserId(authHeader);  // 从 JWT 提取
    BoardDataDto data = boardService.loadBoard(userId, request);
    return ApiResponse.success(data);
}
```

#### 方案 B：统一拦截器（推荐）

**1. 创建 JWT 拦截器：**

```java
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String token = request.getHeader("Authorization");
        String userId = jwtUtil.parseToken(token);
        request.setAttribute("userId", userId);  // 存入 request
        return true;
    }
}
```

**2. Controller 中获取：**

```java
@PostMapping("/load")
public ApiResponse<BoardDataDto> loadBoard(
        @RequestBody LoadBoardRequest request,
        HttpServletRequest httpRequest) {
    String userId = (String) httpRequest.getAttribute("userId");
    // ...
}
```

#### 方案 C：方法参数解析器（更优雅）

```java
@PostMapping("/load")
public ApiResponse<BoardDataDto> loadBoard(
        @RequestBody LoadBoardRequest request,
        @CurrentUser String userId) {  // 自定义注解自动注入
    // ...
}
```

### 3.3 需要添加的文件（JWT 改造时）

```
├── config/
│   └── WebMvcConfig.java          # 注册拦截器
├── security/
│   ├── JwtUtil.java               # JWT 生成/解析工具
│   ├── JwtInterceptor.java        # 拦截器
│   └── CurrentUser.java           # 自定义注解
└── exception/
    └── UnauthorizedException.java # 401 异常
```

### 3.4 测试时的 Token 处理

**JWT 改造前（当前）：**
- 直接调用，无需 Header

**JWT 改造后：**
```bash
# 1. 先登录获取 token
curl -X POST http://localhost:8080/auth/login \
  -d '{"username": "xxx", "password": "xxx"}'
# 返回: { "token": "eyJhbGciOiJIUzI1NiIs..." }

# 2. 调用接口时带 Header
curl -X POST http://localhost:8080/board/load \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

## 四、现在就可以测试

虽然认证系统还没做，但当前接口是完整的，可以：

1. **启动应用**：`mvnw.cmd spring-boot:run`
2. **创建看板**：用上面任意一种方法调用 `/board/create`
3. **加载看板**：调用 `/board/load` 查看数据

等 JWT 功能完成后，只需要把 `userId = "user-1"` 改成从 Token 提取即可，Service 层逻辑不需要改动。

---

## 五、测试检查清单

| 检查项 | 期望结果 |
|--------|----------|
| 创建看板 | 返回 200，包含 id、name、5个默认列 |
| 加载看板（不传 id） | 返回第一个看板的完整数据 |
| 加载看板（传正确 id） | 返回对应看板数据 |
| 加载看板（传错误 id） | 返回 404 或错误提示 |
| 数据库 | `board` 表和 `kanban_column` 表有数据 |
