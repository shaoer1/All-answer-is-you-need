import axios from 'axios'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8080'
})

export const apiBaseUrl = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
