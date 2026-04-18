import http from './http'

export const sendChat = (payload) => http.post('/chat/send', payload)
