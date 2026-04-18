import http from './http'

export const createSession = (payload) => http.post('/session/create', payload)
export const listSessions = () => http.get('/session/list')
export const getSessionDetail = (id) => http.get(`/session/${id}`)
export const deleteSession = (id) => http.delete(`/session/${id}`)
