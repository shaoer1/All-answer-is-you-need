import { apiClient } from './client'

export async function listKnowledgeBases(userId) {
  const { data } = await apiClient.get('/api/knowledge-base/list', { params: { userId } })
  return data
}

export async function createKnowledgeBase({ userId, name, description }) {
  const { data } = await apiClient.post('/api/knowledge-base/create', { userId, name, description })
  return data
}

export async function deleteKnowledgeBase(id) {
  await apiClient.post('/api/knowledge-base/delete', null, { params: { id } })
}

export async function updateKnowledgeBase({ id, name, description }) {
  const { data } = await apiClient.post('/api/knowledge-base/update', { id, name, description })
  return data
}

export async function getKnowledgeBase(id) {
  const { data } = await apiClient.get('/api/knowledge-base/get', { params: { id } })
  return data
}