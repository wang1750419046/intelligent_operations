<template>
  <div class="chat-workspace">
    <section class="workspace-topbar">
      <div class="title-stack">
        <p class="section-kicker">Command Center</p>
        <h2>AIOps 运维分析台</h2>
        <p>把告警、日志、指标与知识库收束到同一个诊断上下文。</p>
      </div>
      <div class="topbar-stats" aria-label="当前工作区状态">
        <div class="stat-tile">
          <span>会话</span>
          <strong>{{ sessions.length }}</strong>
        </div>
        <div class="stat-tile">
          <span>模型</span>
          <strong>{{ modelConfigs.length }}</strong>
        </div>
        <div class="stat-tile">
          <span>Trace</span>
          <strong>{{ traceSteps.length }}</strong>
        </div>
      </div>
    </section>

    <div class="page-grid chat-layout">
      <section class="panel session-panel">
        <div class="panel-header">
          <div>
            <p class="section-kicker">Sessions</p>
            <h3>会话</h3>
          </div>
          <button class="icon-button ghost-button" title="刷新" aria-label="刷新" @click="refreshAll">↻</button>
        </div>

        <div class="quick-create">
          <select v-model="chatModelConfigId">
            <option :value="null">使用默认模型</option>
            <option v-for="item in modelConfigs" :key="item.id" :value="item.id">
              {{ item.name }} / {{ item.modelName }}
            </option>
          </select>
          <button class="full-width" @click="handleCreateSession">新建会话</button>
        </div>

        <div class="session-list">
          <button
            v-for="item in sessions"
            :key="item.sessionId"
            class="session-card"
            :class="{ active: item.sessionId === activeSessionId }"
            @click="selectSession(item.sessionId)"
          >
            <strong>{{ item.title }}</strong>
            <span class="session-meta">模型 {{ item.modelConfigId || '默认' }}</span>
            <time>{{ item.updatedAt }}</time>
          </button>
          <div v-if="!sessions.length" class="compact-empty">
            暂无会话
          </div>
        </div>
      </section>

      <section class="panel chat-panel">
        <div class="panel-header chat-header">
          <div>
            <p class="section-kicker">Live Analysis</p>
            <h3>{{ activeSessionTitle || '运维对话' }}</h3>
            <p>{{ activeSessionId ? '当前会话已载入' : '选择或新建会话后开始诊断' }}</p>
          </div>
          <div class="inline-actions">
            <button class="secondary" :disabled="!activeSessionId" @click="removeSession">删除会话</button>
          </div>
        </div>

        <div class="chat-log" ref="chatLogRef">
          <div v-if="!messages.length" class="empty-state">
            <strong>开始一次诊断</strong>
            <span>例如：昨天 2 点到 3 点订单接口响应变慢，帮我分析原因。</span>
          </div>
          <div
            v-for="message in messages"
            :key="message.messageId"
            class="message-card"
            :class="message.role"
          >
            <div class="message-role">
              <span>{{ roleLabel(message.role) }}</span>
            </div>
            <div v-if="message.role === 'assistant'" class="message-markdown">
              <template v-for="(block, index) in renderMessageBlocks(message.content || message.status || '')" :key="index">
                <div v-if="block.type === 'table'" class="markdown-table-wrap">
                  <table>
                    <thead v-if="block.headers.length">
                      <tr>
                        <th v-for="(cell, cellIndex) in block.headers" :key="cellIndex">{{ cell }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex">
                        <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <pre v-else>{{ block.content }}</pre>
              </template>
            </div>
            <pre v-else>{{ message.content || message.status || '' }}</pre>
          </div>
        </div>

        <div v-if="lastReferences.length" class="reference-panel">
          <div class="panel-header">
            <div>
              <p class="section-kicker">Similar Cases</p>
              <h3>相似故障案例</h3>
            </div>
          </div>
          <div class="reference-list">
            <article v-for="item in lastReferences" :key="`${item.source}-${item.title}`" class="reference-item">
              <strong>{{ item.title }}</strong>
              <span>{{ item.source }}<template v-if="item.score"> / 相似度 {{ Number(item.score).toFixed(2) }}</template></span>
              <p>{{ item.content }}</p>
            </article>
          </div>
        </div>

        <div class="composer">
          <textarea
            v-model="userInput"
            placeholder="输入故障现象、时间范围、服务名或你已经观察到的异常..."
          />
          <div class="composer-actions">
            <button :disabled="sending || !userInput.trim()" @click="handleSend">
              {{ sending ? '分析中...' : '发送给 Agent' }}
            </button>
            <button class="secondary" :disabled="!lastTraceId" @click="loadTrace">查看最近 Trace</button>
          </div>
          <div v-if="errorMessage" class="result-box danger-text">{{ errorMessage }}</div>
        </div>
      </section>

      <section class="panel trace-panel">
        <div class="panel-header">
          <div>
            <p class="section-kicker">Trace</p>
            <h3>执行链路</h3>
            <p class="trace-id">{{ lastTraceId || '暂无 traceId' }}</p>
          </div>
        </div>
        <div v-if="!traceSteps.length" class="empty-state trace-empty">
          <strong>等待 Agent 运行</strong>
          <span>执行后这里会展示工具调用、观察结果和最终输出链路。</span>
        </div>
        <div v-for="step in traceSteps" :key="step.stepNo" class="trace-step">
          <div class="trace-step-head">
            <strong>#{{ step.stepNo }} {{ step.actionName }}</strong>
            <span>{{ step.createdAt }}</span>
          </div>
          <p><b>Thought</b>{{ step.thoughtSummary }}</p>
          <p><b>Params</b>{{ step.actionParams }}</p>
          <pre>{{ step.observation }}</pre>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { createSession, deleteSession, getSessionDetail, listSessions } from '../api/session'
import { streamChat } from '../api/chat'
import { getTrace } from '../api/trace'
import { listModelConfigs } from '../api/modelConfig'

const sessions = ref([])
const modelConfigs = ref([])
const activeSessionId = ref(null)
const activeSessionTitle = ref('')
const messages = ref([])
const traceSteps = ref([])
const lastTraceId = ref('')
const lastReferences = ref([])
const sending = ref(false)
const errorMessage = ref('')
const userInput = ref('')
const chatModelConfigId = ref(null)
const chatLogRef = ref(null)
let scrollPending = false

const roleLabel = (role) => {
  if (role === 'user') return '你'
  if (role === 'assistant') return 'Agent'
  return role
}

const formatElapsed = (elapsedMs) => {
  if (elapsedMs === null || elapsedMs === undefined) return ''
  if (elapsedMs < 1000) return `${elapsedMs}ms`
  return `${(elapsedMs / 1000).toFixed(1)}s`
}

const scrollChatToBottom = () => {
  if (scrollPending) return
  scrollPending = true
  requestAnimationFrame(() => {
    scrollPending = false
    const element = chatLogRef.value
    if (element) {
      element.scrollTop = element.scrollHeight
    }
  })
}

const formatStatusMessage = (status, fallbackElapsedMs) => {
  const message = status.message || '正在处理'
  const elapsed = formatElapsed(status.elapsedMs ?? fallbackElapsedMs)
  return elapsed ? `${message}（后端已执行 ${elapsed}）` : message
}

const isTableSeparator = (line) => {
  const cells = parseTableCells(line)
  return cells.length > 1 && cells.every((cell) => /^:?-{3,}:?$/.test(cell.replace(/\s/g, '')))
}

const isTableLine = (line) => {
  const trimmed = line.trim()
  return trimmed.startsWith('|') && trimmed.endsWith('|') && parseTableCells(trimmed).length > 1
}

const cleanMarkdownCell = (value) => value
  .replace(/^\*\*(.*)\*\*$/, '$1')
  .replace(/`([^`]+)`/g, '$1')
  .trim()

const parseTableCells = (line) => line
  .trim()
  .replace(/^\|/, '')
  .replace(/\|$/, '')
  .split('|')
  .map(cleanMarkdownCell)

const normalizeTableRows = (rows) => {
  const maxColumns = Math.max(...rows.map((row) => row.length))
  return rows.map((row) => {
    if (row.length >= maxColumns) return row
    return [...row, ...Array(maxColumns - row.length).fill('')]
  })
}

const renderMessageBlocks = (text) => {
  const blocks = []
  const lines = (text || '').split('\n')
  let index = 0

  while (index < lines.length) {
    if (isTableLine(lines[index])) {
      const tableLines = []
      while (index < lines.length && isTableLine(lines[index])) {
        tableLines.push(lines[index])
        index += 1
      }

      if (tableLines.length > 1) {
        const rows = normalizeTableRows(tableLines.map(parseTableCells))
        const hasHeader = rows.length > 2 && isTableSeparator(tableLines[1])
        blocks.push({
          type: 'table',
          headers: hasHeader ? rows[0] : [],
          rows: hasHeader ? rows.slice(2) : rows,
        })
      } else {
        blocks.push({ type: 'text', content: tableLines[0] })
      }
      continue
    }

    const textLines = []
    while (index < lines.length && !isTableLine(lines[index])) {
      textLines.push(lines[index])
      index += 1
    }
    const content = textLines.join('\n').trim()
    if (content) {
      blocks.push({ type: 'text', content })
    }
  }

  return blocks.length ? blocks : [{ type: 'text', content: text || '' }]
}

const loadSessions = async () => {
  const response = await listSessions()
  sessions.value = response.data || []
}

const loadModelConfigs = async () => {
  const response = await listModelConfigs('CHAT')
  modelConfigs.value = (response.data || []).filter((item) => item.enabled)
  if (!activeSessionId.value && !chatModelConfigId.value) {
    const readyConfig = modelConfigs.value.find((item) => item.defaultConfig && item.hasApiKey)
      || modelConfigs.value.find((item) => item.hasApiKey)
    if (readyConfig) {
      chatModelConfigId.value = readyConfig.id
    }
  }
}

const selectSession = async (sessionId) => {
  if (!sessionId) return
  activeSessionId.value = sessionId
  const response = await getSessionDetail(sessionId)
  activeSessionTitle.value = response.data.title
  chatModelConfigId.value = response.data.modelConfigId
  messages.value = response.data.messages || []
  traceSteps.value = []
  lastReferences.value = []
  scrollChatToBottom()
}

const handleCreateSession = async () => {
  const response = await createSession({
    title: '新建运维会话',
    modelConfigId: chatModelConfigId.value,
  })
  await loadSessions()
  await selectSession(response.data.sessionId)
}

const ensureSession = async () => {
  if (activeSessionId.value) {
    return activeSessionId.value
  }
  const title = userInput.value.trim().slice(0, 30) || '新建运维会话'
  const response = await createSession({
    title,
    modelConfigId: chatModelConfigId.value,
  })
  await loadSessions()
  await selectSession(response.data.sessionId)
  return response.data.sessionId
}

const handleSend = async () => {
  if (!userInput.value.trim()) return
  sending.value = true
  errorMessage.value = ''
  const prompt = userInput.value.trim()
  const userMessageId = `local_user_${Date.now()}`
  const assistantMessageId = `local_assistant_${Date.now()}`
  const updateAssistantMessage = (updater) => {
    const index = messages.value.findIndex((message) => message.messageId === assistantMessageId)
    if (index === -1) return
    updater(messages.value[index])
  }
  try {
    const sessionId = await ensureSession()
    userInput.value = ''
    messages.value.push({
      messageId: userMessageId,
      role: 'user',
      content: prompt,
    })
    const assistantMessage = {
      messageId: assistantMessageId,
      role: 'assistant',
      content: '',
      status: '正在理解问题',
      stage: 'understanding',
      serverElapsedMs: 0,
    }
    messages.value.push(assistantMessage)
    scrollChatToBottom()

    let finalResponse = null
    await streamChat({
      sessionId,
      userInput: prompt,
      modelConfigId: chatModelConfigId.value,
    }, {
      onTrace: (traceId) => {
        lastTraceId.value = traceId
      },
      onStatus: (status) => {
        updateAssistantMessage((message) => {
          message.stage = status.stage || message.stage
          message.serverElapsedMs = status.elapsedMs ?? message.serverElapsedMs
          message.status = formatStatusMessage(status, message.serverElapsedMs)
        })
        scrollChatToBottom()
      },
      onDelta: (token) => {
        updateAssistantMessage((message) => {
          message.status = ''
          message.content += token
        })
        scrollChatToBottom()
      },
      onDone: (response) => {
        finalResponse = response
        lastTraceId.value = response.traceId || lastTraceId.value
        updateAssistantMessage((message) => {
          message.status = ''
          message.content = response.answer || message.content
        })
        scrollChatToBottom()
      },
    })
    await selectSession(activeSessionId.value)
    lastReferences.value = (finalResponse?.references || []).filter((item) => item.type === 'kb')
    await loadSessions()
  } catch (error) {
    updateAssistantMessage((message) => {
      message.status = message.content ? message.status : `处理失败：${error.message}`
    })
    scrollChatToBottom()
    errorMessage.value = error.message
  } finally {
    sending.value = false
  }
}

const loadTrace = async () => {
  if (!lastTraceId.value) return
  const response = await getTrace(lastTraceId.value)
  traceSteps.value = response.data.steps || []
}

const removeSession = async () => {
  if (!activeSessionId.value) return
  await deleteSession(activeSessionId.value)
  activeSessionId.value = null
  activeSessionTitle.value = ''
  messages.value = []
  traceSteps.value = []
  await loadSessions()
}

const refreshAll = async () => {
  await Promise.all([loadSessions(), loadModelConfigs()])
  if (activeSessionId.value) {
    await selectSession(activeSessionId.value)
  } else if (sessions.value.length) {
    await selectSession(sessions.value[0].sessionId)
  }
}

onMounted(refreshAll)
</script>
