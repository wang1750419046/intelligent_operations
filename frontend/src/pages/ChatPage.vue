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

        <div class="chat-log">
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
            <pre>{{ message.content }}</pre>
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
import { sendChat } from '../api/chat'
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

const roleLabel = (role) => {
  if (role === 'user') return '你'
  if (role === 'assistant') return 'Agent'
  return role
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
  try {
    const sessionId = await ensureSession()
    const response = await sendChat({
      sessionId,
      userInput: userInput.value,
      modelConfigId: chatModelConfigId.value,
    })
    lastTraceId.value = response.traceId
    userInput.value = ''
    await selectSession(activeSessionId.value)
    lastReferences.value = (response.data?.references || []).filter((item) => item.type === 'kb')
    await loadSessions()
  } catch (error) {
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
