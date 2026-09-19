import apiClient from './client'

export async function login(payload) {
  const r = await apiClient.post('/auth/login', payload)
  return r.data
}

export async function register(payload) {
  const r = await apiClient.post('/auth/register', payload)
  return r.data
}

export async function logout() {
  try {
    await apiClient.post('/auth/logout')
  } finally {
    localStorage.removeItem('srs_token')
    localStorage.removeItem('srs_user')
  }
}

export async function updatePreference(aiPreference, analysisLanguage) {
  const r = await apiClient.put('/auth/preference', { aiPreference, analysisLanguage })
  return r.data
}
