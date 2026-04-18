import http from './http'

export const getTrace = (traceId) => http.get(`/trace/${traceId}`)
