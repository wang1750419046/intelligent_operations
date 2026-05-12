<template>
  <div class="config-workspace">
    <section class="workspace-topbar">
      <div class="title-stack">
        <p class="section-kicker">Model Gateway</p>
        <h2>集中管理聊天模型与向量模型接入</h2>
        <p>维护生产分析所需的模型、密钥状态和知识库向量索引。</p>
      </div>
    </section>

    <div class="segmented-tabs" aria-label="模型类型">
      <button :class="{ active: activeType === 'CHAT' }" @click="switchType('CHAT')">聊天模型</button>
      <button :class="{ active: activeType === 'EMBEDDING' }" @click="switchType('EMBEDDING')">向量模型</button>
      <button :class="{ active: activeType === 'RERANK' }" @click="switchType('RERANK')">Rerank 模型</button>
    </div>

    <section class="model-section-head">
      <div>
        <p class="section-kicker">Providers</p>
        <h3>{{ activeTypeLabel }}配置</h3>
      </div>
      <button @click="openCreateModal">新建配置</button>
    </section>

    <section class="model-card-grid">
      <button
        v-for="item in modelConfigs"
        :key="item.id"
        class="panel model-config-card model-config-button"
        @click="editItem(item)"
      >
        <div class="model-card-head">
          <div>
            <p class="section-kicker">{{ item.configType || activeType }}</p>
            <h3>{{ item.name }}</h3>
          </div>
          <span :class="['status-pill', item.enabled ? 'good' : 'muted']">
            {{ item.enabled ? '启用' : '停用' }}
          </span>
        </div>
        <div class="model-card-body">
          <div>
            <span>Provider</span>
            <strong>{{ item.provider }}</strong>
          </div>
          <div>
            <span>Model</span>
            <strong>{{ item.modelName }}</strong>
          </div>
          <div>
            <span>API Key</span>
            <strong>{{ displayApiKey(item.apiKey) }}</strong>
          </div>
        </div>
        <p class="model-card-message">
          <template v-if="item.defaultConfig">默认配置</template>
          <template v-else>{{ item.updatedAt }}</template>
        </p>
      </button>

      <div v-if="!modelConfigs.length" class="panel compact-empty">
        暂无{{ activeTypeLabel }}配置
      </div>
    </section>

    <section v-if="activeType === 'EMBEDDING'" class="panel vector-index-panel">
      <div class="model-card-head">
        <div>
          <p class="section-kicker">Vector Index</p>
          <h3>Qdrant 向量数据库</h3>
        </div>
        <span :class="['status-pill', vectorStatus.qdrantReachable ? 'good' : 'muted']">
          {{ vectorStatus.qdrantReachable ? '已连接' : '未就绪' }}
        </span>
      </div>
      <div class="model-card-body">
        <div>
          <span>Collection</span>
          <strong>{{ vectorStatus.collection || 'aiops_knowledge_v1' }}</strong>
        </div>
        <div>
          <span>Embedding</span>
          <strong>{{ vectorStatus.embeddingConfigured ? vectorStatus.embeddingModel : '缺少 API Key' }}</strong>
        </div>
        <div>
          <span>Indexed</span>
          <strong>{{ vectorStatus.indexedCount ?? '-' }}</strong>
        </div>
      </div>
      <p class="model-card-message">{{ reindexResult || vectorStatus.message || '等待状态检查' }}</p>
      <div class="inline-actions">
        <button class="secondary" @click="loadVectorStatus">刷新状态</button>
        <button :disabled="reindexing" @click="handleReindex">
          {{ reindexing ? '重建中...' : '重建索引' }}
        </button>
      </div>
    </section>

    <div v-if="activeModal" class="modal-backdrop" @click.self="closeModal">
      <section class="modal-panel model-modal">
        <div class="modal-header">
          <div>
            <p class="section-kicker">Connection</p>
            <h3>{{ form.id ? '编辑配置' : '新建配置' }}</h3>
          </div>
          <button class="icon-button ghost-button" title="关闭" aria-label="关闭" @click="closeModal">×</button>
        </div>

        <div class="form-stack modal-body">
          <label>
            <span>配置名称</span>
            <input v-model="form.name" placeholder="例如：生产 Qwen Plus" />
          </label>
          <label>
            <span>模型服务商</span>
            <select v-model="form.provider" :disabled="activeType !== 'CHAT'" @change="applyPreset">
              <option value="OPENAI">GPT / OpenAI</option>
              <option value="QWEN">Qwen / DashScope</option>
              <option value="MINIMAX">MiniMax</option>
            </select>
          </label>
          <label>
            <span>Base URL</span>
            <input v-model="form.baseUrl" placeholder="Base URL" />
          </label>
          <label>
            <span>模型名称</span>
            <input v-model="form.modelName" placeholder="模型名称" />
          </label>
          <label>
            <span>API Key</span>
            <input v-model="form.apiKey" type="password" placeholder="留空则保留已有 API Key" />
            <span class="field-hint">当前 API Key: {{ displayApiKey(form.apiKeyPreview) }}</span>
          </label>

          <div v-if="activeType === 'CHAT'" class="split-fields">
            <label>
              <span>Temperature</span>
              <input v-model.number="form.temperature" type="number" min="0" max="2" step="0.1" />
            </label>
            <label>
              <span>Max Tokens</span>
              <input v-model.number="form.maxTokens" type="number" min="1" step="1" />
            </label>
          </div>

          <div class="toggle-row">
            <label class="checkbox-line">
              <input v-model="form.enabled" type="checkbox" />
              <span>启用</span>
            </label>
            <label class="checkbox-line">
              <input v-model="form.defaultConfig" type="checkbox" />
              <span>设为默认{{ activeTypeLabel }}</span>
            </label>
          </div>
          <div v-if="testResult" class="result-box">{{ testResult }}</div>
        </div>

        <div class="modal-actions model-modal-actions">
          <button class="danger" :disabled="!form.id" @click="remove">删除</button>
          <span></span>
          <button class="secondary" :disabled="!form.id" @click="test">
            {{ activeType === 'CHAT' ? '测试连接' : activeType === 'EMBEDDING' ? '测试向量模型' : '测试 Rerank' }}
          </button>
          <button class="secondary" @click="closeModal">取消</button>
          <button @click="save">保存</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createModelConfig,
  deleteModelConfig,
  listModelConfigs,
  testModelConfig,
  updateModelConfig,
} from '../api/modelConfig'
import { getVectorStatus, reindexKnowledge } from '../api/knowledge'

const chatPresets = {
  OPENAI: { baseUrl: 'https://api.openai.com/v1', modelName: 'gpt-4.1-mini' },
  QWEN: { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', modelName: 'qwen-plus' },
  MINIMAX: { baseUrl: 'https://api.minimaxi.com/v1', modelName: 'MiniMax-M2.5' },
}

const embeddingPreset = {
  provider: 'QWEN',
  baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  modelName: 'text-embedding-v3',
}

const rerankPreset = {
  provider: 'QWEN',
  baseUrl: 'https://dashscope.aliyuncs.com/compatible-api/v1',
  modelName: 'qwen3-rerank',
}

const activeType = ref('CHAT')
const activeTypeLabel = computed(() => {
  if (activeType.value === 'EMBEDDING') return '向量模型'
  if (activeType.value === 'RERANK') return 'Rerank 模型'
  return '聊天模型'
})
const modelConfigs = ref([])
const testResult = ref('')
const reindexResult = ref('')
const reindexing = ref(false)
const vectorStatus = ref({})
const activeModal = ref('')

const emptyForm = () => {
  if (activeType.value === 'EMBEDDING') {
    return {
      id: null,
      name: 'Qwen Embedding',
      configType: 'EMBEDDING',
      provider: embeddingPreset.provider,
      baseUrl: embeddingPreset.baseUrl,
      modelName: embeddingPreset.modelName,
      apiKey: '',
      apiKeyPreview: null,
      temperature: 0.2,
      maxTokens: null,
      enabled: true,
      defaultConfig: true,
    }
  }
  if (activeType.value === 'RERANK') {
    return {
      id: null,
      name: 'Qwen Rerank',
      configType: 'RERANK',
      provider: rerankPreset.provider,
      baseUrl: rerankPreset.baseUrl,
      modelName: rerankPreset.modelName,
      apiKey: '',
      apiKeyPreview: null,
      temperature: 0.2,
      maxTokens: null,
      enabled: true,
      defaultConfig: true,
    }
  }
  return {
    id: null,
    name: '',
    configType: 'CHAT',
    provider: 'OPENAI',
    baseUrl: chatPresets.OPENAI.baseUrl,
    modelName: chatPresets.OPENAI.modelName,
    apiKey: '',
    apiKeyPreview: null,
    temperature: 0.2,
    maxTokens: 4096,
    enabled: true,
    defaultConfig: false,
  }
}

const form = reactive(emptyForm())

const loadData = async () => {
  try {
    const response = await listModelConfigs(activeType.value)
    modelConfigs.value = response.data || []
    return true
  } catch (error) {
    modelConfigs.value = []
    testResult.value = error.message
    return false
  }
}

const loadVectorStatus = async () => {
  try {
    const response = await getVectorStatus()
    vectorStatus.value = response.data || {}
  } catch (error) {
    vectorStatus.value = { message: error.message }
  }
}

const resetForm = () => {
  Object.assign(form, emptyForm())
  testResult.value = ''
}

const openCreateModal = () => {
  resetForm()
  activeModal.value = 'model'
}

const closeModal = () => {
  activeModal.value = ''
}

const switchType = async (type) => {
  activeType.value = type
  resetForm()
  await loadData()
}

const applyPreset = () => {
  if (activeType.value === 'EMBEDDING') {
    Object.assign(form, embeddingPreset)
    return
  }
  if (activeType.value === 'RERANK') {
    Object.assign(form, rerankPreset)
    return
  }
  const preset = chatPresets[form.provider]
  form.baseUrl = preset.baseUrl
  form.modelName = preset.modelName
}

const editItem = (item) => {
  Object.assign(form, {
    id: item.id,
    name: item.name,
    configType: item.configType || activeType.value,
    provider: item.provider,
    baseUrl: item.baseUrl,
    modelName: item.modelName,
    apiKey: '',
    apiKeyPreview: item.apiKey,
    temperature: item.temperature,
    maxTokens: item.maxTokens,
    enabled: item.enabled,
    defaultConfig: item.defaultConfig,
  })
  testResult.value = item.hasApiKey ? '当前配置已保存 API Key' : '当前配置尚未保存 API Key'
  activeModal.value = 'model'
}

const displayApiKey = (apiKey) => apiKey ?? 'null'

const save = async () => {
  const payload = {
    name: form.name,
    configType: activeType.value,
    provider: activeType.value === 'EMBEDDING' ? embeddingPreset.provider : activeType.value === 'RERANK' ? rerankPreset.provider : form.provider,
    baseUrl: form.baseUrl,
    modelName: form.modelName,
    temperature: form.temperature,
    maxTokens: activeType.value === 'CHAT' ? form.maxTokens : null,
    enabled: form.enabled,
    defaultConfig: form.defaultConfig,
  }
  if (form.apiKey && form.apiKey.trim()) {
    payload.apiKey = form.apiKey.trim()
  }
  try {
    if (form.id) {
      await updateModelConfig(form.id, payload)
    } else {
      await createModelConfig(payload)
    }
    await loadData()
    await loadVectorStatus()
    resetForm()
    closeModal()
  } catch (error) {
    testResult.value = error.message
  }
}

const test = async () => {
  if (!form.id) return
  try {
    const response = await testModelConfig(form.id)
    testResult.value = response.data.message
  } catch (error) {
    testResult.value = error.message
  }
}

const remove = async () => {
  if (!form.id) return
  try {
    await deleteModelConfig(form.id)
    await loadData()
    await loadVectorStatus()
    resetForm()
    closeModal()
  } catch (error) {
    testResult.value = error.message
  }
}

const handleReindex = async () => {
  reindexing.value = true
  reindexResult.value = ''
  try {
    const response = await reindexKnowledge()
    const data = response.data
    reindexResult.value = `索引完成：成功 ${data.successCount}，失败 ${data.failureCount}，跳过 ${data.skippedCount}`
    await loadVectorStatus()
  } catch (error) {
    reindexResult.value = error.message
  } finally {
    reindexing.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadData(), loadVectorStatus()])
})
</script>
