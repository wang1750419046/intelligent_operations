<template>
  <div class="knowledge-workspace">
    <section class="workspace-topbar">
      <div class="title-stack">
        <p class="section-kicker">Knowledge Base</p>
        <h2>知识库管理</h2>
        <p>新增、导入和更新知识后会自动语义切分，并同步写入向量索引。</p>
      </div>
      <div class="inline-actions">
        <button class="secondary" @click="openImportModal">导入文档</button>
        <button @click="openCreateModal">新增知识</button>
      </div>
    </section>

    <section class="panel knowledge-filter-bar">
      <div class="filter-grid">
        <input v-model="filters.keyword" placeholder="标题、正文或标签" />
        <input v-model="filters.country" placeholder="国家" />
        <input v-model="filters.businessLine" placeholder="业务线" />
        <input v-model="filters.systemName" placeholder="系统" />
        <input v-model="filters.permissionCodes" placeholder="权限码，逗号分隔" />
        <button class="secondary" @click="loadDocuments">筛选</button>
      </div>
    </section>

    <div class="knowledge-layout">
      <section class="panel knowledge-list-pane">
        <div class="panel-header">
          <div>
            <p class="section-kicker">Documents</p>
            <h3>文档列表</h3>
          </div>
        </div>

        <div class="document-list">
          <button
            v-for="item in documents"
            :key="item.docId"
            class="session-card knowledge-card"
            :class="{ active: form.docId === item.docId }"
            @click="editDocument(item)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.source }} / {{ item.chunkCount || 0 }} chunks / {{ item.indexStatus }}</span>
            <span>{{ [item.country, item.businessLine, item.systemName].filter(Boolean).join(' / ') || '未设置 metadata' }}</span>
          </button>
          <div v-if="!documents.length" class="compact-empty">暂无知识文档</div>
        </div>
      </section>
    </div>

    <div v-if="activeModal" class="modal-backdrop" @click.self="closeModal">
      <section v-if="activeModal === 'editor'" class="modal-panel knowledge-modal">
        <div class="modal-header">
          <div>
            <p class="section-kicker">Editor</p>
            <h3>{{ form.docId ? '编辑知识' : '新增知识' }}</h3>
          </div>
          <button class="icon-button ghost-button" title="关闭" aria-label="关闭" @click="closeModal">×</button>
        </div>

        <div class="form-stack modal-body">
          <label>
            <span>标题</span>
            <input v-model="form.title" placeholder="例如：支付超时排查手册" />
          </label>
          <div class="split-fields">
            <label>
              <span>来源</span>
              <input v-model="form.source" placeholder="wiki / runbook / upload" />
            </label>
            <label>
              <span>标签</span>
              <input v-model="form.tags" placeholder="timeout,payment,runbook" />
            </label>
          </div>
          <div class="split-fields">
            <label>
              <span>国家</span>
              <input v-model="form.country" placeholder="CN" />
            </label>
            <label>
              <span>业务线</span>
              <input v-model="form.businessLine" placeholder="支付" />
            </label>
          </div>
          <div class="split-fields">
            <label>
              <span>系统</span>
              <input v-model="form.systemName" placeholder="order-service" />
            </label>
            <label>
              <span>事件时间</span>
              <input v-model="form.eventTime" type="datetime-local" />
            </label>
          </div>
          <label>
            <span>权限码</span>
            <input v-model="form.permissionCodes" placeholder="PUBLIC,OPS_CN" />
          </label>
          <label>
            <span>正文</span>
            <textarea v-model="form.content" rows="12" placeholder="粘贴知识正文、复盘、操作手册或 FAQ..." />
          </label>
          <div v-if="message" class="result-box">{{ message }}</div>
        </div>

        <div class="modal-actions">
          <button class="danger" :disabled="saving || !form.docId" @click="removeDocument">删除</button>
          <span></span>
          <button class="secondary" @click="resetForm">清空</button>
          <button :disabled="saving || !canSave" @click="saveDocument">
            {{ saving ? '保存并索引中...' : '保存并同步索引' }}
          </button>
        </div>
      </section>

      <section v-else-if="activeModal === 'import'" class="modal-panel import-modal">
        <div class="modal-header">
          <div>
            <p class="section-kicker">Import</p>
            <h3>外部文档导入</h3>
          </div>
          <button class="icon-button ghost-button" title="关闭" aria-label="关闭" @click="closeModal">×</button>
        </div>

        <div class="form-stack modal-body">
          <input ref="fileInput" type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.md,.csv,.json" />
          <div class="split-fields">
            <input v-model="importForm.title" placeholder="标题，留空则使用文件名" />
            <input v-model="importForm.source" placeholder="来源，默认 upload" />
          </div>
          <div class="split-fields">
            <input v-model="importForm.country" placeholder="国家" />
            <input v-model="importForm.businessLine" placeholder="业务线" />
          </div>
          <div class="split-fields">
            <input v-model="importForm.systemName" placeholder="系统" />
            <input v-model="importForm.permissionCodes" placeholder="权限码，默认 PUBLIC" />
          </div>
          <div v-if="importMessage" class="result-box">{{ importMessage }}</div>
        </div>

        <div class="modal-actions">
          <span></span>
          <button class="secondary" @click="closeModal">取消</button>
          <button :disabled="importing" @click="importDocument">
            {{ importing ? '导入并索引中...' : '导入文件' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createKnowledgeDocument,
  deleteKnowledgeDocument,
  importKnowledgeDocument,
  listKnowledgeDocuments,
  updateKnowledgeDocument,
} from '../api/knowledge'

const documents = ref([])
const loading = ref(false)
const saving = ref(false)
const importing = ref(false)
const message = ref('')
const importMessage = ref('')
const fileInput = ref(null)
const activeModal = ref('')

const filters = reactive({
  keyword: '',
  country: '',
  businessLine: '',
  systemName: '',
  permissionCodes: '',
})

const emptyForm = () => ({
  docId: '',
  title: '',
  content: '',
  source: '',
  tags: '',
  country: '',
  businessLine: '',
  systemName: '',
  eventTime: '',
  permissionCodes: '',
})

const form = reactive(emptyForm())

const importForm = reactive({
  title: '',
  source: 'upload',
  country: '',
  businessLine: '',
  systemName: '',
  permissionCodes: 'PUBLIC',
})

const canSave = computed(() => form.title.trim() && form.content.trim() && form.source.trim())

const splitCodes = (value) => (value || 'PUBLIC')
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

const normalizeDateTime = (value) => value ? value.replace('T', ' ') + (value.length === 16 ? ':00' : '') : ''

const loadDocuments = async () => {
  loading.value = true
  try {
    const response = await listKnowledgeDocuments({
      keyword: filters.keyword || undefined,
      country: filters.country || undefined,
      businessLine: filters.businessLine || undefined,
      systemName: filters.systemName || undefined,
      permissionCodes: filters.permissionCodes || undefined,
    })
    documents.value = response.data || []
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, emptyForm())
  message.value = ''
}

const openCreateModal = () => {
  resetForm()
  activeModal.value = 'editor'
}

const openImportModal = () => {
  importMessage.value = ''
  activeModal.value = 'import'
}

const closeModal = () => {
  activeModal.value = ''
}

const editDocument = (item) => {
  Object.assign(form, {
    docId: item.docId,
    title: item.title || '',
    content: item.content || '',
    source: item.source || '',
    tags: item.tags || '',
    country: item.country || '',
    businessLine: item.businessLine || '',
    systemName: item.systemName || '',
    eventTime: item.eventTime ? item.eventTime.replace(' ', 'T').slice(0, 16) : '',
    permissionCodes: item.permissionCodes || '',
  })
  message.value = ''
  activeModal.value = 'editor'
}

const toPayload = () => ({
  title: form.title.trim(),
  content: form.content.trim(),
  source: form.source.trim(),
  tags: form.tags.trim(),
  country: form.country.trim(),
  businessLine: form.businessLine.trim(),
  systemName: form.systemName.trim(),
  eventTime: normalizeDateTime(form.eventTime),
  permissionCodes: splitCodes(form.permissionCodes),
})

const saveDocument = async () => {
  if (!canSave.value) return
  saving.value = true
  message.value = ''
  try {
    const response = form.docId
      ? await updateKnowledgeDocument(form.docId, toPayload())
      : await createKnowledgeDocument(toPayload())
    message.value = `已同步索引：${response.data.chunkCount || 0} chunks，状态 ${response.data.indexStatus}`
    await loadDocuments()
    editDocument(response.data)
  } catch (error) {
    message.value = error.message
  } finally {
    saving.value = false
  }
}

const removeDocument = async () => {
  if (!form.docId) return
  saving.value = true
  try {
    await deleteKnowledgeDocument(form.docId)
    resetForm()
    await loadDocuments()
  } catch (error) {
    message.value = error.message
  } finally {
    saving.value = false
  }
}

const importDocument = async () => {
  const file = fileInput.value?.files?.[0]
  if (!file) {
    importMessage.value = '请选择文件'
    return
  }
  importing.value = true
  importMessage.value = ''
  try {
    const payload = new FormData()
    payload.append('file', file)
    Object.entries(importForm).forEach(([key, value]) => {
      if (key === 'permissionCodes') {
        splitCodes(value).forEach((code) => payload.append('permissionCodes', code))
      } else if (value) {
        payload.append(key, value)
      }
    })
    const response = await importKnowledgeDocument(payload)
    importMessage.value = `导入完成：${response.data.extractedCharacters} 字，${response.data.chunkCount} chunks，状态 ${response.data.indexStatus}`
    fileInput.value.value = ''
    await loadDocuments()
  } catch (error) {
    importMessage.value = error.message
  } finally {
    importing.value = false
  }
}

onMounted(loadDocuments)
</script>
