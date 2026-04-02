import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const api = axios.create({ baseURL })

export async function initUser(username) {
  const { data } = await api.post('/api/user/init', { username })
  return data
}

export async function createSession({ username, kbId, sessionName }) {
  const { data } = await api.post('/api/session/create', { username, kbId, sessionName })
  return data
}

export async function listSessions(username) {
  const { data } = await api.get('/api/session/list', { params: { username } })
  return data
}

export async function deleteSession({ username, sessionId }) {
  await api.delete('/api/session/delete', { params: { username, sessionId } })
}

export async function listMessages({ username, sessionId, limit = 50 }) {
  const { data } = await api.get('/api/message/list', { params: { username, sessionId, limit } })
  return data
}

export async function uploadKnowledge({ username, kbId, file }) {
  const formData = new FormData()
  formData.append('username', username)
  formData.append('kbId', kbId)
  formData.append('file', file)
  const { data } = await api.post('/api/knowledge/upload', formData)
  return data
}

export async function streamChat({ username, kbId, sessionId, question, onToken, onDone }) {
  const resp = await fetch(baseURL + '/api/chat/stream', {
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
      if (event === 'token') onToken(data)
      if (event === 'done') onDone()
    }
  }
}
