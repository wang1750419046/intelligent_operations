# AIOps 智能运维分析 Agent

这是一个前后端分离的 AIOps Agent 演示项目：

- 后端：`Spring Boot 3 + MyBatis + MySQL + LangChain4j`
- 前端：`Vue 3 + Vite`
- 能力：会话管理、模型配置、真实 LLM tool calling、日志/指标/知识库工具、trace 追踪

## 当前架构

### 后端

- `Spring Boot 3`
- `MyBatis`
- `MySQL`，默认连接：
  - host: `localhost:3306`
  - database: `ai_ops_agent`
  - username: `root`
  - password: `root`
- `LangChain4j + OpenAI-compatible`

### 前端

- `Vue 3`
- `Vue Router`
- `Axios`
- `Vite`

## 支持的模型系列

项目内置了三类模型配置模板，页面可直接配置：

1. `GPT / OpenAI`
2. `Qwen / DashScope`
3. `MiniMax`

当前后端统一走 OpenAI-compatible 接入，所以每条配置的核心字段是：

- `provider`
- `baseUrl`
- `apiKey`
- `modelName`
- `temperature`
- `maxTokens`

默认种子配置：

- OpenAI: `https://api.openai.com/v1`
- Qwen: `https://dashscope.aliyuncs.com/compatible-mode/v1`
- MiniMax: `https://api.minimaxi.com/v1`

说明：我这里将你提到的 “mimax” 按 `MiniMax` 处理了。

## 目录结构

```text
.
├── frontend/                       # Vue 前端
│   ├── src/
│   │   ├── api/
│   │   ├── pages/
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
├── src/main/java/com/example/aiops/
│   ├── agent/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── exception/
│   ├── mapper/
│   ├── memory/
│   ├── rag/
│   ├── service/
│   ├── tools/
│   └── util/
├── src/main/resources/
│   ├── application.yml
│   ├── prompts/
│   ├── schema-mysql.sql
│   ├── data-mysql.sql
│   ├── schema-h2.sql
│   └── data-h2.sql
└── src/test/java/com/example/aiops/
```

## 已实现能力

### 后端接口

- `POST /api/session/create`
- `GET /api/session/list`
- `GET /api/session/{id}`
- `DELETE /api/session/{id}`
- `POST /api/chat/send`
- `GET /api/tools`
- `GET /api/trace/{traceId}`
- `POST /api/kb/search`
- `GET /api/model-configs`
- `GET /api/model-configs/{id}`
- `POST /api/model-configs`
- `PUT /api/model-configs/{id}`
- `DELETE /api/model-configs/{id}`
- `POST /api/model-configs/{id}/test`

### Agent 能力

- 基于 LangChain4j 的真实 tool calling
- 日志工具 `queryLogs`
- 指标工具 `queryMetrics`
- 知识库工具 `searchKnowledge`
- 会话记忆持久化到数据库
- trace 链路持久化到数据库

### 前端页面

- 运维对话页
  - 新建会话
  - 选择模型配置
  - 发送问题
  - 查看消息历史
  - 查看最近 trace
- 模型配置页
  - 新建配置
  - 编辑配置
  - 测试模型连接
  - 删除配置

## 本地启动

### 1. 启动后端

```bash
mvn -s settings.xml spring-boot:run
```

后端地址：

- [http://localhost:8080](http://localhost:8080)
- 健康检查：[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：

- [http://localhost:5173](http://localhost:5173)

## 当前状态

我已经完成并验证：

- 后端 `mvn -s settings.xml test` 通过
- 前端 `npm run build` 通过
- Vue 开发服务已在 `5173` 启动
- Spring Boot 后端已在 `8080` 启动

## 真实模型接入说明

后端已经接入真实 LLM 调用链，但默认种子配置里的 `apiKey` 为空，所以第一次使用前需要到模型配置页补上对应的 key。

建议的使用顺序：

1. 打开模型配置页
2. 选择 GPT / Qwen / MiniMax 模板
3. 填入 `API Key`
4. 点击“测试连接”
5. 回到运维对话页创建会话并发起分析

## 测试

执行：

```bash
mvn -s settings.xml test
```

当前覆盖：

- Controller 集成测试
- Service 测试
- Tool 测试
- Agent 主流程测试

## 后续建议

1. 接入真实日志源、Prometheus、发布记录系统
2. 给模型配置增加密文存储
3. 增加会话级模型切换和模型调用审计
4. 增加 SSE 流式输出
5. 补一个登录和角色权限层
