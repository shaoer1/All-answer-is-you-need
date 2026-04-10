import { apiClient } from './client'

export async function listMessages({ username, sessionId, limit = 50 }) {
  const { data } = await apiClient.get('/api/message/list', { params: { username, sessionId, limit } })
  return data
}
