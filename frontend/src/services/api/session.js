import { apiClient } from './client'

export async function createSession({ username, kbId, sessionName }) {
  const { data } = await apiClient.post('/api/session/create', { username, kbId: Number(kbId), sessionName })
  return data
}

export async function listSessions(username) {
  const { data } = await apiClient.get('/api/session/list', { params: { username } })
  return data
}

export async function deleteSession({ username, sessionId }) {
  await apiClient.delete('/api/session/delete', { params: { username, sessionId } })
}

export async function updateSessionName({ username, sessionId, name }) {
  await apiClient.post('/api/session/update-name', null, { params: { username, sessionId, name } })
}

export async function listBubbleStates({ username, sessionId }) {
  const { data } = await apiClient.get('/api/session/bubbles', { params: { username, sessionId } })
  return data
}

export async function saveBubbleStates({ username, sessionId, states }) {
  await apiClient.post('/api/session/bubbles/save', { username, sessionId, states })
}