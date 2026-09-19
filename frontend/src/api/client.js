import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000
})

apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('srs_token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('srs_token')
      localStorage.removeItem('srs_user')
      window.dispatchEvent(new Event('auth-expired'))
    }
    return Promise.reject(error)
  }
)

export default apiClient
