import http from './http'

export const getVectorStatus = () => http.get('/kb/vector/status')
export const reindexKnowledge = () => http.post('/kb/reindex')
export const listKnowledgeDocuments = (params = {}) => http.get('/kb/documents', { params })
export const createKnowledgeDocument = (payload) => http.post('/kb/documents', payload)
export const updateKnowledgeDocument = (docId, payload) => http.put(`/kb/documents/${docId}`, payload)
export const deleteKnowledgeDocument = (docId) => http.delete(`/kb/documents/${docId}`)
export const searchKnowledge = (payload) => http.post('/kb/search', payload)
export const importKnowledgeDocument = (payload) => http.post('/kb/documents/import', payload, {
  headers: { 'Content-Type': 'multipart/form-data' },
  timeout: 120000,
})
