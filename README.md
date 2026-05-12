# AIOps 智能运维分析 Agent

这是一个前后端分离的 AIOps Agent 演示项目，用于把运维对话、模型接入、知识库检索、向量索引和工具调用串成一个完整诊断流程。

## 技术栈

- 后端：Spring Boot 3.3、MyBatis、MySQL、LangChain4j、Qdrant、Apache Tika
- 前端：Vue 3、Vue Router、Axios、Vite
- 测试：JUnit、Spring Boot Test、H2、Playwright

## 核心能力

- 运维对话：创建会话、选择模型、发送问题、查看回答与 Trace。
- Agent 工具调用：日志查询、指标查询、知识库检索。
- 模型配置：支持聊天模型、向量模型、Rerank 模型配置，编辑和新增均使用弹窗。
- 知识库管理：文档列表、顶部筛选、新增知识弹窗、外部文件导入弹窗、编辑/删除知识。
- RAG 检索：知识正文与切片存 MySQL，向量索引写入 Qdrant，通过 chunkId 回查正文。
- 向量索引管理：在“向量模型”页单独展示 Qdrant 状态，可刷新状态与重建索引。

## 页面说明

### 运维对话

- 左侧管理会话与模型选择。
- 中间展示 Agent 回答，长回答以整宽报告卡片呈现。
- 右侧展示 Trace 执行链路。
- 聊天返回的知识库引用会在回答下方展示相似案例。

### 知识库

- 顶部提供文档筛选条件。
- 主体展示知识文档列表。
- “新增知识”打开编辑弹窗。
- “导入文档”打开文件导入弹窗。
- 点击已有文档进入编辑弹窗，回显数据库中已有内容。

### 模型配置

- 按类型切换：聊天模型、向量模型、Rerank 模型。
- 每个模型配置以卡片展示。
- “新建配置”在当前类型标题旁，点击后打开弹窗。
- 点击模型卡片打开编辑弹窗。
- Qdrant 向量数据库状态只在“向量模型”页中单独展示。

## RAG 数据流

上传或新增知识后，处理流程如下：

```text
文档内容
 -> Apache Tika 提取文本
 -> MySQL 保存原始文档 knowledge_document.content
 -> KnowledgeChunker 切片
 -> MySQL 保存切片 knowledge_chunk.chunk_content
 -> 向量模型生成 embedding
 -> Qdrant 保存向量 + docId/chunkId/metadata
```

检索流程如下：

```text
用户问题
 -> Query Rewrite
 -> 向量召回 Qdrant chunkId
 -> MySQL 按 chunkId 回查 chunk_content
 -> Keyword fallback
 -> Rerank
 -> 返回知识片段给 Agent 或接口调用方
```

当前设计中，Qdrant 不保存完整知识正文，只保存向量与必要元数据。MySQL 是知识正文和切片正文的主存储。

## 模型配置

模型配置表支持三类配置：

- `CHAT`：聊天模型，用于 Agent 回答与工具调用。
- `EMBEDDING`：向量模型，用于知识库切片向量化。
- `RERANK`：重排模型，用于知识库召回后的结果排序。

核心字段：

- `provider`
- `configType`
- `baseUrl`
- `apiKey`
- `modelName`
- `temperature`
- `maxTokens`
- `enabled`
- `defaultConfig`

默认种子配置中的 `apiKey` 为空，首次使用真实模型前需要在模型配置页补充 API Key。

## 后端接口

### 会话与对话

- `POST /api/session/create`
- `GET /api/session/list`
- `GET /api/session/{id}`
- `DELETE /api/session/{id}`
- `POST /api/chat/send`

### Trace 与工具

- `GET /api/tools`
- `GET /api/trace/{traceId}`

### 模型配置

- `GET /api/model-configs`
- `GET /api/model-configs/{id}`
- `POST /api/model-configs`
- `PUT /api/model-configs/{id}`
- `DELETE /api/model-configs/{id}`
- `POST /api/model-configs/{id}/test`

### 知识库

- `GET /api/kb/documents`
- `POST /api/kb/documents`
- `POST /api/kb/documents/import`
- `PUT /api/kb/documents/{docId}`
- `DELETE /api/kb/documents/{docId}`
- `POST /api/kb/search`
- `POST /api/kb/reindex`
- `GET /api/kb/vector/status`

## 本地启动

### 后端

```bash
mvn -s settings.xml spring-boot:run
```

默认后端地址：

- `http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`

默认 MySQL 配置见 `src/main/resources/application.yml`：

- database: `ai_ops_agent`
- username: `root`
- password: `root`

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

- `http://localhost:5173`

## 测试与构建

后端测试：

```bash
mvn -s settings.xml test
```

前端构建：

```bash
cd frontend
npm run build
```

## 目录结构

```text
frontend/
  src/
    api/
    pages/
    App.vue
    main.js

src/main/java/com/example/aiops/
  agent/        Agent 执行与会话上下文
  controller/   REST API
  dto/          请求与响应对象
  entity/       数据实体
  mapper/       MyBatis Mapper
  memory/       会话记忆持久化
  rag/          知识库、切片、向量检索、Rerank
  service/      业务服务
  tools/        Agent 工具
  util/         通用工具

src/main/resources/
  application.yml
  prompts/
  schema-mysql.sql
  data-mysql.sql
  schema-h2.sql
  data-h2.sql
```

## 后续建议

- 将模型 API Key 改为加密存储。
- 将知识库索引改为异步任务，支持失败重试和索引状态追踪。
- 为知识库切片增加更强的 FAQ/Q&A 语义切分策略。
- 接入真实日志、指标、发布记录和告警源。
- 增加用户、角色、权限与知识库可见性控制。
- 支持 SSE/流式输出，提升 Agent 回答体验。
