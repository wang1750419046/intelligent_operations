import http from './http'

export const getVectorStatus = () => http.get('/kb/vector/status')
export const reindexKnowledge = () => http.post('/kb/reindex')
