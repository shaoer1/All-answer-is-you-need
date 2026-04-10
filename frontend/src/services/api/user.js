import { apiClient } from './client'

export async function initUser(username) {
  const { data } = await apiClient.post('/api/user/init', { username })
  return data
}

export async function registerUser({ username, password, nickname }) {
  const { data } = await apiClient.post('/api/user/register', { username, password, nickname })
  return data
}

export async function loginUser({ username, password }) {
  const { data } = await apiClient.post('/api/user/login', { username, password })
  return data
}

export async function getUserId(username) {
  const { data } = await apiClient.get('/api/user/id', { params: { username } })
  return data
}