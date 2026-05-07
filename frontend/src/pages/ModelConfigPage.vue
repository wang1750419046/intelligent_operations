<template>
  <div class="config-workspace">
    <section class="page-hero compact-hero">
      <div>
        <p class="eyebrow">Model Gateway</p>
        <h2>集中管理聊天模型与向量模型接入</h2>
      </div>
      <button class="secondary" @click="resetForm">新建配置</button>
    </section>

    <section class="panel vector-status-panel">
      <div class="panel-header">
        <div>
          <p class="section-kicker">Vector Index</p>
          <h3>知识库向量索引</h3>
          <p>{{ vectorStatus.message || '等待状态检查' }}</p>
        </div>
        <div class="inline-actions">
          <button class="secondary" @click="loadVectorStatus">刷新状态</button>
          <button :disabled="reindexing" @click="handleReindex">
            {{ reindexing ? '重建中...' : '重建索引' }}
          </button>
        </div>
      </div>
      <div class="status-grid">
        <div>
          <span>Qdrant</span>
          <strong>{{ vectorStatus.qdrantReachable ? '已连接' : '未就绪' }}</strong>
        </div>
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
      <div v-if="reindexResult" class="result-box">{{ reindexResult }}</div>
    </section>

    <div class="segmented-tabs">
      <button :class="{ active: activeType === 'CHAT' }" @click="switchType('CHAT')">聊天模型</button>
      <button :class="{ active: activeType === 'EMBEDDING' }" @click="switchType('EMBEDDING')">向量模型</button>
    </div>

    <div class="dual-grid">
      <section class="panel config-list-pane">
        <div class="panel-header">
          <div>
            <p class="section-kicker">Providers</p>
            <h3>{{ activeType === 'CHAT' ? '聊天模型列表' : '向量模型列表' }}</h3>
          </div>
        </div>

        <div class="model-list">
          <button
            v-for="item in modelConfigs"
            :key="item.id"
            class="session-card model-card"
            :class="{ active: form.id === item.id }"
            @click="editItem(item)"
          >
            <strong>{{ item.name }}</strong>
            <span>{{ item.provider }} / {{ item.modelName }}</span>
            <span>{{ item.defaultConfig ? '默认配置' : item.updatedAt }}</span>
          </button>
        </div>
      </section>

      <section class="panel config-form-pane">
        <div class="panel-header">
          <div>
            <p class="section-kicker">Connection</p>
            <h3>{{ form.id ? '编辑配置' : '新建配置' }}</h3>
          </div>
        </div>

        <div class="form-stack">
          <label>
            <span>配置名称</span>
            <input v-model="form.name" placeholder="例如：生产 Qwen Plus" />
          </label>
          <label>
            <span>模型服务商</span>
            <select v-model="form.provider" :disabled="activeType === 'EMBEDDING'" @change="applyPreset">
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
            <input v-model="form.modelName" :disabled="activeType === 'EMBEDDING'" placeholder="模型名称" />
          </label>
          <label>
            <span>API Key</span>
            <input v-model="form.apiKey" type="password" placeholder="留空则保留已有 API Key" />
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
              <span>设为默认{{ activeType === 'CHAT' ? '聊天' : '向量' }}模型</span>
            </label>
          </div>

          <div class="inline-actions">
            <button @click="save">保存</button>
            <button class="secondary" :disabled="!form.id" @click="test">
              {{ activeType === 'CHAT' ? '测试连接' : '测试向量模型' }}
            </button>
            <button class="danger" :disabled="!form.id" @click="remove">删除</button>
          </div>
          <div v-if="testResult" class="result-box">{{ testResult }}</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
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

const activeType = ref('CHAT')
const modelConfigs = ref([])
const testResult = ref('')
const reindexResult = ref('')
const reindexing = ref(false)
const vectorStatus = ref({})

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
  } catch (error) {
    testResult.value = error.message
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
    temperature: item.temperature,
    maxTokens: item.maxTokens,
    enabled: item.enabled,
    defaultConfig: item.defaultConfig,
  })
  testResult.value = item.hasApiKey ? '当前配置已保存 API Key' : '当前配置尚未保存 API Key'
}

const save = async () => {
  const payload = {
    name: form.name,
    configType: activeType.value,
    provider: activeType.value === 'EMBEDDING' ? embeddingPreset.provider : form.provider,
    baseUrl: form.baseUrl,
    modelName: activeType.value === 'EMBEDDING' ? embeddingPreset.modelName : form.modelName,
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
