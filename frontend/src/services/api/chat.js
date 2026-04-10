import { apiBaseUrl } from './client'

export async function streamChat({ username, kbId, sessionId, question, onToken, onDone, onTrace, onError }) {
  const resp = await fetch(apiBaseUrl + '/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, kbId, sessionId, question })
  })

  if (!resp.ok || !resp.body) {
    throw new Error('流式请求失败')
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    const events = buffer.split('\n\n')
    buffer = events.pop() || ''

    for (const eventBlock of events) {
      const lines = eventBlock.split('\n')
      const event = lines.find((l) => l.startsWith('event:'))?.replace('event:', '').trim()
      const data = lines.find((l) => l.startsWith('data:'))?.replace('data:', '').trim() || ''
      if (event === 'token') onToken?.(data)
      if (event === 'trace') onTrace?.(data)
      if (event === 'error') onError?.(data)
      if (event === 'done') onDone?.()
    }
  }
}
