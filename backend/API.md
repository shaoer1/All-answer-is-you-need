# 后端接口文档（v1）

Base URL: `http://localhost:8080`

## 1. 用户模块

### 1.1 初始化用户
- **URL**: `POST /api/user/init`
- **Body**:
```json
{
  "username": "demo"
}
```
- **Resp**:
```json
{
  "userId": 1,
  "username": "demo"
}
```

---

## 2. 会话模块

### 2.1 创建会话
- **URL**: `POST /api/session/create`
- **Body**:
```json
{
  "username": "demo",
  "kbId": "kb-default",
  "sessionName": "默认会话"
}
```
- **Resp**:
```json
{
  "sessionId": 10
}
```

### 2.2 会话列表
- **URL**: `GET /api/session/list?username=demo`

### 2.3 删除会话
- **URL**: `DELETE /api/session/delete?username=demo&sessionId=10`

---

## 3. 消息模块

### 3.1 历史消息
- **URL**: `GET /api/message/list?username=demo&sessionId=10&limit=50`

---

## 4. 知识库模块

### 4.1 上传语料
- **URL**: `POST /api/knowledge/upload`
- **Content-Type**: `multipart/form-data`
- **Form字段**:
  - `username`（或 `userId`）
  - `kbId`
  - `file`（txt/md/pdf/docx/csv/xlsx）

---

## 5. 问答模块（SSE）

### 5.1 流式问答
- **URL**: `POST /api/chat/stream`
- **Content-Type**: `application/json`
- **Body**:
```json
{
  "username": "demo",
  "kbId": "kb-default",
  "sessionId": 10,
  "question": "这个知识库讲了什么？"
}
```
- **返回**: `text/event-stream`
  - `event: token`
  - `event: done`

---

## 6. 关键策略说明

1. **上下文管理**：滑动窗口 + 会话摘要（MySQL `chat_session.summary_text`）
2. **RAG输入控制**：Top3~5 + 相似度阈值过滤
3. **用户隔离**：`username` 级别隔离（会话、消息、知识库）
4. **幻觉抑制**：强提示约束 + RAG阈值 + 联网校验（可开关） + 规则拦截
