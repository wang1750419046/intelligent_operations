<template>
  <div class="config-workspace">
    <section class="page-hero compact-hero">
      <div>
        <p class="eyebrow">Model Gateway</p>
        <h2>集中管理 OpenAI-compatible 模型接入</h2>
      </div>
      <button class="secondary" @click="resetForm">新建配置</button>
    </section>

    <div class="dual-grid">
      <section class="panel config-list-pane">
        <div class="panel-header">
          <div>
            <p class="section-kicker">Providers</p>
            <h3>模型列表</h3>
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
            <span>{{ item.defaultConfig ? '默认模型' : item.updatedAt }}</span>
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
            <select v-model="form.provider" @change="applyPreset">
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
            <input v-model="form.apiKey" placeholder="留空则沿用旧值或无法调用" />
          </label>

          <div class="split-fields">
            <label>
              <span>Temperature</span>
              <input v-model.number="form.temperature" type="number" min="0" max="2" step="0.1" placeholder="temperature" />
            </label>
            <label>
              <span>Max Tokens</span>
              <input v-model.number="form.maxTokens" type="number" min="1" step="1" placeholder="max tokens" />
            </label>
          </div>

          <div class="toggle-row">
            <label class="checkbox-line">
              <input v-model="form.enabled" type="checkbox" />
              <span>启用</span>
            </label>
            <label class="checkbox-line">
              <input v-model="form.defaultConfig" type="checkbox" />
              <span>设为默认模型</span>
            </label>
          </div>

          <div class="inline-actions">
            <button @click="save">保存</button>
            <button class="secondary" :disabled="!form.id" @click="test">测试连接</button>
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

const presets = {
  OPENAI: { baseUrl: 'https://api.openai.com/v1', modelName: 'gpt-4.1-mini' },
  QWEN: { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', modelName: 'qwen-plus' },
  MINIMAX: { baseUrl: 'https://api.minimaxi.com/v1', modelName: 'MiniMax-M2.5' },
}

const emptyForm = () => ({
  id: null,
  name: '',
  provider: 'OPENAI',
  baseUrl: presets.OPENAI.baseUrl,
  modelName: presets.OPENAI.modelName,
  apiKey: '',
  temperature: 0.2,
  maxTokens: 4096,
  enabled: true,
  defaultConfig: false,
})

const form = reactive(emptyForm())
const modelConfigs = ref([])
const testResult = ref('')

const loadData = async () => {
  try {
    const response = await listModelConfigs()
    modelConfigs.value = response.data || []
  } catch (error) {
    testResult.value = error.message
  }
}

const resetForm = () => {
  Object.assign(form, emptyForm())
  testResult.value = ''
}

const applyPreset = () => {
  const preset = presets[form.provider]
  form.baseUrl = preset.baseUrl
  form.modelName = preset.modelName
}

const editItem = (item) => {
  Object.assign(form, {
    id: item.id,
    name: item.name,
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
    provider: form.provider,
    baseUrl: form.baseUrl,
    modelName: form.modelName,
    temperature: form.temperature,
    maxTokens: form.maxTokens,
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
    resetForm()
  } catch (error) {
    testResult.value = error.message
  }
}

onMounted(loadData)
</script>
