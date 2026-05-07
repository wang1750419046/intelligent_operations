import http from './http'

export const listModelConfigs = (configType) => http.get('/model-configs', { params: configType ? { configType } : {} })
export const getModelConfig = (id) => http.get(`/model-configs/${id}`)
export const createModelConfig = (payload) => http.post('/model-configs', payload)
export const updateModelConfig = (id, payload) => http.put(`/model-configs/${id}`, payload)
export const deleteModelConfig = (id) => http.delete(`/model-configs/${id}`)
export const testModelConfig = (id) => http.post(`/model-configs/${id}/test`)
