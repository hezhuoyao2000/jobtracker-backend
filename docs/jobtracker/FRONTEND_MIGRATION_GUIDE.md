# 前端 UUID 问题修复迁移指南

> 本文档指导前端如何调整后与后端 UUID 类型保持一致，并适配新的注册/登录流程。
>
> **适用范围**：所有前端代码使用非 UUID 格式 ID（如 `"board-initial"`）的情况。

---

## 快速检查清单

在开始前，先检查你的代码中是否存在以下问题：

- [ ] 前端自己生成 ID（如 `Date.now()`、`uuid()` 库等）
- [ ] Mock 数据中使用非 UUID 格式的 ID（如 `"board-1"`、`"col-123"`）
- [ ] TypeScript 类型定义中 ID 字段为 `number` 或其他类型
- [ ] 注册/登录后手动调用 `/board/create` 创建看板
- [ ] 没有正确处理后端返回的 `currentBoard` 字段

---

## 一、类型定义调整

### 1.1 添加 UUID 类型别名

```typescript
// types/common.ts
/** UUID 类型 - 后端所有 ID 字段都使用 UUID 字符串格式 */
export type UUID = string;
```

### 1.2 更新所有 ID 字段类型

```typescript
// types/board.ts - 修改前
interface Board {
  id: string | number;  // ❌ 类型不统一
  userId: string;
  name: string;
}

// types/board.ts - 修改后
import { UUID } from './common';

interface Board {
  id: UUID;             // ✅ 统一使用 UUID 类型
  userId: string;
  name: string;
}
```

### 1.3 更新认证响应类型

```typescript
// types/auth.ts
import { UUID } from './common';

interface BoardInfo {
  boardId: UUID;        // UUID 格式的看板 ID
  boardName: string;    // 看板名称
  hasBoard: boolean;    // 是否有看板（后端始终返回 true）
}

interface LoginResponse {
  userId: UUID;
  username: string;
  displayName: string;
  token: string;
  tokenType: string;
  currentBoard: BoardInfo;  // ✅ 新增字段：当前看板信息
}

interface RegisterResponse {
  userId: UUID;
  username: string;
  displayName: string;
  token: string;
  tokenType: string;
  currentBoard: BoardInfo;  // ✅ 新增字段：初始看板信息
}
```

---

## 二、Mock 数据修复

### 2.1 生成标准 UUID

使用任意 UUID 生成器（如 [uuidgenerator.net](https://www.uuidgenerator.net/)）生成标准 UUID 替换现有的 mock ID。

```typescript
// mock/boards.ts - 修改前 ❌
export const mockBoards = [
  {
    id: 'board-initial',          // ❌ 错误：非 UUID 格式
    name: 'My Board',
    columns: [
      { id: 'col-1', name: 'Wish list' },  // ❌ 错误：非 UUID 格式
      { id: 'col-2', name: 'Applied' }
    ]
  }
];

// mock/boards.ts - 修改后 ✅
export const mockBoards = [
  {
    id: '550e8400-e29b-41d4-a716-446655440000',  // ✅ 标准 UUID
    name: 'My Board',
    columns: [
      { id: '550e8400-e29b-41d4-a716-446655440001', name: 'Wish list' },
      { id: '550e8400-e29b-41d4-a716-446655440002', name: 'Applied' },
      { id: '550e8400-e29b-41d4-a716-446655440003', name: 'Interviewing' },
      { id: '550e8400-e29b-41d4-a716-446655440004', name: 'Offered' },
      { id: '550e8400-e29b-41d4-a716-446655440005', name: 'Rejected' }
    ]
  }
];
```

### 2.2 Mock 卡片数据

```typescript
// mock/cards.ts
export const mockCards = [
  {
    id: '660e8400-e29b-41d4-a716-446655440001',        // ✅ 标准 UUID
    boardId: '550e8400-e29b-41d4-a716-446655440000',   // ✅ 引用存在的 board
    statusId: '550e8400-e29b-41d4-a716-446655440001',  // ✅ 引用存在的 column
    jobTitle: 'Senior Frontend Engineer',
    companyName: 'ByteDance',
    // ... 其他字段
  }
];
```

---

## 三、API 调用调整

### 3.1 移除前端 ID 生成

```typescript
// 修改前 ❌
import { v4 as uuidv4 } from 'uuid';

async function createBoard(name: string) {
  const newBoard = {
    id: uuidv4(),           // ❌ 删除：不要前端生成 ID
    name: name
  };
  return apiCall('/board/create', newBoard);
}

// 修改后 ✅
async function createBoard(name: string) {
  const request = {
    name: name              // ✅ 只传名称，后端生成 ID
  };
  return apiCall('/board/create', request);  // 响应中返回 boardId
}
```

### 3.2 注册流程优化

```typescript
// 修改前 ❌
async function handleRegister(username: string, password: string) {
  // 1. 注册用户
  const user = await register(username, password);

  // 2. 手动创建看板 ❌ 删除：后端已自动创建
  const board = await createBoard(username + ' 的求职看板');

  // 3. 跳转看板
  router.push(`/board/${board.id}`);
}

// 修改后 ✅
async function handleRegister(username: string, password: string) {
  // 1. 注册用户（后端自动创建默认看板）
  const response = await register(username, password);

  // 2. 保存用户信息
  localStorage.setItem('token', response.token);
  localStorage.setItem('userId', response.userId);
  localStorage.setItem('username', response.username);
  localStorage.setItem('displayName', response.displayName);

  // 3. 直接使用后端返回的看板信息 ✅
  if (response.currentBoard?.hasBoard) {
    localStorage.setItem('currentBoardId', response.currentBoard.boardId);
    localStorage.setItem('currentBoardName', response.currentBoard.boardName);

    // 4. 直接跳转到看板（无需再请求创建）
    router.push(`/board/${response.currentBoard.boardId}`);
  }
}
```

### 3.3 登录流程优化

```typescript
// 修改前 ❌
async function handleLogin(username: string, password: string) {
  const response = await login(username, password);
  localStorage.setItem('token', response.token);

  // ❌ 可能需要手动检查/创建看板
  const boards = await fetchBoards();
  if (boards.length === 0) {
    await createBoard('My Board');
  }
}

// 修改后 ✅
async function handleLogin(username: string, password: string) {
  const response = await login(username, password);

  // 保存用户信息
  localStorage.setItem('token', response.token);
  localStorage.setItem('userId', response.userId);
  localStorage.setItem('username', response.username);
  localStorage.setItem('displayName', response.displayName);

  // ✅ 直接使用后端返回的当前看板（老用户无看板时后端会自动创建）
  if (response.currentBoard?.hasBoard) {
    localStorage.setItem('currentBoardId', response.currentBoard.boardId);
    localStorage.setItem('currentBoardName', response.currentBoard.boardName);

    // 跳转到用户的看板
    router.push(`/board/${response.currentBoard.boardId}`);
  }
}
```

### 3.4 创建卡片调整

```typescript
// 修改前 ❌
async function createCard(boardId: string, columnId: string, data: CardData) {
  const request = {
    id: uuidv4(),           // ❌ 删除：不要前端生成 ID
    boardId: boardId,
    statusId: columnId,
    ...data
  };
  return apiCall('/board/card/create', request);
}

// 修改后 ✅
interface CreateCardRequest {
  boardId: UUID;           // 从后端获取的 UUID
  statusId: UUID;          // 从后端获取的 UUID
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

async function createCard(request: CreateCardRequest): Promise<JobCardDto> {
  return apiCall<JobCardDto>('/board/card/create', request);
  // 响应中包含后端生成的 cardId (UUID)
}
```

---

## 四、需要检查的文件清单

请逐一检查以下文件并进行修改：

| 文件/目录 | 检查内容 | 修复要点 |
|-----------|----------|----------|
| `types/*.ts` | 所有 ID 字段类型 | 统一改为 `UUID` 类型别名 |
| `mock/*.ts` | mock 数据中的 ID | 替换为标准 UUID 格式 |
| `api/auth.ts` | 登录/注册响应处理 | 添加 `currentBoard` 处理逻辑 |
| `api/board.ts` | createBoard 调用 | 移除前端 ID 生成 |
| `api/card.ts` | createCard 调用 | 移除前端 ID 生成 |
| `pages/Login.tsx` | 登录成功后的跳转 | 使用 `currentBoard.boardId` |
| `pages/Register.tsx` | 注册成功后的跳转 | 使用 `currentBoard.boardId` |
| `store/*` | 状态管理中的 ID 类型 | 统一为 UUID 类型 |

---

## 五、测试验证步骤

### 5.1 类型检查

```bash
# 运行 TypeScript 类型检查
npx tsc --noEmit
```

### 5.2 功能测试

1. **新用户注册**
   ```bash
   curl -X POST http://localhost:8080/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"123456"}'
   ```
   - ✅ 响应中包含 `currentBoard` 字段
   - ✅ `currentBoard.boardId` 是标准 UUID 格式
   - ✅ 前端注册后直接跳转到看板页面

2. **老用户登录**
   ```bash
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"123456"}'
   ```
   - ✅ 响应中包含 `currentBoard` 字段
   - ✅ 无看板用户后端会自动创建默认看板

3. **创建卡片**
   - ✅ 请求中不包含 `id` 字段
   - ✅ `boardId` 和 `statusId` 是后端返回的 UUID
   - ✅ 响应中 `id` 是后端生成的 UUID

### 5.3 验证 UUID 格式

```typescript
// 验证函数
function isValidUUID(str: string): boolean {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  return uuidRegex.test(str);
}

// 使用示例
console.log(isValidUUID('550e8400-e29b-41d4-a716-446655440000')); // true
console.log(isValidUUID('board-initial'));                         // false
```

---

## 六、常见问题排查

### 问题 1：注册后跳转到空白页面

**原因**：没有正确处理 `currentBoard` 字段，仍然使用旧的跳转逻辑。

**解决**：
```typescript
// 确保使用 response.currentBoard.boardId
router.push(`/board/${response.currentBoard.boardId}`);
```

### 问题 2：创建卡片时报 400 错误

**原因**：请求中包含 `id` 字段，或 `boardId`/`statusId` 格式不正确。

**解决**：
```typescript
// 检查请求体
const request = {
  // ❌ 不要包含 id 字段
  boardId: boardId,      // 确保是后端返回的 UUID
  statusId: statusId,    // 确保是后端返回的 UUID
  jobTitle: 'Engineer',
  companyName: 'Company'
};
```

### 问题 3：Mock 数据与后端数据混合使用报错

**原因**：Mock 数据使用非 UUID ID，后端返回 UUID，混用时导致类型不匹配。

**解决**：统一将所有 mock ID 替换为标准 UUID。

---

## 七、后端返回的默认看板结构

用户注册或登录时，后端自动创建包含以下 5 个列的默认看板：

```json
{
  "board": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "username 的求职看板"
  },
  "columns": [
    { "id": "uuid-1", "name": "Wish list", "sortOrder": 0 },
    { "id": "uuid-2", "name": "Applied", "sortOrder": 1 },
    { "id": "uuid-3", "name": "Interviewing", "sortOrder": 2 },
    { "id": "uuid-4", "name": "Offered", "sortOrder": 3 },
    { "id": "uuid-5", "name": "Rejected", "sortOrder": 4 }
  ],
  "cards": []
}
```

---

## 八、迁移前后对比总结

| 项目 | 迁移前 | 迁移后 |
|------|--------|--------|
| ID 生成 | 前端生成（非 UUID） | 后端生成（UUID） |
| 注册流程 | 注册 → 手动创建看板 → 跳转 | 注册 → 后端自动创建 → 直接跳转 |
| 登录流程 | 登录 → 检查看板 → 可能手动创建 | 登录 → 后端自动检查/创建 → 直接跳转 |
| 类型定义 | `string \| number` | `UUID`（string 别名） |
| Mock 数据 | `"board-1"` `"col-123"` | `"550e8400-e29b-41d4-a716-446655440000"` |
| 创建请求 | 包含前端生成的 `id` | 不包含 `id`，后端生成 |
| 响应处理 | 忽略 `currentBoard` | 使用 `currentBoard` 跳转 |

---

**如有问题，请及时与后端开发人员沟通。**

**最后更新**：2026-03-09
