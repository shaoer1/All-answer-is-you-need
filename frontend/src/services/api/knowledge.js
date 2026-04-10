import { apiClient } from './client'

export async function uploadKnowledge({ username, kbId, file }) {
  const formData = new FormData()
  formData.append('username', username)
  formData.append('kbId', kbId)
  formData.append('file', file)
  const { data } = await apiClient.post('/api/knowledge/upload', formData)
  return data
}
