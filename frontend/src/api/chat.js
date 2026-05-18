import http from './http'

export const sendChat = (payload) => http.post('/chat/send', payload)

const parseSseEvent = (rawEvent) => {
  const event = { name: 'message', data: '' }
  rawEvent.split(/\r?\n/).forEach((line) => {
    if (line.startsWith('event:')) {
      event.name = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      event.data += line.slice(5).trimStart()
    }
  })
  return event
}

const dispatchSseEvent = (rawEvent, handlers) => {
  if (!rawEvent.trim()) return
  const event = parseSseEvent(rawEvent)
  if (!event.data) return

  const payload = JSON.parse(event.data)
  const type = payload.type || event.name
  if (type === 'trace') {
    handlers.onTrace?.(payload.traceId)
  } else if (type === 'status') {
    handlers.onStatus?.(payload)
  } else if (type === 'delta') {
    handlers.onDelta?.(payload.token || '')
  } else if (type === 'done') {
    handlers.onDone?.(payload)
  } else if (type === 'error') {
    const error = new Error(payload.message || 'Stream failed')
    error.code = payload.code
    error.traceId = payload.traceId
    handlers.onError?.(error)
    throw error
  }
}

export const streamChat = async (payload, handlers = {}) => {
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message || response.statusText || 'Request failed')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''
    for (const event of events) {
      dispatchSseEvent(event, handlers)
    }
  }

  buffer += decoder.decode()
  dispatchSseEvent(buffer, handlers)
}
